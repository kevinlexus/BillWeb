package com.ric.bill;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ric.bill.Calc;
import com.ric.bill.Result;
import com.ric.cmn.Utl;
import com.ric.bill.excp.EmptyStorable;
import com.ric.bill.mm.KartMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.mm.TarifMng;
import com.ric.bill.model.ar.House;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.fn.Chng;
import com.ric.bill.model.tr.Serv;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис получения расценки
 * @author Leo
 * @version 1.00
 *
 */
@Service
@Slf4j
public class PriceMng { // тестирование изменений на тур.комп. 31.01.2018 09:17

	@Autowired
	private ParMng parMng;
	@Autowired
	private KartMng kartMng;
	@Autowired
	private TarifMng tarMng;

	/**
	 * DTO для передачи составляющих цены
	 * @author Leo
	 *
	 */
	@Getter@Setter
	public
	class ComplexPrice {
		// расценка свыше соц.нормы
		private Double upStPrice;
		// расценка 0 проживающих
		private Double woKprPrice;
	}
	
	/**
	 * Получить расценку по соцнорме
	 * @param calc
	 * @param kart
	 * @param serv
	 * @param genDt
	 * @param rqn
	 * @param stServ
	 * @return
	 * @throws EmptyStorable
	 */
	public Double getStandartPrice(Calc calc, Kart kart, Serv serv, Date genDt, int rqn, Serv stServ, Chng chng) throws EmptyStorable {
		Double stPrice = null;
		//log.info("serv={}", serv.getId());
		if (calc.getReqConfig().getOperTp()==1 && chng.getTp().getCd().equals("Изменение расценки (тарифа)") ) {
			// перерасчет по расценке
			//log.info("stServ={}", stServ.getId());
			stPrice = tarMng.getChngVal(calc, stServ, genDt, "Изменение расценки (тарифа)", 1);
		}
		
		if (stPrice == null) {
			// если не перерасчет или не найдена расценка по перерасчету
			if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-3"), 0d) == 1d) {
				// обычно услуги ХВ, ГВ, Эл на Общее имущество (ОИ)
				// по этому варианту получить расценку от услуги, хранящей расценку, умножить на норматив, округлить
				Double stVol = kartMng.getServPropByCD(rqn, calc, serv, "Норматив", genDt);
	
				if (serv.getCd().equals("Горячая вода ОДН")) {
					// ГВ на ОИ, получить расценку в зависимости от параметров дома 
					stPrice = getHotWaterPriceByConditions(calc, calc.getHouse(), genDt, rqn, serv, false);
				} else {
					// получить расценку от услуги по которой хранится расценка
					stPrice = kartMng.getServPropByCD(rqn, calc, stServ.getServPrice(), "Цена", genDt);
				}
	
				// если пуст один из параметров - занулить все, чтобы не было exception
				if (stPrice == null || stVol == null) {
					stPrice = 0d;
					stVol = 0d;
				} else {
					// округлить
					stPrice= Math.round (stPrice * stVol * 100.0) / 100.0;
				}
			} else {
				if (stServ.getServPrice() != null) {
					// указана услуга, откуда взять расценку
					stPrice = kartMng.getServPropByCD(rqn, calc, stServ.getServPrice(), "Цена", genDt);
				} else {
					// не указана услуга, откуда взять расценку
					stPrice = kartMng.getServPropByCD(rqn, calc, stServ, "Цена", genDt);
				}
			}
		}
		return stPrice;
	}

	/**
	 * Получить расценку по свыше соцнорме и 0 проживающих
	 * @param calc
	 * @param serv
	 * @param stPrice
	 * @param genDt
	 * @param rqn
	 * @param res
	 * @param isResid
	 * @param kart
	 * @return
	 * @throws EmptyStorable
	 */
	public ComplexPrice getUpStPrice(Calc calc, Serv serv, Double stPrice, Date genDt, int rqn, Result res, 
			Boolean isResid, Kart kart, Chng chng) throws EmptyStorable {
		if (serv.getId() == 35) {
		//	log.info("Serv.id={}", serv.getId());
		}

		ComplexPrice cp = new ComplexPrice();
		Serv upStServ = serv.getServUpst();
		Serv woKprServ = serv.getServWokpr();

		if (calc.getReqConfig().getOperTp()==1 && chng.getTp().getCd().equals("Изменение расценки (тарифа)") ) {
			// перерасчет по расценке
			if (upStServ != null) {
				cp.upStPrice = tarMng.getChngVal(calc, upStServ, genDt, "Изменение расценки (тарифа)", 1);
			}
			if (woKprServ != null) {
				cp.woKprPrice = tarMng.getChngVal(calc, woKprServ, genDt, "Изменение расценки (тарифа)", 1);
			}
		}
		
		if (cp.upStPrice == null) {
			if (upStServ != null) {
				// если не перерасчет или не найдена расценка по перерасчету
				if (upStServ.getServPrice() != null) {
					// указана услуга, откуда взять расценку
					cp.upStPrice = kartMng.getServPropByCD(rqn, calc, upStServ.getServPrice(), "Цена", genDt);
				} else {
					// не указана услуга, откуда взять расценку 
					cp.upStPrice = kartMng.getServPropByCD(rqn, calc, upStServ, "Цена", genDt);
				}
					
				if (cp.upStPrice == null) { 
					cp.upStPrice = 0d; 
				}
				
				if (cp.upStPrice == 0d && isResid) {
					// Добавить ошибку, что отсутствует расценка
					res.addErr(rqn, 5, kart, serv);
				}
				
			} else {
				cp.upStPrice = 0d;
			}
		}
		
		if (cp.woKprPrice == null) {
			// если пуста расценка в перерасчете
			if (woKprServ != null) {
				if (serv.getCd().equals("Горячая вода")) {
					// отдельный расчёт из за необходимости использовать расценки с учетом наличия полотенцесушителя
					// и изолированного стояка
					cp.woKprPrice = getHotWaterPriceByConditions(calc, calc.getKart(), genDt, rqn, woKprServ, true);
				} else if (woKprServ.getServPrice() != null) {
					// указана услуга, откуда взять расценку
					cp.woKprPrice = kartMng.getServPropByCD(rqn, calc, woKprServ.getServPrice(), "Цена", genDt);
				} else {
					// не указана услуга, откуда взять расценку
					//log.info("Check={}", woKprServ.getId());
					cp.woKprPrice = kartMng.getServPropByCD(rqn, calc, woKprServ, "Цена", genDt);
				}
	
				if (cp.woKprPrice == null && isResid) {
					// Добавить ошибку, что отсутствует расценка
					res.addErr(rqn, 6, kart, serv);
					// если не найдена цена с 0 проживающими, подставить цену по свыше соц.нормы, если и она не найдена, то по норме
					if (cp.upStPrice == null || cp.upStPrice == 0d) {
						cp.woKprPrice = stPrice;
					} else {
						cp.woKprPrice = cp.upStPrice;
					}
				} else if (cp.woKprPrice == 0d && isResid) {
					// Добавить ошибку, что отсутствует расценка
					res.addErr(rqn, 6, kart, serv);
				}
				
			} else {
				cp.woKprPrice = 0d;
			}
		}
		return cp;
	}
	
	/**
	 * Получить расценку по горячей воде, в зависимости от условий
	 * @param calc - объект, хранящий текущий дом, лиц.счет
	 * @param genDt - дата формирования
	 * @param rqn - id запроса
	 * @param serv - услуга
	 * @param isLookUpper - искать на уровне выше, если не найдено на текущем
	 * @return
	 * @throws EmptyStorable
	 */
	private Double getHotWaterPriceByConditions(Calc calc, Storable st, Date genDt, int rqn, Serv serv, boolean isLookUpper) throws EmptyStorable {
		//log.info("услуга name={}, id={}", serv.getName(), serv.getId());
		Double stPrice;
		
		Boolean isTowelHeatExist = parMng.getBool(rqn, st, "Наличие полотенцесушителя", genDt);
		if (isTowelHeatExist == null && isLookUpper) {
			// искать на уровне выше, если не найден на текущем
			isTowelHeatExist = parMng.getBool(rqn, calc.getHouse(), "Наличие полотенцесушителя", genDt);
		}
		
		Boolean isHotPipeInsulated = parMng.getBool(rqn, st, "Стояк ГВС изолирован", genDt);
		if (isHotPipeInsulated == null && isLookUpper) {
			// искать на уровне выше, если не найден на текущем
			isHotPipeInsulated = parMng.getBool(rqn, calc.getHouse(), "Стояк ГВС изолирован", genDt);
		}
		
		if (isTowelHeatExist == null) {
			isTowelHeatExist = false;
		}
		if (isHotPipeInsulated == null) {
			isHotPipeInsulated = false;
		}
		
		String cdProp = null;
		
		if (isHotPipeInsulated && isTowelHeatExist) {
			cdProp = "Цена с изолир.стояк.с полот.суш."; 
		} else if (!isHotPipeInsulated && isTowelHeatExist) {
			cdProp = "Цена с неизолир.стояк.c полот.суш."; 
		} else if (isHotPipeInsulated && !isTowelHeatExist) {
			cdProp = "Цена с изолир.стояк.без полот.суш."; 
		} else if (!isHotPipeInsulated && !isTowelHeatExist) {
			cdProp = "Цена с неизолир.стояк.без полот.суш."; 
		} else {
			// если не найдены параметры, то взять простую цену
			cdProp = "Цена"; 
		}
		
		stPrice = kartMng.getServPropByCD(rqn, calc, serv, cdProp, genDt);
		//log.info("параметр={}, цена={}", cdProp, stPrice);
		if (cdProp != "Цена" && stPrice == null) {
			// Если заведён параметр изолир.стояк или полотенцесуш. и не заведён соответствующий тип цены
			// то поискать по обычной цене
			cdProp = "Цена"; 
			stPrice = kartMng.getServPropByCD(rqn, calc, serv, cdProp, genDt);
		}
		return stPrice;
	}

	
}
