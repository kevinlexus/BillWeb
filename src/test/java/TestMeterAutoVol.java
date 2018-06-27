import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.ric.bill.DistServ;
import com.ric.bill.dao.MeterDAO;
import com.ric.bill.mm.KartMng;
import com.ric.bill.mm.MeterLogMng;
import com.ric.bill.model.fn.Chng;
import com.ric.bill.model.mt.Meter;
import com.ric.bill.model.sec.User;
import com.ric.bill.model.tr.Serv;
import com.ric.cmn.Utl;
import com.ric.web.AppConfig;

import lombok.extern.slf4j.Slf4j;

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
    private MeterLogMng metMng;

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

		Meter mm = em.find(Meter.class, 88489);
		Chng ch = em.find(Chng.class, 23);
		User user = em.find(User.class, 4);
		Serv serv = em.find(Serv.class, 79);

		meterDao.getVolPeriodByHouse(null, serv, user, Utl.getDateFromStr("01.02.2018"), Utl.getDateFromStr("30.02.2018")).stream()
		  .forEach(t-> {
			  log.info("meter.id={}, vol={}", t.getMet().getId(), t.getVol1());
		  });
		//metMng.saveMeterVol(mm, 1D, ch, user, Utl.getDateFromStr("01.02.2018"), Utl.getDateFromStr("30.02.2018"));

		//log.info("Date={}",Utl.addMonths(Utl.getDateFromStr("01.02.2018"), -2));
		//distServ.distHouseAutoVol(187);

		log.info("End!");
	}

}
