package com.ric.bill;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.ric.bill.model.bs.Org;
import com.ric.bill.model.tr.Serv;

/**
 * Хранилище записей начисления
 * @author lev
 *
 */
public class ChrgStore {

	// коллекция для группированного начисления
	private List<ChrgRec> store;

	// сгруппированные до главной услуги записи начисления
	// нужна для услуг считающих своё начисление от начисления других услуг
	private List<ChrgMainServRec> storeMainServ;

	//конструктор
	public ChrgStore () {
		super();
		setStore(new ArrayList<ChrgRec>(0));
		setStoreMainServ(new ArrayList<ChrgMainServRec>(0)); 
	}

	/**
	 * добавить строку начисления с учётом группировки записей 
	 * @param sum - сумма
	 * @param vol - объем
	 * @param price - расценка
     * @param stdt - норматив
	 * @param serv - услуга
	 * @param org - организация
	 * @param exsMet - наличие счетчика: false - нет, true - есть
	 * @param dt - дата
	 */
	public void addChrg (BigDecimal vol, BigDecimal price, BigDecimal stdt, Integer cntPers, BigDecimal area, Serv serv, Org org, Boolean exsMet, Date dt) {
		Integer met = 0;
		if (exsMet) {
			met = 1;
		}
		
		// СГРУППИРОВАТЬ по основной услуге TODO! Это нужно не по всем услугам, а только по тем, где есть дочерние услуги, зависимые от суммы начисления -  поправить использование TODO!
		Serv mainServ = serv.getServChrg();
		// умножить расценку на объем, получить сумму)
		BigDecimal sum = vol.multiply(price);
		if (sum.compareTo(BigDecimal.ZERO) != 0) {
			if (getStoreMainServ().size() == 0) {
				//завести новую строку 
				getStoreMainServ().add(new ChrgMainServRec(sum, mainServ, dt));
			} else {
				ChrgMainServRec lastRec = null;
				//получить последний добавленный элемент по данной дате
				for (ChrgMainServRec rec : getStoreMainServ()) {
					if (rec.getDt().equals(dt)) {
						lastRec = rec;
					}
				}
				if (lastRec == null) {
					//последний элемент не найден, - создать
					getStoreMainServ().add(new ChrgMainServRec(sum, mainServ, dt));
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
							getStoreMainServ().add(new ChrgMainServRec(sum, mainServ, dt));
						}
				}
			}
		}

		// СГРУППИРОВАТЬ по услуге, организации, расценке, дате
		if (getStore().size() == 0) {
			//завести новую строку
			getStore().add(new ChrgRec(vol, price, stdt, cntPers, area, serv, org, met, dt, dt));
		} else {
			ChrgRec lastRec = null;
			//получить последний добавленный элемент по данной услуге
			for (ChrgRec rec : getStore()) {
				if (rec.getServ().equals(serv)) {
					lastRec = rec;
				}
			}
			if (lastRec == null) {
				//последний элемент с данной услугой не найден, - создать
				getStore().add(new ChrgRec(vol, price, stdt, cntPers, area, serv, org, met, dt, dt));
			} else {
				//последний элемент найден
				//сравнить по-элементно
				if (lastRec.getPrice().compareTo(price) == 0 &&
					(lastRec.getOrg() == null && org == null ||
					 lastRec.getOrg().equals(org)) &&
						(lastRec.getStdt() == null && stdt == null ||
						 lastRec.getStdt().equals(stdt)) &&
							(lastRec.getCntPers() == null && cntPers == null ||
							 lastRec.getCntPers().equals(cntPers)) &&
								(lastRec.getMet() == null && met == null ||
								 lastRec.getMet().equals(met))
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
						getStore().add(new ChrgRec(vol, price, stdt, cntPers, area, serv, org, met, dt, dt));
					}
			}
		}
	}

	public List<ChrgRec> getStore() {
		return store;
	}

	private void setStore(List<ChrgRec> store) {
		this.store = store;
	}

	public List<ChrgMainServRec> getStoreMainServ() {
		return storeMainServ;
	}

	private void setStoreMainServ(List<ChrgMainServRec> storeMainServ) {
		this.storeMainServ = storeMainServ;
	}

	
}
