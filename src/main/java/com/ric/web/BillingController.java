package com.ric.web;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.ric.bill.BillServ;
import com.ric.bill.Config;
import com.ric.bill.RequestConfig;
import com.ric.bill.Result;
import com.ric.bill.Utl;
import com.ric.bill.dto.AddrTpDTO;
import com.ric.bill.dto.AreaDTO;
import com.ric.bill.dto.DTOBuilder;
import com.ric.bill.dto.KoDTO;
import com.ric.bill.dto.LstDTO;
import com.ric.bill.dto.PayordCmpDTO;
import com.ric.bill.dto.PayordDTO;
import com.ric.bill.dto.PayordFlowDTO;
import com.ric.bill.dto.PayordGrpDTO;
import com.ric.bill.dto.PeriodReportsDTO;
import com.ric.bill.dto.RepItemDTO;
import com.ric.bill.dto.ServDTO;
import com.ric.bill.excp.WrongDate;
import com.ric.bill.mm.LstMng;
import com.ric.bill.mm.OrgMng;
import com.ric.bill.mm.PayordMng;
import com.ric.bill.mm.ReportMng;
import com.ric.bill.mm.SecMng;
import com.ric.bill.mm.ServMng;
import com.ric.bill.mm.TarifMng;
import com.ric.bill.model.bs.PeriodReports;
import com.ric.bill.model.fn.Payord;
import com.ric.bill.model.fn.PayordCmp;
import com.ric.bill.model.fn.PayordFlow;
import com.ric.bill.model.fn.PayordGrp;

@EnableCaching
@RestController
@ComponentScan({ "com.ric.bill" })
// -если убрать - не найдёт бины, например billServ
@EntityScan(basePackages = "com.ric.bill")
@EnableAutoConfiguration
@Scope("prototype")
@Slf4j
public class BillingController {

	@PersistenceContext
	private EntityManager em;
	@Autowired
	private ApplicationContext ctx;
	@Autowired
	private Config config;
	@Autowired
	private LstMng lstMng;
	@Autowired
	private OrgMng orgMng;
	@Autowired
	private ServMng servMng;
	@Autowired
	private TarifMng tarMng;
	@Autowired
	private PayordMng payordMng;
	@Autowired
	private DTOBuilder dtoBuilder;
	@Autowired
	private SecMng secMng;
	@Autowired
	private ReportMng repMng;	
	@Autowired
    private DataSource dataSource;
	
	/**
	 * Получить отчет по платежкам
	 * @param modelMap - служеб. Spring
	 * @param modelAndView - служеб. Spring
	 * @param periodId - Id периода
	 * @return - служеб. Spring
	 */
	@RequestMapping(value = "/rep/payordFlow1", method = RequestMethod.GET, produces = "application/pdf;charset=UTF-8")
	public ModelAndView repPayordFlow1(ModelMap modelMap, ModelAndView modelAndView,
				@RequestParam(value = "periodId") Integer periodId 
				) {
		log.info("GOT /rep/payordFlow1 with periodId={}", periodId);
		PeriodReports pr = em.find(PeriodReports.class, periodId);
		if (pr.getDt() !=null) {
			List<RepItemDTO> lst = payordMng.getPayordRep(pr);
			JRDataSource datasource = new JRBeanCollectionDataSource(lst, true);
			modelMap.put("datasource", datasource);
			modelMap.put("format", "pdf");
			modelMap.put("strDt", Utl.getStrFromDate(pr.getDt()));
			modelAndView = new ModelAndView("repPayordFlow1", modelMap);
			return modelAndView;
		} else {
			return null;
		}
		
	}

	/**
	 * Получить отчет по платежкам - 2,3
	 * @param modelMap - служеб. Spring
	 * @param modelAndView - служеб. Spring
	 * @param periodId1 - Id нач. периода
	 * @param periodId2 - Id кон. периода
	 * @return - служеб. Spring 
	 */
	@RequestMapping(value = "/rep/payordFlow2", method = RequestMethod.GET, produces = "application/pdf;charset=UTF-8")
	public ModelAndView repPayordFlow2(ModelMap modelMap, ModelAndView modelAndView,
				@RequestParam(value = "periodId1") Integer periodId1,
				@RequestParam(value = "periodId2") Integer periodId2, 
				@RequestParam(value = "repCd") String repCd 
				) {
		log.info("GOT /rep/payordFlow2 with periodId1={}, periodId2={}", periodId1, periodId2);
		PeriodReports pr1 = em.find(PeriodReports.class, periodId1);
		PeriodReports pr2 = em.find(PeriodReports.class, periodId2);
		if (pr1.getMg() != null && pr2.getMg() != null ) {
			if (pr1.getMg().equals(pr2.getMg())) {
				modelMap.put("period", "за "+Utl.getPeriodName(pr1.getMg(), 1)+" г."  );
			} else {
				modelMap.put("period", "c "+Utl.getPeriodName(pr1.getMg(), 0)+" по "+Utl.getPeriodName(pr2.getMg(), 1)+" г." );
			}
			modelMap.put("datasource", dataSource);
			modelMap.put("format", "pdf");
			modelMap.put("mg1", pr1.getMg());
			modelMap.put("mg2", pr2.getMg());
			modelMap.put("repCd", repCd);
			modelAndView = new ModelAndView("repPayordFlow2", modelMap);
			return modelAndView;
		} else {
			return null;
		}
	}

	/**
 	 * Получить периоды для элементов интерфейса
 	 * 
 	 * @param repCd - CD отчета
 	 * @param tp    - тип периода 0 - по месяцам, 1 - по дням
 	 * @return
 	 */
 	@RequestMapping("/rep/getPeriodReports")
 	@ResponseBody
 	public List<PeriodReportsDTO> getPeriodReports(
 			@RequestParam(value = "repCd") String repCd,
 			@RequestParam(value = "tp", defaultValue = "0") Integer tp) {
 
 		log.info("GOT /rep/getPeriodReports with repCd={}, tp={}", repCd, tp);
 		return repMng.getPeriodByCD(repCd, tp);
 
 	}
	 
	// Получить все движения по платежкам по Типу и Дате
	@RequestMapping("/payord/getPayordFlowByTpDt")
	@ResponseBody
	public List<PayordFlowDTO> getPayordFlowByTpDt(
			@RequestParam(value = "tp") Integer tp,
			@RequestParam(value = "dt") String dt) {
		log.info("GOT /payord/getPayordFlowByTpDt with tp={}, dt={}", tp, dt);
		Date genDt = null;
		if (dt != null && dt.length()!=0) {
			genDt = Utl.getDateFromStr(dt);
		}
		return dtoBuilder.getPayordFlowDTOLst(payordMng.getPayordFlowByTpDt(tp, genDt));
	}

	// Сохранить движение по платежкам
	@RequestMapping(value = "/payord/setPayordFlow", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public String setPayordFlow(@RequestBody List<PayordFlowDTO> lst) {

		log.info("GOT /payord/setPayordFlow");
		lst.stream().forEach(t -> payordMng.setPayordFlowDto(t));
		return null;
	}

	// Удалить движение платежки
	@RequestMapping(value = "/payord/delPayordFlow", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public String delPayordFlow(@RequestBody List<PayordFlowDTO> lst) {

		log.info("GOT /payord/delPayordFlow");
		lst.stream().forEach(t -> payordMng.delPayordFlowDto(t));
		return null;
	}
	
	// Добавить движение платежки
	@RequestMapping(value = "/payord/addPayordFlow", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public List<PayordFlowDTO> addPayordFlow(@RequestBody List<PayordFlowDTO> lst) {
		log.info("GOT /payord/addPayordFlow");
		List<PayordFlow> lst2 = new ArrayList<PayordFlow>();
		
		// добавить созданные группы платежек в коллекцию
		lst.stream().forEach(t -> lst2.add( payordMng.addPayordFlowDto(t)) );
		// обновить группы платежки из базы
		lst2.stream().forEach(t -> payordMng.refreshPayordFlow(t) );
		
		return dtoBuilder.getPayordFlowDTOLst(lst2);
	}

	// Получить все группы платежек
	@RequestMapping("/payord/getPayordGrpAll")
	@ResponseBody
	public List<PayordGrpDTO> getPayordGrpAll() {

		log.info("GOT /payord/getPayordGrpAll");
		return dtoBuilder.getPayordGrpDTOLst(payordMng.getPayordGrpAll());
	}

	// Сохранить группу платежки
	@RequestMapping(value = "/payord/setPayordGrp", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public String setPayordGrp(@RequestBody List<PayordGrpDTO> lst) {

		log.info("GOT /payord/setPayordGrp");
		lst.stream().forEach(t -> payordMng.setPayordGrpDto(t));
		return null;
	}

	// Добавить группу платежки
	@RequestMapping(value = "/payord/addPayordGrp", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public List<PayordGrpDTO> addPayordGrp(@RequestBody List<PayordGrpDTO> lst) {
		log.info("GOT /payord/addPayordGrp");
		List<PayordGrp> lst2 = new ArrayList<PayordGrp>();
		
		// добавить созданные группы платежек в коллекцию
		lst.stream().forEach(t -> lst2.add( payordMng.addPayordGrpDto(t)) );
		// обновить группы платежки из базы
		lst2.stream().forEach(t -> payordMng.refreshPayordGrp(t) );
		
		return dtoBuilder.getPayordGrpDTOLst(lst2);
	}
	
	// Удалить группу платежки
	@RequestMapping(value = "/payord/delPayordGrp", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public String delPayordGrp(@RequestBody List<PayordGrpDTO> lst) {

		log.info("GOT /payord/delPayordGrp");
		lst.stream().forEach(t -> payordMng.delPayordGrpDto(t));
		return null;
	}

	// Удалить платежку
	@RequestMapping(value = "/payord/delPayord", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public String delPayord(@RequestBody List<PayordDTO> lst) {

		log.info("GOT /payord/delPayord");
		lst.stream().forEach(t -> payordMng.delPayordDto(t));
		return null;
	}

	// Удалить формулу платежки
	@RequestMapping(value = "/payord/delPayordCmp", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public String delPayordCmp(@RequestBody List<PayordCmpDTO> lst) {

		log.info("GOT /payord/delPayordCmp");
		lst.stream().forEach(t -> payordMng.delPayordCmpDto(t));
		return null;
	}
	
	/**
	 * Получить все компоненты платежки по её ID
	 * 
	 * @return
	 */
	@RequestMapping("/payord/getPayordCmp")
	@ResponseBody
	public List<PayordCmpDTO> getPayordCmp(
			@RequestParam(value = "payordId") Integer payordId) {

		log.info("GOT /payord/getPayordCmp with payordId={}", payordId);
		return dtoBuilder.getPayordCmpDTOLst(payordMng
				.getPayordCmpByPayordId(payordId));

	}

	// сохранить компонент платежки
	@RequestMapping(value = "/payord/setPayordCmp", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public String setPayordCmp(@RequestBody List<PayordCmpDTO> lst) {

		log.info("GOT /payord/setPayordCmp");
		lst.stream().forEach(t -> payordMng.setPayordCmpDto(t));
		return null;
	}
	
	
	/**
	 * Получить все платежки
	 * 
	 * @return
	 */
	/*@RequestMapping("/payord/getPayordAll")
	@ResponseBody
	public List<PayordDTO> getPayordAll() {

		log.info("GOT /payord/getPayordAll");
		return dtoBuilder.getPayordDTOLst(payordMng.getPayordAll());

	}

	/**
	 * Получить платежки по Id группы
	 * 
	 * @return
	 */
	@RequestMapping("/payord/getPayord")
	@ResponseBody
	public List<PayordDTO> getPayord(
			@RequestParam(value = "payordGrpId") Integer payordGrpId) {

		log.info("GOT /payord/getPayord with payordGrpId={}", payordGrpId);
		if (payordGrpId==0) {
			return dtoBuilder.getPayordDTOLst(payordMng.getPayordAll());
		} else {
			return dtoBuilder.getPayordDTOLst(payordMng
					.getPayordByPayordGrpId(payordGrpId));
		}
	}


	/**
	 * Сохранить платежку
	 * @param lst - DTO платежки
	 * @return
	 */
	@RequestMapping(value = "/payord/setPayord", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public String setPayord(@RequestBody List<PayordDTO> lst) {

		log.info("GOT /payord/setPayord");
		lst.stream().forEach(t -> payordMng.setPayordDto(t));
		return null;
	}

	/**
	 * Добавить платежку
	 * @param lst - DTO платежки
	 * @return
	 */
	@RequestMapping(value = "/payord/addPayord", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public List<PayordDTO> addPayord(@RequestBody List<PayordDTO> lst) {
		log.info("GOT /payord/addPayord");
		List<Payord> lst2 = new ArrayList<Payord>();
		
		// добавить созданные группы платежек в коллекцию
		lst.stream().forEach(t -> lst2.add( payordMng.addPayordDto(t)) );
		// обновить группы платежки из базы
		lst2.stream().forEach(t -> payordMng.refreshPayord(t) );
		
		return dtoBuilder.getPayordDTOLst(lst2);
	}
	

	/**
	 * Создать формулу платежки
	 * @param lst - DTO формулу платежки
	 * @return
	 */
	@RequestMapping(value = "/payord/addPayordCmp", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
	@ResponseBody
	public List<PayordCmpDTO> addPayordCmp(@RequestBody List<PayordCmpDTO> lst) {
		log.info("GOT /payord/addPayordCmp");
		List<PayordCmp> lst2 = new ArrayList<PayordCmp>();
		
		// добавить созданные группы платежек в коллекцию
		lst.stream().forEach(t -> lst2.add( payordMng.addPayordCmpDto(t)) );
		// обновить группы платежки из базы
		lst2.stream().forEach(t -> payordMng.refreshPayordCmp(t) );
		
		return dtoBuilder.getPayordCmpDTOLst(lst2);
	}

	
	/**
	 * Получить список типов адресов
	 * @param tp - 0 - весь список, 1 - ограниченный основными типами
	 * @return
	 */
	@RequestMapping("/base/getAddrTp")
	@ResponseBody
	public List<AddrTpDTO> getAddrTp(@RequestParam(value = "tp") Integer tp) {
		log.info("GOT /base/getAddrTp");
		return dtoBuilder.getAddrTpDTOLst(lstMng.getAddrTpByTp(tp));
	}

	/**
	 * Получить список по типу
	 * 
	 * @param tp - тип списка
	 *            
	 * @return
	 */
	@RequestMapping("/base/getLstByTp")
	@ResponseBody
	public List<LstDTO> getLstByTp(@RequestParam(value = "tp") String tp) {
		log.info("GOT /base/getLstByTp with tp={}", tp);
		return dtoBuilder.getLstDTOLst(lstMng.getByTp(tp));
	}

	/**
	 * Получить список объектов определённого типа, с фильтром
	 * 
	 * @param addrTp - тип адреса
	 *            
	 * @return
	 */
	@RequestMapping("/base/getKoAddrTpFlt")
	@ResponseBody
	public List<KoDTO> getKoAddrTpFlt(@RequestParam(value = "addrTp") Integer addrTpId,
									 @RequestParam(value = "flt") String flt
			) {
		log.info("GOT /base/getKoAddrTpFlt with addrTp={}, flt={}", addrTpId, flt);
		
//		Ko ko = em.find(Ko.class, 769857);
//		log.info("Org={} cd={}",ko.getOrg(), ko.getAddrTp().getCd());
		
//		dtoBuilder.getKoDTOLst(lstMng.getKoByAddrTpFlt(addrTpId, flt)).stream().forEach(
//				t-> {log.info("lst={}, {}", t.getId(), t.getName());});
		
		//logdtoBuilder.getKoDTOLst(lstMng.getKoByAddrTpFlt(addrTpId, flt)).size()
		return dtoBuilder.getKoDTOLst(lstMng.getKoByAddrTpFlt(addrTpId, flt));
	}

	
	/**
	 * Получить список организаций, доступных текущему пользователю по роли и
	 * действию
	 * 
	 * @return
	 */
	@RequestMapping("/sec/getOrgCurUser")
	@ResponseBody
	public List<KoDTO> getOrgCurUser(
			@RequestParam(value = "roleCd") String roleCd,
			@RequestParam(value = "actCd") String actCd) {
		log.info("GOT /sec/getOrgCurUser with: roleCd={}, actCd={}", roleCd, actCd);
		return secMng.getKoCurUser(roleCd, actCd);
	}

	/**
	 * Получить список всех организаций
	 * 
	 * @return
	 */
	@RequestMapping("/base/getOrgAll")
	@ResponseBody
	public List<KoDTO> getOrgAll() {
		log.info("GOT /base/getOrgAll");
		return dtoBuilder.getOrgDTOLst(orgMng.getOrgAll());
	}

	/**
	 * Получить список всех услуг
	 * 
	 * @return
	 */
	@RequestMapping("/base/getServAll")
	@ResponseBody
	public List<ServDTO> getServAll() {
		log.info("GOT /base/getServAll");
		return dtoBuilder.getServDTOLst(servMng.getServAll());
	}

	/**
	 * Получить список всех типов объекта сбора инф.
	 * 
	 * @return
	 */
	@RequestMapping("/base/getAreaAll")
	@ResponseBody
	public List<AreaDTO> getAreaAll() {
		log.info("GOT /base/getAreaAll");
		return dtoBuilder.getAreaDTOLst(lstMng.getAreaAll());
	}

	private Boolean checkDate(String genDt1, String genDt2) {
		// проверка на заполненные даты, если указаны
		if ((genDt1.length() > 0 && genDt2.length() == 0) || (genDt1.length() == 0 && genDt2.length() > 0)) {
			return false;
		} else if (genDt1.length() == 0 && genDt2.length() == 0) {
			return true;
		}
		
		Date dt1 = Utl.getDateFromStr(genDt1);
		Date dt2 = Utl.getDateFromStr(genDt2);
		
		Date firstDt = Utl.getFirstDate(dt1);
		Date lastDt = Utl.getLastDate(dt1);
		
		// проверить, что даты в одном диапазоне
		if (!(Utl.between(dt1, firstDt, lastDt) && Utl.between(dt2, firstDt, lastDt)))  {
			return false;
		}
			
		return true;
	}
	
	/**
	 * Сформировать платежки
	 * @param genDt - дата формирования
	 * @param isFinal - финальная платежка 
	 * @param isEndMonth - итоговое формирование сальдо
	 * @return
	 */
	@RequestMapping("/genPayord")
	public String genPayord(
			@RequestParam(value = "genDt", required = false) String genDt,
			@RequestParam(value = "isFinal") Boolean isFinal,
			@RequestParam(value = "isEndMonth") Boolean isEndMonth
			) {
		
		log.info("GOT /genPayord with: genDt={}, isFinal={}, isEndMonth={}", genDt, isFinal, isEndMonth);
		Date dt = Utl.getDateFromStr(genDt);
		try {
			payordMng.genPayord(dt, isFinal, isEndMonth);
		} catch (WrongDate | ParseException e) {
			e.printStackTrace();
			// TODO сделать возврат ошибки!
		}
		return null;
	}
	
	@RequestMapping("/chrglsk")
	public String chrgLsk(
			@RequestParam(value = "lsk", defaultValue = "00000000") Integer lsk,
			@RequestParam(value = "dist", defaultValue = "0") String dist,
			@RequestParam(value = "tp", defaultValue = "0") String tp,
			@RequestParam(value = "chngId", defaultValue = "") String chngId,
			@RequestParam(value = "dt1", defaultValue = "", required = false) String genDt1,
			@RequestParam(value = "dt2", defaultValue = "", required = false) String genDt2,
			@RequestParam(value = "user", defaultValue = "", required = false) String user
			) {
		
		log.info("GOT /chrglsk with: lsk={}, dist={}, tp={}, chngId={}, dt1={}, dt2={}", lsk,
				dist, tp, chngId, genDt1, genDt2);
		
		if (!checkDate(genDt1, genDt2)) {
			log.info("Заданы некорректные даты dt1={}, dt2={}!", genDt1, genDt2);
			return "ERROR IN DATES";
		}
		
		// получить уникальный номер запроса
		int rqn = config.incNextReqNum();

		log.info("RQN={}, user={}", rqn, user);
		long beginTime = System.currentTimeMillis();

		// получить доступ к лиц.счету
		int i = 0;
		while (!config.checkLsk(lsk)) {
			i++;
			if (i > 100) {
				/*log.info(
						"********ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ! /chrglsk with: lsk={}",
						lsk);
				log.info(
						"********НЕ ВОЗМОЖНО РАЗБЛОКИРОВАТЬ ЛС В ТЕЧЕНИИ 10 сек! /chrglsk with: lsk={}",
						lsk);*/
				i = 0;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return "ERROR";
			}
		}

		Future<Result> fut;

		// если пустой ID перерасчета
		Integer chId = null;
		if (chngId.length() != 0 && chngId != null) {
			log.info("chngId={}", chngId);
			chId = Integer.valueOf(chngId);
		}

		RequestConfig reqConfig = ctx.getBean(RequestConfig.class);
		long endTime1 = System.currentTimeMillis() - beginTime;
		beginTime = System.currentTimeMillis();

		reqConfig.setUp(config, dist, tp, chId, rqn, genDt1, genDt2);

		long endTime2 = System.currentTimeMillis() - beginTime;
		beginTime = System.currentTimeMillis();

		BillServ billServ = ctx.getBean(BillServ.class); // добавил, было Autowired
		// Расчет начисления
		fut = billServ.chrgLsk(reqConfig, null, lsk);

		long endTime3 = System.currentTimeMillis() - beginTime; // время
																// инициализации
																// billServ bean
		beginTime = System.currentTimeMillis();

		// ждать окончание потока
		try {
			fut.get();
		} catch (InterruptedException | ExecutionException e1) {
			e1.printStackTrace();
			log.info("BEGINING UNLOCK /chrglsk with: lsk={}", lsk);
			config.unCheckLsk(lsk); // снять лицевой с обработки
			log.info("ENDING UNLOCK /chrglsk with: lsk={}", lsk);
			return "ERROR";
		}

		long endTime4 = System.currentTimeMillis() - beginTime;

		log.info(
				"TIMING: доступ.к л.с.={}, конфиг={}, инит. bean={}, расчет={}",
				endTime1, endTime2, endTime3, endTime4);

		try {
			if (fut.get().getErr() == 0) {
				log.info("BEGINING UNLOCK /chrglsk with: lsk={}", lsk);
				config.unCheckLsk(lsk); // снять лицевой с обработки
				log.info("ENDING UNLOCK /chrglsk with: lsk={}", lsk);
				log.info("OK /chrglsk with: lsk={}, dist={}, tp={}, chngId={}",
						lsk, dist, tp, chngId);
				// Создать и отправить список некритических ошибок, если есть
				String msg = "";
				log.info("size err={}", fut.get().getLstErr().size());
				for (Result.Err t: fut.get().getLstErr()) {
					msg = msg.concat("Услуга:"+t.getServ().getName()+", "+t.getErrMsg()+"; ");
					log.info("msg={}", msg);
				}
				return "OK"+(msg.equals("") ? "" : ":"+msg);
			} else {
				log.info("BEGINING UNLOCK /chrglsk with: lsk={}", lsk);
				config.unCheckLsk(lsk); // снять лицевой с обработки
				log.info("ENDING UNLOCK /chrglsk with: lsk={}", lsk);
				log.info(
						"ERROR /chrglsk with: lsk={}, dist={}, tp={}, chngId={}",
						lsk, dist, tp, chngId);
				return "ERROR";
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.info("BEGINING UNLOCK /chrglsk with: lsk={}", lsk);
			config.unCheckLsk(lsk); // снять лицевой с обработки
			log.info("ENDING UNLOCK /chrglsk with: lsk={}", lsk);
			return "ERROR";
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.info("BEGINING UNLOCK /chrglsk with: lsk={}", lsk);
			config.unCheckLsk(lsk); // снять лицевой с обработки
			log.info("ENDING UNLOCK /chrglsk with: lsk={}", lsk);
			return "ERROR";
		}

	}

	@RequestMapping("/chrgall")
	public String chrgAll(
			@RequestParam(value = "dist", defaultValue = "0", required = true) String dist,
			@RequestParam(value = "houseId", defaultValue = "", required = false) Integer houseId,
			@RequestParam(value = "areaId", defaultValue = "", required = false) Integer areaId,
			@RequestParam(value = "dt1", defaultValue = "", required = false) String genDt1,
			@RequestParam(value = "dt2", defaultValue = "", required = false) String genDt2,
			@RequestParam(value = "user", defaultValue = "", required = false) String user,
			@RequestParam(value = "isAutoChrg", defaultValue = "0", required = false) Integer isAutoChrg
			) {

		log.info("GOT /chrgall with: houseId={}, dist={}, areaId={}, dt1={}, dt2={}, isAutoChrg={}", houseId,
				dist, areaId, genDt1, genDt2, isAutoChrg);
		
		if (!checkDate(genDt1, genDt2)) {
			log.info("Заданы некорректные даты dt1={}, dt2={}!", genDt1, genDt2);
			return "ERROR IN DATES";
		}

		// получить уникальный номер запроса
		int rqn = config.incNextReqNum();
		log.info("RQN={}, user={}", rqn, user);

		Future<Result> fut = null;

		RequestConfig reqConfig = ctx.getBean(RequestConfig.class);
		reqConfig.setUp(config, dist, "0", null, rqn, genDt1, genDt2);

		BillServ billServ = ctx.getBean(BillServ.class); // добавил, было Autowired
		fut = billServ.chrgAll(reqConfig, houseId, areaId);

		while (!fut.isDone()) {
			try {
				Thread.sleep(100);
				// 100-millisecond Задержка
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			if (fut.get().getErr() == 0) {
				return "OK";
			} else {
				return "ERROR";
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "ERROR";
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "ERROR";
		}
	}

}