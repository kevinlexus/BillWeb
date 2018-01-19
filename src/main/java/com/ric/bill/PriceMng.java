package com.ric.bill;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ric.bill.Calc;
import com.ric.bill.Result;
import com.ric.bill.Utl;
import com.ric.bill.excp.EmptyStorable;
import com.ric.bill.mm.KartMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.tr.Serv;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Сервис получения расценки
 * @author Leo
 *
 */
@Service
@Slf4j
public class PriceMng {

	@Autowired
	private ParMng parMng;
	@Autowired
	private KartMng kartMng;

	/**
	 * DTO для передачи составляющих цены
	 * @author Leo
	 *
	 */
	@Getter@Setter
	public
	class ComplexPrice {
		private Double upStPrice;
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
	public Double getStandartPrice(Calc calc, Kart kart, Serv serv, Date genDt, int rqn, Serv stServ) throws EmptyStorable {
		Double stPrice;
		if (Utl.nvl(parMng.getDbl(rqn, serv, "Вариант расчета по общей площади-3"), 0d) == 1d) {
			// обычно услуги ХВ, ГВ, Эл на Общее имущество (ОИ)
			// по этому варианту получить расценку от услуги, хранящей расценку, умножить на норматив, округлить
			Double stVol = kartMng.getServPropByCD(rqn, calc, serv, "Норматив", genDt);
			if (stServ.getServPrice()==null) {
				// если пуста услуга по которой хранится расценка, получить из текущей услуги - по нормативу
				stPrice = kartMng.getServPropByCD(rqn, calc, stServ, "Цена", genDt);
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
			
			if (serv.getCd().equals("Горячая вода")) {
				// отдельный расчёт из за необходимости использовать расценки с учетом наличия полотенцесушителя
				// и изолированного стояка
				
				stPrice = getHotWaterPriceByConditions(calc, kart, genDt, rqn, stServ);

			} else {
				// прочие варианты
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
			Boolean isResid, Kart kart) throws EmptyStorable {
		ComplexPrice cp = new ComplexPrice();
		Serv upStServ = serv.getServUpst();
		Serv woKprServ = serv.getServWokpr();
		if (upStServ != null) {
			if (serv.getCd().equals("Горячая вода")) {
				// отдельный расчёт из за необходимости использовать расценки с учетом наличия полотенцесушителя
				// и изолированного стояка
				cp.upStPrice = getHotWaterPriceByConditions(calc, kart, genDt, rqn, serv);
			} if (upStServ.getServPrice() != null) {
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

		if (woKprServ != null) {
			if (serv.getCd().equals("Горячая вода")) {
				// отдельный расчёт из за необходимости использовать расценки с учетом наличия полотенцесушителя
				// и изолированного стояка
				cp.woKprPrice = getHotWaterPriceByConditions(calc, kart, genDt, rqn, serv);
			} if (woKprServ.getServPrice() != null) {
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

		return cp;
	}
	
	/**
	 * Получить расценку по горячей воде, в зависимости от условий
	 * @param calc
	 * @param kart
	 * @param genDt
	 * @param rqn
	 * @param serv
	 * @return
	 * @throws EmptyStorable
	 */
	private Double getHotWaterPriceByConditions(Calc calc, Kart kart, Date genDt, int rqn, Serv serv) throws EmptyStorable {
		Double stPrice;
		Boolean isTowelHeatExist = Utl.nvl(parMng.getBool(rqn, kart, "Наличие полотенцесушителя", genDt), false);
		Boolean isHotPipeInsulated = Utl.nvl(parMng.getBool(rqn, kart, "Стояк ГВС изолирован", genDt), false);
		String cdProp = null;
		
		if (isHotPipeInsulated && isTowelHeatExist) {
			cdProp = "Цена с изолир.стояк.с полот.суш."; 
		} else if (!isHotPipeInsulated && isTowelHeatExist) {
			cdProp = "Цена с неизолир.стояк.c полот.суш."; 
		} else if (isHotPipeInsulated && !isTowelHeatExist) {
			cdProp = "Цена с изолир.стояк.без полот.суш."; 
		} else if (!isHotPipeInsulated && !isTowelHeatExist) {
			cdProp = "Цена с неизолир.стояк.без полот.суш."; 
		}
		
		stPrice = kartMng.getServPropByCD(rqn, calc, serv, cdProp, genDt);
		return stPrice;
	}

	
}
