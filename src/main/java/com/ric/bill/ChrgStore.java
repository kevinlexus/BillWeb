package com.ric.bill;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ric.bill.dto.ChrgRec;
import com.ric.bill.dto.ChrgRecDet;
import com.ric.bill.model.bs.Org;
import com.ric.bill.model.fn.Privilege;
import com.ric.bill.model.ps.Pers;
import com.ric.bill.model.tr.Serv;

import lombok.extern.slf4j.Slf4j;

/**
 * Хранилище записей начисления
 * @author lev
 *
 */
@Slf4j
public class ChrgStore {

	// хранилище для группированного, детализированного до льгот по проживающим начисления
	private List<ChrgRecDet> storeRecDet;
	// хранилище для группированного начисления
	//private List<ChrgRec> storeRec;

	// сгруппированные до главной услуги записи начисления
	// нужна для услуг считающих своё начисление от начисления других услуг
	private List<ChrgMainServRec> storeMainServ;

    // для подготовки начисления для записи
    private List<ChrgRec> storeChrgRec;

    //конструктор
	public ChrgStore () {
		super();
		//setStoreRec(new ArrayList<ChrgRec>(0));
		setStoreRecDet(new ArrayList<ChrgRecDet>(100));
		setStoreMainServ(new ArrayList<ChrgMainServRec>(100)); 
		setStoreChrgRec(new ArrayList<ChrgRec>(100));
	}

	/**
	 * добавить строку начисления с учётом группировки записей 
	 * @param vol - объем
	 * @param price - расценка
	 * @param pricePriv - расценка по льготе (null если нельготная услуга)
	 * @param discount - дисконт по расценке (null если нельготная услуга)
     * @param stdt - норматив
     * @param cntFact - кол-во прожив по факту (без собственников)
	 * @param serv - услуга
	 * @param org - организация
	 * @param exsMet - наличие счетчика: false - нет, true - есть
	 * @param entry - номер ввода
	 * @param dt - дата
     * @param cntOwn - кол-во собственников
     * @param pers - проживающий для учета льготы (заполняется не по всем услугам)
     * @param priv - привилегия (заполняется не по всем услугам)
     *  если по нельготной услуге, то передавать null
	 */
	public void addChrg (BigDecimal vol, BigDecimal price, BigDecimal pricePriv, BigDecimal discount, BigDecimal stdt, Integer cntFact, BigDecimal area, 
						 Serv serv, Org org, Boolean exsMet, Integer entry, Date dt, Integer cntOwn, Pers pers, Privilege priv) {
		Integer met = 0;
		if (exsMet) {
			met = 1;
		}
		
		// СГРУППИРОВАТЬ по основной услуге TODO! Это нужно не по всем услугам, а только по тем, где есть дочерние услуги,
		groupMainStore(vol, price, serv, dt, getStoreMainServ());
		// сгруппировать детализированное хранилище 
		groupStore(vol, price, pricePriv, discount, stdt, cntFact, area, serv, org, entry, dt, cntOwn, pers, priv, met, getStoreRecDet());
		
	}

	/**
	 * Сгруппировать хранилище по основной услуге TODO! Это нужно не по всем услугам, а только по тем, где есть дочерние услуги,
	 * @param vol - объем
	 * @param price - цена
	 * @param serv - услуга
	 * @param dt - дата
	 * @param store - хранилище
	 */
	private void groupMainStore(BigDecimal vol, BigDecimal price, Serv serv, Date dt, List<ChrgMainServRec> store) {
		// зависимые от суммы начисления -  поправить использование TODO!
		Serv mainServ = serv.getServChrg();
		// умножить расценку на объем, получить сумму)
		BigDecimal sum = vol.multiply(price);
		if (sum.compareTo(BigDecimal.ZERO) != 0) {
			if (store.size() == 0) {
				//завести новую строку 
				store.add(new ChrgMainServRec(sum, mainServ, dt));
			} else {
				ChrgMainServRec lastRec = null;
				//получить последний добавленный элемент по данной дате
				for (ChrgMainServRec rec : store) {
					if (rec.getDt().equals(dt)) {
						lastRec = rec;
					}
				}
				if (lastRec == null) {
					//последний элемент не найден, - создать
					store.add(new ChrgMainServRec(sum, mainServ, dt));
				} else {
					//последний элемент найден, добавить в него сумму начисления
					//сравнить по дате
					if (lastRec.getDt().equals(dt)) {
							//добавить данные в последнюю строку
							if (lastRec.getSum() != null) {
								lastRec.setSum(lastRec.getSum().add(sum));
							} else {
								lastRec.setSum(sum);
							}
						} else {
							//завести новую строку, если отличается датой
							store.add(new ChrgMainServRec(sum, mainServ, dt));
						}
				}
			}
		}
	}


	
	public List<ChrgRecDet> getStoreRecDet() {
		return storeRecDet;
	}

	private void setStoreRecDet(List<ChrgRecDet> storeRecDet) {
		this.storeRecDet = storeRecDet;
	}

/*	public List<ChrgRec> getStoreRec() {
		return storeRec;
	}

	private void setStoreRec(List<ChrgRec> storeRec) {
		this.storeRec = storeRec;
	}
*/
	public List<ChrgMainServRec> getStoreMainServ() {
		return storeMainServ;
	}

	private void setStoreMainServ(List<ChrgMainServRec> storeMainServ) {
		this.storeMainServ = storeMainServ;
	}

	private void setStoreChrgRec(ArrayList<ChrgRec> store) {
		this.storeChrgRec = store;
	}

	public List<ChrgRec> getStoreChrgRec() {
		return this.storeChrgRec;
	}

	/**
	 * Сгруппировать хранилище предварительно по услуге, льготе и проживающему (если есть льгота)
	 * @param vol - объем
	 * @param price - расценка
	 * @param pricePriv - расценка по льготе (null если нельготная услуга)
	 * @param discount - дисконт по расценке (null если нельготная услуга)
     * @param stdt - норматив
     * @param cntFact - кол-во прожив по факту (без собственников)
	 * @param area - площадь
	 * @param serv - услуга
	 * @param org - организация
	 * @param entry - номер ввода
	 * @param dt - дата
     * @param cntOwn - кол-во собственников
     * @param pers - проживающий для учета льготы (заполняется не по всем услугам)
     * @param priv - привилегия (заполняется не по всем услугам)
	 * @param store - хранилище
	 */
	private void groupStore(BigDecimal vol, BigDecimal price, BigDecimal pricePriv, BigDecimal discount, BigDecimal stdt, Integer cntFact,
			BigDecimal area, Serv serv, Org org, Integer entry, Date dt, Integer cntOwn, Pers pers, Privilege priv,
			Integer met, List<ChrgRecDet> store) {
		if (store.size() == 0) {
			// завести новую строку
			store.add(new ChrgRecDet(vol, price, pricePriv, discount, stdt, cntFact, area, 
						serv, org, met, entry, dt, dt, cntOwn, pers, priv));
		} else {
			ChrgRecDet lastRec = null;
			// получить последний добавленный элемент. Поиск по услуге, льготе (если задано), проживающему (если задано) 
			for (ChrgRecDet rec : store) {
				//log.info("pers1={},pers2={} is={} priv1={},priv2={} is={}", pers, rec.getPers(), Utl.cmp(rec.getPers(), pers), priv, rec.getPriv(), Utl.cmp(rec.getPriv(), priv));
				if (rec.getServ().equals(serv) && Utl.cmp(rec.getPriv(), priv)  && Utl.cmp(rec.getPers(), pers) ) {
					lastRec = rec;
				}
			}
			if (lastRec == null) {
				//последний элемент с данной услугой не найден, - создать
				store.add(new ChrgRecDet(vol, price, pricePriv, discount, stdt, cntFact, area, 
						serv, org, met, entry, dt, dt, cntOwn, pers, priv));
			} else {
				//последний элемент найден
				//сравнить по-элементно
				if (Utl.cmp(lastRec.getPrice(), price) &&
					 Utl.cmp(lastRec.getPricePriv(), pricePriv) &&
					  Utl.cmp(lastRec.getDiscount(), discount) &&
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
						store.add(new ChrgRecDet(vol, price, pricePriv, discount, stdt, cntFact, area, 
								serv, org, met, entry, dt, dt, cntOwn, 
								pers, priv));
					}
			}
		}
	}
	
	/**
	 * Сгруппировать хранилище, для записи в таблицу
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
	public void groupStoreChrgRec(BigDecimal sumFull, BigDecimal sumPriv, BigDecimal sumAmnt, BigDecimal price, 
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
}
