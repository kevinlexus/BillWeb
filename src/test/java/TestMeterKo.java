import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.ric.bill.dao.MeterDAO;
import com.ric.bill.model.oralv.Ko;
import com.ric.bill.model.tr.Serv;
import com.ric.web.AppConfig;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes=AppConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DataJpaTest
@Slf4j
public class TestMeterKo {


	@Autowired
    private MeterDAO meterDao;

	@PersistenceContext
    private EntityManager em;

	/**
	 * получить список физических счетчиков для перерасчета
	 */
	@Test
	public void testMeterKo() {
		log.info("Start testMeterKo!");
		Serv serv = em.find(Serv.class, 35);
		Ko koLsk = em.find(Ko.class, 809334);
		//Eolink.id=468493, premiseKo.id=809334, number=6839297, servCd=Горячая вода
		meterDao.getKoByLskNum(koLsk, serv, "6839297").forEach(t-> {
			log.info("Обнаружен счетчик Ko.id={} AddrTp.cd={}",
					t.getId(), t.getAddrTp().getCd());
		});

		log.info("End testMeterKo!");
	}
}
