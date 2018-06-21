package com.ric.bill;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.ric.bill.dao.KartDAO;
import com.ric.bill.excp.ErrorWhileChrg;
import com.ric.bill.excp.ErrorWhileDist;
import com.ric.bill.mm.HouseMng;
import com.ric.bill.mm.KartMng;
import com.ric.bill.model.ar.Kart;
import com.ric.cmn.Utl;
import com.ric.cmn.excp.CantLock;

import lombok.extern.slf4j.Slf4j;

/**
 * Главный сервис биллинга
 * @version 1.0
 * @author lev
 *
 */
@Service
@Scope("prototype") // странно что стоит prototype, а вызвается как Autowired в BillingController - поменял это, но проверить! TODO!
@Slf4j
public class BillServ {

	@Autowired
	private KartMng kartMng;
	@Autowired
	private KartDAO kartDao;
	@Autowired
	private ApplicationContext ctx;
	@PersistenceContext
	private EntityManager em;
	@Autowired
	private HouseMng houseMng;

	// конструктор
	public BillServ() {

	}

	// получить список N следующих объектов, для расчета в потоках
	private List<ResultSet> getNextItem(int cnt, List<ResultSet> lst) {
		List<ResultSet> lstRet = new ArrayList<ResultSet>();
		int i = 1;
		Iterator<ResultSet> itr = lst.iterator();
		while (itr.hasNext()) {
			ResultSet rs = itr.next();
			lstRet.add(rs);
			itr.remove();
			i++;
			if (i > cnt) {
				break;
			}
		}

		return lstRet;
	}

	// получить следующий объект, для расчета в потоках
	private ResultSet getNextItem(List<ResultSet> lst) {
		Iterator<ResultSet> itr = lst.iterator();
		ResultSet item = null;
		if (itr.hasNext()) {
			item  = itr.next();
			itr.remove();
		}

		return item;
	}

	// Exception из потока
	Thread.UncaughtExceptionHandler expThr = new Thread.UncaughtExceptionHandler() {
		@Override
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
	 * @throws ExecutionException
	 * @throws InterruptedException
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
	public Future<Result> chrgAll(RequestConfig reqConfig, Integer houseId, Integer areaId, Integer tempLskId) throws InterruptedException, ExecutionException {
		// Logger.getLogger("org.hibernate.SQL").setLevel(Level.DEBUG);
		// Logger.getLogger("org.hibernate.type").setLevel(Level.TRACE);
		Result res = new Result();
		res.setErr(0);
		AsyncResult<Result> futM = new AsyncResult<Result>(res);

		// кол-во потоков
		int cntThreads;

		// РАСПРЕДЕЛЕНИЕ ОБЪЕМОВ, если задано
		if (reqConfig.getIsDist()) {
			cntThreads = 10;
			// загрузить все необходимые Дома
			List<ResultSet> lstItem = houseMng.findAll2(houseId, areaId, tempLskId, reqConfig.getCurDt1(), reqConfig.getCurDt2()).stream()
					.map(t-> new ResultSet(t.getId())).collect(Collectors.toList());
			log.info("Список House.id для Начисления:");
			lstItem.stream().forEach(t-> {
				log.info("House.id={}", t.getId());
			});
			try {
				log.info("BillServ.chrgAll: Распределение по заданным домам начато!");
				invokeThreads(reqConfig, cntThreads, lstItem, 1);
				log.info("BillServ.chrgAll: Распределение по заданным домам выполнено!");
			} catch (ErrorWhileChrg e) {
				log.info("НЕОБРАБОТАННАЯ ОШИБКА В ПОТОКЕ!");
			} catch (CantLock e) {
				// ошибка блокировки
				log.error(Utl.getStackTraceString(e));
				res.setErr(2);
			} catch (ErrorWhileDist e) {
				log.error("Ошибка при распределении объемов по дому house.id={}", houseId);
				res.setErr(1);
			}
		}

		// РАСЧЕТ НАЧИСЛЕНИЯ ПО ЛС В ПОТОКАХ
		if (res.getErr() ==0) {
			cntThreads = 20;
			// загрузить все необходимые Лиц.счета
			List<ResultSet> lstItem = kartDao.findAllLsk(houseId, areaId, tempLskId, reqConfig.getCurDt1(), reqConfig.getCurDt2());

/*			lstItem.stream().forEach(t-> {
				log.info("Для начисления: lsk={}", t.getId());
			});
*/
			try {
				log.info("BillServ.chrgAll: Начисление по заданным домам начато!");
				invokeThreads(reqConfig, cntThreads, lstItem, 0);
				log.info("BillServ.chrgAll: Начисление по заданным домам выполнено!");
			} catch (ErrorWhileDist | ErrorWhileChrg e) {
				log.info("НЕОБРАБОТАННАЯ ОШИБКА В ПОТОКЕ!");
			} catch (CantLock e) {
				// ошибка блокировки
				log.error(Utl.getStackTraceString(e));
				res.setErr(2);
			}
		}
		return futM;
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
		Result res = new Result();
		Future<Result> fut = new AsyncResult<Result>(res);

		ChrgServThr chrgServThr = ctx.getBean(ChrgServThr.class);
		DistServ distServ = ctx.getBean(DistServ.class);

		// признак распределения по дому (в случае перерасчета по Отоплению)
		boolean isDistHouse = false;

		res.setErr(0);
		// Если был передан идентификатор лицевого, то найти лиц.счет
		if (lsk != null) {
			kart = em.find(Kart.class, lsk);
			if (kart == null) {
				log.error("УКАЗАННЫЙ ЛИЦЕВОЙ СЧЕТ={} НЕ НАЙДЕН!", lsk);
				res.setErr(1);
				return fut;
			}
		}
		Calc calc = new Calc(reqConfig);

		// установить дом и счет
		calc.setHouse(kart.getKw().getHouse());
		calc.setKart(kart);

		// установить признак распределения объема по дому, если перерасчет по Отоплению
		if (reqConfig.getOperTp().equals(1) && !reqConfig.getChng().getTp().getCd().equals("Изменение расценки (тарифа)") // если не пересчет расценки
				) { // кроме изменения расценки
			List<String> lstServCd = reqConfig.getChng().getChngLsk().stream()
					.filter(t -> t.getKart().getLsk().equals(lsk))
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
				List<ResultSet> lstItem = houseMng.findAll2(calc.getHouse().getId(), null, null, reqConfig.getCurDt1(), reqConfig.getCurDt2()).stream()
						.map(t-> new ResultSet(t.getId())).collect(Collectors.toList());
				try {
					invokeThreads(reqConfig, 1, lstItem, 1);
				} catch (ErrorWhileChrg e) {
					log.info("НЕОБРАБОТАННАЯ ОШИБКА В ПОТОКЕ!");
				}

				// присвоить обратно лиц.счет, который мог быть занулён в предыдущ методах
				calc.setKart(kart);
			} else if (reqConfig.getIsDist()) {
				distServ.distKartVol(calc);
				// присвоить обратно лиц.счет, который мог быть занулён в предыдущ методах
				calc.setKart(kart);
			}
		} catch (ErrorWhileDist | ExecutionException e) {
			// ошибка
			log.error(Utl.getStackTraceString(e));
			res.setErr(1);
			return fut;
		} catch (CantLock e) {
			// ошибка блокировки
			log.error(Utl.getStackTraceString(e));
			res.setErr(2);
			return fut;
		}

		// РАСЧЕТ НАЧИСЛЕНИЯ ПО 1 ЛС
		try {
			fut = chrgServThr.chrgAndSaveLsk(reqConfig, kart.getLsk());
		} catch (ErrorWhileChrg | ExecutionException e) {
			// ошибка
			e.printStackTrace();
			res.setErr(1);
			return fut;
		} catch (CantLock e) {
			// ошибка блокировки
			log.error(Utl.getStackTraceString(e));
			res.setErr(2);
		}

		return fut;
	}


	/**
	 * Вызвать выполнение потоков распределения объемов/ начисления
	 * @param reqConfig - конфиг запроса
	 * @param cntThreads - кол-во потоков
	 * @param lstItem - список Id на обработку
	 * @param tp - тип 0 - начисление, 1 - распределение
	 * @throws ErrorWhileDist
	 * @throws ErrorWhileChrg
	 * @throws ExecutionException
	 * @throws CantLock
	 */
	private void invokeThreads(RequestConfig reqConfig, int cntThreads, List<ResultSet> lstItem, int tp) throws ErrorWhileDist, ErrorWhileChrg, ExecutionException, CantLock {
		long startTime = System.currentTimeMillis();

		List<Future<Result>> frl = new ArrayList<Future<Result>>(cntThreads);
		for (int i=1; i<=cntThreads; i++) {
			frl.add(null);
		}
		// проверить окончание всех потоков и запуск новых потоков
		ResultSet itemWork = null;
		boolean isStop = false;
		while (!isStop) {
			//log.info("========================================== Ожидание выполнения потоков ===========");
			Future<Result> fut;
			int i=0;
			// флаг наличия потоков
			isStop = true;
			for (Iterator<Future<Result>> itr = frl.iterator(); itr.hasNext();) {

/*				frl.stream().forEach(t -> {
					log.info("frl isDone={}, val={}", t!=null?t.isDone():null, t);
				});
*/				fut = itr.next();

				if (fut == null) {
					// получить новый объект
					itemWork = getNextItem(lstItem);
					if (itemWork != null) {
						// создать новый поток
						if (tp==0) {
							ChrgServThr chrgServThr = ctx.getBean(ChrgServThr.class);
							fut = chrgServThr.chrgAndSaveLsk(reqConfig, itemWork.getId());
							log.info("================================ Начат поток начисления для лс={} ==================", itemWork.getId());
						} else if (tp==1) {
							DistServ distServ = ctx.getBean(DistServ.class);
							fut = distServ.distHouseVol(reqConfig, itemWork.getId());
							log.info("================================ Начат поток распределения объемов для house.id={} ==================", itemWork.getId());
						}
						frl.set(i, fut);
						// не завершен поток
						//isStop = false;
					}
				} else if (!fut.isDone()) {
					// не завершен поток
					//isStop = false;
					//log.info("========= Поток НЕ завершен! лс={}", fut.get().getLsk());
					//log.info("..................................... CHK1");
				} else {
					//log.info("------------------------------------- CHK2");
					try {
						if (fut.get().getErr() == 1) {
						  //log.error("ОШИБКА ПОЛУЧЕНА ПОСЛЕ ЗАВЕРШЕНИЯ ПОТОКА!");
							if (tp==0) {
								log.error("================================ ОШИБКА ПОЛУЧЕНА ПОСЛЕ ЗАВЕРШЕНИЯ ПОТОКА начисления для лс={} ==================", fut.get().getItemId());
							} else if (tp==1) {
								log.error("================================ ОШИБКА ПОЛУЧЕНА ПОСЛЕ ЗАВЕРШЕНИЯ ПОТОКА распределения объемов для house.id={} ==================", fut.get().getItemId());
							}
						} else {
							if (tp==0) {
								log.info("================================ Успешно завершен поток начисления для лс={} ==================", fut.get().getItemId());
							} else if (tp==1) {
								log.info("================================ Успешно завершен поток распределения объемов для house.id={} ==================", fut.get().getItemId());
							}
						}
					} catch (InterruptedException | ExecutionException e1) {
						e1.printStackTrace();
						log.error("ОШИБКА ВО ВРЕМЯ ВЫПОЛНЕНИЯ ПОТОКА!", e1);
					} finally {
						// очистить переменную потока
						frl.set(i, null);
					}

				}

				if (fut !=null) {
					// не завершен поток TODO ПРОВЕРИТЬ! 27.05.2018
					isStop = false;
				}
				i++;
			}

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		if (lstItem.size() > 0) {
			log.info("Итоговое время выполнения одного {} cnt={}, мс.",
					tp==0?"лиц.счета":"дома", totalTime / lstItem.size());
		}
	}

	/**
	 * ТЕСТ-вызов не удалять!
	 * @param id
	 * @return
	 */
	@Async
	public Future<Result> chrgTest(Integer id) {
		Result res = new Result();
		Future<Result> fut = new AsyncResult<Result>(res);
		DistServ distServ = ctx.getBean(DistServ.class);
		distServ.distTest(id);
		log.info("======chrgTest");

		return fut;
	}
}
