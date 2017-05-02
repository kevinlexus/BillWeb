package com.ric.bill;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

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
import com.ric.bill.model.tr.Serv;

/**
 * РАСЧЕТ НАЧИСЛЕНИЯ, ВЫЗЫВАЕТСЯ В ПОТОКЕ
 * @author lev
 *
 */


@Component
@Scope("prototype") //собственный бин для каждого потока
@Slf4j
public class ChrgThr {
	
	@Autowired
	private LstMng lstMng;
	@Autowired
	private Config config;
	@Autowired
	private ParMng parMng;
	@Autowired
	private TarifMng tarMng;
	
	@Autowired
	private KartMng kartMng;
	@Autowired
	private MeterLogMng metMng;

	@Autowired
	private ApplicationContext ctx;
	
    @PersistenceContext
    private EntityManager em;

	// основная услуга
    private Serv serv;
	//временное хранилище записей
	private ChrgStore chStore;

	private String thrName;
	
    //вспомогательные коллекции
    private List<Chrg> prepChrg;
    private List<ChrgMainServRec> prepChrgMainServ;
    
    private HashMap<Serv, BigDecimal> mapServ;
    private HashMap<Serv, BigDecimal> mapVrt;

    private Calc calc;
    
    //конструктор
	public ChrgThr() {
		super();
	}
	
	/**
	 * 
	 * @param calc 	   - объект calc
	 * @param serv 	   - услуга
	 * @param mapServ  - коллекция для округления
	 * @param mapVrt   - коллекция для округления
	 * @param prepChrg - коллекция для сгруппированных записей начисления
	 */
	public void set(Calc calc, Serv serv,  
			HashMap<Serv, BigDecimal> mapServ, 
			HashMap<Serv, BigDecimal> mapVrt, 
			List<Chrg> prepChrg,
			List<ChrgMainServRec> prepChrgMainServ) {
		this.calc = calc;
		this.serv = serv;
		this.mapServ = mapServ;
		this.mapVrt = mapVrt;
		this.prepChrg = prepChrg;
		this.prepChrgMainServ = prepChrgMainServ;
	}

	@Async
	public  Future<Result> run1() {
		Kart kart = calc.getKart();
		
		Result res = new Result();
		res.err=0;

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
		if (serv.getId()==79) {
			log.trace("ChrThr.run1: "+thrName+", Услуга:"+serv.getCd()+" Id="+serv.getId());
		}
		log.trace("ChrThr.run1: "+thrName+", Услуга:"+serv.getCd()+" Id="+serv.getId());
		/*if (serv.getId()==72) {
			log.trace("ChrThr.run1: "+thrName+", Услуга:"+serv.getCd()+" Id="+serv.getId());
		}*/
		
		//}
		Calendar c = Calendar.getInstance();
		
		// РАСЧЕТ по дням
		for (c.setTime(dt1); !c.getTime().after(dt2); c.add(Calendar.DATE, 1)) {
			genDt = c.getTime();
			// только там, где нет статуса "не начислять" за данный день
			try {
				if (Utl.nvl(parMng.getDbl(rqn, kart, "IS_NOT_CHARGE", genDt), 0d) == 1d) {
					continue;
				}
			} catch (EmptyStorable e) {
				e.printStackTrace();
				throw new RuntimeException();
			}
			// только там, где лиц.счет существует в данном дне и существует услуга
			if (Utl.between(genDt, kart.getDt1(), kart.getDt2()) &&  kartMng.getServ(rqn, calc, serv, genDt)) {
				String tpOwn = null;
				try {
					tpOwn = parMng.getStr(rqn, kart, "FORM_S", genDt);
				} catch (EmptyStorable e) {
					e.printStackTrace();
					throw new RuntimeException();
				}
				
				if (tpOwn == null) {
					log.info("ОШИБКА! Не указанна форма собственности! lsk="+kart.getLsk(), 2);
				}
				// где лиц.счет является нежилым помещением, не начислять за данный день
				if (tpOwn != null && (tpOwn.equals("Нежилое собственное") || tpOwn.equals("Нежилое муниципальное")
					|| tpOwn.equals("Аренда некоммерч.") || tpOwn.equals("Для внутр. пользования"))) {
					continue;
				}
					try {
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
		
		// ДОБАВИТЬ сгруппированные по основной услуге суммы начислений
		chrgMainServAppend(chStore.getStoreMainServ());
		
		// ОКОНЧАТЕЛЬНО рассчитать данные (умножить расценку на объем, округлить)
		for (ChrgRec rec : chStore.getStore()) {
			BigDecimal vol, area, sum;
			vol = rec.getVol();
			// округлить объем
			vol = vol.setScale(5, BigDecimal.ROUND_HALF_UP);

			area = rec.getArea();
			// округлить площадь
			if (area != null) {
				area = area.setScale(2, BigDecimal.ROUND_HALF_UP);
			}
			// умножить на расценку
			sum = vol.multiply(rec.getPrice());
			// округлить до копеек
			sum = sum.setScale(2, BigDecimal.ROUND_HALF_UP);
			
			// записать, для будущего округления по виртуальной услуге
			if (rec.getServ().getServVrt() != null) {
				putMapServVal(rec.getServ().getServVrt(), sum);
			}
			// записать, сумму по виртуальной услуге
			if (rec.getServ().getVrt()) {
					putMapVrtVal(rec.getServ(), sum);
			}

			if (!rec.getServ().getVrt()) {
				if (sum.compareTo(BigDecimal.ZERO) != 0) {
					Chrg chrg = new Chrg(kart, rec.getServ(), rec.getOrg(), 1, calc.getReqConfig().getPeriod(), sum, sum, 
							vol, rec.getPrice(), rec.getStdt(), rec.getCntPers(), area, chrgTpRnd, 
							calc.getReqConfig().getChng(), rec.getMet(), rec.getDt1(), rec.getDt2());
					chrgAppend(chrg);
				}
			}
		} 

		return new AsyncResult<Result>(res);
	}

	// получить подмененную организацию по перерасчету
	private Org getChngOrg(Serv serv, Date genDt) {
		Org org = null; 
		if ( Utl.between(genDt, calc.getReqConfig().getChng().getDt1(), calc.getReqConfig().getChng().getDt2()) &&
				calc.getReqConfig().getChng().getServ().equals(serv) ) {
			org = calc.getReqConfig().getChng().getOrg();
		}
		return org;
	}
	
	
	/**
	 * РАСЧЕТ начисления по дню
	 * @param serv - услуга
	 * @throws InvalidServ 
	 */
	private void genChrg(Calc calc, Serv serv, String tpOwn, Date genDt) throws EmptyStorable, EmptyOrg, InvalidServ {
		//log.info("serv.cd={}", serv.getCd());
		Kart kart = calc.getKart();
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
		
		log.trace("Расчет услуги id={}, cd={}, genDt={}", serv.getId(), serv.getCd(), genDt);
		// получить необходимые услуги
		stServ = serv.getServSt();
		upStServ = serv.getServUpst();
		woKprServ = serv.getServWokpr();
		// если услуга по соцнорме пустая, присвоить изначальную услугу
		if (stServ == null) {
			stServ = serv;
		}
		
		// контроль наличия услуги св.с.нормы (по ряду услуг)
		if ((Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-1"), 0d) == 1d || 
				Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-1"), 0d) == 1d) && serv.getServUpst() == null) {
			throw new EmptyStorable("По услуге Id="+serv.getId()+" обнаружена пустая услуга свыше соц.нормы");
		}

		//Получить кол-во проживающих 
		kartMng.getCntPers(rqn, calc, kart, serv, cntPers, genDt);

		//получить расценку по норме	
		stPrice = kartMng.getServPropByCD(rqn, calc, stServ, "Цена", genDt);
		
		if (stPrice == null) {
			stPrice = 0d;
		}

		// получить составляющие перерасчета
		Chng chng = calc.getReqConfig().getChng();
		ChngLsk chngLsk = null;
		if (chng != null && chng.getServ().equals(serv)) {
			chngLsk = chng.getChngLsk().stream().filter(t-> t.getKart().equals(kart))
					.findFirst().orElse(null);
		}
		
		
		//получить долю соц.нормы.свыше (не объем!!!)
		if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-1"), 0d) == 1d || 
				Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-1"), 0d) == 1d ||
				Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-2"), 0d) == 1d ||
				Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета для полива"), 0d) == 1d) {
			
			
			stdt = kartMng.getStandartVol(rqn, calc, serv, cntPers, genDt, 0);
			//здесь же получить расценки по свыше соц.нормы и без проживающих 
			if (serv.getServUpst() != null) {
				
				upStPrice = kartMng.getServPropByCD(rqn, calc, upStServ, "Цена", genDt);
				if (upStPrice == null) {
					upStPrice = 0d;
				}
			} else {
				upStPrice = 0d;
			}

			if (serv.getServWokpr() != null) {
				woKprPrice = kartMng.getServPropByCD(rqn, calc, woKprServ, "Цена", genDt);
				if (woKprPrice == null) {
					// если не найдена цена с 0 проживающими, подставить цену по свыше соц.нормы, если и она не найдена, то по норме
					if (upStPrice == null || upStPrice == 0d) {
						woKprPrice = stPrice;
					} else {
						woKprPrice = upStPrice;
					}
				} 
			} else {
				woKprPrice = 0d;
			} 
		}
			
		
		//получить организацию
		if (serv.getCheckOrg()) {
		  org = kartMng.getOrg(rqn, calc, serv.getServOrg(), genDt);
		  if (org == null) {
				throw new EmptyOrg("При расчете л.с.="+kart.getLsk()+" , обнаружена пустая организция по услуге Id="+serv.getServOrg().getId());
		  }
		}
		

		// в случае перерасчета по расценке или по организации, выполнить замену
		if (calc.getReqConfig().getOperTp()==1 && calc.getReqConfig().getChng().getTp().getCd().equals("Изменение расценки (тарифа)") ) {
			
			// организация
			Org chngOrg = getChngOrg(serv.getServOrg(), genDt);
			if (chngOrg != null) {
				org = chngOrg; 
			}

			// расценка по норме
			Double chngPrice = tarMng.getChngVal(calc, stServ, genDt, "Изменение расценки (тарифа)", 1);
			if (chngPrice != null) {
				stPrice = chngPrice; 
			}
			
			// расценка св.нормы
			chngPrice = tarMng.getChngVal(calc, upStServ, genDt, "Изменение расценки (тарифа)", 1);
			if (chngPrice != null) {
				upStPrice = chngPrice; 
			}
				
			// расценка без проживающих
			chngPrice = tarMng.getChngVal(calc, woKprServ, genDt, "Изменение расценки (тарифа)", 1);
			if (chngPrice != null) {
				woKprPrice = chngPrice; 
			}
			
		}
		
		Double raisCoeff = 0d;
		// при отсутствии ПУ и возможности его установки, применить повышающий коэффициент для определённых услуг
		if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему осн.род.усл."), 0d) == 1d) {
			if (!metMng.checkExsKartMet(rqn, kart, serv.getServDep(), genDt)) {	
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
						raisCoeff = Utl.nvl(kartMng.getServPropByCD(rqn, calc, serv, "Коэффициент начисления осн.усл.", genDt), 0d); // Hard code установить коэфф 0.5
					}
				}
			}
		}		
		
		// получить базу для начисления
		baseCD = parMng.getStr(rqn, serv, "Name_CD_par_base_charge");
	
		// получить объем для начисления
		if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по кол-ву точек-1"), 0d) == 1d || 
				Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-1"), 0d) == 1d ||
				Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-2"), 0d) == 1d) {
			//получить объем одного дня
			vol = Utl.nvl(parMng.getDbl(rqn, kart, baseCD, genDt), 0d);
			
			vol = vol / calc.getReqConfig().getCntCurDays();
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
			
		} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета для полива"), 0d) == 1d) {
			//получить объем за месяц
			vol = Utl.nvl(parMng.getDbl(rqn, kart, baseCD, genDt), 0d);
			//получить долю объема за день HARD CODE
			//площадь полива (в доле 1 дня)/100 * 60 дней / 12мес * норматив / среднее кол-во дней в месяце
			vol = vol/100d*60d/12d*stdt.partVol/30.4d/calc.getReqConfig().getCntCurDays();
			
		} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-1"), 0d) == 1d ||
				   Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему-2"), 0d) == 1d) {
			
			
			// Вариант подразумевает объём по лог.счётчику, Распределённый по дням
			if (serv.getServMet() == null) {
				throw new InvalidServ("По услуге Id="+serv.getId()+" не установлена соответствующая услуга счетчика");
			}
			
//			if (serv.getId() == 35) {
				//log.info("check");
			//}
			// получить наличие физ.счетчика в данном периоде
			exsMet = metMng.checkExsKartMet(rqn, kart, serv.getServMet(), genDt);
			
			// получить объем по лицевому счету и услуге за ДЕНЬ
			if (calc.getReqConfig().getOperTp()==1 && chng.getTp().getCd().equals("Начисление за прошлый период") && chngLsk != null ) {
				// если перерасчет, то разделить на кол-во дней в месяце, так как передаётся объем за месяц
				vol = tarMng.getChngVal(calc, serv, null, "Начисление за прошлый период", 0) / calc.getReqConfig().getCntCurDays();
			} else {
				// обычное начисление
				SumNodeVol tmpNodeVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), kart, serv.getServMet(), genDt, genDt);
				vol = tmpNodeVol.getVol();
			}
			
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
				SumNodeVol tmpNodeVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), kart, serv.getServMet(), 
						calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2());
				vol = tmpNodeVol.getVol();
			}
			// разделить на кол-во дней в месяце, так как получен объем за весь месяц
			vol = vol / calc.getReqConfig().getCntCurDays();
		} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по готовой сумме"), 0d) == 1d) {
			vol = 1 / calc.getReqConfig().getCntCurDays();
		}

		// доля площади в день, для любой услуги, чтобы записать в chrg
		sqr = Utl.nvl(parMng.getDbl(rqn, kart, "Площадь.Общая", genDt), 0d) / calc.getReqConfig().getCntCurDays();
		
		/****************************/
		// ВЫПОЛНИТЬ РАСЧЕТ
		/****************************/
		 if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему осн.род.усл."), 0d) == 1d) {
			
			Optional<ChrgMainServRec> rec;
			// обязательно синхронизировать (в prepChrgMainServ идёт запись из других потоков)
			synchronized(prepChrgMainServ) {
				 rec = prepChrgMainServ.parallelStream().filter(t -> t.getMainServ().equals(serv.getServDep()) && t.getDt().equals(genDt) ).findAny();
			}
			if (rec.isPresent()) {
				// взять сумму в качестве объема, повыш.коэфф в качестве цены - СТРАННОВАТО! TODO!
				//log.info("CHECK! = {}, {}", rec.get().getSum(), raisCoeff);
				chStore.addChrg(rec.get().getSum(), BigDecimal.valueOf(raisCoeff), null, null, BigDecimal.valueOf(sqr), stServ, org, exsMet, genDt);
			}
			
		} else if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-2"), 0d) == 1d ||
			Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по кол-ву точек-1"), 0d) == 1d ||
			Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по объему без исп.норматива-1"), 0d) == 1d) {
			//без соцнормы и свыше!
			//тип расчета, например:Взносы на капремонт
			//Вариант подразумевает объём, по параметру - базе, жилого фонда РАСПределённый по дням
	        //тип расчета, например Х.В.ОДН, Г.В.ОДН, Эл.эн.ОДН
			chStore.addChrg(BigDecimal.valueOf(vol), BigDecimal.valueOf(stPrice), null, cntPers.cntVol, 
					BigDecimal.valueOf(sqr), stServ, org, exsMet, genDt);
		} if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по готовой сумме"), 0d) == 1d) {
			//тип расчета, например:Коммерческий найм, где цена = сумме
			chStore.addChrg(BigDecimal.valueOf(vol), BigDecimal.valueOf(stPrice), null, cntPers.cntVol, 
					BigDecimal.valueOf(sqr), stServ, org, exsMet, genDt);
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

				chStore.addChrg(BigDecimal.valueOf(tmpVol * Math.signum(vol)), BigDecimal.valueOf(stPrice), 
								BigDecimal.valueOf(stdt.vol), cntPers.cntVol, tmpSqr, stServ, org, exsMet, genDt);

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
				chStore.addChrg(BigDecimal.valueOf(tmpVol * Math.signum(vol)), BigDecimal.valueOf(upStPrice), 
								BigDecimal.valueOf(stdt.vol), cntPers.cntVol, tmpSqr, upStServ, org, exsMet, genDt);
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
					chStore.addChrg(BigDecimal.valueOf(vol), BigDecimal.valueOf(woKprPrice), 
							 	BigDecimal.valueOf(stdt.vol), cntPers.cntVol /*здесь не cntEmpt*/, tmpSqr, woKprServ, org, exsMet, genDt);
				} else {
					// услуги без проживающих не существует, поставить на свыше соц.нормы
					chStore.addChrg(BigDecimal.valueOf(vol), BigDecimal.valueOf(stPrice), 
								BigDecimal.valueOf(stdt.vol), cntPers.cntVol, /*здесь не cntEmpt*/ tmpSqr, upStServ, org, exsMet, genDt);
				}
				
			}

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
					if (!tmpVolD.equals(BigDecimal.ZERO)) {
						chStore.addChrg(tmpVolD, BigDecimal.valueOf(stPrice), null, cntPers.cntVol, tmpArea, stServ, org, exsMet, genDt);
					}
					//свыше соцнормы
					tmpVolD = BigDecimal.valueOf(vol).subtract(tmpVolD);
					if (!tmpVolD.equals(BigDecimal.ZERO)) {
						chStore.addChrg(tmpVolD, BigDecimal.valueOf(upStPrice), null, cntPers.cntVol, tmpUpArea, upStServ, org, exsMet, genDt);
					}
				} else {
					//нет проживающих
					if (woKprServ != null) {
						//если есть услуга "без проживающих"
						tmpVolD = BigDecimal.valueOf(vol);
						chStore.addChrg(tmpVolD, BigDecimal.valueOf(woKprPrice), null, cntPers.cntVol, 
								BigDecimal.valueOf(sqr), woKprServ, org, exsMet, genDt);
					} else {
						//если нет услуги "без проживающих", взять расценку, по услуге свыше соц.нормы
						tmpVolD = BigDecimal.valueOf(vol);
						chStore.addChrg(tmpVolD, BigDecimal.valueOf(upStPrice), null, cntPers.cntVol, 
								BigDecimal.valueOf(sqr), upStServ, org, exsMet, genDt);
					}
					
				}
			}
		} if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета для полива"), 0d) == 1d) {
			
			if (cntPers.cntEmpt != 0) {
				//есть проживающие
				//tmpSum = BigDecimal.valueOf(vol).multiply( BigDecimal.valueOf(stPrice) );
				//addChrg(kart, serv, tmpSum, vol, stPrice, genDt, chrgTpDet);
				chStore.addChrg( BigDecimal.valueOf(vol), BigDecimal.valueOf(stPrice), null, cntPers.cntVol, 
						BigDecimal.valueOf(sqr), stServ, org, exsMet, genDt);
			} else {
				//нет проживающих
				//tmpSum = BigDecimal.valueOf(vol).multiply( BigDecimal.valueOf(woKprPrice) );
				//addChrg(kart, serv, tmpSum, vol, woKprPrice, genDt, chrgTpDet);
				chStore.addChrg( BigDecimal.valueOf(vol), BigDecimal.valueOf(woKprPrice), null, cntPers.cntVol, 
						BigDecimal.valueOf(sqr), woKprServ, org, exsMet, genDt);
			}			
			
		}
		endTime   = System.currentTimeMillis();
		totalTime = endTime - startTime2;
		if (totalTime >10) {
		  log.trace("ВРЕМЯ НАЧИСЛЕНИЯ по дате "+genDt.toLocaleString()+" услуге:"+totalTime);
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
	 * добавить из потока строку начисления 
	 * @param chrg - строка начисления
	 */
	private void chrgAppend(Chrg chrg) {
		synchronized (prepChrg) {
		  prepChrg.add(chrg);
		}
	}

	/**
	 * добавить из потока все строки сумм начислений по основной услуге
	 * @param list
	 */
	private void chrgMainServAppend(List<ChrgMainServRec> lst) {
		synchronized (prepChrgMainServ) {
			prepChrgMainServ.addAll(lst);
		}
	}
	
}
