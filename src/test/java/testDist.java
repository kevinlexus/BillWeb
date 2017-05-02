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
import com.ric.bill.mm.ParMng;
import com.ric.bill.model.mt.MLogs;
import com.ric.bill.model.mt.MeterLog;
import com.ric.web.AppConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=AppConfig.class)
//@ContextConfiguration(locations = { "classpath:spring.xml" }) //defines class-level metadata that is used to determine how to load and configure an ApplicationContext for integration tests.
@Slf4j
public class testDist {


	@Autowired
	private ApplicationContext ctx;

	@Autowired
    private ParMng parMng;
	
	@Autowired
    private BillServ billServ;

	@PersistenceContext
    private EntityManager em;

	// проверка корректности работы hibernate (если SIZE>2 то плохо))
	@Test
	public void testSomething() {
		MLogs ml = em.find(MeterLog.class, 485158);
		if (ml.getId()==485158) {
			log.info("сч");
			log.info("SIZE={}", ml.getInside().size());
			System.out.println("SIZE="+ ml.getInside().size());
		}    
	}

/*	@Test
	public void testSomething2() {

		Expression e = new Expression("(2.2839+1.001)/6.04*2.89");
		mXparser.consolePrintln("Res: " + e.getExpressionString() + " = " + e.calculate());
		
	}
	
	@Test
	public void testComboBoxLst() {
		System.out.println("Check:Reports:");
		repMng.getPeriodsByCD("RptPayDocList", 0).stream().forEach(t -> System.out.println("Reports:"+t.getId()+" "+t.getPeriod())
				);
		repMng.getPeriodsByCD("RptPayDocList", 1).stream().forEach(t -> System.out.println("Reports:"+t.getId()+" "+t.getPeriod())
				);
		
	}*/
}
