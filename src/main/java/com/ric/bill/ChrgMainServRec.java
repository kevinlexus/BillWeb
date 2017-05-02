package com.ric.bill;


import java.math.BigDecimal;
import java.util.Date;

import com.ric.bill.model.tr.Serv;

/**
 * Строка начисления, сгруппированная по главной услуге
 * @author lev
 *
 */
public class ChrgMainServRec {

	private BigDecimal sum;
	private Serv mainServ;
    private Date dt;

    /**
     * конструктор 
     * @param sum - сумма
     * @param mainServ - главная услуга
     * @param dt - дата формирования
     */
	public ChrgMainServRec(BigDecimal sum, Serv mainServ, Date dt) {
		super();
		setSum(sum);
		setMainServ(mainServ);
		setDt(dt);
	}

	public Serv getMainServ() {
		return mainServ;
	}

	public void setMainServ(Serv mainServ) {
		this.mainServ = mainServ;
	}

	public Date getDt() {
		return dt;
	}

	public void setDt(Date dt) {
		this.dt = dt;
	}

	public BigDecimal getSum() {
		return sum;
	}

	public void setSum(BigDecimal sum) {
		this.sum = sum;
	}

	
}

