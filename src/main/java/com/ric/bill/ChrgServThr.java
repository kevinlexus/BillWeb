package com.ric.bill;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ric.bill.excp.ErrorWhileChrg;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.fn.ChngLsk;
import com.ric.bill.model.tr.TarifKlsk;

import lombok.extern.slf4j.Slf4j;

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
	@Autowired
	private Config config;

	@Async
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public Future<Result> chrgAndSaveLsk(RequestConfig reqConfig, Integer kartId) throws ErrorWhileChrg, ExecutionException {
//		Result res = new Result();
//		return new AsyncResult<Result>(res );

		// под каждый поток - свой Calc
		Calc calc = new Calc(reqConfig);
		Kart kart = em.find(Kart.class, kartId);
		calc.setKart(kart);
		calc.setHouse(kart.getKw().getHouse());
		if (calc.getArea() ==null) {
			throw new ErrorWhileChrg("Ошибка! По записи house.id={}, в его street, не заполнено поле area!");
		}

		Integer lsk = calc.getKart().getLsk();
		Integer houseId = calc.getKart().getKw().getHouse().getId();
		// блокировка лиц.счета
		int waitTick = 0;
		while (!config.lock.setLockChrgLsk(calc.getReqConfig().getRqn(), lsk, houseId)) {
			waitTick++;
			if (waitTick > 60) {
				log.error(
						"********ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!ВНИМАНИЕ!");
				log.error(
						"********НЕВОЗМОЖНО РАЗБЛОКИРОВАТЬ к lsk={} В ТЕЧЕНИИ 60 сек!{}", lsk);
				throw new ErrorWhileChrg("Ошибка при блокировке лс lsk="+lsk);
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				throw new ErrorWhileChrg("Ошибка при блокировке лс lsk="+lsk);
			}
		}

		try {
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
			if (res.getErr()==0) {
				chrgServ.save(lsk);
			}
			chrgServ = null; //### TODO;
			return new AsyncResult<Result>(res);

		} finally {
			// разблокировать лс
			config.lock.unlockChrgLsk(calc.getReqConfig().getRqn(), lsk, houseId);
			calc = null;  //### TODO;
		}
	}

}
