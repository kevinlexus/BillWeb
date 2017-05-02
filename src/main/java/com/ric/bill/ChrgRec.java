package com.ric.bill;


import java.math.BigDecimal;
import java.util.Date;

import com.ric.bill.model.bs.Org;
import com.ric.bill.model.tr.Serv;

/**
 * Строка начисления
 * @author lev
 *
 */
public class ChrgRec {

	private BigDecimal vol;
	private BigDecimal price;
	private Serv serv;
	private Org org;
    private Date dt1, dt2;
	private BigDecimal stdt;
	private Integer cntPers;
	private BigDecimal area;
	private Integer met;

    /**
     * конструктор 
     * @param sum - сумма
     * @param vol - объем
     * @param price - расценка
     * @param stdt - норматив
     * @param serv - услуга
     * @param org - организация
     * @param dt1 - дата начала
     * @param dt2 - дата окончания
     */
	public ChrgRec(BigDecimal vol, BigDecimal price, BigDecimal stdt, Integer cntPers, BigDecimal area, Serv serv, Org org, Integer met, Date dt1, Date dt2) {
		super();
		setVol(vol);
		setPrice(price);
		setStdt(stdt);
		setCntPers(cntPers);
		setArea(area);
		setServ(serv);
		setOrg(org);
		setMet(met);
		setDt1(dt1);
		setDt2(dt2);
	}
	
	public BigDecimal getVol() {
		return vol;
	}

	public void setVol(BigDecimal vol) {
		this.vol = vol;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Date getDt1() {
		return dt1;
	}

	public void setDt1(Date dt1) {
		this.dt1 = dt1;
	}

	public Org getOrg() {
		return org;
	}

	public void setOrg(Org org) {
		this.org = org;
	}

	public Date getDt2() {
		return dt2;
	}

	public void setDt2(Date dt2) {
		this.dt2 = dt2;
	}

	public Serv getServ() {
		return serv;
	}

	public void setServ(Serv serv) {
		this.serv = serv;
	}

	public BigDecimal getStdt() {
		return stdt;
	}

	public void setStdt(BigDecimal stdt) {
		this.stdt = stdt;
	}

	public Integer getCntPers() {
		return cntPers;
	}

	public void setCntPers(Integer cntPers) {
		this.cntPers = cntPers;
	}

	public BigDecimal getArea() {
		return area;
	}

	public void setArea(BigDecimal area) {
		this.area = area;
	}

	public Integer getMet() {
		return met;
	}

	public void setMet(Integer met) {
		this.met = met;
	}

}

