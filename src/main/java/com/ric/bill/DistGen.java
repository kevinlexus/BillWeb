package com.ric.bill;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.ric.bill.excp.EmptyPar;
import com.ric.bill.excp.EmptyServ;
import com.ric.bill.excp.EmptyStorable;
import com.ric.bill.excp.NotFoundNode;
import com.ric.bill.excp.NotFoundODNLimit;
import com.ric.bill.excp.WrongGetMethod;
import com.ric.bill.excp.WrongValue;
import com.ric.bill.mm.HouseMng;
import com.ric.bill.mm.KartMng;
import com.ric.bill.mm.LstMng;
import com.ric.bill.mm.MeterLogMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.mm.ServMng;
import com.ric.bill.mm.VolMng;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.bs.Lst;
import com.ric.bill.model.fn.Chng;
import com.ric.bill.model.mt.MLogs;
import com.ric.bill.model.mt.Meter;
import com.ric.bill.model.mt.MeterExs;
import com.ric.bill.model.mt.MeterLog;
import com.ric.bill.model.mt.MeterLogGraph;
import com.ric.bill.model.mt.Vol;
import com.ric.bill.model.tr.Serv;

/**
 * Сервис распределения объема по узлам
 * @author lev
 *
 */

@Service
@Scope("prototype")
@Slf4j
public class DistGen {

	@Autowired
	private MeterLogMng metMng;
	@Autowired
	private KartMng kartMng;
	@Autowired
	private ParMng parMng;
	@Autowired
	private LstMng lstMng;
	@Autowired
	private ServMng servMng;
	@Autowired
	private HouseMng houseMng;
	@Autowired
	private VolMng volMng;
	@Autowired
	private Config config;
	
	//EntityManager - EM нужен на каждый DAO или сервис свой!
    @PersistenceContext
    private EntityManager em;

	private List<Check> lstCheck;

	/**
	 * внутренний класс, для проверок расчета узлов
	 * @author lev
	 *
	 */
	private class Check {
		private int id; //ID объекта
		private int tp; //тип расчета
		private Date genDt; //дата расчета
		private NodeVol nodeVol;//рассчитанные объемы
		
		public Check(int id, int tp, Date genDt, NodeVol nodeVol) {
			setId(id);
			setTp(tp);
			setGenDt(genDt);
			setNodeVol(nodeVol);
		}
		
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public int getTp() {
			return tp;
		}
		public void setTp(int tp) {
			this.tp = tp;
		}
		public Date getGenDt() {
			return genDt;
		}
		public void setGenDt(Date genDt) {
			this.genDt = genDt;
		}

		public NodeVol getNodeVol() {
			return nodeVol;
		}

		public void setNodeVol(NodeVol nodeVol) {
			this.nodeVol = nodeVol;
		}
		
	}
	
	/**
	 * конструктор
	 */
	public DistGen() {
		super();
		lstCheck = new ArrayList<Check>();
	}
	
	/**
	 * Распределить узел, следуя по графу (рекурсивная процедура)
	 * @param ml - вх.узел
	 * @param tp - тип распределения (0-по расчетной связи, 1-по связи по площади и кол.прож., 2-по расчетной связи ОДН, 3-по расчетной связи пропорц.площади
	 * @param genDt - дата формирования
	 * @return NodeVol - объект содержащий объемы
	 * @throws WrongGetMethod
	 * @throws EmptyServ
	 * @throws NotFoundODNLimit
	 * @throws NotFoundNode
	 * @throws EmptyStorable 
	 * @throws EmptyPar 
	 * @throws WrongValue 
	 */
	public NodeVol distNode (Calc calc, MLogs ml, int tp, Date genDt) throws WrongGetMethod, EmptyServ, NotFoundODNLimit, NotFoundNode, EmptyStorable, EmptyPar, WrongValue {
		// номер текущего запроса
		if (ml.getId()==518451 && tp==0) {
			log.trace("счетчик!");
		}

		int rqn = calc.getReqConfig().getRqn();
		
		NodeVol nv = findLstCheck(ml.getId(), tp, genDt); 
		// вытащить перерасчет, для удобства
		Chng chng = calc.getReqConfig().getChng();
		//если рассчитанный узел найден, вернуть готовый объем
		if (nv != null) { 
			return nv; 
		}

		Double partArea =0d; //текущая доля площади, по узлу
		Double partPers =0d; //текущая доля кол-ва прожив, по узлу
		Double vl =0d; //текущая доля объема, по узлу
		//занулить текущие, расчетные объемы
		nv = new NodeVol();
		//получить лицевой счет, к которому привязан счетчик, для удобства
		Kart kart = ml.getKart();
		//Kart kart = metMng.getKart(ml); <--тормозит!

		//присвоить лиц.счет, чтобы использовать calc в подсчете например нормативов
		calc.setKart(kart); 

		if (!ml.getTp().getCd().equals("ЛИПУ") && !ml.getTp().getCd().equals("ЛНрм")) {
			log.trace("Счетчик:id="+ml.getId()+" тип="+ml.getTp().getCd()+" ввод:"+ml.getEntry());
		}

		String mLogTp = ml.getTp().getCd(); //тип лог счетчика
		Serv servChrg = ml.getServ().getServChrg(); //получить основную услугу, для начисления
		if (servChrg == null) {
			throw new EmptyServ("При расчете счетчика MeterLog.Id="+ml.getId()+" , обнаружена пустая услуга для расчета начисления");
		}

		
		if (tp==0) {
			//log.info("Наличие услуги={} услуга={}",kartMng.getServ(rqn, calc, ml.getServ().getServOrg(), genDt), ml.getServ().getServOrg().getId()); 
			// по расчетной связи
			// только там, где существует услуга в данном дне (по услуге, содержащей Поставщика) (для ЛИПУ)
			if (mLogTp.equals("ЛИПУ") && kartMng.getServ(rqn, calc, ml.getServ().getServOrg(), genDt) || 
			   mLogTp.equals("ЛОДПУ") || mLogTp.equals("ЛГрупп")) {
				// посчитать объемы, по физическим счетчикам, прикрепленным к узлу
			    // (если такие есть) в пропорции на кол-во дней объема
				for (Meter m : ml.getMeter()) { 		// физ.сч
					for (Vol v : m.getVol()) {    		// фактические объемы
						if (v.getTp().getCd().equals("Фактический объем") && Utl.between(genDt, v.getDt1(), v.getDt2()) ) {
							//log.info("Записано1 genDt={}, {}, {}", genDt, v.getDt1(), v.getDt2());
							for (MeterExs e : m.getExs()) { // периоды сущ.
								// добавить объем в объект объема
								// умножить объем на процент существования счетчика и на долю дня действия объема 
								if (Utl.between(genDt, e.getDt1(), e.getDt2())) {
									vl=vl+Utl.nvl(v.getVol1(), 0d) * Utl.nvl(e.getPrc(), 0d) * Utl.getPartDays(v.getDt1(), v.getDt2());
								}
							}
						}
					}
					
					// перерасчет по изменению показаний ИПУ, добавляет объем к имеющемуся 
					if (calc.getReqConfig().getOperTp()==1 && calc.getReqConfig().getChng().getTp().getCd().equals("Корректировка показаний ИПУ") ) {
						Double vlChng =0d; // объем перерасчета
						vlChng=calc.getReqConfig().getChng().getChngLsk().parallelStream()
						.filter(t -> t.getKart().getLsk().equals(calc.getKart().getLsk())) // фильтр по лиц.счету
						.flatMap(t -> t.getChngVal().parallelStream().filter(d -> ml.equals(d.getMeter().getMeterLog()) // фильтр по getChngVal() 
																	&& Utl.between(genDt, d.getDtVal1(), d.getDtVal2()))
																	.filter(d -> d.getDtVal1() != null && d.getDtVal2() != null ))
						.mapToDouble(d -> Utl.nvl(d.getVal(), 0d) * Utl.getPartDays(d.getDtVal1(), d.getDtVal2()) ) // преобразовать в массив Double
						.sum(); // просуммировать

						vl = vl + vlChng; 
					}
					
					
				}
			} else if (mLogTp.equals("ЛНрм")){
				// по нормативу, только там, где существует услуга в данном дне (по услуге, содержащей Поставщика)
				// и если не существует физического счетчика
/*				if (ml.getServ().getId() == 71) {
					log.info("ttttt dt={}", genDt);
				}*/
				
				if (kartMng.getServ(rqn, calc, ml.getServ().getServOrg(), genDt) && !metMng.checkExsKartMet(rqn, kart, ml.getServ(), genDt)) {
					vl = kartMng.getStandartVol(rqn, calc, ml.getServ(), null, genDt, 1).partVol; // здесь tp=1, для определения объема
				}
			}
				
		} else if (tp==1 && (mLogTp.equals("ЛНрм") || mLogTp.equals("ЛИПУ") || mLogTp.equals("Лсчетчик"))) {
			//только там, где существует услуга в данном дне (по услуге, содержащей Поставщика)
			if (kartMng.getServ(rqn, calc, ml.getServ().getServOrg(), genDt)) {
				//по связи по площади и кол.прож. (только по Лнрм, ЛИПУ) в доле 1 дня
				//площадь
				partArea = Utl.nvl(parMng.getDbl(rqn, kart, "Площадь.Общая", genDt), 0d) / calc.getReqConfig().getCntCurDays(); 
				//проживающие
				CntPers cntPers= new CntPers();
				kartMng.getCntPers(rqn, calc, kart, servChrg, cntPers, genDt);
				partPers = cntPers.cntVol / calc.getReqConfig().getCntCurDays();
			}
		} else if (tp==2 && mLogTp.equals("Лсчетчик")) {
			//по расчетной связи ОДН (только у лог.счетчиков, при наличии расчетной связи ОДН)
			//получить дельту ОДН, площадь, кол-во людей, для расчета пропорции в последствии
			//сохранить счетчик ЛОДН
			//только там, где существует услуга в данном дне (по услуге, содержащей Поставщика)
			if (kartMng.getServ(rqn, calc, ml.getServ().getServOrg(), genDt)) {
				SumNodeVol lnkODNVol = null;
				MLogs lnkLODN = null;
				MLogs lnkSumODPU = null;
				MLogs lnkODPU = null;
				//поиск счетчика ЛОДН
				lnkLODN = metMng.getLinkedNode(rqn, ml, "ЛОДН", genDt);
				//параметр Доначисление по ОДН
				Double parAddODN = Utl.nvl(parMng.getDbl(rqn, (Storable)lnkLODN, "Доначисление по ОДН", genDt), 0d);
				Double parLimitODN = parMng.getDbl(rqn, (Storable)lnkLODN, "Лимит по ОДН", genDt);
				
				if (lnkLODN == null) {
					// не найден счетчик
			        throw new NotFoundNode("Не найден счетчик ЛОДН, связанный со счетчиком id="+ml.getId());  
				}
				//поиск счетчика ЛСумОдпу
				lnkSumODPU = metMng.getLinkedNode(rqn, lnkLODN, "ЛСумОДПУ", genDt);
				if (lnkSumODPU == null) {
					// не найден счетчик
			        throw new NotFoundNode("Не найден счетчик ЛСумОДПУ, связанный со счетчиком id="+lnkLODN.getId());  
				}
				//поиск счетчика Ф/Л ОДПУ
				lnkODPU = metMng.getLinkedNode(rqn, lnkSumODPU, "ЛОДПУ", genDt);
				SumNodeVol lnkODPUVol = new SumNodeVol();  
				if (lnkODPU == null) {
					// не найден счетчик (лог.счетчик должен быть обязательно, а физ.сч. к нему привязанных, может и не быть!)
				    //переделал из ошибки в Warning:
					//log.info("Warning: Не найден счетчик ЛОДПУ, связанный со счетчиком id="+lnkSumODPU.getId(), 2);
					//return null;
				} else {
					lnkODPUVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), lnkODPU, tp, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2());
				}
	
				//получить объем за период по счетчику ЛОДН и наличие ОДПУ
				lnkODNVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), lnkLODN, tp, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2());
	
				//получить проживающих и площадь за период по счетчику данного лиц.счета (основываясь на meter_vol)
				SumNodeVol sumVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), ml, tp, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2());
				
				if (lnkODPU != null && metMng.checkExsMet(rqn, lnkODPU, genDt) && lnkODPUVol.getVol() >= 0d) {
					//при наличии физ.счетчика(ков) ОДПУ и объема по нему
					if (lnkODNVol.getVol() > 0 && lnkODNVol.getArea() > 0) {
						//ПЕРЕРАСХОД
						if (lnkODNVol.getArea()==0d) {
							vl = 0d;
						} else {
							vl = lnkODNVol.getVol() * sumVol.getArea() / lnkODNVol.getArea();
						}
						//применить лимит по ОДН
						Double limitVol = lnkODNVol.getLimit();
						if (parLimitODN == null) {
							throw new NotFoundODNLimit("Не найден параметр рассчитываемый программно - лимит по ОДН в счетчике="+lnkLODN.getId());
						} else if (parLimitODN == 1 && limitVol > 0) {
							//если больше лимита - ограничить лимит * площадь
							if (vl > limitVol * sumVol.getArea()) {
								vl = limitVol * sumVol.getArea();
							}
						}
					} else {
						//ЭКОНОМИЯ
						//экономия, но в пределах потреблённого по основной услуге объема. Внимание! в квартплате решили так не учитывать, а учитывать в контексте услуги ОДН!
	
						//получить основную услугу
						Serv mainServ = servMng.findMain(servChrg);
						//получить счетчик основной услуги
						log.trace("check serv="+mainServ.getServMet().getId());
						double tmpVol=0d;
						SumNodeVol sumMainVol;
						List<MLogs> lstMain = metMng.getAllMetLogByServTp(rqn, kart, mainServ.getServMet(), null);
						for (MLogs mLog2 : lstMain) {
							//получить объем за период, по лог счетчику основной услуги, если найден
							sumMainVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), mLog2, tp, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2());
							tmpVol = tmpVol + sumMainVol.getVol();
						}
						
						//если есть проживающие по узлу ОДН
						if (lnkODNVol.getPers() > 0 ) {
							vl = lnkODNVol.getVol() * sumVol.getPers() / lnkODNVol.getPers();  
							//округлить до 4 знаков
							vl = (double)Math.round(vl * 10000d) / 10000d;
						}
						//ограничить экономию текущим потреблением по основному счетчику
						if (Math.abs(vl) > tmpVol) {
							vl = -1 * tmpVol;
						}
					}
					
				} else if (parAddODN > 0d) {
					//если есть объем дораспределения ОДН на м2 - то использовать его
					vl = parAddODN * sumVol.getArea();
				} else {
					//не найден счётчик ОДПУ (должно начислиться по лимиту ОДН (Нормативу) * площадь л.с.)
					//или нет объема по ОДПУ
					Double limit = parLimitODN;
					if (limit == null) {
						log.warn("Не найден обязательный параметр - лимит по ОДН в счетчике="+lnkLODN.getId());
						//throw new NotFoundODNLimit("Не найден обязательный параметр - лимит по ОДН в счетчике="+lnkLODN.getId());
					} else if (limit == 1) {
						//если больше лимита - ограничить лимит * площадь
						Double limitVol = lnkODNVol.getLimit();
						vl = limitVol * sumVol.getArea();
					}
				}
				
			}
		} else if (tp==3 && mLogTp.equals("Лсчетчик")) {
			//по расчетной связи пропорц.площади (Отопление например)
			//только там, где существует услуга в данном дне (по услуге, содержащей Поставщика)
			if (kartMng.getServ(rqn, calc, ml.getServ().getServOrg(), genDt)) {
				MLogs lnkLODN = null;
				MLogs lnkSumODPU = null;
				MLogs lnkODPU = null;
	
				//поиск счетчика ЛОДН
				lnkLODN = metMng.getLinkedNode(rqn, ml, "ЛОДН", genDt);
				//поиск счетчика ЛСумОдпу
				lnkSumODPU = metMng.getLinkedNode(rqn, lnkLODN, "ЛСумОДПУ", genDt);
				// поиск установленного физ.счетчика ОДПУ
				lnkODPU = metMng.getLinkedNode(rqn, lnkSumODPU, "ЛОДПУ", genDt);
				
				if (lnkLODN == null) {
					// не найден счетчик
			        throw new NotFoundNode("Не найден счетчик ЛОДН, связанный со счетчиком id="+ml.getId());  
				}
				
				if (lnkODPU != null) {
					// если существует физ.счетчик ОДПУ
					//получить объем за период по счетчику ЛОДН и наличие ОДПУ
					SumNodeVol lnkODNVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), lnkLODN, tp, genDt, genDt);
					//получить проживающих и площадь за период по счетчику данного лиц.счета (основываясь на meter_vol)
					SumNodeVol sumVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), ml, tp, genDt, genDt);
					//узнать наличие "Введено гкал." для расчета по значению, рассчитанному экономистом
					Double tmp =parMng.getDbl(rqn, lnkLODN, "VOL_SQ_MT", genDt);
					if (tmp != null) {
						//установлено значение "Введено гкал." 
						vl = tmp * sumVol.getArea();
					} else {
						//НЕ установлено значение "Введено гкал.", распределить по объему счетчика пропорционально площадей квартир
						if (lnkODNVol.getArea()==0d) {
							vl = 0d;
						} else {
							vl = lnkODNVol.getVol() * sumVol.getArea() / lnkODNVol.getArea();
						}
					}
				} else {
					// если НЕ существует физ.счетчик ОДПУ
					// начислить по "Введённое значение объёма на м2"
					Double tmp =parMng.getDbl(rqn, lnkLODN, "VOL_SQ_MT", genDt);
					//получить проживающих и площадь за период по счетчику данного лиц.счета (основываясь на meter_vol)
					SumNodeVol sumVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), ml, tp, genDt, genDt);
	
					if (tmp != null) {
						//установлено значение "Введено гкал." 
						vl = tmp * sumVol.getArea();
					} else {
						//НЕ установлено значение "Введено гкал."
						// TODO сделать ветку если нет параметра "Введённое значение объёма на м2", рассчитать по строительному объему!
					}
				}
			}
		}
		
		// добавить собственные объемы
		nv.addPartArea(partArea);
		nv.addPartPers(partPers);
		nv.addVol(vl);

		// найти все направления, с необходимым типом, указывающие в точку из других узлов, получить их объемы
		for (MeterLogGraph g : ml.getInside()) {
			//по соотв.периоду
			if (Utl.between(genDt, g.getDt1(), g.getDt2())) {
				if (tp==0 && g.getTp().getCd().equals("Расчетная связь") 
				 || tp==1 && g.getTp().getCd().equals("Связь по площади и кол-во прож.")
				 || tp==2 && g.getTp().getCd().equals("Расчетная связь ОДН")
				 || tp==3 && g.getTp().getCd().equals("Расчетная связь пропорц.площади")) {
						
						NodeVol nvChld = distNode(calc, g.getSrc(), tp, genDt);
						if (nvChld != null){
							//добавить объемы от дочерних узлов
							nv.addPartArea(nvChld.getPartArea());
							nv.addPartPers(nvChld.getPartPers());
							nv.addVol(nvChld.getVol() * Utl.nvl(g.getPrc(), 0d));
						}
				}
			}
		}
		if (ml.getInside().size() > 0) {
			log.trace("}");
		}
		
		//после рекурсивного расчета дочерних узлов, и только по последней дате, выполнить расчет Лимита ОДН
		if (tp==1 && mLogTp.equals("ЛОДН") && genDt.getTime() == calc.getReqConfig().getCurDt2().getTime() /*genDt.equals(Calc.getCurDt2()*/) {
			SumNodeVol lnkODNVol = null;
			Lst volTp = lstMng.getByCD("Лимит ОДН");
			double lmtVol;
			//по связи по площади и кол.прож. и только по ЛОДН счетчику
			if (servChrg.getCd().equals("Холодная вода") || servChrg.getCd().equals("Горячая вода")) {
				//получить площадь и кол-во прожив по вводу, за месяц  
				lnkODNVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), ml, tp, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2());
				//расчитать лимит кубов
				//если кол-во прожив. > 0
				if (lnkODNVol.getPers() > 0d) {
					double oplMan;
					if (lnkODNVol.getPers()==0d) {
						oplMan = 0d;
					} else {
						oplMan = lnkODNVol.getArea() /  lnkODNVol.getPers();   
					}
					lmtVol = oplLiter(oplMan)/1000;
					//записать лимит ОДН
					// TODO для перерасчета!
					Vol vol = new Vol((MeterLog) ml, volTp, lmtVol, null, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2(), 
							calc.getReqConfig().getOperTp(), calc.getReqConfig().getStatusVol());
					//saveVol(ml, vol);
					ml.getVol().add(vol);
				}
				
			} else if (servChrg.getCd().equals("Электроснабжение")) {
				//получить площадь и кол-во прожив по вводу, за месяц  
				lnkODNVol = metMng.getVolPeriod(rqn, calc.getReqConfig().getStatusVol(), ml, tp, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2());
				
				Double areaComm = parMng.getDbl(rqn, ml, "Площадь общего имущества.Электроэнергия", genDt);
				if (areaComm != null) {
					//log.warn("ВНИМАНИЕ! НЕ проверяется параметр Площадь общего имущества.Электроэнергия!!!!!!");
					//throw new EmptyPar("Не установлен параметр Площадь общего имущества.Электроэнергия во вводе с id="+ml.getId()+" по дате="+genDt);
					log.warn("Не установлен параметр Площадь общего имущества.Электроэнергия во вводе с id="+ml.getId()+" по дате="+genDt);
					
					if (lnkODNVol.getArea() > 0) {
						Boolean isLift = parMng.getBool(rqn, ml.getHouse(), "Признак наличия лифта", genDt);
						if (isLift == null) {
							log.warn("Отсутствует параметр Признак наличия лифта в доме id="+ml.getHouse().getId()+" по дате="+genDt);
						} else {
							// лимит кВт ОДН на м2
							if (isLift) {
					            // есть лифт в доме
								if (lnkODNVol.getArea()==0d) {
									lmtVol = 0d;
								} else {
									lmtVol = 4.1d * areaComm/lnkODNVol.getArea();
								}
							} else {
					            // нет лифта в доме
								if (lnkODNVol.getArea()==0d) {
									lmtVol = 0d;
								} else {
									lmtVol = 2.7d * areaComm/lnkODNVol.getArea();
								}
							}
							//записать лимит ОДН
							// TODO для перерасчета!
							Vol vol = new Vol((MeterLog) ml, volTp, lmtVol, null, calc.getReqConfig().getCurDt1(), calc.getReqConfig().getCurDt2(),
											calc.getReqConfig().getOperTp(), calc.getReqConfig().getStatusVol());
							//saveVol(ml, vol);
							ml.getVol().add(vol);
							//log.warn("ЛИМИТ ОДН по ЭлектроЭнергии="+lmtVol);
						}
					}
				}
			}
		}
		
		Lst volTp=null;
		//записать объем или площадь или кол-во прож. в текущий узел (лог.счетчик)
		if ((tp==0||tp==2||tp==3) && nv.getVol() != 0d) {
			//расчетная связь, расчетная связь ОДН
			volTp = lstMng.getByCD("Фактический объем");

			// TODO для перерасчета!
			Vol vol = new Vol((MeterLog) ml, volTp, nv.getVol(), null, genDt, genDt,
					calc.getReqConfig().getOperTp(), calc.getReqConfig().getStatusVol());
			ml.getVol().add(vol);
			
		} if (tp==1 && (nv.getPartArea() != 0d || nv.getPartPers() !=0d) ) {
			//связь подсчета площади, кол-во проживающих, сохранять, если только в тестовом режиме TODO 
			volTp = lstMng.getByCD("Площадь и проживающие");
			// TODO для перерасчета!
			Vol vol = new Vol((MeterLog) ml, volTp, nv.getPartArea(), nv.getPartPers(), genDt, genDt,
							calc.getReqConfig().getOperTp(), calc.getReqConfig().getStatusVol());

			ml.getVol().add(vol);
			//saveVol(ml, vol);

			if (ml.getId()==3625271 && !ml.getTp().getCd().equals("ЛИПУ") && !ml.getTp().getCd().equals("ЛНрм")) {
				log.trace("записана площадь по счетчику id="+ml.getId()+" area="+nv.getPartArea()+" pers="+nv.getPartPers());
			}
		}
		
		
		//добавить в список рассчитанных узлов
		addLstCheck(ml.getId(), tp, genDt, nv);
		//счетчик рекурсии на -1
		//rec--;

		if (ml.getId().equals(468737)) {
			log.trace("Счетчик!");
		}

		//if (nv.getVol().isNaN() || nv.getPartArea().isNaN() || nv.getPartPers().isNaN()) {
		//	log.info("*********VOL id={}, nv.vol={},{}", ml.getId(), nv.getVol(), nv.getPartArea(), nv.getPartPers());
		//}

		return nv;
	}
	
	
	/**
	 * Поиск расчитанного объема заданного по ID объекта
	 * @param id - ID
	 * @param tp - тип расчета
	 * @param genDt - дата расчета
	 * @return - найденный объем
	 */
	@Cacheable(cacheNames="rrr3", key="{ #id, #tp, #genDt }")
	private NodeVol findLstCheck(int id, int tp, Date genDt) { //TODO переделать на ParallelStream Java 8!!!
		Optional<Check> nv = lstCheck.parallelStream().filter(t -> t.getId()==id && t.getTp()==tp && t.getGenDt().equals(genDt)).findAny();
		if (nv.isPresent()) {
			return nv.get().getNodeVol();
		} else {
			return null;
		}
	}
	
	/**
	 * Добавление расчитанного объекта
	 * @param id - ID
	 * @param tp - тип расчета
	 * @param genDt - дата расчета
	 */
	private void addLstCheck(int id, int tp, Date genDt, NodeVol nodeVol) {
		  lstCheck.add(new Check(id, tp, genDt, nodeVol));
	}

	/**
	 * Почистить коллекцию, содержащую расчитанные объемы
	 */
	public void clearLstChecks() {
		lstCheck.clear();
	}

	/**
	 * таблица для возврата норматива потребления (в литрах) по соотв.площади на человека
	 * @param oplMan - площадь на человека
	 * @return
	 */
	public double oplLiter(Double oplMan) {
		int inVal = (int) Math.round(oplMan);
		double val=0d;
		
		switch (inVal) {
		case 1: val = 2;
		break;
		case 2: val = 2;
		break;
		case 3: val = 2;
		break;
		case 4: val = 10;
		break;
		case 5: val = 10;
		break;
		case 6: val = 10;
		break;
		case 7: val = 10;
		break;
		case 8: val = 10;
		break;
		case 9: val = 10;
		break;
		case 10: val = 9;
		break;
		case 11: val = 8.2;
		break;
		case 12: val = 7.5;
		break;
		case 13: val = 6.9;
		break;
		case 14: val = 6.4;
		break;
		case 15: val = 6.0;
		break;
		case 16: val = 5.6;
		break;
		case 17: val = 5.3;
		break;
		case 18: val = 5.0;
		break;
		case 19: val = 4.7;
		break;
		case 20: val = 4.5;
		break;
		case 21: val = 4.3;
		break;
		case 22: val = 4.1;
		break;
		case 23: val = 3.9;
		break;
		case 24: val = 3.8;
		break;
		case 25: val = 3.6;
		break;
		case 26: val = 3.5;
		break;
		case 27: val = 3.3;
		break;
		case 28: val = 3.2;
		break;
		case 29: val = 3.1;
		break;
		case 30: val = 3.0;
		break;
		case 31: val = 2.9;
		break;
		case 32: val = 2.8;
		break;
		case 33: val = 2.7;
		break;
		case 34: val = 2.6;
		break;
		case 35: val = 2.6;
		break;
		case 36: val = 2.5;
		break;
		case 37: val = 2.4;
		break;
		case 38: val = 2.4;
		break;
		case 39: val = 2.3;
		break;
		case 40: val = 2.3;
		break;
		case 41: val = 2.2;
		break;
		case 42: val = 2.1;
		break;
		case 43: val = 2.1;
		break;
		case 44: val = 2;
		break;
		case 45: val = 2;
		break;
		case 46: val = 2;
		break;
		case 47: val = 1.9;
		break;
		case 48: val = 1.9;
		break;
		case 49: val = 1.8;
		break;
		default: val = 1.8;
		
		}
		
		return val;
	}
	
	@PostConstruct
	public void constr() {

	}
	@PreDestroy
	public void dest() {

	}

}

