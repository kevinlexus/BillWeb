import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.ric.bill.dao.VolDAO;
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
	private VolDAO volDao;

	/**
	 * получить список физических счетчиков для перерасчета
	 */
	@Test
	public void test() {
		log.info("Start!");
		//volDao.testMe();
		log.info("End!");
	}

}
