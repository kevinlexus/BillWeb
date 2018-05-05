import java.util.Date;
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
import com.ric.bill.DistServ;
import com.ric.bill.Utl;
import com.ric.bill.dao.MeterDAO;
import com.ric.bill.mm.KartMng;
import com.ric.bill.mm.MeterLogMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.model.ar.House;
import com.ric.bill.model.ar.Kart;
import com.ric.bill.model.bs.Dw;
import com.ric.bill.model.mt.MLogs;
import com.ric.bill.model.mt.Meter;
import com.ric.bill.model.mt.MeterLog;
import com.ric.bill.model.mt.Vol;
import com.ric.bill.model.tr.Serv;
import com.ric.web.AppConfig;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes=AppConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DataJpaTest
@Slf4j
public class TestMeterAutoVol {


	@Autowired
	private ApplicationContext ctx;

	@Autowired
    private MeterDAO meterDao;
	
	@Autowired
    private MeterLogMng meterLogMng;

	@Autowired
    private KartMng kartMng;

	@Autowired
    private DistServ distServ;
	
	@PersistenceContext
    private EntityManager em;

	/**
	 * выполнить автоначисление
	 */
	@Test
	public void testDistHouseAutoVol() {
		log.info("Start testDistHouseAutoVol!");
		
		distServ.distHouseAutoVol(187);
		
		log.info("End!");
	}

}
