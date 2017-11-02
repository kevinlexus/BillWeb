package com.ric.bill;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ric.bill.excp.EmptyStorable;
import com.ric.bill.excp.ErrorWhileChrg;
import com.ric.bill.mm.HouseMng;
import com.ric.bill.mm.KartMng;
import com.ric.bill.mm.LstMng;
import com.ric.bill.mm.MeterLogMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.mm.ServMng;
import com.ric.bill.mm.TarifMng;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.bs.Org;
import com.ric.bill.model.fn.Chrg;
import com.ric.bill.model.tr.Serv;

/**
 * Сервис формирования начисления
 * @author lev
 *
 */
@Service
@Scope("prototype")
@Slf4j
public class ChrgServ {

	@Autowired
	private ParMng parMng;
	@Autowired
	private Config config;
	@Autowired
	private ServMng servMng;
	@Autowired
	private TarifMng tarMng;
	@Autowired
	private HouseMng houseMng;
	@Autowired
	private KartMng kartMng;
	@Autowired
	private MeterLogMng metMng;
	@Autowired
	private LstMng lstMng;
	
	@Autowired
	private ApplicationContext ctx;
	
	//EntityManager - EM нужен на каждый DAO или сервис свой!
    @PersistenceContext
    private EntityManager em;

    //вспомогательные коллекции
    private List<Chrg> prepChrg;
    private List<ChrgMainServRec> prepChrgMainServ;
    
    private HashMap<Serv, BigDecimal> mapServ;
    private HashMap<Serv, BigDecimal> mapVrt;
    //коллекция для формирования потоков
    private List<Serv> servThr;
    
    //флаг ошибки, произошедшей в потоке
    private static Boolean errThread;
    private Calc calc;
    
	// текущий уровень очереди
    Integer servLevel;
    
	//конструктор
    public ChrgServ() {
    	super();
    }

    Map<Serv, Integer> queBatch; // ключ - услуга, value - уровень
    
    //внутренний класс, контроля
    private class Control {
		Integer orgId;
    	Integer servId;
    	
    	@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((orgId == null) ? 0 : orgId.hashCode());
			result = prime * result
					+ ((servId == null) ? 0 : servId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Control)) {
				return false;
			}
			Control other = (Control) obj;
			if (orgId == null) {
				if (other.orgId != null) {
					return false;
				}
			} else if (!orgId.equals(other.orgId)) {
				return false;
			}
			if (servId == null) {
				if (other.servId != null) {
					return false;
				}
			} else if (!servId.equals(other.servId)) {
				return false;
			}
			return true;
		}

		public Control(Integer servId, Integer orgId) {
    		this.orgId=orgId;
    		this.servId=servId;
    		
    		
		}

    }

    /**
     * получить список N следующих услуг, для расчета в потоках
     * что такое уровни? они необходимы, чтобы услуги вызывались последовательно 
     * по зависимости друг от друга, эта зависимость прописывается в serv.fk_dep
     * @param cnt // кол-во услуг
     */
    private List<Serv> getNextServ(int cnt) {
    	List<Serv> lst;
    	while (true) {
    		// поискать на текущем уровне
    		lst = getNextServByLevel(servLevel, cnt);
			if (lst.size() !=0) {
				return lst;
			} else {
				// увеличить уровень на +1, попробовать поискать услуги
				servLevel++;
	    		lst = getNextServByLevel(servLevel, cnt);
	    		return lst;
			}
    	}
    	
    }
    
    /**
     * получить список N следующих услуг, по уровню, для расчета в потоках 
     * @param level // уровень
     * @param cnt // кол-во услуг
     */
    private List<Serv> getNextServByLevel(int level, int cnt) {
    	List<Serv> lst = new ArrayList<Serv>(); 
		int i=1;
    	
		Iterator<Entry<Serv, Integer>> itr = queBatch.entrySet().iterator();
		while (itr.hasNext()) {
			Entry<Serv, Integer> entry =  itr.next();
			if (entry.getValue().equals(level)) {
				// добавить услугу по соответствующему уровню
				lst.add(entry.getKey());
	    		itr.remove();
				//log.info("entrySet.serv.cd = {}, level={}", entry.getKey().getCd(), entry.getValue());
	    		i++;
	    		if (i > cnt) {
					break;
				}
			}
		}
		
		return lst;
    }
    
    
    /**
     * заполнить пачки услуг
     */
    private void setQueBatch() {
    	Integer level=0, old=-1;
    	while (true) {
    		if (addQueBatch(level, old) != 0){
        		level++; 
        		old++;
    		} else {
    			break;
    		}
    	}
    }

    /**
     * заполнить пачки услуг по уровню
     * @param level - текущий уровень
     * @param old - предыдущий уровень
     * @return - вернуть кол-во услуг добавленных
     */
    private Integer addQueBatch(Integer level, Integer old) {
    	Integer len;
    	if (level==0) {
     		// 0 уровень - найти независящие ни от каких услуг услуги))
			Map<Serv, Integer> lst = servThr.parallelStream().filter(t -> t.getServDep() == null).collect(Collectors.toMap( (t) -> t , (t) -> level));
			queBatch.putAll(lst);
			len=lst.size();
    	} else {
    		// остальные итерации - зависимые услуги от уровня отстающего на -1
       		// отфильтровать по значению уровня -1, по родительской услуге, записать новый список 
			Map<Serv, Integer> lst = servThr.parallelStream().filter(t -> queBatch.get(t.getServDep())!=null && queBatch.get(t.getServDep()).equals(old) 
							).collect(Collectors.toMap( (t) -> t , (t) -> level));
			queBatch.putAll(lst);
			len=lst.size();
    	}
    	return len;
    }
    
    /**
	 * выполнить расчет начисления по лиц.счету
	 * @param kart - объект лиц.счета
	 * @throws ErrorWhileChrg 
	 */
	public Result chrgLsk(Calc calc) throws ErrorWhileChrg, ExecutionException {
    	this.calc=calc;
		log.info("Начисление по лс="+calc.getKart().getLsk());
		Result res = new Result();
		res.setErr(0);

		prepChrg = new ArrayList<Chrg>(100); 
		prepChrgMainServ = new ArrayList<ChrgMainServRec>(100);
		// создать очередь
		queBatch = new HashMap<Serv, Integer>(0);
		
		//для виртуальной услуги	
		mapServ = new HashMap<Serv, BigDecimal>(100);  
		mapVrt = new HashMap<Serv, BigDecimal>(100);  

		//найти все услуги, действительные в лиц.счете
		//и создать потоки по кол-ву услуг
		
		//загрузить все услуги по данному л.с.
		servThr = kartMng.getServAll(calc.getReqConfig().getRqn(), calc);
		
		//servThr.stream().forEach(t-> log.info("serv.id={}", t.getId() ));
		
		// сбросить уровень
		servLevel=0;
		// создать список обрабатываемых услуг, с очередями
		setQueBatch();
		
		errThread=false;

		ChrgThr chrgThr = ctx.getBean(ChrgThr.class);
		
		while (true) {
			log.trace("ChrgServ: Loading servs for threads");
			//получить следующие N услуг, рассчитать их в потоке
			List<Serv> servWork = getNextServ(1); // ограничил 1 потоком (подозрение на нехватку памяти в JVM при начислении многих лс) 

			if (servWork.size()==0) {
				//выйти, если все услуги обработаны
				break;
			}

			// РАСЧЕТ услуг в потоке
			for (Serv serv : servWork) {
 					chrgThr.setUp(calc, serv, mapServ, mapVrt, prepChrg, prepChrgMainServ);
			    	try {
						chrgThr.run1();
					} catch (EmptyStorable e) {
						e.printStackTrace();
						throw new ErrorWhileChrg ("ОШИБКА! Была попытка получить параметр по пустому объекту хранения");
					}
			    	// добавить некритические ошибки выполнения 
					res.getLstErr().addAll(res.getLstErr());
			}
		}
		
//		log.info("err SIZE={}", res.getLstErr().size());

		// Если была ошибка в потоке - приостановить выполнение, выйти
		if (errThread) {
			log.info("ChrgServ.chrgLsk: Error in thread, exiting!", 2);
			res.setErr(1);
			return res;
		}
		
		// КОРРЕКЦИЯ на сумму разности между основной и виртуальной услугой
		for (Map.Entry<Serv, BigDecimal> entryVrt : mapVrt.entrySet()) {
		    Serv servVrt = entryVrt.getKey();
			//найти сумму, для сравнения, полученную от основных услуг
		    for (Map.Entry<Serv, BigDecimal> entryServ : mapServ.entrySet()) {
				if (entryServ.getKey().equals(servVrt)) {
					BigDecimal diff = entryVrt.getValue().subtract(entryServ.getValue());
					if (diff.compareTo(BigDecimal.ZERO) != 0) {
						//существует разница, найти услугу, куда кинуть округление
						Serv servRound = servVrt.getServRound(); 
						//найти только что добавленные строки начисления, и вписать в одну из них
						boolean flag = false; //флаг, чтобы больше не корректировать, если уже раз найдено
						for (Chrg chrg : prepChrg) {
							if (!flag && chrg.getStatus() == 1 && chrg.getServ().equals(servRound)) {
								flag = true;
								chrg.setSumAmnt(BigDecimal.valueOf(chrg.getSumAmnt()).add(diff).doubleValue()) ;
								chrg.setSumFull(BigDecimal.valueOf(chrg.getSumFull()).add(diff).doubleValue()) ;
							}
						}
					}
				}
			}		    
		}		

		// проверить коллекцию
/*		prepChrg.stream().forEach(t->{
			log.info("*************Check: serv.cd={}, vol={}, area={}, cntPers={}, summa={}, price={}", 
					t.getServ().getCd(), t.getVol(), t.getArea(), t.getCntFact(), t.getSumAmnt(), t.getPrice());
		});*/
		
		return res;
	}


	/**
	 * перенести в архив предыдущее и сохранить новое начисление
	 * @param lsk - лиц.счет передавать строкой!
	 * @throws ErrorWhileChrg 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	public void save (Integer lsk) throws ErrorWhileChrg {
		Utl.logger(false, 28, -1, -1); //###

		long beginTime = System.currentTimeMillis();

		Integer status;
		if (calc.getReqConfig().getOperTp().equals(1)) {
			// перерасчет
			status=3;
		} else {
			// начисление
			status=1;
		}
		
		Session session = em.unwrap(Session.class);
		Filter filter = session.enableFilter("FILTER_CHRG1");
		
		session.enableFilter("FILTER_CHRG1").setParameter("PERIOD", calc.getReqConfig().getPeriod())
		   .setParameter("STATUS", 1);
		
		//коллекция для сумм по укрупнённым услугам, для нового начисления 
	    MultiKeyMap mapDeb = new MultiKeyMap();

		Kart kart = em.find(Kart.class, lsk); //здесь так, иначе записи не прикрепятся к объекту не из этой сессии!
		
		long endTime1=System.currentTimeMillis()-beginTime;
		beginTime = System.currentTimeMillis();
		
		Utl.logger(false, 29, -1, -1); //###
		
		//ДЕЛЬТА
		//ПОДГОТОВИТЬСЯ для сохранения дельты
		//сгруппировать до укрупнённых услуг текущий расчет по debt
		for (Chrg chrg : prepChrg) { 
			Serv servMain = null;
			try {
				servMain = servMng.getUpper(chrg.getServ(), "serv_tree_kassa");
			}catch(Exception e) {
			    e.printStackTrace();
				throw new ErrorWhileChrg("ChrgServ.save: ChrgThr: ErrorWhileChrg");
			}
			//Сохранить сумму по укрупнённой услуге, для расчета дельты для debt
			putSumDeb(mapDeb, servMain, chrg.getOrg(), BigDecimal.valueOf(chrg.getSumAmnt()));
		}

		Utl.logger(false, 30, -1, -1); //###

		beginTime = System.currentTimeMillis();

		MultiKeyMap mapDebLogBefore = new MultiKeyMap(); // логгинг
		mapDebLogBefore.putAll(mapDeb);
		
		//сгруппировать до укрупнённых услуг предыдущий расчет по debt
		for (Chrg chrg : kart.getChrg()) {
			//Только необходимые строки
			if (chrg.getStatus().equals(1) && chrg.getPeriod().equals(calc.getReqConfig().getPeriod())) {
				Serv servMain = null;
				try {
					servMain = servMng.getUpper(chrg.getServ(), "serv_tree_kassa");
				} catch(Exception e) {
				    e.printStackTrace();
					throw new ErrorWhileChrg("ChrgServ.save: ChrgThr: ErrorWhileChrg");
				}
				//Вычесть сумму по укрупнённой услуге из нового начисления, для расчета дельты для debt
				putSumDeb(mapDeb, servMain, chrg.getOrg(), BigDecimal.valueOf(-1d * Utl.nvl(chrg.getSumAmnt(), 0d)));
			}
		}
		Utl.logger(false, 31, -1, -1); //###
		
		MultiKeyMap mapDebLogAfter = new MultiKeyMap(); // логгинг
		mapDebLogAfter.putAll(mapDeb);

		long endTime3=System.currentTimeMillis()-beginTime;
		beginTime = System.currentTimeMillis();
		
		//перенести предыдущий расчет начисления в статус "архив" (1->0)
		Query query = null;

		if (calc.getReqConfig().getOperTp().equals(0)) {
			// начисление
			query = em.createNativeQuery("update fn.chrg t set t.status=0 where t.lsk=:lsk "
					+ "and t.status=1 "
					//+ "and t.dt1 between :dt1 and :dt2 " -нет смысла, есть period
					//+ "and t.dt2 between :dt1 and :dt2 "
					+ "and t.period=:period"
					);
		} else if (calc.getReqConfig().getOperTp().equals(1)) {
			// перерасчет
			query = em.createNativeQuery("delete from fn.chrg t where t.lsk=:lsk "
					+ "and t.status=3 "
					+ "and t.period=:period "
					+ "and t.fk_chng=:chngId "
					);
			query.setParameter("chngId", calc.getReqConfig().getChng().getId());
		}
		query.setParameter("lsk", kart.getLsk());
		query.setParameter("period", calc.getReqConfig().getPeriod());
		query.executeUpdate();
		Utl.logger(false, 32, -1, -1); //###
		
		long endTime4=System.currentTimeMillis()-beginTime;
		beginTime = System.currentTimeMillis();
		
		//ДЕЛЬТА
		//НАЙТИ и передать дельту в функцию долгов (выполнить только в начислении)
		if (calc.getReqConfig().getOperTp().equals(0)) {
			Set<Control> ctrlSet = new HashSet();
			MapIterator it = mapDeb.mapIterator();
			MultiKey mk;
			BigDecimal val;
			Boolean flag = false;
			while (it.hasNext()) {
				it.next();
				mk = (MultiKey) it.getKey();
				//log.trace("Проверка дельты: serv="+mk.getKey(0)+" org="+mk.getKey(1)+" sum="+it.getValue(),2);
				val = (BigDecimal)it.getValue();
				if (!(val.compareTo(BigDecimal.ZERO)==0)) {
				  //проверка на дубли
				  if (ctrlSet.contains(new Control(((Serv) mk.getKey(0)).getId(), ((Org) mk.getKey(1)).getId()))) {
						throw new ErrorWhileChrg("ChrgServ.save: Found dublicate elements while sending delta");
				  }
				  //вызвать хранимую функцию, для пересчёта долга
				  StoredProcedureQuery qr = em.createStoredProcedureQuery("fn.transfer_change");
				  qr.registerStoredProcedureParameter("P_LSK", Integer.class, ParameterMode.IN);
				  qr.registerStoredProcedureParameter("P_FK_SERV", Integer.class, ParameterMode.IN);
				  qr.registerStoredProcedureParameter("P_FK_ORG", Integer.class, ParameterMode.IN);
				  qr.registerStoredProcedureParameter("P_PERIOD", String.class, ParameterMode.IN);
				  qr.registerStoredProcedureParameter("P_SUMMA_CHNG", Double.class, ParameterMode.IN);
				  qr.registerStoredProcedureParameter("P_TP_CHNG", Integer.class, ParameterMode.IN);
				  qr.registerStoredProcedureParameter("P_FK_CHNG", Integer.class, ParameterMode.IN);
				  qr.setParameter("P_LSK", kart.getLsk());
				  qr.setParameter("P_FK_SERV", ((Serv) mk.getKey(0)).getId());
				  qr.setParameter("P_FK_ORG", ((Org) mk.getKey(1)).getId());
				  qr.setParameter("P_PERIOD", calc.getReqConfig().getPeriod());
				  qr.setParameter("P_SUMMA_CHNG", val.doubleValue());
				  qr.setParameter("P_TP_CHNG", 1);
				  qr.setParameter("P_FK_CHNG", 1); // TODO Передавать Диману итерацию расчета (придумать и сделать)
				  
				  qr.execute();

				  log.info("*** ОТПРАВКА ДЕЛЬТЫ ***: RQN={}, Lsk={} ,serv.id={}, serv.name={}, org.id={}, org.name={}, period={}, sum={}",
						  calc.getReqConfig().getRqn(), lsk, ((Serv) mk.getKey(0)).getId(), ((Serv) mk.getKey(0)).getName(),
				          ((Org) mk.getKey(1)).getId(), ((Org) mk.getKey(1)).getName(), calc.getReqConfig().getPeriod(), 
				           val.doubleValue());

				  flag = true;
				}
			}
			
			if (flag) {
				// ЛОГГИНГ
				log.info("");

				kart.getChrg().stream().forEach(t -> log.info("*** ЛОГГИНГ kart.getChrg()  ***: RQN={}, lsk={}, id={}, serv.id={}, org.id={}, sum={}",
						calc.getReqConfig().getRqn(), kart.getLsk(), t.getId(), t.getServ().getId(), t.getOrg().getId(), t.getSumAmnt()) 
						);
				log.info("");
				
				prepChrg.stream().forEach(t -> 		 log.info("*** ЛОГГИНГ prepChrg        ***: RQN={}, lsk={}, id={}, serv.id={}, org.id={}, sum={}",
						calc.getReqConfig().getRqn(), kart.getLsk(), t.getId(), t.getServ().getId(), t.getOrg().getId(), t.getSumAmnt()) 
						);
				
				log.info("");
				
				it = mapDebLogBefore.mapIterator();
				while (it.hasNext()) {
					it.next();
					mk = (MultiKey) it.getKey();
					val = (BigDecimal)it.getValue();
					log.info("*** ЛОГГИНГ mapDeb Before ***: RQN={}, lsk={}, serv.id={}, org.id={}, sum={}",
						  calc.getReqConfig().getRqn(), kart.getLsk(), ((Serv) mk.getKey(0)).getId(), ((Org) mk.getKey(1)).getId(),  
							val.doubleValue());
				}

				log.info("");
				
				it = mapDebLogAfter.mapIterator();
				while (it.hasNext()) {
					it.next();
					mk = (MultiKey) it.getKey();
					val = (BigDecimal)it.getValue();
					log.info("*** ЛОГГИНГ mapDeb After  ***: RQN={}, lsk={}, serv.id={}, org.id={}, sum={}",
						  calc.getReqConfig().getRqn(), kart.getLsk(), ((Serv) mk.getKey(0)).getId(), ((Org) mk.getKey(1)).getId(),  
							val.doubleValue());
				}
				
			}
			
		}
		Utl.logger(false, 33, -1, -1); //###

		long endTime5=System.currentTimeMillis()-beginTime;
		beginTime = System.currentTimeMillis();

		//Сохранить новое начисление (переписать из prepChrg)
		for (Chrg chrg : prepChrg) {
			Chrg chrg2 = new Chrg(kart, chrg.getServ(), chrg.getOrg(), status, calc.getReqConfig().getPeriod(), chrg.getSumFull(), chrg.getSumAmnt(), 
					chrg.getVol(), chrg.getPrice(), chrg.getStdt(), chrg.getCntFact(), chrg.getArea(),  chrg.getTp(), chrg.getDt1(), chrg.getDt2(), 
					chrg.getMet(), chrg.getEntry(), calc.getReqConfig().getChng(), chrg.getCntOwn()); 

			kart.getChrg().add(chrg2); 
		}
		Utl.logger(false, 34, -1, -1); //###

		// Почистить коллекции
	    prepChrg=null;
	    prepChrgMainServ=null;
	    mapServ=null;
	    mapVrt=null;
	    servThr=null;
		
		long endTime6=System.currentTimeMillis()-beginTime;
	}
	

	
	/**
	 * сохранить запись о сумме, предназаначенной для сохранения дельты в долгах 
	 * @param serv - услуга
	 * @param sum - сумма
	 */
	private /*synchronized */void putSumDeb(MultiKeyMap mkMap, Serv serv, Org org, BigDecimal sum) {
		BigDecimal s = (BigDecimal) mkMap.get(serv, org);
		if (s != null) {
		  s=s.add(sum);
		} else {
		  s = sum;
		}
	    //записать в элемент массива
		mkMap.put(serv, org, s);
	}


}


