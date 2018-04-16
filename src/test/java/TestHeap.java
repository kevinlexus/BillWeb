import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import com.ric.bill.BillServ;
import com.ric.bill.Utl;
import com.ric.bill.mm.KartMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.bs.Dw;
import com.ric.bill.model.mt.MLogs;
import com.ric.bill.model.mt.MeterLog;
import com.ric.web.AppConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes=AppConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DataJpaTest
@Slf4j
public class TestHeap {


	@Autowired
	private ApplicationContext ctx;

	@Autowired
    private ParMng parMng;
	
	@Autowired
    private KartMng kartMng;

	@PersistenceContext
    private EntityManager em;

	// проверка Java Heap
	@Test
	public void testJavaHeap() {
		log.info("Start!");
/*		List<Kart> lstKart = kartMng.findAll(8229, null, null, Utl.getDateFromStr("01.01.2017"), Utl.getDateFromStr("01.04.2018"));
		
		while (true) {
		for (Kart kart : lstKart) {
			//log.info("lsk={}", kart.getLsk());
			//log.info("House.Id={}", kart.getKw().getHouse().getId());
			//log.info("House.Nd={}", kart.getKw().getHouse().getNd());

			for (MLogs m : kart.getMlog()) {
				//log.info("Mlog.id={}", m.getId());
				if (m.getDw() != null) {
					for (Dw d : m.getDw()) {
						
						//log.info("Dw.N1={}", d.getN1());
						//log.info("Dw.Par.cd={}", d.getPar().getCd());
					}
				}
			}
			
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
		}
*///		log.info("End!");
		
		
	}

}
