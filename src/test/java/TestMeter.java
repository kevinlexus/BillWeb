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
public class TestMeter {


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
	 * получить список физических счетчиков для перерасчета
	 */
	@Test
	public void testMeterByHouse() {
		log.info("Start testMeterByHouse!");
		Serv serv = em.find(Serv.class, 79);
		House house = em.find(House.class, 187);
		Date dt1 = Utl.getDateFromStr("01.01.2018");
		Date dt2 = Utl.getDateFromStr("01.02.2018");

		log.info("Кол-во месяцев========{}", Utl.getDiffMonths(dt1, dt2));

		/*meterDao.getXxx().stream().forEach(t -> {
			log.info("meter.id={}", t.getId());
		}
		);*/
		meterLogMng.getAllMeterAutoVol(house, serv, dt1, dt2).stream().forEach(t-> {
			log.info("kart.lsk={}, meter.id={}, tp={}", t.getMeter().getMeterLog().getKart().getLsk(), t.getMeter().getId(), t.getTp());
		});;
		log.info("End!");
	}

	/**
	 * получить список физических счетчиков
	 */
	@Test
	public void testMeterDaoByHouse() {
		log.info("Start testMeterDaoByHouse!");

		Serv serv = em.find(Serv.class, 79);
		House house = em.find(House.class, 187);
		Date dt1 = Utl.getDateFromStr("01.04.2018");
		Date dt2 = Utl.getDateFromStr("30.04.2018");
		
		log.info("Сломанные счетчики:");
		meterDao.getAllBrokenMeterByHouseServ(house, serv, dt2).stream().forEach(t-> {
			log.info("kart.lsk={}, meter.id={}, tp={} ", t.getMeter().getMeterLog().getKart().getLsk(), t.getMeter().getId(), t.getTp());
		});
		log.info("Не переданы показания:");
		meterDao.getAllWoVolMeterByHouseServ(house, serv, dt1, dt2).stream().forEach(t-> {
			log.info("kart.lsk={}, meter.id={}, tp={} ", t.getMeter().getMeterLog().getKart().getLsk(), t.getMeter().getId(), t.getTp());
		});
		
		
		log.info("End!");
		
		
	}


	/**
	 * получить последнее показание по физ.счетчику
	 */
	@Test
	public void testGetLastVol() {
		log.info("Start testGetLastVol!");

		Meter meter = em.find(Meter.class, 87096);
		Vol vol = meterDao.getLastVol(meter);
		if (vol != null) {
			log.info("meter vol.id={}, vol.vol1={}", vol.getId(), vol.getVol1());
		}
		
		log.info("End!");
		
		
	}

}
