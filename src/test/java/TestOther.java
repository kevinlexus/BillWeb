import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.ric.bill.ResultSet;
import com.ric.bill.dao.KartDAO;
import com.ric.bill.dao.PayordDAO;
import com.ric.bill.dao.VolDAO;
import com.ric.bill.mm.MeterLogMng;
import com.ric.web.AppConfig;

import lombok.extern.slf4j.Slf4j;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes=AppConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DataJpaTest
@Slf4j
public class TestOther {


	@Autowired
	private ApplicationContext ctx;
	@Autowired
	private PayordDAO payordDao;
	@Autowired
	private KartDAO kartDao;
	@Autowired
	private MeterLogMng meterLogMng;

	/**
	 * получить список физических счетчиков для перерасчета
	 */
	@Test
	public void test() {
		log.info("Start!");
		//volDao.testMe();
		//List<ResultSet> lstItem = kartDao.findAllLsk(7966, null, null, null, null);
/*		lstItem.stream().forEach(t-> {
			log.info("lsk={}", t.getId());
		});
*/		//payordDao.getPayordByPayordGrpId2(1).size();
/*		while (true) {
			meterLogMng.testTransact();
		}
*/	}

}
