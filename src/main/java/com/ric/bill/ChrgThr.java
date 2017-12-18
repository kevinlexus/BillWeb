package com.ric.bill;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ric.bill.dto.ChrgRec;
import com.ric.bill.dto.ChrgRecDet;
import com.ric.bill.excp.EmptyOrg;
import com.ric.bill.excp.EmptyStorable;
import com.ric.bill.excp.InvalidServ;
import com.ric.bill.mm.KartMng;
import com.ric.bill.mm.LstMng;
import com.ric.bill.mm.MeterLogMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.mm.TarifMng;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.bs.Lst;
import com.ric.bill.model.bs.Org;
import com.ric.bill.model.fn.Chng;
import com.ric.bill.model.fn.ChngLsk;
import com.ric.bill.model.fn.Chrg;
import com.ric.bill.model.fn.PrivilegeServ;
import com.ric.bill.model.ps.Pers;
import com.ric.bill.model.tr.Serv;

import lombok.extern.slf4j.Slf4j;

/**
 * РАСЧЕТ НАЧИСЛЕНИЯ ПО УСЛУГЕ, ВЫЗЫВАЕТСЯ В ПОТОКЕ
 * @author lev
 *
 */

@Component
@Scope("prototype") //собственный бин для каждого потока по услуге
@Slf4j
public class ChrgThr {
	
	@Autowired
	private LstMng lstMng;
	@Autowired
	private ParMng parMng;
	@Autowired
	private TarifMng tarMng;
	
	@Autowired
	private KartMng kartMng;
	@Autowired
	private MeterLogMng metMng;

	@PersistenceContext
    private EntityManager em;

	// основная услуга
    private Serv serv;
	//временное хранилище записей
	private ChrgStore chStore;

	private String thrName;
	
    //вспомогательные коллекции
    private List<Chrg> prepChrg;
    // для расчета услуг типа ГВС(одн) п.344 РФ
    private List<ChrgMainServRec> prepChrgMainServ;
    
    private HashMap<Serv, BigDecimal> mapServ;
    private HashMap<Serv, BigDecimal> mapVrt;

    private Calc calc;
    
    // Результат исполнения
    Result res;
    
    //конструктор
	public ChrgThr() {
		super();
		this.prepChrgMainServ = new ArrayList<ChrgMainServRec>(100);
	}
	
	/**
	 * 
	 * @param calc 	   - объект calc
	 * @param serv 	   - услуга
	 * @param mapServ  - коллекция для округления
	 * @param mapVrt   - коллекция для округления
	 * @param prepChrg - коллекция для сгруппированных записей начисления
	 */
	public void setUp(Calc calc, Serv serv,  
			HashMap<Serv, BigDecimal> mapServ, 
			HashMap<Serv, BigDecimal> mapVrt, 
			List<Chrg> prepChrg) {
		this.calc = calc;
		this.serv = serv;
		this.mapServ = mapServ;
		this.mapVrt = mapVrt;
		this.prepChrg = prepChrg;
	}

	public Result run1() throws EmptyStorable {
		Kart kart = calc.getKart();
		// перерасчет
		Chng chng = calc.getReqConfig().getChng();
		res = new Result();
		res.setErr(0);

		Thread t = Thread.currentThread();
	    thrName = t.getName();
	      
		//необходимый для формирования диапазон дат
		Date dt1, dt2, genDt;
		dt1 = calc.getReqConfig().getCurDt1();
		dt2 = calc.getReqConfig().getCurDt2();

		// номер текущего запроса
		int rqn = calc.getReqConfig().getRqn();
		
		//типы записей начисления
		Lst chrgTpRnd = lstMng.getByCD("Начислено свернуто, округлено");
		
		//Объект, временно хранящий записи начисления
		chStore = new ChrgStore(); 
		log.trace("ChrThr.run1: "+thrName+", Услуга:"+serv.getCd()+" Id="+serv.getId());
		if (serv.getId()==20) {
			log.trace("ChrThr.run1: "+thrName+", Услуга:"+serv.getCd()+" Id="+serv.getId());
		}
		
		//}
		Calendar c = Calendar.getInstance();
		
		// РАСЧЕТ по дням
		for (c.setTime(dt1); !c.getTime().after(dt2); c.add(Calendar.DATE, 1)) {
			genDt = c.getTime();
			// только там, где нет статуса "не начислять" за данный день
			try {
				if (Utl.nvl(parMng.getDbl(rqn, kart, "IS_NOT_CHARGE", genDt, chng), 0d) == 1d) {
					continue;
				}
			} catch (EmptyStorable e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
			// только там, где лиц.счет существует в данном дне и существует услуга
			if (Utl.between(genDt, kart.getDt1(), kart.getDt2()) &&  kartMng.getServ(rqn, calc, serv, genDt)) {
				String tpOwn = null;
				tpOwn = parMng.getStr(rqn, kart, "FORM_S", genDt);
				
				if (tpOwn == null) {
					res.addErr(rqn, 4, kart, serv);
					//log.info("ОШИБКА! Не указанна форма собственности! lsk="+kart.getLsk(), 2);
				}
				// где лиц.счет является нежилым помещением, не начислять за данный день - ред.28.09.17 отключил - решили занулить таким расценки, чтобы оставалась площадь
				//и гигакаллории по нежилым
				/*if (tpOwn != null && (tpOwn.equals("Нежилое собственное") || tpOwn.equals("Нежилое муниципальное")
					|| tpOwn.equals("Аренда некоммерч.") || tpOwn.equals("Для внутр. пользования"))) {
					continue;
				}*/
					try {
					  // Расчет начисления по каждой услуге и дню
					  genChrg(calc, serv, tpOwn, genDt);
					} catch (EmptyStorable e) {
						e.printStackTrace();
						throw new RuntimeException();
					} catch (EmptyOrg e) {
						e.printStackTrace();
						throw new RuntimeException();
					} catch (InvalidServ e) {
						e.printStackTrace();
						throw new RuntimeException();
					}
					
			}
			//break;
		}
		// Сохранить сгруппированные по основной услуге суммы начислений
		// для последующего использования в таких услугах как ГВС(одн) п.344 РФ
		chrgMainServAppend(chStore.getStoreMainServ());
		
/*		chStore.getStoreRecDet().stream().filter(f-> f.getPers() != null).forEach(f-> {
			log.info("persId={}, price={}, pricePriv={}, discount={}, vol={}, dt1={}, dt2={}", f.getPers().getId(), f.getPrice(), 
					f.getPricePriv(), f.getDiscount(), f.getVol(), f.getDt1(), f.getDt2());
		});*/
		
		
		// ОКОНЧАТЕЛЬНО рассчитать данные (умножить расценку на объем, округлить)
		for (ChrgRecDet rec : chStore.getStoreRecDet()) {
			//log.info("услуга={}", rec.getServ().getCd());
			BigDecimal vol, area;
			vol = rec.getVol();
			// округлить объем
			vol = vol.setScale(5, BigDecimal.ROUND_HALF_UP);

			area = rec.getArea();
			// округлить площадь до 5 знаков (понадобилось, так как садим сюда еще и дополнительный объем, для услуг ОИ)
			if (area != null) {
				area = area.setScale(5, BigDecimal.ROUND_HALF_UP);
			}
			
			// Полная сумма
			// умножить на расценку
			BigDecimal sumFull = vol.multiply(rec.getPrice());
			// округлить до копеек
			sumFull = sumFull.setScale(2, BigDecimal.ROUND_HALF_UP);

			BigDecimal sumPriv = BigDecimal.ZERO; 
			if (rec.getDiscount() != null) {
//				log.info("discount={}", rec.getDiscount());
				BigDecimal cf = BigDecimal.valueOf(1).subtract(rec.getDiscount());
//				log.info("cf={}", cf);
				// сумма льготы (объем * (1-дисконт) = сумма льг.)
				sumPriv = vol.multiply(cf);
				sumPriv = sumPriv.setScale(2, BigDecimal.ROUND_HALF_UP);
			}
			// Сумма итога
			BigDecimal sumAmnt = sumFull.subtract(sumPriv);
			
			// записать, для будущего округления по виртуальной услуге
			if (rec.getServ().getServVrt() != null) {
				putMapServVal(rec.getServ().getServVrt(), sumFull);
			}
			// записать, сумму по виртуальной услуге
			if (rec.getServ().getVrt()) {
					putMapVrtVal(rec.getServ(), sumFull);
			}

			if (!rec.getServ().getVrt()) {
				if (sumFull.compareTo(BigDecimal.ZERO) != 0 || 
						sumAmnt.compareTo(BigDecimal.ZERO) != 0 || sumPriv.compareTo(BigDecimal.ZERO) != 0 ||
						Utl.nvl(parMng.getDbl(rqn, rec.getServ(), "Сохранять_CHRG.AREA_CHRG.CNT_PERS"), 0d) == 1) {  
					/*sum - сумма
					vol - объем
					price - расценка
					pricePriv - расценка по льготе (null по нельготной услуге)
					stdt - норматив
					cntFact - кол-во прожив по факту (без собственников)
					serv - услуга
					org - организация
					met - наличие счетчика
					entry - номер ввода
					dt1 - дата начала
					dt2 - дата окончания
					cntOwn - кол-во собственников*/
					log.info("контроль: serv={}, org={}, pers={}, priv={}, cntFact={}, cntOwn={}, entry={}, met={}, price={}, pricePriv={}, stdt={}, sumAmnt={}, "
							+ "sumFull={}, sumPriv={}, vol={}, dt1={}, dt2={}", 
							rec.getServ().getCd(), rec.getOrg().getId(), rec.getPers()!=null?rec.getPers().getId(): null, rec.getPriv()!=null?rec.getPriv().getId(): null,  
							rec.getCntFact(), rec.getCntOwn(), rec.getEntry(), rec.getMet(), rec.getPrice(), rec.getPricePriv(),
							rec.getStdt(), sumAmnt, sumFull, sumPriv, vol, Utl.getStrFromDate(rec.getDt1()), Utl.getStrFromDate(rec.getDt2())); 

					if (Utl.nvl(parMng.getDbl(rqn, rec.getServ(), "Вариант расчета по объему осн.род.усл."), 0d) == 1d) {
						// Убрать расценку и объем по данному типу услуг
						chStore.groupStoreChrgRec(sumFull, sumPriv, sumAmnt, null, rec.getServ(), rec.getOrg(), 
								rec.getDt1(), rec.getDt2(), rec.getStdt(), rec.getCntFact(), rec.getCntOwn(), rec.getArea(), 
								rec.getMet(), rec.getEntry(), null);
					} else {
						chStore.groupStoreChrgRec(sumFull, sumPriv, sumAmnt, rec.getPrice(), rec.getServ(), rec.getOrg(), 
								rec.getDt1(), rec.getDt2(), rec.getStdt(), rec.getCntFact(), rec.getCntOwn(), rec.getArea(), 
								rec.getMet(), rec.getEntry(), vol);
					}
					// Если сумма <> 0 или по услуге принудительно сохранить объемы при нулевой сумме 	
					
/*
					Chrg chrg = new Chrg();
					try {
						if (Utl.nvl(parMng.getDbl(rqn, rec.getServ(), "Вариант расчета по объему осн.род.усл."), 0d) == 1d) {
							// Убрать расценку и объем по данному типу услуг
							chrg = new Chrg(kart, rec.getServ(), rec.getOrg(), 1, calc.getReqConfig().getPeriod(), sum, sum, 
									null, null, rec.getStdt(), rec.getCntFact(), area, chrgTpRnd, 
									chng, rec.getMet(), rec.getEntry(), rec.getDt1(), rec.getDt2(), rec.getCntOwn());
						} else {
							chrg = new Chrg(kart, rec.getServ(), rec.getOrg(), 1, calc.getReqConfig().getPeriod(), sum, sum, 
									vol, rec.getPrice(), rec.getStdt(), rec.getCntFact(), area, chrgTpRnd, 
									chng, rec.getMet(), rec.getEntry(), rec.getDt1(), rec.getDt2(), rec.getCntOwn());
						}
					} catch (EmptyStorable e) {
						throw new RuntimeException();
					}
					
					chrgAppend(chrg);*/
				}
			}
		} 

		chStore.getStoreChrgRec().stream().forEach(f-> {
			log.info("запись: serv={}, org={}, cntFact={}, cntOwn={}, entry={}, met={}, price={}, stdt={}, sumAmnt={}, "
					+ "sumFull={}, sumPriv={}, vol={}, dt1={}, dt2={}", 
					f.getServ().getCd(), f.getOrg().getId(), f.getCntFact(), f.getCntOwn(), f.getEntry(), f.getMet(), f.getPrice(),
					f.getStdt(), f.getSumAmnt(), f.getSumFull(), f.getSumPriv(), f.getVol(), f.getDt1(), f.getDt2()); 
		});
		
		// Почистить коллекции
		chStore = null;
	    prepChrg = null;
	    mapServ = null;
	    mapVrt = null;

		
		//Future ar = new AsyncResult<Result>(res);
		return res;
	}

	// получить подмененную организацию по перерасчету
	private Org getChngOrg(Serv serv, Date genDt, Chng chng) {
		Org org = null; 
		if ( Utl.between(genDt, chng.getDt1(), chng.getDt2()) &&
				chng.getServ().equals(serv) ) {
			org = chng.getOrg();
		}
		return org;
	}
	
	
	/**
	 * РАСЧЕТ НАЧИСЛЕНИЯ ПО ДНЮ
	 * @param serv - услуга
	 * @throws InvalidServ 
	 */
	private void genChrg(Calc calc, Serv serv, String tpOwn, Date genDt) throws EmptyStorable, EmptyOrg, InvalidServ {

		//log.info("serv.cd={}", serv.getCd());
		if (serv.getId()==32) {
			log.trace("ChrThr.run1: "+thrName+", Услуга:"+serv.getCd()+" Id="+serv.getId());
		}

		Kart kart = calc.getKart();
		// перерасчет
		Chng chng = calc.getReqConfig().getChng();
		//Utl.logger(true, 1, kart.getLsk(), serv.getId());
		long startTime2;
		long endTime;
		long totalTime;
		startTime2 = System.currentTimeMillis();

		// номер текущего запроса
		int rqn = calc.getReqConfig().getRqn();

		// услуги по норме, свыше и без проживающих
		Serv stServ, upStServ, woKprServ;
		// нормативный объем, доля норматива
		Standart stdt = null;
		// расценки
		Double stPrice = 0d, upStPrice = 0d, woKprPrice = 0d;
		// организация
		Org org = null;
		// база для начисления
		String baseCD;
		// объем
		Double vol = 0d;
		// доля площади в день
		Double sqr = 0d;
		// № порядк.записи
		// int npp;
		// Временная сумма
		// BigDecimal tmpSum;
		// Временный объем
		Double tmpVol;
		// Кол-во проживающих
		CntPers cntPers = new CntPers();
		// Временные переменные
		// Double tmp = 0d;
		BigDecimal cf = BigDecimal.ZERO;
		BigDecimal tmpVolD = BigDecimal.ZERO;
		// Наличие счетчика в периоде
		Boolean exsMet = false;
		// Номер ввода
		Integer entry = null;
		
		// признак жилого помещения
		Boolean isResid = true;
		if (tpOwn != null && (tpOwn.equals("Нежилое собственное") || tpOwn.equals("Нежилое муниципальное")
				|| tpOwn.equals("Аренда некоммерч.") || tpOwn.equals("Для внутр. пользования"))) {
			isResid = false;
		} else {
			isResid = true;
		}
		
		//Utl.logger(false, 2, kart.getLsk(), serv.getId()); //###
		// проверить отключение услуги в данном дне (по наличию параметра, а не по его значению)
		Double switсhOff = kartMng.getServPropByCD(rqn, calc, serv, "Отключение", genDt);
		if (switсhOff != null) {
			log.trace("Услуга id={}, cd={}, genDt={} отключена!", serv.getId(), serv.getCd(), genDt);
		} else if (switсhOff == null) {
			log.trace("Расчет услуги id={}, cd={}, genDt={}", serv.getId(), serv.getCd(), genDt);
			// получить необходимые подуслуги
			stServ = serv.getServSt();
			upStServ = serv.getServUpst();
			woKprServ = serv.getServWokpr();
			// если услуга по соцнорме пустая, присвоить изначальную услугу
			if (stServ == null) {
				stServ = serv;
			}
			
			// контроль наличия услуги св.с.нормы (по ряду услуг) в справочнике услуг (не в тарифе!)
			if ((Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-1"), 0d) == 1d || 
					Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-1"), 0d) == 1d) && serv.getServUpst() == null) {
				throw new EmptyStorable("По услуге Id="+serv.getId()+" обнаружена пустая услуга свыше соц.нормы");
			}
			//Utl.logger(false, 3, kart.getLsk(), serv.getId()); //###

			// Получить кол-во проживающих 
			kartMng.getCntPers(rqn, calc, kart, serv, cntPers, genDt);
	
			//Utl.logger(false, 4, kart.getLsk(), serv.getId()); //###

			// получить расценку по норме	
			if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-3"), 0d) == 1d) {
				// по этому варианту получить расценку от услуги, хранящей расценку, умножить на норматив, округлить
				Double stVol = kartMng.getServPropByCD(rqn, calc, serv, "Норматив", genDt);
				if (stServ.getServPrice()==null) {
					// если пуста услуга по которой хранится расценка, получить из текущей услуги - по нормативу
					stPrice = kartMng.getServPropByCD(rqn, calc, stServ, "Цена", genDt);
				} else {
					// получить расценку от услуги по которой хранится расценка
					stPrice = kartMng.getServPropByCD(rqn, calc, stServ.getServPrice(), "Цена", genDt);
				}
	
				// если пуст один из параметров - занулить все, чтобы не было exception
				if (stPrice == null || stVol == null) {
					stPrice = 0d;
					stVol = 0d;
				} else {
					// округлить
					stPrice= Math.round (stPrice * stVol * 100.0) / 100.0;
				}
			} else {
				// прочие варианты
				if (stServ.getServPrice() != null) {
					// указана услуга, откуда взять расценку
					stPrice = kartMng.getServPropByCD(rqn, calc, stServ.getServPrice(), "Цена", genDt);
				} else {
					// не указана услуга, откуда взять расценку
					stPrice = kartMng.getServPropByCD(rqn, calc, stServ, "Цена", genDt);
				}
			}
	
			//Utl.logger(false, 5, kart.getLsk(), serv.getId()); //###
			
			if (stPrice == null && isResid) {
				// Добавить ошибку, что нет расценки по услуге (если это контролируется)
				res.addErr(rqn, 8, kart, serv);
				stPrice = 0d;
			}
	
			// получить составляющие перерасчета
			ChngLsk chngLsk = null;
			if (chng != null && chng.getServ().equals(serv)) {
				chngLsk = chng.getChngLsk().stream().filter(t-> t.getKart().equals(kart))
						.findFirst().orElse(null);
			}
			
			
			// получить долю соц.нормы.свыше (не объем!!!)
			if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-1"), 0d) == 1d || 
					Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-1"), 0d) == 1d ||
					Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-2"), 0d) == 1d ||
					Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета для полива"), 0d) == 1d) {
				
				
				stdt = kartMng.getStandartVol(rqn, calc, serv, cntPers, genDt, 0);
				// здесь же получить расценки по свыше соц.нормы и без проживающих 
				if (serv.getServUpst() != null) {
					if (upStServ.getServPrice() != null) {
						// указана услуга, откуда взять расценку
						upStPrice = kartMng.getServPropByCD(rqn, calc, upStServ.getServPrice(), "Цена", genDt);
					} else {
						// не указана услуга, откуда взять расценку
						upStPrice = kartMng.getServPropByCD(rqn, calc, upStServ, "Цена", genDt);
					}
						
					if (upStPrice == null) {
						upStPrice = 0d;
					}
					
					if (upStPrice == 0d && isResid) {
						// Добавить ошибку, что отсутствует расценка
						res.addErr(rqn, 5, kart, serv);
					}
					
				} else {
					upStPrice = 0d;
				}
	
				if (serv.getServWokpr() != null) {
					if (woKprServ.getServPrice() != null) {
						// указана услуга, откуда взять расценку
						woKprPrice = kartMng.getServPropByCD(rqn, calc, woKprServ.getServPrice(), "Цена", genDt);
					} else {
						// не указана услуга, откуда взять расценку
						//log.info("Check={}", woKprServ.getId());
						woKprPrice = kartMng.getServPropByCD(rqn, calc, woKprServ, "Цена", genDt);
					}
	
					if (woKprPrice == null && isResid) {
						// Добавить ошибку, что отсутствует расценка
						res.addErr(rqn, 6, kart, serv);
						// если не найдена цена с 0 проживающими, подставить цену по свыше соц.нормы, если и она не найдена, то по норме
						if (upStPrice == null || upStPrice == 0d) {
							woKprPrice = stPrice;
						} else {
							woKprPrice = upStPrice;
						}
					} else if (woKprPrice == 0d && isResid) {
						// Добавить ошибку, что отсутствует расценка
						res.addErr(rqn, 6, kart, serv);
					}
				} else {
					woKprPrice = 0d;
				} 
			}
			//Utl.logger(false, 6, kart.getLsk(), serv.getId()); //###
				
			
			  // получить организацию
			  org = kartMng.getOrg(rqn, calc, serv.getServOrg(), genDt);
	  		  if (serv.getCheckOrg()) {
				  if (org == null) {
				    throw new EmptyOrg("При расчете л.с.="+kart.getLsk()+" , обнаружена пустая организция по услуге Id="+serv.getServOrg().getId());
				  } else {
					  log.trace("Организация по услуге: org.id={}", org.getId());
				  }
			}
			
	  		//Utl.logger(false, 7, kart.getLsk(), serv.getId()); //###
	
			// в случае перерасчета по расценке или по организации, выполнить их замену 
			if (calc.getReqConfig().getOperTp()==1 && chng.getTp().getCd().equals("Изменение расценки (тарифа)") ) {
				
				// организация
				Org chngOrg = getChngOrg(serv.getServOrg(), genDt, chng);
				if (chngOrg != null) {
					org = chngOrg; 
				}
	
				// расценка по норме
				Double chngPrice;
				if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-3"), 0d) == 1d) {
					// по этому варианту получить расценку от услуги, хранящей расценку, умножить на норматив, округлить
					Double stVol = kartMng.getServPropByCD(rqn, calc, serv, "Норматив", genDt);
					if (stServ.getServPrice()==null) {
						// если пуста услуга по которой хранится расценка, получить из текущей услуги - по нормативу
						chngPrice = tarMng.getChngVal(calc, stServ, genDt, "Изменение расценки (тарифа)", 1);
						if (chngPrice == null) {
							// если не найдена расценка в перерасчете, поставить из тарифа 
							chngPrice = kartMng.getServPropByCD(rqn, calc, stServ, "Цена", genDt);
						}
					} else {
						// получить расценку от услуги по которой хранится расценка
						chngPrice = tarMng.getChngVal(calc, stServ.getServPrice(), genDt, "Изменение расценки (тарифа)", 1);
						if (chngPrice == null) {
							// если не найдена расценка в перерасчете, поставить из тарифа 
							chngPrice = kartMng.getServPropByCD(rqn, calc, stServ.getServPrice(), "Цена", genDt);
						}
					}
		
					// если пуст один из параметров - занулить все, чтобы не было exception
					if (chngPrice == null || stVol == null) {
						chngPrice = 0d;
						stVol = 0d;
					} else {
						// округлить
						chngPrice= Math.round (chngPrice * stVol * 100.0) / 100.0;
					}
					
					
				} else {
					// прочие варианты (здесь точно должна быть расценка, иначе нет смысла в перерасчете)
					if (stServ.getServPrice() != null) {
						// указана услуга, откуда взять расценку
						chngPrice = tarMng.getChngVal(calc, stServ.getServPrice(), genDt, "Изменение расценки (тарифа)", 1);
					} else {
						// не указана услуга, откуда взять расценку
						chngPrice = tarMng.getChngVal(calc, stServ, genDt, "Изменение расценки (тарифа)", 1);
					}
				}
	
				if (chngPrice != null) {
					stPrice = chngPrice; 
				}
				
				//log.info("Serv.id={}, upStServ={}", serv.getId(), upStServ);
				// расценка св.нормы
				if (serv.getServUpst() != null) {
					if (upStServ.getServPrice() != null) {
						// указана услуга, откуда взять расценку
						chngPrice = tarMng.getChngVal(calc, upStServ.getServPrice(), genDt, "Изменение расценки (тарифа)", 1);
					} else {
						// не указана услуга, откуда взять расценку
						chngPrice = tarMng.getChngVal(calc, upStServ, genDt, "Изменение расценки (тарифа)", 1);
					}
					if (chngPrice != null) {
						upStPrice = chngPrice; 
					} else {
						// если не найдена расценка в перерасчете, поставить из тарифа 
						if (upStServ.getServPrice() != null) {
							// указана услуга, откуда взять расценку
							upStPrice = kartMng.getServPropByCD(rqn, calc, upStServ.getServPrice(), "Цена", genDt);
						} else {
							// не указана услуга, откуда взять расценку
							upStPrice = kartMng.getServPropByCD(rqn, calc, upStServ, "Цена", genDt);
						}
					}
				}
				
				// расценка без проживающих
				if (serv.getServWokpr() != null) {
					if (woKprServ.getServPrice() != null) {
						// указана услуга, откуда взять расценку
						chngPrice = tarMng.getChngVal(calc, woKprServ.getServPrice(), genDt, "Изменение расценки (тарифа)", 1);
					} else {
						// не указана услуга, откуда взять расценку
						chngPrice = tarMng.getChngVal(calc, woKprServ, genDt, "Изменение расценки (тарифа)", 1);
					}
					if (chngPrice != null) {
						woKprPrice = chngPrice; 
					} else {
						// если не найдена расценка в перерасчете, поставить из тарифа 
						if (woKprServ.getServPrice() != null) {
							// указана услуга, откуда взять расценку
							woKprPrice = kartMng.getServPropByCD(rqn, calc, woKprServ.getServPrice(), "Цена", genDt);
						} else {
							// не указана услуга, откуда взять расценку
							woKprPrice = kartMng.getServPropByCD(rqn, calc, woKprServ, "Цена", genDt);
						}
					}
				}
				
			}
			
			//Utl.logger(false, 8, kart.getLsk(), serv.getId()); //###

			Double raisCoeff = 0d;
			// при отсутствии ПУ и возможности его установки, применить повышающий коэффициент для определённых услуг
			if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему осн.род.усл."), 0d) == 1d) {
				if (serv.getServDep()!=null && !metMng.checkExsKartMet(rqn, kart, serv.getServDep(), genDt)) {	
					// если не установлен счетчик по основной родительской услуге	
					String parCd = null;
					if (serv.getCd().equals("ГВС(инд) п.344 РФ")) {
						parCd = "Возможность установки ИПУ по гор. воде";
					} else if (serv.getCd().equals("ХВС(инд) п.344 РФ")) {
						parCd = "Возможность установки ИПУ по хол. воде";
					} else if (serv.getCd().equals("Эл/эн(инд) п.344 РФ")) {
						parCd = "Возможность установки ИПУ по эл/эн";
					}
					
					// проверить наличие параметров возможности установки счетчиков
					if (parCd!= null) {
						if (parMng.getBool(rqn, kart, parCd, genDt) !=null && parMng.getBool(rqn, kart, parCd, genDt)) {
							// с возможностью установки счетчика
							raisCoeff = Utl.nvl(kartMng.getServPropByCD(rqn, calc, serv, "Коэффициент начисления осн.усл.", genDt), 0d);
						}
					}
				}
			}		
			
			//Utl.logger(false, 9, kart.getLsk(), serv.getId()); //###

			// получить базу для начисления
			baseCD = parMng.getStr(rqn, serv, "Name_CD_par_base_charge");
			
			//Utl.logger(false, 10, kart.getLsk(), serv.getId()); //###
			
			// доля площади в день, для любой услуги, чтобы записать в chrg
			/* ред.28.09.17  СОСТОЯЛСЯ разговор с Дмитрием М. на тему того что почему параметр chng_val.fk_val_tp смотрит на list?
			 мною было предложено чтобы он смотрел на Par и тогда можно будет переписать получение параметра parMng.getDbl
			 на то, что он будет проверять наличие перерасчета и брать значение оттуда
			*/
			if (calc.getReqConfig().getOperTp()==1 && chng.getTp().getCd().equals("Изменение площади квартиры")
					 && chng.getServ().equals(serv)
					) {
				// Проверить наличие перерасчета по данному параметру
				OptionalDouble chngSqr = chng.getChngLsk().stream()
						.flatMap(t -> t.getChngVal().stream()
								.filter(d-> Utl.between(genDt, d.getDtVal1(), d.getDtVal2())) // фильтр по дате 
								.filter(d -> d.getDtVal1() != null && d.getDtVal2() != null ))  // фильтр по не пустой дате
								.filter(d -> d.getValTp().getCd().equals("Площадь (м2)")) // фильтр по типу параметра
								.mapToDouble(d -> Utl.nvl(d.getVal(), 0d)) // преобразовать в массив Double
								.max(); // макс.значение
				if (chngSqr.isPresent()) {
					// значение из перерасчета
					sqr = chngSqr.getAsDouble();
					//log.info("******** площадь из перерасч={}, {}, serv.name={}, id={}", sqr, genDt, serv.getName(), serv.getId());
				} else {
					//получить объем
					sqr = Utl.nvl(parMng.getDbl(rqn, kart, "Площадь.Общая", genDt, chng), 0d);
					//log.info("******** 0площадь без перерасч={}, {}, serv.name={}, id={}", sqr, genDt, serv.getName(), serv.getId());
				}
			} else {
				//получить объем
				sqr = Utl.nvl(parMng.getDbl(rqn, kart, "Площадь.Общая", genDt, chng), 0d);
				//log.info("******** площадь без перерасч={}, {}, serv.name={}, id={}", sqr, genDt, serv.getName(), serv.getId());
			}
			//Utl.logger(false, 11, kart.getLsk(), serv.getId()); //###
			
			//получить площадь одного дня
			sqr =  sqr / calc.getReqConfig().getCntCurDays();
	
			/*******************************
			 * ПОЛУЧИТЬ ОБЪЕМ ДЛЯ НАЧИСЛЕНИЯ
			 *******************************/
			if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по кол-ву точек-1"), 0d) == 1d) {
				//получить объем за месяц
				vol = Utl.nvl(parMng.getDbl(rqn, kart, baseCD, genDt, chng), 0d);
				//получить долю объема за день
				if (vol != null) {
					vol = vol / calc.getReqConfig().getCntCurDays();
				} else {
					vol = 0D;
				}
			} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-1"), 0d) == 1d ||
					   Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-2"), 0d) == 1d) {
				
				//получить объем одного дня
				vol = sqr;
				//проверить по капремонту, чтобы не была квартира муниципальной
				if (serv.getCd().equals("Взносы на кап.рем.")) {
					if (tpOwn != null && !(tpOwn.equals("Подсобное помещение") || tpOwn.equals("Приватизированная") || tpOwn.equals("Собственная"))) {
						//не начислять, выход
						return;
					} else {
						//применить льготу по капремонту по 70 - летним
						vol = vol * kartMng.getCapPrivs(rqn, calc, kart, genDt);
					}
				}
				//Utl.logger(false, 12, kart.getLsk(), serv.getId()); //###

			} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-3"), 0d) == 1d) {
				// обычно услуги ХВ, ГВ, Эл на Общее имущество (ОИ)
				// получить норматив 
				Double stVol = kartMng.getServPropByCD(rqn, calc, serv, "Норматив", genDt);
				// объем: норматив * долю площади
				if (stVol != null) {
					vol = stVol * sqr;
					//log.info("vol={} stVol={} sqr={}", vol, stVol, sqr);
				} else {
					vol = 0d;
				}
				//Utl.logger(false, 13, kart.getLsk(), serv.getId()); //###
			} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета для полива"), 0d) == 1d) {
				//получить объем за месяц
				vol = Utl.nvl(parMng.getDbl(rqn, kart, baseCD, genDt, chng), 0d);
				//получить долю объема за день HARD CODE
				//площадь полива (в доле 1 дня)/100 * 60 дней / 12мес * норматив / среднее кол-во дней в месяце
				vol = vol/100d*60d/12d*stdt.partVol/30.4d/calc.getReqConfig().getCntCurDays();
				//Utl.logger(false, 14, kart.getLsk(), serv.getId()); //###
				
			} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-1"), 0d) == 1d ||
					   Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-2"), 0d) == 1d) {
				// например Электроэнергия, Отопление гКал
				
				// Вариант подразумевает объём по лог.счётчику, Распределённый по дням
				if (serv.getServMet() == null) {
					throw new InvalidServ("По услуге Id="+serv.getId()+" не установлена соответствующая услуга счетчика");
				}
				
				if (serv.getId() == 89) {
//					log.info("check");
				}
				// получить наличие физ.счетчика в данном периоде
				exsMet = metMng.checkExsKartMet(rqn, kart, serv.getServMet(), genDt);
				
				// получить объем по лицевому счету и услуге за ДЕНЬ 
				if (calc.getReqConfig().getOperTp()==1 && chng.getTp().getCd().equals("Начисление за прошлый период") && chngLsk != null ) {
					// если перерасчет в гКал!!, то разделить на кол-во дней в месяце, так как передаётся объем за месяц
					// получить объем дня
					vol = tarMng.getChngVal(calc, serv, null, "Начисление за прошлый период", 0) / calc.getReqConfig().getCntCurDays();
				} else {
					// обычное начисление
					// получить объем дня
					SumNodeVol tmpNodeVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getChng()==null ? null : calc.getReqConfig().getChng().getId(), 
							calc.getReqConfig().getChng(),
							kart, serv.getServMet(), genDt, genDt);
					vol = tmpNodeVol.getVol();
					// сохранить номер ввода
					entry = tmpNodeVol.getEntry();
				}
				//Utl.logger(false, 15, kart.getLsk(), serv.getId()); //###
				
			} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему без исп.норматива-1"), 0d) == 1d) {
				// Вариант подразумевает объём по лог.счётчику, НЕ распределённый по дням,
				// а записанный одной строкой (одним периодом дата нач.-дата кон.)
				if (serv.getServMet() == null) {
					throw new InvalidServ("По услуге Id="+serv.getId()+" не установлена соответствующая услуга счетчика");
				}
				if (calc.getReqConfig().getOperTp()==1 && chng.getTp().getCd().equals("Начисление за прошлый период") && chngLsk != null ) {
					// перерасчет
					vol = tarMng.getChngVal(calc, serv, null, "Начисление за прошлый период", 0) / calc.getReqConfig().getCntCurDays();
				} else {
					// получить объем по услуге за период
					SumNodeVol tmpNodeVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getChng()==null ? null : calc.getReqConfig().getChng().getId(), 
							calc.getReqConfig().getChng(),
							kart, serv.getServMet(), 
							calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2());
					vol = tmpNodeVol.getVol();
				}
				// разделить на кол-во дней в месяце, так как получен объем за весь месяц
				vol = vol / calc.getReqConfig().getCntCurDays();
				//Utl.logger(false, 16, kart.getLsk(), serv.getId()); //###
				
			} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по готовой сумме"), 0d) == 1d) {
				vol = 1 / calc.getReqConfig().getCntCurDays();
			}
	
			//Utl.logger(false, 17, kart.getLsk(), serv.getId()); //###

			/***************************
			 *	   ВЫПОЛНИТЬ РАСЧЕТ
			 ***************************/
			 if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему осн.род.усл."), 0d) == 1d) {
				
				Optional<ChrgMainServRec> rec;
				rec = prepChrgMainServ.parallelStream()
						.filter(t -> t.getMainServ().equals(serv.getServDep()) && t.getDt().equals(genDt) ).findAny();
				if (rec.isPresent()) {
					// взять сумму в качестве объема, повыш.коэфф в качестве цены (потом занулить объем и цену в методе их умножения)
					chStore.addChrg(rec.get().getSum(), BigDecimal.valueOf(raisCoeff), null, null, null, null, 
							BigDecimal.valueOf(sqr), stServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
				}
				//Utl.logger(false, 18, kart.getLsk(), serv.getId()); //###

			} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-2"), 0d) == 1d ||
				Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по кол-ву точек-1"), 0d) == 1d ||
				Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему без исп.норматива-1"), 0d) == 1d) {
				//без соцнормы и свыше!
				//тип расчета, например:Взносы на капремонт
				//Вариант подразумевает объём, по параметру - базе, жилого фонда РАСПределённый по дням
		        //тип расчета, например Х.В.ОДН, Г.В.ОДН, Эл.эн.ОДН
				chStore.addChrg(BigDecimal.valueOf(vol), BigDecimal.valueOf(stPrice), null, null, null, cntPers.cntFact, 
						BigDecimal.valueOf(sqr), stServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
				//Utl.logger(false, 19, kart.getLsk(), serv.getId()); //###

			} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-3"), 0d) == 1d) {
				// обычно услуги ХВ, ГВ, Эл на Общее имущество (ОИ)
				chStore.addChrg(BigDecimal.valueOf(sqr), BigDecimal.valueOf(stPrice), null, null, null, cntPers.cntFact, 
							BigDecimal.valueOf(vol), stServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);			
				//Utl.logger(false, 20, kart.getLsk(), serv.getId()); //###
			} if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по готовой сумме"), 0d) == 1d) {
				//тип расчета, например:Коммерческий найм, где цена = сумме
				chStore.addChrg(BigDecimal.valueOf(vol), BigDecimal.valueOf(stPrice), null, null, null, cntPers.cntFact, 
						BigDecimal.valueOf(sqr), stServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
				//Utl.logger(false, 21, kart.getLsk(), serv.getId()); //###
			} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-1"), 0d) == 1d
					&& serv.getCd().equals("Электроснабжение")) {
				// тип расчета, например:текущее содержание, Х.В., Г.В., Канализ
				// Вариант подразумевает объём по лог.счётчику, РАСПределённый по дням
				// или по параметру - базе, жилого фонда, так же распределенного по дням
				BigDecimal tmpSqr;
				BigDecimal absVol = BigDecimal.valueOf(Math.abs(vol));
				BigDecimal partVol = BigDecimal.valueOf(stdt.partVol);
				BigDecimal cnt = BigDecimal.valueOf(cntPers.cnt);
				BigDecimal tmpVold = BigDecimal.ZERO;
				//Double tempAbsVol = absVol; 
				//log.info("Дата={}", genDt);
				//log.info("Объем={}", absVol);
				//log.info("Соцнорма на 1 прож.={}", stdt.vol);
	
				if (cntPers.cntEmpt != 0) {
					// есть проживающие
					if (absVol.compareTo(partVol) < 0) {
						// получить новую соцнорму, если объем меньше текущей соцнормы
						tmpVold = absVol.divide(cnt);  
					} else {
						tmpVold = partVol.divide(cnt);
					}
					
					//log.info("соцнорма на одного человека: заданная={}, по факту={}", partVol.divide(cnt), tmpVold);
					BigDecimal tmpInsVol;
					for (Pers t : cntPers.persLst) {
						PrivilegeServ privServ = kartMng.getPrivilegeServByPers(t, serv, genDt);
//						log.info("Проживающий id={}, фамилия={}, имя={}, дисконт={}", t.getId(), t.getLastname(), t.getFirstname(),
								//privServ!=null?privServ.getDiscount(): null);
						if (absVol.compareTo(tmpVold) > 0) {
							tmpInsVol = tmpVold.multiply(BigDecimal.valueOf(Math.signum(vol))); // умножить на знак
							absVol = absVol.subtract(tmpVold);  
						} else {
							tmpInsVol = absVol.multiply(BigDecimal.valueOf(Math.signum(vol))); // умножить на знак
							absVol = BigDecimal.ZERO;  
						}
						
						//log.info("объем={}", tmpInsVol);
						if (tmpInsVol.compareTo(BigDecimal.ZERO) !=0) {
							chStore.addChrg(tmpInsVol, BigDecimal.valueOf(stPrice), 
									privServ!=null ? BigDecimal.valueOf(stPrice * privServ.getDiscount()) : null, 
									privServ!=null ? BigDecimal.valueOf(privServ.getDiscount()) : null, 
									BigDecimal.valueOf(stdt.vol), cntPers.cntFact, null /* TODO площадь!*/, stServ, org, exsMet, 
									entry, genDt, cntPers.cntOwn, privServ!=null? t : null, privServ!=null? privServ.getPrivilege() : null);
						}
					}
					if (absVol.compareTo(BigDecimal.ZERO) > 0) {
						// свыше соц.нормы
						chStore.addChrg(absVol.multiply(BigDecimal.valueOf(Math.signum(vol))), BigDecimal.valueOf(upStPrice), 
								null, null,
								BigDecimal.valueOf(stdt.vol), cntPers.cntFact, null /* TODO площадь!*/, stServ, org, exsMet, 
								entry, genDt, cntPers.cntOwn, null, null);
						//log.info("свыше соц.нормы={}", absVol);
					}
					
				} else {
					// нет проживающих
					tmpSqr = BigDecimal.ZERO;
					if (BigDecimal.valueOf(vol) != BigDecimal.ZERO &&
							BigDecimal.valueOf(woKprPrice) != BigDecimal.ZERO ) {
						// записать площадь только в одну из услуг, по норме или свыше, где есть объем и цена!
						tmpSqr = BigDecimal.valueOf(sqr);
					}
				
					if (woKprServ != null) {
						// если существует услуга "без проживающих"
						chStore.addChrg(BigDecimal.valueOf(vol), BigDecimal.valueOf(woKprPrice), null, null, 
									BigDecimal.valueOf(stdt.vol), //- убрал по просьбе ИВ (чтобы не было нормы в услуге св.соц нормы) 12.05.2017 --обратно добавил по её просьбе 16.05.2017
								 	cntPers.cntFact /*здесь не cntEmpt*/, 
								 	tmpSqr, woKprServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
					} else {
						// услуги без проживающих не существует, поставить на свыше соц.нормы
						chStore.addChrg(BigDecimal.valueOf(vol), BigDecimal.valueOf(stPrice), null, null, 
									BigDecimal.valueOf(stdt.vol), //- убрал по просьбе ИВ (чтобы не было нормы в услуге св.соц нормы) 12.05.2017 --обратно добавил по её просьбе 16.05.2017
									cntPers.cntFact, /*здесь не cntEmpt*/ 
									tmpSqr, upStServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
					}
					
				}
				
			} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-1"), 0d) == 1d ||
					   Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-1"), 0d) == 1d) {
				// тип расчета, например:текущее содержание, Х.В., Г.В., Канализ
				// Вариант подразумевает объём по лог.счётчику, РАСПределённый по дням
				// или по параметру - базе, жилого фонда, так же распределенного по дням
				Double absVol= Math.abs(vol);
	
				if (cntPers.cntEmpt != 0) {
					// есть проживающие
					// соцнорма
					if (absVol <= stdt.partVol) {
						tmpVol= absVol;
					} else {
						tmpVol= stdt.partVol;
					}
	
					BigDecimal tmpSqr = BigDecimal.ZERO;
					if ( BigDecimal.valueOf(tmpVol * Math.signum(vol)) != BigDecimal.ZERO &&
							BigDecimal.valueOf(stPrice) != BigDecimal.ZERO ) {
						// записать площадь только в одну из услуг, по норме или свыше, где есть объем и цена!
						tmpSqr = BigDecimal.valueOf(sqr);
					}
					if (serv.getId()==32) {
						//log.info("serv.id={}, vol={}, stPrice={}, stdt.vol={}", serv.getId(), vol, stPrice, stdt.vol);
					}
	
					chStore.addChrg(BigDecimal.valueOf(tmpVol * Math.signum(vol)), BigDecimal.valueOf(stPrice), null, null, 
									BigDecimal.valueOf(stdt.vol), cntPers.cntFact, tmpSqr, stServ, org, exsMet, 
									entry, genDt, cntPers.cntOwn, null, null);
	
					// выше соцнормы
					if (tmpSqr == BigDecimal.ZERO && BigDecimal.valueOf(tmpVol * Math.signum(vol)) != BigDecimal.ZERO &&
							BigDecimal.valueOf(upStPrice) != BigDecimal.ZERO ) {
						// записать площадь только в одну из услуг, по норме или свыше, где есть объем и цена!
						tmpSqr = BigDecimal.valueOf(sqr);
					} else {
						tmpSqr = BigDecimal.ZERO;
					}
					tmpVol = absVol - tmpVol;
					/*if (serv.getId() == 71) {
						log.info("свыше dt={}, tmpVol={}", genDt, tmpVol);
					}*/
					chStore.addChrg(BigDecimal.valueOf(tmpVol * Math.signum(vol)), BigDecimal.valueOf(upStPrice), null, null, 
									BigDecimal.valueOf(stdt.vol), //- убрал по просьбе ИВ (чтобы не было нормы в услуге св.соц нормы) 12.05.2017 --обратно добавил по её просьбе 16.05.2017
									cntPers.cntFact, tmpSqr, upStServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
				} else {
					// нет проживающих
					BigDecimal tmpSqr = BigDecimal.ZERO;
					if (BigDecimal.valueOf(vol) != BigDecimal.ZERO &&
							BigDecimal.valueOf(woKprPrice) != BigDecimal.ZERO ) {
						// записать площадь только в одну из услуг, по норме или свыше, где есть объем и цена!
						tmpSqr = BigDecimal.valueOf(sqr);
					}
				
					if (woKprServ != null) {
						// если существует услуга "без проживающих"
						chStore.addChrg(BigDecimal.valueOf(vol), BigDecimal.valueOf(woKprPrice), null, null, 
									BigDecimal.valueOf(stdt.vol), //- убрал по просьбе ИВ (чтобы не было нормы в услуге св.соц нормы) 12.05.2017 --обратно добавил по её просьбе 16.05.2017
								 	cntPers.cntFact /*здесь не cntEmpt*/, 
								 	tmpSqr, woKprServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
					} else {
						// услуги без проживающих не существует, поставить на свыше соц.нормы
						chStore.addChrg(BigDecimal.valueOf(vol), BigDecimal.valueOf(stPrice), null, null, 
									BigDecimal.valueOf(stdt.vol), //- убрал по просьбе ИВ (чтобы не было нормы в услуге св.соц нормы) 12.05.2017 --обратно добавил по её просьбе 16.05.2017
									cntPers.cntFact, /*здесь не cntEmpt*/ 
									tmpSqr, upStServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
					}
					
				}
				//Utl.logger(false, 22, kart.getLsk(), serv.getId()); //###

			} if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-2"), 0d) == 1d) {
				//тип расчета, например:Отопление по Гкал
				//Вариант подразумевает объём по лог.счётчику, записанный одной строкой, за период
				//расчет долей соц.нормы и свыше
				if (sqr > 0d) {
					if (cntPers.cntEmpt != 0) {	
						// площадь по норме
						BigDecimal tmpArea = BigDecimal.ZERO;
						// площадь св.нормы
						BigDecimal tmpUpArea = BigDecimal.ZERO;
						
						//есть проживающие
						if (stdt.partVol > sqr) {
							// соцнорма больше площади
							tmpArea = BigDecimal.valueOf(sqr);
						} else {
							// соцнорма меньше или равна площади
							tmpArea = BigDecimal.valueOf(stdt.partVol);
						}
						tmpUpArea = BigDecimal.valueOf(sqr).subtract(tmpArea);
						//найти коэфф соц.нормы к площади лиц.сч.
						cf = tmpArea.divide(BigDecimal.valueOf(sqr), 15, RoundingMode.HALF_UP);
						//соцнорма
						tmpVolD = BigDecimal.valueOf(vol).multiply(cf);
						if (tmpVolD.compareTo(BigDecimal.ZERO)!=0 ||
								Utl.nvl(parMng.getDbl(rqn, stServ, "Сохранять_CHRG.AREA_CHRG.CNT_PERS"), 0d) == 1) {
							chStore.addChrg(tmpVolD, BigDecimal.valueOf(stPrice), null, null, null, cntPers.cntFact, 
									tmpArea, stServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
						}
						//свыше соцнормы
						tmpVolD = BigDecimal.valueOf(vol).subtract(tmpVolD);
						if (tmpVolD.compareTo(BigDecimal.ZERO)!=0 ||
								Utl.nvl(parMng.getDbl(rqn, upStServ, "Сохранять_CHRG.AREA_CHRG.CNT_PERS"), 0d) == 1) {
							chStore.addChrg(tmpVolD, BigDecimal.valueOf(upStPrice), null, null, null, cntPers.cntFact, 
									tmpUpArea, upStServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
						}
					} else {
						//нет проживающих
						if (woKprServ != null) {
							//если есть услуга "без проживающих"
							tmpVolD = BigDecimal.valueOf(vol);
							if (tmpVolD.compareTo(BigDecimal.ZERO)!=0 ||
									Utl.nvl(parMng.getDbl(rqn, upStServ, "Сохранять_CHRG.AREA_CHRG.CNT_PERS"), 0d) == 1) {
								chStore.addChrg(tmpVolD, BigDecimal.valueOf(woKprPrice), null, null, null, cntPers.cntFact, 
										BigDecimal.valueOf(sqr), woKprServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
							}
						} else {
							//если нет услуги "без проживающих", взять расценку, по услуге свыше соц.нормы
							tmpVolD = BigDecimal.valueOf(vol);
							if (tmpVolD.compareTo(BigDecimal.ZERO)!=0 ||
									Utl.nvl(parMng.getDbl(rqn, upStServ, "Сохранять_CHRG.AREA_CHRG.CNT_PERS"), 0d) == 1) {
								chStore.addChrg(tmpVolD, BigDecimal.valueOf(upStPrice), null, null, null, cntPers.cntFact, 
										BigDecimal.valueOf(sqr), upStServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
							}
						}
						
					}
				}
				//Utl.logger(false, 23, kart.getLsk(), serv.getId()); //###

			} if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета для полива"), 0d) == 1d) {
				
				if (cntPers.cntEmpt != 0) {
					//есть проживающие
					//tmpSum = BigDecimal.valueOf(vol).multiply( BigDecimal.valueOf(stPrice) );
					//addChrg(kart, serv, tmpSum, vol, stPrice, genDt, chrgTpDet);
					chStore.addChrg( BigDecimal.valueOf(vol), BigDecimal.valueOf(stPrice), null, null, null, cntPers.cntFact, 
							BigDecimal.valueOf(sqr), stServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
				} else {
					//нет проживающих
					//tmpSum = BigDecimal.valueOf(vol).multiply( BigDecimal.valueOf(woKprPrice) );
					//addChrg(kart, serv, tmpSum, vol, woKprPrice, genDt, chrgTpDet);
					chStore.addChrg( BigDecimal.valueOf(vol), BigDecimal.valueOf(woKprPrice), null, null, null, cntPers.cntFact, 
							BigDecimal.valueOf(sqr), woKprServ, org, exsMet, entry, genDt, cntPers.cntOwn, null, null);
				}			

				//Utl.logger(false, 24, kart.getLsk(), serv.getId()); //###

			}
			endTime   = System.currentTimeMillis();
			totalTime = endTime - startTime2;
			if (totalTime >10) {
			  log.trace("ВРЕМЯ НАЧИСЛЕНИЯ по дате "+genDt.toLocaleString()+" услуге:"+totalTime);
			}
		}
	}
	
	/**
	 * сохранить запись о сумме, предназаначенной для коррекции 
	 * @param serv - услуга
	 * @param sum - сумма
	 */
	private void putMapServVal(Serv serv, BigDecimal sum) {
		BigDecimal tmpSum;
		//HaspMap считает разными услуги, если они одинаковые, но пришли из разных потоков, пришлось искать for - ом - <-- Проверить это TODO!  
		synchronized (mapServ) {
		for (Map.Entry<Serv, BigDecimal> entry : mapServ.entrySet()) {
	    	if (entry.getKey().equals(serv)) { 
	    		tmpSum = Utl.nvl(entry.getValue(), BigDecimal.ZERO);
	    		tmpSum = tmpSum.add(sum);
	    	    mapServ.put(entry.getKey(), tmpSum);
	    		return;
	    	}
	    }
	    mapServ.put(serv, sum);
		}
	}
	
	/**
	 * сохранить запись о сумме, предназаначенной для коррекции 
	 * @param serv - услуга
	 * @param sum - сумма
	 */
	private void putMapVrtVal(Serv serv, BigDecimal sum) {
		BigDecimal tmpSum;
		synchronized (mapVrt) {
	    for (Map.Entry<Serv, BigDecimal> entry : mapVrt.entrySet()) {
	    	if (entry.getKey().equals(serv)) {
	    		tmpSum = Utl.nvl(entry.getValue(), BigDecimal.ZERO);
	    		tmpSum = tmpSum.add(sum);
	    		mapVrt.put(entry.getKey(), tmpSum);
	    		return;
	    	}
	    }
	    mapVrt.put(serv, sum);
		}
	}

	/**
	 * добавить строку начисления 
	 * @param chrg - строка начисления
	 */
	private void chrgAppend(Chrg chrg) {
		//synchronized (prepChrg) {
		  prepChrg.add(chrg);
		//}
	}

	/**
	 * добавить из потока все строки сумм начислений по основной услуге
	 * @param list
	 */
	private void chrgMainServAppend(List<ChrgMainServRec> lst) {
		prepChrgMainServ.addAll(lst);
	}

}
