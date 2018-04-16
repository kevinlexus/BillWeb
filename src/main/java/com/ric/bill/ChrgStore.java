package com.ric.bill;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ric.bill.dto.ChrgRec;
import com.ric.bill.dto.PrivRec;
import com.ric.bill.dto.VolDet;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.bs.Lst;
import com.ric.bill.model.bs.Org;
import com.ric.bill.model.fn.Chng;
import com.ric.bill.model.fn.Chrg;
import com.ric.bill.model.fn.PersPrivilege;
import com.ric.bill.model.fn.Privilege;
import com.ric.bill.model.ps.Pers;
import com.ric.bill.model.tr.Serv;

import lombok.extern.slf4j.Slf4j;

/**
 * Хранилище вспомогательных коллекций начисления
 * @author lev
 *
 */
@Slf4j
public class ChrgStore {

	// хранилище для группированного, детализированного до льгот по проживающим начисления
	private List<VolDet> storeVolDet;
	// сгруппированные до главной услуги записи начисления
	// нужна для услуг считающих своё начисление от начисления других услуг
	private List<ChrgMainServRec> storeMainServ;
    // итоговое начисление по одной услуге
    private List<ChrgRec> storeChrgRec;
    // итоговое начисление льготы по одной услуге
    private List<PrivRec> storePrivRec;
    // итоговое начисление, для записи в таблицу
    private List<ChrgRec> prepChrg;
    // итоговое возмещение по льготам, для записи в таблицу
    private List<PrivRec> prepPriv;

    // вспомогательные коллекции для расчета дочерних услуг
    private HashMap<Serv, BigDecimal> mapServ;
    private HashMap<Serv, BigDecimal> mapVrt;

    // результат всего начисления, для сохранения
    //private List<Chrg> prepChrg;

    //конструктор
	public ChrgStore () {
		super();
		//для виртуальной услуги	
		this.mapServ = new HashMap<Serv, BigDecimal>(100);
		this.mapVrt = new HashMap<Serv, BigDecimal>(100);
		this.storeMainServ = new ArrayList<ChrgMainServRec>(100); 
		this.prepChrg = new ArrayList<ChrgRec>(100);
		this.prepPriv = new ArrayList<PrivRec>(100);
	}

	// очистить вспомогательные коллекции
	public void init() {
		this.storeVolDet = new ArrayList<VolDet>(100);
		this.storeChrgRec = new ArrayList<ChrgRec>(100);
		this.storePrivRec = new ArrayList<PrivRec>(100);
	}
	
	public List<VolDet> getStoreVolDet() {
		return storeVolDet;
	}

	public List<ChrgMainServRec> getStoreMainServ() {
		return storeMainServ;
	}

    public HashMap<Serv, BigDecimal> getMapServ() {
		return mapServ;
	}

	public HashMap<Serv, BigDecimal> getMapVrt() {
		return mapVrt;
	}
	
	public List<ChrgRec> getPrepChrg() {
		return prepChrg;
	}

	public List<PrivRec> getPrepPriv() {
		return prepPriv;
	}

	/**
	 * Добавить строку начисления с учётом группировки записей 
	 * @param vol - объем
	 * @param price - расценка
	 * @param pricePriv - расценка по льготе (null если нельготная услуга)
	 * @param tp - вариант расчета льготы: 0 - полное нач. - нач.со льготой=льгота (отоп.), 1 - полное нач. - льгота=итог нач.(эл.эн.)
     * @param stdt - норматив
     * @param cntFact - кол-во прожив по факту (без собственников)
	 * @param serv - услуга
	 * @param org - организация
	 * @param exsMet - наличие счетчика: false - нет, true - есть
	 * @param entry - номер ввода
	 * @param dt - дата
     * @param cntOwn - кол-во собственников
     * @param persPriv - льгота по проживающему
     *  если по нельготной услуге, то передавать null
	 */
	public void addChrg(BigDecimal vol, BigDecimal price, BigDecimal pricePriv, Integer tp, BigDecimal stdt, Integer cntFact, BigDecimal area, 
						 Serv serv, Org org, Boolean exsMet, Integer entry, Date dt, Integer cntOwn, PersPrivilege persPriv) {
		Integer met = 0;
		if (exsMet) {
			met = 1;
		}
		
		// добавить с группировкой по основной услуге TODO! Это нужно не по всем услугам, а только по тем, где есть дочерние услуги,
		addGroupMainStore(vol, price, serv, dt);
		// добавить с группировкой в детализированное хранилище 
		addGroupVolDet(vol, price, pricePriv, tp, stdt, cntFact, area, serv, org, entry, dt, cntOwn, persPriv, met);
	}
	
	/**
	 * Добавить с группировкой в хранилище по основной услуге TODO! Это нужно не по всем услугам, а только по тем, где есть дочерние услуги,
	 * @param vol - объем
	 * @param price - цена
	 * @param serv - услуга
	 * @param dt - дата
	 * @param storeMainServ - хранилище
	 */
	private void addGroupMainStore(BigDecimal vol, BigDecimal price, Serv serv, Date dt) {
		//log.info("3 CHECK SAVE serv.id={}, vol={} size={}", serv.getId(), vol, storeMainServ.size());
		// зависимые от суммы начисления -  поправить использование TODO!
		Serv mainServ = serv.getServChrg();
		// умножить расценку на объем, получить сумму
		BigDecimal sum = vol.multiply(price);
		if (sum.compareTo(BigDecimal.ZERO) != 0) {
			if (storeMainServ.size() == 0) {
				//завести новую строку 
				storeMainServ.add(new ChrgMainServRec(sum, mainServ, dt));
			} else {
				ChrgMainServRec lastRec = null;
				//получить последний добавленный элемент по данной дате
				for (ChrgMainServRec rec : storeMainServ) {
					if (rec.getMainServ().equals(mainServ) && rec.getDt().equals(dt)) {
						lastRec = rec;
					}
				}
				if (lastRec == null) {
					//последний элемент не найден, - создать
					storeMainServ.add(new ChrgMainServRec(sum, mainServ, dt));
				} else {
					//последний элемент найден, добавить в него сумму начисления
					//сравнить по дате
					if (lastRec.getMainServ().equals(mainServ) && lastRec.getDt().equals(dt)) {
							//добавить данные в последнюю строку
							if (lastRec.getSum() != null) {
								lastRec.setSum(lastRec.getSum().add(sum));
							} else {
								lastRec.setSum(sum);
							}
						} else {
							//завести новую строку, если отличается датой
							storeMainServ.add(new ChrgMainServRec(sum, mainServ, dt));
						}
				}
			}
		}
	}
	
	/**
	 * Добавить с группировкой в хранилище объемов, по услуге, льготе проживающего (если есть льгота)
	 * @param vol - объем
	 * @param price - расценка
	 * @param pricePriv - расценка по льготе (null если нельготная услуга)
	 * @param tp - вариант расчета льготы: 0 - полное нач. - нач.со льготой=льгота (отоп.), 1 - полное нач. - льгота=итог нач.(эл.эн.)
     * @param stdt - норматив
     * @param cntFact - кол-во прожив по факту (без собственников)
	 * @param area - площадь
	 * @param serv - услуга
	 * @param org - организация
	 * @param entry - номер ввода
	 * @param dt - дата
     * @param cntOwn - кол-во собственников
     * @param persPriv - льгота по проживающему
	 */
	private void addGroupVolDet(BigDecimal vol, BigDecimal price, BigDecimal pricePriv, Integer tp, BigDecimal stdt, Integer cntFact,
			BigDecimal area, Serv serv, Org org, Integer entry, Date dt, Integer cntOwn, PersPrivilege persPriv,
			Integer met) {
		//if (serv.getId().equals(80)) {
		//	log.info("store: serv.Id={}, vol={}, pricePriv={}, dt={}", serv.getId(), vol, pricePriv, dt);
		//}

		List<VolDet> store = getStoreVolDet();
		if (store.size() == 0) {
			// завести новую строку
			store.add(new VolDet(vol, price, pricePriv, tp, stdt, cntFact, area, 
						serv, org, met, entry, dt, dt, cntOwn, persPriv));
		} else {
			VolDet lastRec = null;
			// получить последний добавленный элемент. Поиск по услуге, льготе (если задано), проживающему (если задано) 
			for (VolDet rec : store) {
				//log.info("pers1={},pers2={} is={} priv1={},priv2={} is={}", pers, rec.getPers(), Utl.cmp(rec.getPers(), pers), priv, rec.getPriv(), Utl.cmp(rec.getPriv(), priv));
				if (rec.getServ().equals(serv) && Utl.cmp(rec.getPersPriv(), persPriv)) {
					lastRec = rec;
				}
			}
			if (lastRec == null) {
				//последний элемент с данной услугой не найден, - создать
				store.add(new VolDet(vol, price, pricePriv, tp, stdt, cntFact, area, 
						serv, org, met, entry, dt, dt, cntOwn, persPriv));
			} else {
				//последний элемент найден
				//сравнить по-элементно
				if (Utl.cmp(lastRec.getPrice(), price) &&
					 Utl.cmp(lastRec.getPricePriv(), pricePriv) &&
					  Utl.cmp(lastRec.getTp(), tp) &&
					   Utl.cmp(lastRec.getOrg(), org) &&
						Utl.cmp(lastRec.getStdt(), stdt) &&
						 Utl.cmp(lastRec.getCntFact(), cntFact) &&
						  Utl.cmp(lastRec.getMet(), met) &&
						   Utl.cmp(lastRec.getEntry(), entry) &&
							Utl.cmp(lastRec.getCntOwn(), cntOwn)
					) {
						//добавить данные в последнюю строку, прибавить объем и площадь
						if (lastRec.getVol() != null) {
							lastRec.setVol(lastRec.getVol().add(vol));
						} else {
							lastRec.setVol(vol);
						}

						if (lastRec.getArea() != null) {
							lastRec.setArea(lastRec.getArea().add(area));
						} else {
							lastRec.setArea(area);
						}
						
						//установить заключительную дату периода
						lastRec.setDt2(dt);
					} else {
						//завести новую строку, если отличается расценкой или организацией
						store.add(new VolDet(vol, price, pricePriv, tp, stdt, cntFact, area, 
								serv, org, met, entry, dt, dt, cntOwn, persPriv));
					}
			}
		}
	}
	
	/**
	 * Добавить с группировкой в хранилище недетализированных записей начисления
	 * @param sumFull
	 * @param sumPriv
	 * @param sumAmnt
	 * @param price
	 * @param serv
	 * @param org
	 * @param dt1
	 * @param dt2
	 * @param stdt
	 * @param cntFact
	 * @param cntOwn
	 * @param area
	 * @param met
	 * @param entry
	 * @param vol
	 */
	public void addGroupChrgRec(BigDecimal sumFull, BigDecimal sumPriv, BigDecimal sumAmnt, BigDecimal price, 
			Serv serv, Org org, Date dt1, Date dt2, BigDecimal stdt, Integer cntFact, Integer cntOwn, BigDecimal area,
			Integer met, Integer entry, BigDecimal vol) {
		// СГРУППИРОВАТЬ
		if (storeChrgRec.size() == 0) {
			// завести новую строку
			storeChrgRec.add(new ChrgRec(sumFull, sumPriv, sumAmnt, price, serv, org, dt1, dt2, 
					stdt, cntFact, cntOwn, area, met, entry, vol));
			//log.info("init1={}", vol);
		} else {
			ChrgRec lastRec = null;
			// получить последний добавленный элемент по данной услуге
			for (ChrgRec t : storeChrgRec) {
				if (t.getServ().equals(serv)) {
					lastRec = t;
				}
			}
			if (lastRec == null) {
				//последний элемент с данной услугой не найден, - создать
				storeChrgRec.add(new ChrgRec(sumFull, sumPriv, sumAmnt, price, serv, org, dt1, dt2, 
						stdt, cntFact, cntOwn, area, met, entry, vol));
				//log.info("init2={}", vol);
			} else {
				//последний элемент найден
				//сравнить по-элементно
				if (Utl.cmp(lastRec.getPrice(), price) &&
 					  Utl.cmp(lastRec.getOrg(), org) &&
						Utl.cmp(lastRec.getStdt(), stdt) &&
						  Utl.cmp(lastRec.getCntFact(), cntFact) &&
							Utl.cmp(lastRec.getMet(), met) &&
							  Utl.cmp(lastRec.getEntry(), entry) &&
								Utl.cmp(lastRec.getCntOwn(), cntOwn)
					) {
						//добавить данные в последнюю строку, прибавить объем и площадь, проживающих и суммы
						if (lastRec.getVol() != null) {
							//log.info("before={}, plus={}", lastRec.getVol(), vol);
							lastRec.setVol(lastRec.getVol().add(vol));
							//log.info("after={}", lastRec.getVol());
						} else {
							lastRec.setVol(vol);
							//log.info("init3={}", vol);
						}

						if (lastRec.getArea() != null) {
							lastRec.setArea(lastRec.getArea().add(area));
						} else {
							lastRec.setArea(area);
						}
						
						if (lastRec.getSumAmnt() != null) {
							lastRec.setSumAmnt(lastRec.getSumAmnt().add(sumAmnt));
						} else {
							lastRec.setSumAmnt(sumAmnt);
						}

						if (lastRec.getSumPriv() != null) {
							lastRec.setSumPriv(lastRec.getSumPriv().add(sumPriv));
						} else {
							lastRec.setSumPriv(sumPriv);
						}
						
						if (lastRec.getSumFull() != null) {
							lastRec.setSumFull(lastRec.getSumFull().add(sumFull));
						} else {
							lastRec.setSumFull(sumFull);
						}
						
						//установить заключительную дату периода
						lastRec.setDt2(dt2);
					} else {
						//завести новую строку, если отличается расценкой или организацией и т.п.
						storeChrgRec.add(new ChrgRec(sumFull, sumPriv, sumAmnt, price, serv, org, dt1, dt2, 
								stdt, cntFact, cntOwn, area, met, entry, vol));
					}
			}
		}
		
	}

	
	/**
	 * Добавить с группировкой в хранилище записей льгот
	 * @param summa - сумма льготы
	 * @param serv - услуга
	 * @param org - организация
	 * @param persPriv - льгота проживающего
	 * @param vol - объем
	 * @param price - расценка с учётом льготы
	 * @param dt1 - дата начала
	 * @param dt2 - дата окончания
	 */
	public void addGroupPrivRec(BigDecimal summa, Serv serv, Org org, PersPrivilege persPriv, BigDecimal vol, BigDecimal price, Date dt1, Date dt2) {
		// СГРУППИРОВАТЬ
		if (storePrivRec.size() == 0) {
			// завести новую строку
			//(Serv serv, Org org, BigDecimal summa, BigDecimal vol, PersPrivilege persPriv, Date dt1, Date dt2)
			storePrivRec.add(new PrivRec(serv, org, summa, vol, persPriv, price, dt1, dt2));
			//log.info("init1={}", vol);
		} else {
			PrivRec lastRec = null;
			// получить последний добавленный элемент по данной услуге
			for (PrivRec t : storePrivRec) {
				if (t.getServ().equals(serv)) {
					lastRec = t;
				}
			}
			if (lastRec == null) {
				//последний элемент с данной услугой не найден, - создать
				storePrivRec.add(new PrivRec(serv, org, summa, vol, persPriv, price, dt1, dt2));
				//log.info("init2={}", vol);
			} else {
				//последний элемент найден
				//сравнить по-элементно
				if (Utl.cmp(lastRec.getOrg(), org) &&
						Utl.cmp(lastRec.getPersPriv(), persPriv) &&
							Utl.cmp(lastRec.getPrice(), price)
					) {
						//добавить данные в последнюю строку, прибавить объем и площадь, проживающих и суммы
						if (lastRec.getVol() != null) {
							lastRec.setVol(lastRec.getVol().add(vol));
						} else {
							lastRec.setVol(vol);
						}

						if (lastRec.getSumma() != null) {
							lastRec.setSumma(lastRec.getSumma().add(summa));
						} else {
							lastRec.setSumma(summa);
						}

						//установить заключительную дату периода
						lastRec.setDt2(dt2);
					} else {
						//завести новую строку, если отличается расценкой или организацией и т.п.
						storePrivRec.add(new PrivRec(serv, org, summa, vol, persPriv, price, dt1, dt2));
					}
			}
		}
		
	}
	
	/**
	 * сохранить запись о сумме, предназаначенной для коррекции 
	 * @param serv - услуга
	 * @param sum - сумма
	 */
	public void putMapServVal(Serv serv, BigDecimal sum) {
		BigDecimal tmpSum;
		//HaspMap считает разными услуги, если они одинаковые, но пришли из разных потоков, пришлось искать for - ом - <-- Проверить это TODO!  
		for (Map.Entry<Serv, BigDecimal> entry : getMapServ().entrySet()) {
	    	if (entry.getKey().equals(serv)) { 
	    		tmpSum = Utl.nvl(entry.getValue(), BigDecimal.ZERO);
	    		tmpSum = tmpSum.add(sum);
	    	    getMapServ().put(entry.getKey(), tmpSum);
	    		return;
	    	}
	    }
		getMapServ().put(serv, sum);
	}
	
	/**
	 * сохранить запись о сумме, предназаначенной для коррекции 
	 * @param serv - услуга
	 * @param sum - сумма
	 */
	public void putMapVrtVal(Serv serv, BigDecimal sum) {
		BigDecimal tmpSum;
	    for (Map.Entry<Serv, BigDecimal> entry : getMapVrt().entrySet()) {
	    	if (entry.getKey().equals(serv)) {
	    		tmpSum = Utl.nvl(entry.getValue(), BigDecimal.ZERO);
	    		tmpSum = tmpSum.add(sum);
	    		getMapVrt().put(entry.getKey(), tmpSum);
	    		return;
	    	}
	    }
	    getMapVrt().put(serv, sum);
	}

	/**
	 * Добавить записи в итоговое хранилище
	 */
	public void loadPrepChrg() {
		prepChrg.addAll(storeChrgRec);
		prepPriv.addAll(storePrivRec);
	}
	
	/**
	 * Сохранить в prepChrg
	 */
	
/*	public void loadPrepChrg(Kart kart, Calc calc, Lst chrgTpRnd, Chng chng) {
		storeChrgRec.stream().forEach(rec-> {
			prepChrg.add(new Chrg(kart, rec.getServ(), rec.getOrg(), 1, calc.getReqConfig().getPeriod(), 
					rec.getSumFull(), rec.getSumPriv(), rec.getSumAmnt(), 
					rec.getVol(), rec.getPrice(), rec.getStdt(), rec.getCntFact(), rec.getArea(), chrgTpRnd, 
					chng, rec.getMet(), rec.getEntry(), rec.getDt1(), rec.getDt2(), rec.getCntOwn()));
		});
	}*/
}
