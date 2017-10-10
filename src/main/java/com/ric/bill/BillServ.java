package com.ric.bill;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ric.bill.excp.ErrorWhileChrg;
import com.ric.bill.excp.ErrorWhileDist;
import com.ric.bill.mm.HouseMng;
import com.ric.bill.mm.KartMng;
import com.ric.bill.mm.ObjMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.model.ar.Kart;

/**
 * Главный сервис биллинга
 * 
 * @author lev
 *
 */
@Service
@Scope("prototype") // странно что стоит prototype, а вызвается как Autowired в BillingController - поменял это, но проверить! TODO!  
@Slf4j
public class BillServ {

	@Autowired
	private HouseMng houseMng;
	@Autowired
	private Config config;
	@Autowired
	private KartMng kartMng;
	@Autowired
	private ObjMng objMng; // TODO убрать!
	@Autowired
	private ParMng parMng;// TODO убрать!
	@Autowired
	private ApplicationContext ctx;
	@PersistenceContext
	private EntityManager em;

	// коллекция для формирования потоков
	private List<Kart> kartThr;

	// конструктор
	public BillServ() {

	}

	// получить список N следующих лиц.счетов, для расчета в потоках
	private List<Kart> getNextKart(int cnt) {
		List<Kart> lst = new ArrayList<Kart>();
		int i = 1;
		Iterator<Kart> itr = kartThr.iterator();
		while (itr.hasNext()) {
			Kart kart = itr.next();
			lst.add(kart);
			itr.remove();
			i++;
			if (i > cnt) {
				break;
			}
		}

		return lst;
	}

	// Exception из потока
	Thread.UncaughtExceptionHandler expThr = new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread th, Throwable ex) {
			System.out.println("BillServ: Error in thread: " + th.getName()
					+ " " + ex.getMessage());
			ex.printStackTrace();
		}
	};

	/**
	 * выполнить распределение объемов и начисление по всем домам в потоках
	 * 
	 * @param isDist
	 *            - распределять ли объемы
	 * @param isChrg
	 *            - выполнять ли начисление
	 * @return
	 */
	@Async(value = "BWEEEEE: chrgAll")
	@CacheEvict(value = {"TarifMngImpl.getOrg", "KartMngImpl.getOrg", "KartMngImpl.getServ", "KartMngImpl.getServAll", 
			"KartMngImpl.getCapPrivs", "KartMngImpl.getServPropByCD", "KartMngImpl.getStandartVol", "KartMngImpl.getCntPers", "KartMngImpl.checkPersNullStatus",
			"KartMngImpl.checkPersStatusExt", "KartMngImpl.checkPersStatus", "ObjDAOImpl.getByCD", "MeterLogDAOImpl.getKart", "OrgDAOImpl.getByKlsk", "ParDAOImpl.getByCd",
			"ParDAOImpl.checkPar", "ServDAOImpl.findMain", "ServDAOImpl.getByCD", "DistGen.findLstCheck", "MeterLogMngImpl.getAllMetLogByServTp", "MeterLogMngImpl.checkExsKartMet",
			"MeterLogMngImpl.checkExsMet", "MeterLogMngImpl.getVolPeriod1", "MeterLogMngImpl.getVolPeriod2", "MeterLogMngImpl.getLinkedNode", 
			"MeterLogMngImpl.getKart", "ParMngImpl.isExByCd", "ParMngImpl.getBool1", "ParMngImpl.getBool2", "ParMngImpl.getDbl1", "ParMngImpl.getDbl2", "ParMngImpl.getDate",
			"ParMngImpl.getStr1", "ParMngImpl.getStr2", "TarifMngImpl.getProp", "TarifDAOImpl.getPropByCD", "VsecDAOImpl.getPrivByUserRoleAct", "LstMngImpl.getByCD",
			"ServMngImpl.getUpper", "ServMngImpl.getUpperTree", "MeterLogMngImpl.delNodeVol",
			"rrr1" }, allEntries = true)
	public Future<Result> chrgAll(RequestConfig reqConfig, Integer houseId, Integer areaId, Integer tempLskId) {
		// Logger.getLogger("org.hibernate.SQL").setLevel(Level.DEBUG);
		// Logger.getLogger("org.hibernate.type").setLevel(Level.TRACE);
		Result res = new Result();
		res.setErr(0);
		// кол-во потоков
		int cntThreads = 20;
		// кол-во обраб.лиц.сч.
		int cntLsk = 0;

		long startTime;
		long endTime;
		long totalTime;
		long totalTime3;

		startTime = System.currentTimeMillis();
		DistServ distServ = ctx.getBean(DistServ.class);

		// РАСПРЕДЕЛЕНИЕ ОБЪЕМОВ, если задано
		try {
			if (reqConfig.getIsDist()) {
				Calc calc = new Calc(reqConfig);
					distServ.distAll(calc, houseId, areaId, tempLskId);
				log.info("BillServ.chrgAll: Распределение по всем домам выполнено!");
			}
		} catch (ErrorWhileDist e) {
			log.error("Ошибка при распределении объемов по дому house.id={}", houseId);
			res.setErr(1);
		}

		// РАСЧЕТ НАЧИСЛЕНИЯ ПО ЛС В ПОТОКАХ
		if (res.getErr() ==0) {
			long startTime3 = System.currentTimeMillis();
			// загрузить все необходимые Лиц.счета
			kartThr = kartMng.findAll(houseId, areaId, tempLskId, config.getCurDt1(), config.getCurDt2());
			cntLsk = kartThr.size();
			while (true) {
				log.trace("BillServ.chrgAll: Loading karts for threads");
				// получить следующие N лиц.счетов, рассчитать их в потоке
				long startTime2;
				long endTime2;
				long totalTime2;
				startTime2 = System.currentTimeMillis();
	
				List<Kart> kartWork = getNextKart(cntThreads);
				if (kartWork.isEmpty()) {
					// выйти, если все услуги обработаны
					break;
				}
	
				List<Future<Result>> frl = new ArrayList<Future<Result>>();
	
				for (Kart kart : kartWork) {
	
	//				log.info("BillServ.chrgAll: Prepare thread for lsk="
	//						+ kart.getLsk());
					Future<Result> fut = null;
					ChrgServThr chrgServThr = ctx.getBean(ChrgServThr.class);
	
					// под каждый поток - свой Calc
					Calc calc = new Calc(reqConfig);
	
					calc.setKart(kart);
					calc.setHouse(kart.getKw().getHouse());
					if (calc.getArea() ==null) {
						log.error("Ошибка! По записи house.id={}, в его street, не заполнено поле area!");
						res.setErr(1);
					}
					if (kart.getLsk() == 1511) {
						log.info("area.id={}", calc.getArea().getId());
					}
					
					try {
						fut = chrgServThr.chrgAndSaveLsk(calc);
					} catch (ErrorWhileChrg | ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					frl.add(fut);
					log.trace("BillServ.chrgAll: Begins thread for lsk="
							+ kart.getLsk());
				}
	
				// проверить окончание всех потоков
				int flag2 = 0;
				while (flag2 == 0) {
					log.trace("BillServ.chrgAll: ========================================== Waiting for threads-2");
					flag2 = 1;
					for (Future<Result> fut : frl) {
						if (!fut.isDone()) {
							flag2 = 0;
						} else {
							try {
								log.trace("ChrgServ: Done Result.err:="
										+ fut.get().getErr());
								if (fut.get().getErr() == 1) {
								}
							} catch (InterruptedException | ExecutionException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
	
						}
					}
	
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	
				}
	
				endTime2 = System.currentTimeMillis();
				totalTime2 = endTime2 - startTime2;
				log.info("Time for chrg One Lsk:" + totalTime2 / cntThreads, 2);
	
			}
			endTime = System.currentTimeMillis();
			totalTime = endTime - startTime;
			totalTime3 = endTime - startTime3;
			log.info("Ver=2.0", 2);
			log.info("Counted lsk:" + cntLsk, 2);
			log.info("Time for all process:" + totalTime, 2);
			if (cntLsk > 0) {
				log.info("Time of charging per one Lsk: " + totalTime3 / cntLsk + " ms.", 2);
			}
		}
		return new AsyncResult<Result>(res);
	}

	/**
	 * выполнить начисление по лиц.счету
	 * 
	 * @param kart
	 *            - лиц счет (заполнен либо он, либо lsk)
	 * @param lsk
	 *            - номер лиц.счета
	 * @param dist
	 *            - распределить объемы?
	 * @throws InterruptedException
	 * @throws ErrorWhileChrg
	 */
	@Async
	@CacheEvict(value = {"TarifMngImpl.getOrg", "KartMngImpl.getOrg", "KartMngImpl.getServ", "KartMngImpl.getServAll", 
			"KartMngImpl.getCapPrivs", "KartMngImpl.getServPropByCD", "KartMngImpl.getStandartVol", "KartMngImpl.getCntPers", "KartMngImpl.checkPersNullStatus",
			"KartMngImpl.checkPersStatusExt", "KartMngImpl.checkPersStatus", "ObjDAOImpl.getByCD", "MeterLogDAOImpl.getKart", "OrgDAOImpl.getByKlsk", "ParDAOImpl.getByCd",
			"ParDAOImpl.checkPar", "ServDAOImpl.findMain", "ServDAOImpl.getByCD", "DistGen.findLstCheck", "MeterLogMngImpl.getAllMetLogByServTp", "MeterLogMngImpl.checkExsKartMet",
			"MeterLogMngImpl.checkExsMet", "MeterLogMngImpl.getVolPeriod1", "MeterLogMngImpl.getVolPeriod2", "MeterLogMngImpl.getLinkedNode", 
			"MeterLogMngImpl.getKart", "ParMngImpl.isExByCd", "ParMngImpl.getBool1", "ParMngImpl.getBool2", "ParMngImpl.getDbl1", "ParMngImpl.getDbl2", "ParMngImpl.getDate",
			"ParMngImpl.getStr1", "ParMngImpl.getStr2", "TarifMngImpl.getProp", "TarifDAOImpl.getPropByCD", "VsecDAOImpl.getPrivByUserRoleAct", "LstMngImpl.getByCD",
			"ServMngImpl.getUpper", "ServMngImpl.getUpperTree", "MeterLogMngImpl.delNodeVol",
			"rrr1" }, allEntries = true)
	public Future<Result> chrgLsk(RequestConfig reqConfig, Kart kart,
			Integer lsk) {
		long beginTime = System.currentTimeMillis();
		ChrgServThr chrgServThr = ctx.getBean(ChrgServThr.class);
		DistServ distServ = ctx.getBean(DistServ.class);

		Result res = new Result();
		Future<Result> fut = new AsyncResult<Result>(res);
		// признак распределения по дому (в случае перерасчета по Отоплению)
		boolean isDistHouse = false;

		res.setErr(0);
		// Если был передан идентификатор лицевого, то найти лиц.счет
		if (lsk != null) {
			kart = em.find(Kart.class, lsk);
			if (kart == null) {
				res.setErr(1);
				return fut;
			}
		}
		Calc calc = new Calc(reqConfig);

		// установить дом и счет
		calc.setHouse(kart.getKw().getHouse());
		calc.setKart(kart);

		beginTime = System.currentTimeMillis();

		// установить признак распределения объема по дому, если перерасчет
		if (reqConfig.getOperTp().equals(1)) {
			List<String> lstServCd = reqConfig.getChng().getChngLsk().stream().filter(t -> t.getKart().getLsk().equals(lsk))
					.filter(t -> t.getServ() != null)
					.map(t-> t.getServ().getCd()).collect(Collectors.toList());
			if (lstServCd.contains("Отопление") || lstServCd.contains("Отопление(объем), соц.н.") || 
					lstServCd.contains("Отопление(объем), св.соц.н.") || lstServCd.contains("Отопление(объем), без прожив.")) {
				isDistHouse = true;
			}
		}
		// РАСПРЕДЕЛЕНИЕ ОБЪЕМОВ, если задано
		try {
			if (isDistHouse == true) {
				// задано распределить по дому, для перерасчета (например по отоплению, когда поменялась площадь и надо пересчитать гКал)
				distServ.distAll(calc, calc.getHouse().getId(), null, null);
			} else if (reqConfig.getIsDist()) {
				distServ.distKartVol(calc);
				// присвоить обратно лиц.счет, который мог быть занулён в предыдущ методах
				calc.setKart(kart);
			}
		} catch (ErrorWhileDist e) {
			e.printStackTrace();
			res.setErr(1);
			return fut;
		}

		long endTime2 = System.currentTimeMillis() - beginTime;
		beginTime = System.currentTimeMillis();

		// РАСЧЕТ НАЧИСЛЕНИЯ ПО 1 ЛС
		try {
			fut = chrgServThr.chrgAndSaveLsk(calc);
		} catch (ErrorWhileChrg | ExecutionException e) {
			e.printStackTrace();
			res.setErr(1);
			return fut;
		}

		long endTime3 = System.currentTimeMillis() - beginTime;
		//log.info("TIMING: найти дом, л.с.={}, распр.объем={}, начислить={}", endTime1, endTime2,
				//endTime3);

		return fut;
	}
}
