package com.ric.bill;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
import com.ric.bill.mm.LstMng;
import com.ric.bill.mm.MeterLogMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.mm.ServMng;
import com.ric.bill.model.ar.House;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.bs.Lst;
import com.ric.bill.model.mt.MLogs;
import com.ric.bill.model.mt.MeterLog;
import com.ric.bill.model.mt.Vol;
import com.ric.bill.model.tr.Serv;

import lombok.extern.slf4j.Slf4j;

/**
 * Сервис распределения объемов
 * 
 * @author lev
 *
 */
@Service
@Scope("prototype")
@Slf4j
public class DistServ {

	@Autowired
	private Config config;
	@Autowired
	private MeterLogMng metMng;
	@Autowired
	private ParMng parMng;
	@Autowired
	private LstMng lstMng;
	@Autowired
	private ServMng servMng;

	@Autowired // -здесь не надо autowire, так как prototype
	private DistGen distGen;

	// EntityManager - EM нужен на каждый DAO или сервис свой!
	@PersistenceContext
	private EntityManager em;

	private Calc calc;
	/**
	 * Установить фильтры для сессии -убрал пока
	 * 
	 */
	/*
	 * private void setFilters() { Session session = em.unwrap(Session.class);
	 * log.trace("Установлен фильтр: c:"+Calc.getCurDt1()+" по:"+Calc.getCurDt2());
	 * session.enableFilter("FILTER_GEN_DT_OUTER").setParameter("DT1",
	 * Calc.getCurDt1()) .setParameter("DT2", Calc.getCurDt2()); //отдельно
	 * установить фильтр существования счетчиков }
	 */

	/**
	 * Удалить объем по вводам дома
	 * 
	 * @param serv
	 *            - заданная услуга
	 * @throws CyclicMeter
	 */
	private void delHouseVolServ(int rqn) throws CyclicMeter {

		log.info("RQN={}, Удаление объемов по House.id={}  по услуге cd={}", rqn, calc.getHouse().getId(),
				calc.getServ().getCd(), 2);

		delHouseServVolTp(rqn, calc.getServ().getServMet(), 1);
		delHouseServVolTp(rqn, calc.getServ().getServMet(), 0);
		delHouseServVolTp(rqn, calc.getServ().getServMet(), 2);
		delHouseServVolTp(rqn, calc.getServ().getServMet(), 3);
		if (calc.getServ().getServOdn() != null) {
			delHouseServVolTp(rqn, calc.getServ().getServOdn(), 0);// счетчики ОДН
		}
	}

	/**
	 * Удалить объем по вводу, определённой услуге
	 * 
	 * @param serv - услуга
	 * @throws CyclicMeter
	 */
	private void delHouseServVolTp(int rqn, Serv serv, int tp) throws CyclicMeter {
		// перебрать все необходимые даты, за период
		// необходимый для формирования диапазон дат
		Date dt1, dt2;
		if (calc.getCalcTp() == 2) {
			// формирование по ОДН - задать последнюю дату
			dt1 = calc.getReqConfig().getCurDt2();
			dt2 = calc.getReqConfig().getCurDt2();
		} else {
			// прочее формирование
			dt1 = calc.getReqConfig().getCurDt1();
			dt2 = calc.getReqConfig().getCurDt2();
		}

		// удалить объемы по всем вводам по дому и по услуге
		for (MLogs ml : metMng.getAllMetLogByServTp(rqn, calc.getHouse(), serv, "Ввод")) {
			metMng.delNodeVol(rqn, calc.getReqConfig().getChng()==null ? null : calc.getReqConfig().getChng().getId(),
					calc.getReqConfig().getChng(), ml, tp, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2()
							);
		}

	}

	/**
	 * Распределить объемы по домам
	 * 
	 * @param calc
	 * @param houseId
	 *            - Id дома
	 * @param areaId
	 *            - Id области
	 * @throws ErrorWhileDist
	 */
/*	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public void distAll(Calc calc, Integer houseId, Integer areaId, Integer tempLskId) throws ErrorWhileDist {
		this.calc = calc;
		int rqn = calc.getReqConfig().getRqn();
		long startTime;
		long endTime;
		long totalTime;
		for (House o : houseMng.findAll2(houseId, areaId, tempLskId, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2())) {
			log.info("RQN={}, Распределение объемов по House.id={}", calc.getReqConfig().getRqn(), o.getId());
			// распределить объемы
			startTime = System.currentTimeMillis();

			// распределение объемов по дому
			distHouseVol(rqn, o.getId());
			endTime = System.currentTimeMillis();
			totalTime = endTime - startTime;
			log.info("RQN={}, House.id={}, Время распределения: {}", calc.getReqConfig().getRqn(), o.getId(),
					totalTime);
		}
	}
*/
	/**
	 * распределить объем по всем услугам, по лиц.счету Как правило вызывается из
	 * начисления, поэтому не нуждается в блокировке лиц.счета
	 * 
	 * @throws ErrorWhileDist
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void distKartVol(Calc calc) throws ErrorWhileDist {
		Integer lsk = calc.getKart().getLsk();
		Integer houseId = calc.getKart().getKw().getHouse().getId();
		// блокировка лиц.счета
		int waitTick = 0;
		while (!config.lock.setLockChrgLsk(calc.getReqConfig().getRqn(), lsk, houseId)) {
			waitTick++;
			if (waitTick > 60) {
				log.error("********ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!");
				log.error("********НЕ ВОЗМОЖНО РАЗБЛОКИРОВАТЬ к lsk={} В ТЕЧЕНИИ 60 сек!{}", lsk);
				throw new ErrorWhileDist("Ошибка при блокировке лс lsk=" + lsk);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Ошибка при блокировке лс lsk=" + lsk);
			}
		}

		try {
			this.calc = calc;
			int rqn = calc.getReqConfig().getRqn();

			Kart kart = em.find(Kart.class, calc.getKart().getLsk());
			// почистить коллекцию обработанных счетчиков
			distGen.clearLstChecks();

			// найти все необходимые услуги для удаления объемов, здесь только по типу 0,1,2
			// и только те услуги, которые надо удалить для ЛС
			try {
				for (Serv serv : servMng.findForDistVolForKart()) {
					Boolean v = parMng.getBool(rqn, serv, "Распределять объем по только по дому");
					if (v == null || !v) {
						// Если задано, распределять услугу, только при выборе дома (не ЛС!)
						log.trace("Удаление объема по услуге" + serv.getCd());
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
				throw new ErrorWhileDist("Ошибка при удалении объемов счетчиков в лс=" + calc.getKart().getLsk());
			}

			log.trace("Распределение объемов");
			// найти все необходимые услуги для распределения

			try {
				for (Serv serv : servMng.findForDistVol()) {
					Boolean v = parMng.getBool(rqn, serv, "Распределять объем по только по дому");
					if (v == null || !v) {
						// Если задано, распределять услугу, только при выборе дома (не ЛС!)
						log.trace("Распределение услуги: " + serv.getCd());
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
				throw new ErrorWhileDist("Ошибка при распределении объемов счетчиков в лс=" + calc.getKart().getLsk());
			}
		} finally {
			// разблокировать лс
			config.lock.unlockChrgLsk(calc.getReqConfig().getRqn(), lsk, houseId);
		}
	}

	/**
	 * Удалить объем по Лиц.счету, определённой услуге
	 * 
	 * @param Kart
	 *            - лиц.счет
	 * @param serv
	 *            - услуга
	 * @param tp
	 *            - тип расчета
	 * @throws CyclicMeter
	 */
	private void delKartServVolTp(int rqn, Kart kart, Serv serv) throws CyclicMeter {
		log.trace("delKartServVolTp: kart.lsk=" + kart.getLsk() + ", serv.cd=" + serv.getCd() + " tp="
				+ calc.getCalcTp());
		// перебрать все необходимые даты, за период
		Calendar c = Calendar.getInstance();
		// необходимый для формирования диапазон дат
		Date dt1, dt2;
		if (calc.getCalcTp() == 2) {
			// формирование по ОДН - задать последнюю дату
			dt1 = calc.getReqConfig().getCurDt2();
			dt2 = calc.getReqConfig().getCurDt2();
		} else {
			// прочее формирование
			dt1 = calc.getReqConfig().getCurDt1();
			dt2 = calc.getReqConfig().getCurDt2();
		}

		// найти все счетчики по Лиц.счету, по услуге
		for (c.setTime(dt1); !c.getTime().after(dt2); c.add(Calendar.DATE, 1)) {
			// calc.setGenDt(c.getTime());
			for (MLogs ml : metMng.getAllMetLogByServTp(rqn, kart, serv, null)) {
				metMng.delNodeVol(rqn, calc.getReqConfig().getChng()==null ? null : calc.getReqConfig().getChng().getId(),
						calc.getReqConfig().getChng(), ml, calc.getCalcTp(), calc.getReqConfig().getCurDt1(),
						calc.getReqConfig().getCurDt2());
			}

		}
	}

	/**
	 * Распределить объем по счетчикам лицевого
	 * 
	 * @param calcTp
	 *            - тип обработки
	 * @throws ErrorWhileDist
	 */
	private void distKartServTp(int rqn, Kart kart, Serv serv) throws ErrorWhileDist {
		// найти все начальные узлы расчета по лиц.счету и по услуге
		/*for (MLogs ml : metMng.getAllMetLogByServTp(rqn, kart, serv, null)) {
			 log.info("Услуга: serv.cd={}, Узел id={}", serv.getCd() , ml.getId());
		}*/
		
		for (MLogs ml : metMng.getAllMetLogByServTp(rqn, kart, serv, null)) {
			 //log.info("Услуга: serv.cd={}, Узел id={}", serv.getCd() , ml.getId());
			distGraph(ml);
		}
	}

	/**
	 * распределить объем по дому
	 * 
	 * @param houseId
	 *            - Id дома, иначе кэшируется, если передавать объект дома
	 * @return
	 * @throws ErrorWhileDist
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
	public void distHouseVol(Calc calc, int rqn, int houseId) throws ErrorWhileDist {
		this.calc = calc;
		House h = em.find(House.class, houseId);

		// установить инициализацию дома
		// установить дом и счет
		calc.setHouse(h);
		if (calc.getArea() == null) {
			throw new ErrorWhileDist(
					"Ошибка! По записи house.id=" + houseId + ", в его street, не заполнено поле area!");
		}

		// блокировка дома для распределения объемов
		int waitTick = 0;
		while (!config.lock.setLockDistHouse(rqn, houseId)) {
			waitTick++;
			if (waitTick > 60) {
				log.error("********ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!");
				log.error(
						"********НЕ ВОЗМОЖНО РАЗБЛОКИРОВАТЬ к houseId={} для распределения объемов в ТЕЧЕНИИ 100 сек!{}",
						houseId);
				throw new ErrorWhileDist("Ошибка при попытке блокировки дома house.id=" + houseId);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Ошибка при попытке блокировки дома house.id=" + houseId);
			}
		}

		try {
			log.info("RQN={}, Очистка объемов по House.id={}, house.klsk={}", rqn, calc.getHouse().getId(),
					calc.getHouse().getKlskId());
			// почистить коллекцию обработанных счетчиков
			distGen.clearLstChecks();
	
			// найти все необходимые услуги для удаления объемов
			for (Serv s : servMng.findForDistVol()) {
				calc.setServ(s);
				try {
					delHouseVolServ(rqn);
				} catch (CyclicMeter e) {
					e.printStackTrace();
					throw new ErrorWhileDist("Ошибка при распределении счетчиков в доме house.id=" + houseId);
				}
			}
	
			log.info("RQN={}, Распределение объемов по House.id={} house.klsk={}", calc.getReqConfig().getRqn(),
					calc.getHouse().getId(), calc.getHouse().getKlskId(), 2);
			// найти все необходимые услуги для распределения
			try {
				for (Serv s : servMng.findForDistVol()) {
					calc.setServ(s);
					log.info("RQN={}, Распределение услуги: cd={}", calc.getReqConfig().getRqn(), s.getCd(), 2);
					distHouseServ(rqn);
				}
			} catch (ErrorWhileDist e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Ошибка при распределении объемов по дому: house.id=" + houseId);
			}
		} finally {
			// разблокировать дом
			config.lock.unlockDistHouse(rqn, houseId);
		}
	}

	/**
	 * распределить объем по услуге данного дома
	 * 
	 * @throws ErrorWhileDist
	 */
	private void distHouseServ(int rqn) throws ErrorWhileDist {
		log.trace("******************Услуга*************=" + calc.getServ().getCd());
		calc.setCalcTp(1);
		distHouseServTp(rqn, calc.getServ().getServMet());// Расчет площади, кол-во прожив
		calc.setCalcTp(0);
		distHouseServTp(rqn, calc.getServ().getServMet());// Распределение объема
		calc.setCalcTp(2);
		distHouseServTp(rqn, calc.getServ().getServMet());// Расчет ОДН
		calc.setCalcTp(3);
		distHouseServTp(rqn, calc.getServ().getServMet());// Расчет пропорц.площади
		if (calc.getServ().getServOdn() != null) {
			calc.setCalcTp(0);
			distHouseServTp(rqn, calc.getServ().getServOdn());// Суммировать счетчики ОДН
		}
	}

	/**
	 * Распределить объем по вводам дома
	 * 
	 * @param calcTp
	 *            - тип обработки
	 * @throws ErrorWhileDist
	 */
	private void distHouseServTp(int rqn, Serv serv) throws ErrorWhileDist {
		log.trace("Распределение по типу:" + calc.getCalcTp());
		// найти все вводы по дому и по услуге
		for (MLogs ml : metMng.getAllMetLogByServTp(rqn, calc.getHouse(), serv, "Ввод")) {
			log.trace("Вызов distGraph c id=" + ml.getId());
			distGraph(ml);
		}
	}

	/**
	 * Распределить граф начиная с mLog
	 * 
	 * @param ml
	 *            - начальный узел распределения
	 * @throws ErrorWhileDist
	 */
	private void distGraph(MLogs ml) throws ErrorWhileDist {
		log.trace("DistServ.distGraph: Распределение счетчика:" + ml.getId());
		// перебрать все необходимые даты, за период
		Calendar c = Calendar.getInstance();
		// необходимый для формирования диапазон дат
		Date dt1, dt2;
		if (calc.getCalcTp() == 2) {
			// формирование по ОДН - задать последнюю дату
			dt1 = calc.getReqConfig().getCurDt2();
			dt2 = calc.getReqConfig().getCurDt2();
		} else {
			// прочее формирование
			dt1 = calc.getReqConfig().getCurDt1();
			dt2 = calc.getReqConfig().getCurDt2();
		}
		for (c.setTime(dt1); !c.getTime().after(dt2); c.add(Calendar.DATE, 1)) {
			calc.setGenDt(c.getTime());
			@SuppressWarnings("unused")
			NodeVol dummy;
			try {
				dummy = distGen.distNode(calc, ml, calc.getCalcTp(), calc.getGenDt());

			} catch (WrongGetMethod e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Dist.distGraph: При расчете счетчика MeterLog.Id=" + ml.getId()
						+ " , обнаружен замкнутый цикл");
			} catch (EmptyServ e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Dist.distGraph: Пустая услуга при рекурсивном вызове BillServ.distNode()");
			} catch (EmptyStorable e) {
				e.printStackTrace();
				throw new ErrorWhileDist(
						"Dist.distGraph: Пустой хранимый компонент при рекурсивном вызове BillServ.distNode()");
			} catch (NotFoundODNLimit e) {
				e.printStackTrace();
				throw new ErrorWhileDist(
						"Dist.distGraph: Не найден лимит ОДН в счетчике ОДН, при вызове BillServ.distNode()");
			} catch (NotFoundNode e) {
				e.printStackTrace();
				throw new ErrorWhileDist("Dist.distGraph: Не найден нужный счетчик, при вызове BillServ.distNode(): ");
			} catch (EmptyPar e) {
				e.printStackTrace();
				throw new ErrorWhileDist(
						"Dist.distGraph: Не найден необходимый параметр объекта, при вызове BillServ.distNode(): ");
			} catch (WrongValue e) {
				e.printStackTrace();
				throw new ErrorWhileDist(
						"Dist.distGraph: Некорректное значение в расчете, при вызове BillServ.distNode(): ");
			}
		}

	}

	/**
	 * ТЕСТ-вызов не удалять!
	 * 
	 * @param id
	 * @return
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void distTest(Integer id) {
		log.info("====== distTest");
		distTest2(id);
	}

	/**
	 * ТЕСТ-вызов не удалять!
	 * 
	 * @param id
	 * @return
	 */
	public void distTest2(Integer id) {
		log.info("====== distTest2");

		MLogs mLog = em.find(MeterLog.class, 520459);
		for (Iterator<Vol> iterator = mLog.getVol().iterator(); iterator.hasNext();) {
			Vol vol = iterator.next();
			iterator.remove();
		}

		Date dd1 = Utl.getDateFromStr("01.10.2017");
		Date dd2 = Utl.getDateFromStr("31.10.2017");
		Lst volTp = lstMng.getByCD("Лимит ОДН");
		Vol vol = new Vol((MeterLog) mLog, volTp, Double.valueOf(id), null, dd1, dd2, null, null);
		mLog.getVol().add(vol);
		mLog.getVol().stream()
				.filter(t -> t.getDt1().getTime() >= dd1.getTime() && t.getDt1().getTime() <= dd2.getTime())
				.forEach(t -> {
					log.info("id={} БЫЛО ЗАПИСАНО dt={}, vol={}", id, t.getDt1(), t.getVol1());
				});

		log.info("Записан id={}", id);
		try {
			Thread.sleep(id);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
