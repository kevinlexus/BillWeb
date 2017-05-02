import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ric.bill.BillServ;
import com.ric.bill.Calc;
import com.ric.bill.Config;
import com.ric.bill.DistServ;
import com.ric.bill.RequestConfig;
import com.ric.bill.Result;
import com.ric.bill.mm.ParMng;
import com.ric.bill.model.fn.Chng;
import com.ric.web.AppConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=AppConfig.class)
//@ContextConfiguration(locations = { "classpath:spring.xml" }) //defines class-level metadata that is used to determine how to load and configure an ApplicationContext for integration tests.
@Slf4j
public class testRkcOneLsk {


	@Autowired
	private ApplicationContext ctx;

	@Autowired
    private ParMng parMng;
	
	@Autowired
    private BillServ billServ;

    @PersistenceContext
    private EntityManager em;

	// проверка НАЧИСЛЕНИЯ
	@Test
	public void testChrg() {
		Config config = ctx.getBean(Config.class);
    	// получить уникальный номер запроса
    	int rqn = config.incNextReqNum();

    	Future<Result> fut = null;

    	RequestConfig reqConfig = ctx.getBean(RequestConfig.class);
    	reqConfig.setUp(config, "1", "0", null, rqn, null, null); 
    	
    	fut = billServ.chrgLsk(reqConfig, null, 1083); // FLSK=00001083
    	
	   	//проверить окончание потока 
	    while (!fut.isDone()) {
	         try {
				Thread.sleep(100);
				//100-millisecond задержка
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error("ERROR");
			} 
	     }

	    try {
			if (fut.get().err ==0) {
			} else {
				log.error("ERROR");
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("ERROR");
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("ERROR");
		}
		
	}

}
