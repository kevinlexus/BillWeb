package com.ric.bill;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import com.ric.bill.excp.EmptyStorable;
import com.ric.bill.mm.ParMng;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.tr.Serv;
import com.ric.web.AppConfig;

import org.apache.wicket.spring.injection.annot.SpringBean;

/**
 * Результат выполнения начисления
 * @author lev
 *
 */
@Slf4j
public class Result {

    // Внутренний класс ошибки
	public class Err {
		// Услуга
    	Serv serv;
    	// Id ошибки
    	int errId;
    	// Описание некритической ошибки (Ошибки, которая не приведёт к остановке процесса начисления)
		String errMsg;
		
		public Err(Serv serv, int errId, String errMsg) {
			super();
			this.serv = serv;
			this.errId = errId;
			this.errMsg = errMsg;
		}
		
		public Serv getServ() {
			return serv;
		}
		public void setServ(Serv serv) {
			this.serv = serv;
		}
		public int getErrId() {
			return errId;
		}
		public void setErrId(int errId) {
			this.errId = errId;
		}
		public String getErrMsg() {
			return errMsg;
		}
		public void setErrMsg(String errMsg) {
			this.errMsg = errMsg;
		}
	}

	// код ошибки
	private int err;
	// Список ошибок по услуге
    private List<Err> lstErr;
    static ApplicationContext ctx = null;
    
	// Конструктор
	public Result() {
		super();
		lstErr = new ArrayList<Err>();
	}

	public int getErr() {
		return err;
	}

	public void setErr(int err) {
		this.err = err;
	}

	public List<Err> getLstErr() {
		return lstErr;
	}

	public void setLstErr(List<Err> lstErr) {
		this.lstErr = lstErr;
	}

	@Autowired
	  private AutowireCapableBeanFactory autowireBeanFactory;
	
	/**
	 * Добавить новое значение ошибки, не дублируя
	 * @param errId - код ошибки
	 * @param serv - услуга
	 * @param err - ошибка
	 * @throws EmptyStorable 
	 */
	public void addErr(int rqn, int errId, Kart kart, Serv serv) throws EmptyStorable {
		Err findErr = lstErr.stream().filter(t-> t.getServ().equals(serv) && t.getErrId()==errId).findFirst().orElse(null);
		if (findErr == null){
			String str = null;
			
			// Получаем bean таким образом, так как находимся не в сервисе
			ParMng parMng = AppConfig.getContext().getBean(ParMng.class);
			
			switch (errId) {
			case 2:
				if (!Utl.nvl(parMng.getBool(rqn, serv, "Контроль наличия услуги св.нормы"), false) ) {
					return;
				}
				str="В тарифе отсутствует услуга свыше соцнормы!";
				break;
			case 3:
				if (!Utl.nvl(parMng.getBool(rqn, serv, "Контроль наличия услуги 0 прожив"), false)) {
					return;
				}
				str="В тарифе отсутствует услуга с 0 проживающими!";
				break;
			case 4:
				str="Не указанна форма собственности!";
				break;
			case 5:
				if (!Utl.nvl(parMng.getBool(rqn, serv, "Контроль наличия расценки св.нормы"), false)) {
					return;
				}
				str="В тарифе отсутствует расценка свыше соцнормы!";
				break;
			case 6:
				if (Utl.nvl(!parMng.getBool(rqn, serv, "Контроль наличия расценки 0 прожив"), false)) {
					return;
				}
				str="В тарифе отсутствует расценка с 0 проживающими!";
				break;
			case 7:
				if (!Utl.nvl(parMng.getBool(rqn, serv, "Контроль наличия услуги по нормативу"), false)) {
					return;
				}
				str="В тарифе отсутствует нормативная услуга!";
				break;
			case 8:
				if (Utl.nvl(!parMng.getBool(rqn, serv, "Контроль наличия расценки по нормативу"), false)) {
					return;
				}
				str="В тарифе отсутствует расценка по нормативу!";
				break;
			}
			
			lstErr.add(new Err(serv, errId, str));
			log.info("ОШИБКА! Услуга id={}, cd={}, {}, lsk={}", serv.getId(), serv.getCd(), str, kart.getLsk());
			
		}
		
	}
	
}
