package com.ric.bill;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ric.bill.excp.CyclicMeter;
import com.ric.bill.excp.EmptyPar;
import com.ric.bill.excp.EmptyServ;
import com.ric.bill.excp.EmptyStorable;
import com.ric.bill.excp.ErrorWhileDist;
import com.ric.bill.excp.NotFoundNode;
import com.ric.bill.excp.NotFoundODNLimit;
import com.ric.bill.excp.WrongGetMethod;
import com.ric.bill.excp.WrongValue;
import com.ric.bill.mm.HouseMng;
import com.ric.bill.mm.KartMng;
import com.ric.bill.mm.LstMng;
import com.ric.bill.mm.MeterLogMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.mm.ServMng;
import com.ric.bill.mm.VolMng;
import com.ric.bill.model.ar.House;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.mt.MLogs;
import com.ric.bill.model.tr.Serv;

/**
 * Сервис распределения объемов
 * @author lev
 *
 */
@Service
@Scope("prototype")
@Slf4j
public class DistServ {

	@Autowired
	private ApplicationContext ctx;
	@Autowired
	private Config config;
	@Autowired
	private MeterLogMng metMng;
	@Autowired
	private KartMng kartMng;
	@Autowired
	private ParMng parMng;
	@Autowired
	private LstMng lstMng;
	@Autowired
	private ServMng servMng;
	@Autowired
	private HouseMng houseMng;
	@Autowired
	private VolMng volMng;
	
	@Autowired //-здесь не надо autowire, так как prototype
	private DistGen distGen;
	
	@Autowired
    private ChrgServ chrg;

	//EntityManager - EM нужен на каждый DAO или сервис свой!
    @PersistenceContext
    private EntityManager em;

    private Calc calc;
    // статус записи объёма зависит от типа операции (0 - начисление, 1 - перерасчет)
    private Integer statusVol;
    
    /**
	 * Установить фильтры для сессии -убрал пока
	 * 
	 */
/*	private void setFilters() {
		Session session = em.unwrap(Session.class);
		log.trace("Установлен фильтр: c:"+Calc.getCurDt1()+" по:"+Calc.getCurDt2());
		session.enableFilter("FILTER_GEN_DT_OUTER").setParameter("DT1", Calc.getCurDt1())
		   .setParameter("DT2", Calc.getCurDt2());
		//отдельно установить фильтр существования счетчиков
	}*/

    
	/**
	 * Удалить объем по вводам дома
	 * 
	 * @param serv - заданная услуга
	 * @throws CyclicMeter 
	 */
    private void delHouseVolServ(int rqn) throws CyclicMeter {

    	log.info("Удаление объемов по Дому: id="+calc.getHouse().getId()+" по услуге cd="+calc.getServ().getCd(), 2);

		delHouseServVolTp(rqn, calc.getServ().getServMet(), 1);
		delHouseServVolTp(rqn, calc.getServ().getServMet(), 0);
		delHouseServVolTp(rqn, calc.getServ().getServMet(), 2);
		delHouseServVolTp(rqn, calc.getServ().getServMet(), 3);
		if (calc.getServ().getServOdn() != null){
			delHouseServVolTp(rqn, calc.getServ().getServOdn(), 0);//счетчики ОДН
		} 
	}

	
	/**
	 * Удалить объем по вводу, определённой услуге
	 * @param serv - услуга
	 * @throws CyclicMeter 
	 */
	private void delHouseServVolTp(int rqn, Serv serv, int tp) throws CyclicMeter {
		//перебрать все необходимые даты, за период
		Calendar c = Calendar.getInstance();
		//необходимый для формирования диапазон дат
		Date dt1, dt2;
		if (calc.getCalcTp()==2) {
			//формирование по ОДН - задать последнюю дату
			dt1 = calc.getReqConfig().getCurDt2();
			dt2 = calc.getReqConfig().getCurDt2();
		} else {
			//прочее формирование
			dt1 = calc.getReqConfig().getCurDt1();
			dt2 = calc.getReqConfig().getCurDt2();
		}

		// удалить объемы по всем вводам по дому и по услуге
		for (MLogs ml : metMng.getAllMetLogByServTp(rqn, calc.getHouse(), serv, "Ввод")) {
			metMng.delNodeVol(rqn, ml, tp, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2(), getStatusVol());
		}
		
	}
	
	/**
	 * Распределить объемы по домам
	 * @param calc
	 * @param areaId 
	 * @throws ErrorWhileDist 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
    public void distAll(Calc calc, Integer houseId, Integer areaId) throws ErrorWhileDist {
			//distGen = ctx.getBean(DistGen.class);
			this.calc=calc;
			int rqn = calc.getReqConfig().getRqn();
			// статус записи зависит от типа операции (0 - начисление, 1 - перерасчет)
			switch (calc.getReqConfig().getOperTp()) {
				case 0: 
					setStatusVol(0);
					break;
				case 1: 
					setStatusVol(1);
					break;
			}
			long startTime;
			long endTime;
			long totalTime;
			for (House o: houseMng.findAll2(houseId, areaId, config.getCurDt1())) {
				log.info("ДОМ:"+o.getId());
				//dist.clearCache();
				//распределить объемы
				startTime = System.currentTimeMillis();
				//Logger.getLogger("org.hibernate.SQL").setLevel(Level.DEBUG);
				//Logger.getLogger("org.hibernate.type").setLevel(Level.TRACE);
		    	
				distHouseVol(rqn, o.getId());
				
				endTime   = System.currentTimeMillis();
				totalTime = endTime - startTime;
				log.info("Время исполнения-1:"+totalTime);
			}
		
    }
	
	
	/**
	 * распределить объем по всем услугам, по лиц.счету
	 * @throws ErrorWhileDist 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void distKartVol(Calc calc) throws ErrorWhileDist {
		this.calc=calc;
		int rqn = calc.getReqConfig().getRqn();
		// статус записи объема зависит от типа операции (0 - начисление, 1 - перерасчет)
		switch (calc.getReqConfig().getOperTp()) {
		case 0: 
			setStatusVol(0);
			break;
		case 1: 
			setStatusVol(1);
			break;
		}
		

		Kart kart = em.find(Kart.class, calc.getKart().getLsk());
		// почистить коллекцию обработанных счетчиков
		distGen.clearLstChecks();

		// найти все необходимые услуги для удаления объемов, здесь только по типу 0,1,2 и только те услуги, которые надо удалить для ЛС 
		try {
			for (Serv serv : servMng.findForDistVolForKart()) {
				Boolean v = parMng.getBool(rqn, serv, "Распределять объем по только по дому");   
				if (v==null || !v) {
					// Если задано, распределять услугу, только при выборе дома (не ЛС!)
					log.trace("Удаление объема по услуге"+serv.getCd());
						// тип обработки = 0 - расход
						calc.setCalcTp(0);
							delKartServVolTp(rqn, kart, serv);
						// тип обработки = 1 - площадь и кол-во прож.
						calc.setCalcTp(1);
						delKartServVolTp(rqn, kart, serv);
						// тип обработки = 3 - пропорц.площади (отопление)
						calc.setCalcTp(3);
						delKartServVolTp(rqn, kart, serv);
				}
			}
		} catch (CyclicMeter | EmptyStorable e) {
			e.printStackTrace();
			throw new ErrorWhileDist("Ошибка при удалении объемов счетчиков в лс="+calc.getKart().getLsk());
		}
		
		log.trace("Распределение объемов");
		// найти все необходимые услуги для распределения
		 
		 try {
			for (Serv serv : servMng.findForDistVol()) {
				Boolean v = parMng.getBool(rqn, serv, "Распределять объем по только по дому");   
				if (v==null || !v) {
					// Если задано, распределять услугу, только при выборе дома (не ЛС!)
					log.trace("Распределение услуги: "+serv.getCd());
					calc.setCalcTp(0);
				    distKartServTp(rqn, kart, serv);
				    if (serv.getCd().equals("Отопление")) {
					    // тип обработки = 1 - площадь и кол-во прож.
						calc.setCalcTp(1);
					    distKartServTp(rqn, kart, serv);
						// тип обработки = 3 - пропорц.площади (отопление)
						calc.setCalcTp(3);
					    distKartServTp(rqn, kart, serv);
				    }
				}
			}
		} catch (ErrorWhileDist | EmptyStorable e) {
			e.printStackTrace();
			throw new ErrorWhileDist("Ошибка при распределении объемов счетчиков в лс="+calc.getKart().getLsk());
		}

	}
	
	/**
	 * Удалить объем по Лиц.счету, определённой услуге
	 * @param Kart - лиц.счет
	 * @param serv - услуга
	 * @param tp - тип расчета
	 * @throws CyclicMeter 
	 */
	private void delKartServVolTp(int rqn, Kart kart, Serv serv) throws CyclicMeter {
		log.trace("delKartServVolTp: kart.lsk="+kart.getLsk()+", serv.cd="+serv.getCd()+" tp="+calc.getCalcTp());
		//перебрать все необходимые даты, за период
		Calendar c = Calendar.getInstance();
		//необходимый для формирования диапазон дат
		Date dt1, dt2;
		if (calc.getCalcTp()==2) {
			//формирование по ОДН - задать последнюю дату
			dt1 = calc.getReqConfig().getCurDt2();
			dt2 = calc.getReqConfig().getCurDt2();
		} else {
			//прочее формирование
			dt1 = calc.getReqConfig().getCurDt1();
			dt2 = calc.getReqConfig().getCurDt2();
		}

		//найти все счетчики по Лиц.счету, по услуге
		for (c.setTime(dt1); !c.getTime().after(dt2); c.add(Calendar.DATE, 1)) {
			//calc.setGenDt(c.getTime());
			for (MLogs ml : metMng.getAllMetLogByServTp(rqn, kart, serv, null)) {
				metMng.delNodeVol(rqn, ml, calc.getCalcTp(), calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2(), getStatusVol());
			}
			
		}
	}

	
	/**
	 * Распределить объем по счетчикам дома
	 * @param calcTp - тип обработки
	 * @throws ErrorWhileDist 
	 */
	private void distKartServTp(int rqn, Kart kart, Serv serv) throws ErrorWhileDist {
		//найти все начальные узлы расчета по лиц.счету и по услуге
		for (MLogs ml : metMng.getAllMetLogByServTp(rqn, kart, serv, null)) {
				//log.info("Услуга: serv.cd={},  Узел id={}", serv.getCd() , ml.getId());
				distGraph(ml);
		}
	}
	
	/**
	 * распределить объем по дому
	 * @param houseId - Id дома, иначе кэшируется, если передавать объект дома
	 * @throws ErrorWhileDist 
	 */
	public void distHouseVol(int rqn, int houseId) throws ErrorWhileDist {
		House h = em.find(House.class, houseId);
		//установить инициализацию дома
    	//установить дом и счет
		calc.setHouse(h);
		if (calc.getArea() ==null) {
			throw new ErrorWhileDist("Ошибка! По записи house.id="+houseId+", в его street, не заполнено поле area!");
		}
		
		log.info("DistServ.distHouseVol: Очистка объемов по дому id="+calc.getHouse().getId()+" klsk="+calc.getHouse().getKlskId(), 2);
		//почистить коллекцию обработанных счетчиков
		distGen.clearLstChecks();

		//найти все необходимые услуги для удаления объемов
		for (Serv s : servMng.findForDistVol()) {
				calc.setServ(s);
				try {
					delHouseVolServ(rqn);
				} catch (CyclicMeter e) {
					e.printStackTrace();
					throw new ErrorWhileDist("Ошибка при распределении счетчиков в доме id="+houseId);
				}
		}

		log.info("DistServ.distHouseVol: Распределение объемов по дому id="+calc.getHouse().getId()+" klsk="+calc.getHouse().getKlskId(), 2);
		//найти все необходимые услуги для распределения
		try {
			for (Serv s : servMng.findForDistVol()) {
				calc.setServ(s);
				log.info("DistServ.distHouseVol: Распределение услуги: "+s.getCd(),2);
				  distHouseServ(rqn);
			}
		} catch (ErrorWhileDist e) {
			e.printStackTrace();
			throw new ErrorWhileDist("Dist.distHouseVol: ");
		}
	}
	
	
	/**
	 * распределить объем по услуге данного дома
	 * @throws ErrorWhileDist 
	 */
	private void distHouseServ(int rqn) throws ErrorWhileDist {
		log.trace("******************Услуга*************="+calc.getServ().getCd());
		calc.setCalcTp(1);
		distHouseServTp(rqn, calc.getServ().getServMet());//Расчет площади, кол-во прожив
		calc.setCalcTp(0);
		distHouseServTp(rqn, calc.getServ().getServMet());//Распределение объема
		calc.setCalcTp(2);
		distHouseServTp(rqn, calc.getServ().getServMet());//Расчет ОДН
		calc.setCalcTp(3);
		distHouseServTp(rqn, calc.getServ().getServMet());//Расчет пропорц.площади
		if (calc.getServ().getServOdn() != null){
			calc.setCalcTp(0);
			distHouseServTp(rqn, calc.getServ().getServOdn());//Суммировать счетчики ОДН
		}
	}
	
	/**
	 * Распределить объем по вводам дома
	 * @param calcTp - тип обработки
	 * @throws ErrorWhileDist 
	 */
	private void distHouseServTp(int rqn, Serv serv) throws ErrorWhileDist {
		log.trace("Распределение по типу:"+calc.getCalcTp());
		//найти все вводы по дому и по услуге
		for (MLogs ml : metMng.getAllMetLogByServTp(rqn, calc.getHouse(), serv, "Ввод")) {
				log.trace("Вызов distGraph c id="+ml.getId());
				distGraph(ml);
		}
	}
	
	/**
	 * Распределить граф начиная с mLog
	 * @param ml - начальный узел распределения
	 * @throws ErrorWhileDist 
	 */
	private void distGraph (MLogs ml) throws ErrorWhileDist {
		log.trace("DistServ.distGraph: Распределение счетчика:"+ml.getId());
		//перебрать все необходимые даты, за период
		Calendar c = Calendar.getInstance();
		//необходимый для формирования диапазон дат
		Date dt1, dt2;
		if (calc.getCalcTp()==2) {
			//формирование по ОДН - задать последнюю дату
			dt1 = calc.getReqConfig().getCurDt2();
			dt2 = calc.getReqConfig().getCurDt2();
		} else {
			//прочее формирование
			dt1 = calc.getReqConfig().getCurDt1();
			dt2 = calc.getReqConfig().getCurDt2();
		}
		for (c.setTime(dt1); !c.getTime().after(dt2); c.add(Calendar.DATE, 1)) {
			calc.setGenDt(c.getTime());
			@SuppressWarnings("unused")
			NodeVol dummy;
			try {
				dummy=distGen.distNode(calc, ml, calc.getCalcTp(), calc.getGenDt());
				
			} catch (WrongGetMethod e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Dist.distGraph: При расчете счетчика MeterLog.Id="+ml.getId()+" , обнаружен замкнутый цикл");  
			} catch (EmptyServ e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Dist.distGraph: Пустая услуга при рекурсивном вызове BillServ.distNode()");
			} catch (EmptyStorable e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Dist.distGraph: Пустой хранимый компонент при рекурсивном вызове BillServ.distNode()");
			} catch (NotFoundODNLimit e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Dist.distGraph: Не найден лимит ОДН в счетчике ОДН, при вызове BillServ.distNode()");
			} catch (NotFoundNode e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Dist.distGraph: Не найден нужный счетчик, при вызове BillServ.distNode(): ");
			} catch (EmptyPar e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Dist.distGraph: Не найден необходимый параметр объекта, при вызове BillServ.distNode(): ");
			} catch (WrongValue e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Dist.distGraph: Некорректное значение в расчете, при вызове BillServ.distNode(): ");
			}
		}

		
	}


	public Integer getStatusVol() {
		return statusVol;
	}


	public void setStatusVol(Integer statusVol) {
		this.statusVol = statusVol;
	}



}
