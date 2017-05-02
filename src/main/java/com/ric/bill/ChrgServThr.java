package com.ric.bill;

import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import com.ric.bill.Calc;
import com.ric.bill.ChrgServ;
import com.ric.bill.Result;
import com.ric.bill.excp.ErrorWhileChrg;
import com.ric.bill.model.fn.ChngLsk;
import com.ric.bill.model.tr.TarifKlsk;

/**
 * Асинхронный поток начисления, выполняет вызовы функций начисления
 * сервис необходим, так как @Transactional должно находиться в другом сервисе,
 * подробнее тут: http://stackoverflow.com/questions/11275471/calling-transactional-methods-from-another-thread-runnable
 * @author lev
 *
 */
@Service
@Scope("prototype")
@Slf4j
public class ChrgServThr {

	@Autowired
	private ApplicationContext ctx;
    @PersistenceContext
    private EntityManager em;
	
	@Async
	public Future<Result> chrgAndSaveLsk(Calc calc) throws ErrorWhileChrg {
		ChrgServ chrgServ = ctx.getBean(ChrgServ.class);
		//загрузить все Lazy параметры, чтобы не было concurrensy issue в потоках например, на getDbl()
		//ну или использовать EAGER в дочерних коллекциях, что более затратно
		calc.getKart().getDw().size();
		calc.getKart().getTarifklsk().size();
		for (TarifKlsk k : calc.getKart().getTarifklsk()) {
			k.getTarprop().size();
		}
		calc.getKart().getReg().size();
		calc.getKart().getRegState().size();

		calc.getKart().getMlog().size();

		calc.getHouse().getTarifklsk().size();
		for (TarifKlsk k : calc.getHouse().getTarifklsk()) {
			k.getTarprop().size();
		}

		calc.getArea().getTarifklsk().size();
		for (TarifKlsk k : calc.getArea().getTarifklsk()) {
			k.getTarprop().size();
		}
		
		if (calc.getReqConfig().getChng() != null) {
			calc.getReqConfig().getChng().getChngLsk().size();
			for (ChngLsk a : calc.getReqConfig().getChng().getChngLsk()) {
				a.getChngVal().size();
			}
		}
		
		//Выполнить начисление
		Result res = chrgServ.chrgLsk(calc);
		//Сохранить результат
		if (res.err==0) {
			chrgServ.save(calc.getKart().getLsk()); 
		}
		
		return new AsyncResult<Result>(res);
	}
	
}
