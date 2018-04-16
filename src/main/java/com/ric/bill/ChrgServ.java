package com.ric.bill;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
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

import com.ric.bill.dto.ChrgRec;
import com.ric.bill.dto.PrivRec;
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
import com.ric.bill.model.bs.Lst;
import com.ric.bill.model.bs.Org;
import com.ric.bill.model.fn.Chng;
import com.ric.bill.model.fn.Chrg;
import com.ric.bill.model.fn.PersPrivilege;
import com.ric.bill.model.fn.PrivilegeChrg;
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
	private LstMng lstMng;
	@Autowired
	private KartMng kartMng;
	@Autowired
	private ApplicationContext ctx;
	//EntityManager - EM нужен на каждый DAO или сервис свой!
    @PersistenceContext
    private EntityManager em;
    //коллекция для формирования потоков
    private List<Serv> servThr;
    private Calc calc;
    
	// текущий уровень очереди
    Integer servLevel;
    
    // хранилище коллекций
    ChrgStore chStore;
	//конструктор
    public ChrgServ() {
    	super();
		chStore = new ChrgStore();
    }

    Map<Serv, Integer> queBatch; // ключ - услуга, value - уровень

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
			Map<Serv, Integer> lst = servThr.stream().filter(t -> t.getServDep() == null).collect(Collectors.toMap( (t) -> t , (t) -> level));
			queBatch.putAll(lst);
			len=lst.size();
    	} else {
    		// остальные итерации - зависимые услуги от уровня отстающего на -1
       		// отфильтровать по значению уровня -1, по родительской услуге, записать новый список 
			Map<Serv, Integer> lst = servThr.stream().filter(t -> queBatch.get(t.getServDep())!=null && queBatch.get(t.getServDep()).equals(old) 
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
		log.info("RQN={}, Начисление по лс={}", calc.getReqConfig().getRqn(), calc.getKart().getLsk());
		Result res = new Result();
		res.setErr(0);
		res.setLsk(calc.getKart().getLsk());

		// создать очередь
		queBatch = new HashMap<Serv, Integer>(0);
		
		//найти все услуги, действительные в лиц.счете
		//и создать потоки по кол-ву услуг
		
		//загрузить все услуги по данному л.с.
		servThr = kartMng.getServAll(calc.getReqConfig().getRqn(), calc);
		
		// сбросить уровень
		servLevel=0;
		// создать список обрабатываемых услуг, с очередями
		setQueBatch();
		
		ChrgThr chrgThr = ctx.getBean(ChrgThr.class);
		
		while (true) {
			//log.trace("ChrgServ: Loading servs for threads");
			//получить следующие N услуг, рассчитать их в потоке
			List<Serv> servWork = getNextServ(1); // ограничил 1 потоком (подозрение на нехватку памяти в JVM при начислении многих лс) 

			if (servWork.size()==0) {
				//выйти, если все услуги обработаны
				break;
			}
			
			// РАСЧЕТ услуг по циклу
			for (Serv serv : servWork) {
			    //log.info("RQN={}, Начисление по лс={}, услуге serv.id={} начато!", calc.getReqConfig().getRqn(), 
			    //		calc.getKart().getLsk(), serv.getId());

//			    log.info("--------1CHECK: serv.id={} size={}", serv.getId(), chStore.getStoreMainServ().size());

			    // очистить вспомогательные коллекции
			    chStore.init();
			    // инциализировать модуль начисления
			    chrgThr.setUp(calc, serv, chStore);

			    Result res1;
			    // выполнить начисление
				try {
					res1 = chrgThr.run1();
				} catch (EmptyStorable e) {
					e.printStackTrace();
					throw new ErrorWhileChrg ("ОШИБКА! Была попытка получить параметр по пустому объекту хранения! lsk="+calc.getKart().getLsk());
				} catch (Exception e) {
					e.printStackTrace();
					log.error("ОШИБКА при вызове начисления по услуге! Прочие ошибки! serv.cd="+serv.getCd()+" lsk="+calc.getKart().getLsk(), e);
					throw new ErrorWhileChrg ("ОШИБКА при вызове начисления по услуге! Прочие ошибки! serv.cd="+serv.getCd()+" lsk="+calc.getKart().getLsk());
				}
		    	
		    	// добавить некритические ошибки выполнения 
				res.getLstErr().addAll(res1.getLstErr());
			}
		}
		
		// КОРРЕКЦИЯ на сумму разности между основной и виртуальной услугой
		for (Map.Entry<Serv, BigDecimal> entryVrt : chStore.getMapVrt().entrySet()) {
		    Serv servVrt = entryVrt.getKey();
			//найти сумму, для сравнения, полученную от основных услуг
		    for (Map.Entry<Serv, BigDecimal> entryServ : chStore.getMapServ().entrySet()) {
				if (entryServ.getKey().equals(servVrt)) {
					BigDecimal diff = entryVrt.getValue().subtract(entryServ.getValue());
					if (diff.compareTo(BigDecimal.ZERO) != 0) {
						//существует разница, найти услугу, куда кинуть округление
						Serv servRound = servVrt.getServRound(); 
						//найти только что добавленные строки начисления, и вписать в одну из них
						boolean flag = false; //флаг, чтобы больше не корректировать, если уже раз найдено
						for (ChrgRec chrg : chStore.getPrepChrg()) {
							if (!flag /*&& chrg.getStatus() == 1*/ && chrg.getServ().equals(servRound)) {
								flag = true;
								chrg.setSumAmnt(chrg.getSumAmnt().add(diff)) ;
								chrg.setSumFull(chrg.getSumFull().add(diff)) ;
							}
						}
					}
				}
			}		    
		}		
		chrgThr = null; // ### TODO
		return res;
	}


	/**
	 * перенести в архив предыдущее и сохранить новое начисление
	 * @param lsk - лиц.счет передавать строкой!
	 * @throws ErrorWhileChrg 
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
	public void save (Integer lsk) throws ErrorWhileChrg {
		Integer status;
		if (calc.getReqConfig().getOperTp().equals(1)) {
			// перерасчет
			status=3;
		} else {
			// начисление
			status=1;
		}
		
		Session session = em.unwrap(Session.class);
		
		session.enableFilter("FILTER_CHRG1").setParameter("PERIOD", calc.getReqConfig().getPeriod())
		   .setParameter("STATUS", 1);
		Kart kart = em.find(Kart.class, lsk); //здесь так, иначе записи не прикрепятся к объекту не из этой сессии!
		
		Query query1 = null, query2 = null;
		//перенести предыдущий расчет начисления в статус "архив" (1->0)
		if (calc.getReqConfig().getOperTp().equals(0)) {
			// начисление
			query1 = em.createNativeQuery("update fn.chrg t set t.status=0 where t.lsk=:lsk "
					+ "and t.status=1 "
					+ "and t.period=:period"
					);
			// возмещение по льготе
			query2 = em.createNativeQuery("update fn.privilege_chrg t set t.status=0 where t.lsk=:lsk "
					+ "and t.status=1 "
					+ "and t.period=:period"
					);
		} else if (calc.getReqConfig().getOperTp().equals(1)) {
			// Перерасчет
			// начисление
			query1 = em.createNativeQuery("delete from fn.chrg t where t.lsk=:lsk "
					+ "and t.status=3 "
					+ "and t.period=:period "
					+ "and t.fk_chng=:chngId"
					);
			query1.setParameter("chngId", calc.getReqConfig().getChng().getId());
			// возмещение по льготе
			query2 = em.createNativeQuery("update fn.privilege_chrg t set t.status=0 where t.lsk=:lsk "
					+ "and t.status=3 "
					+ "and t.period=:period "
					+ "and t.fk_chng=:chngId"
					);
			query2.setParameter("chngId", calc.getReqConfig().getChng().getId());
		}
		query1.setParameter("lsk", kart.getLsk());
		query1.setParameter("period", calc.getReqConfig().getPeriod());
		query1.executeUpdate();

		query2.setParameter("lsk", kart.getLsk());
		query2.setParameter("period", calc.getReqConfig().getPeriod());
		query2.executeUpdate();

		// типы записей начисления
		Lst chrgTp = lstMng.getByCD("Начислено свернуто, округлено");

		// сохранить новое начисление (переписать из prepChrg)
		for (ChrgRec chrg : chStore.getPrepChrg()) {
			
			Chrg chrg2 = new Chrg(kart, chrg.getServ(), chrg.getOrg(), status, calc.getReqConfig().getPeriod(), 
					chrg.getSumFull(), chrg.getSumPriv(), chrg.getSumAmnt(), 
					chrg.getVol(), chrg.getPrice(), chrg.getStdt(), chrg.getCntFact(), chrg.getArea(), chrgTp ,  
					calc.getReqConfig().getChng(), chrg.getMet(), chrg.getEntry(), chrg.getDt1(), chrg.getDt2(), chrg.getCntOwn()); 
			
			kart.getChrg().add(chrg2); 
		}

		// сохранить возмещение по льготе (переписать из prepPriv)
		for (PrivRec t : chStore.getPrepPriv()) {
			PrivilegeChrg priv = new PrivilegeChrg(kart, t.getServ(), t.getOrg(), status, t.getPersPriv(), calc.getReqConfig().getPeriod(), t.getSumma().doubleValue(),
					t.getVol().doubleValue(), calc.getReqConfig().getChng(), t.getPrice().doubleValue(), t.getDt1(), t.getDt2());
					
			kart.getPrivilegeChrg().add(priv); 
		}
		
		chStore = null; // ### TODO
	}
	

}


