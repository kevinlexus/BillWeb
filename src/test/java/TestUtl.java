import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ric.bill.excp.EmptyStorable;
import com.ric.cmn.Utl;

import lombok.extern.slf4j.Slf4j;

/**
 * Тестирование модуля Utl
 * @author lev
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Slf4j
public class TestUtl {


	/**
	 * Проверка получения даты +- месяц к текущей
	 * @throws EmptyStorable
	 */
    @Test
	public void mainTest() throws EmptyStorable {
		log.info("Test start");
		Date dt1 = Utl.getDateFromStr("28.02.2018");
		Date dt2 = Utl.addMonths(dt1, -3);
		Date dt3 = Utl.getDateFromStr("28.11.2017");
		log.info("Test dt1={}, dt2={}, dt3={}", dt1, dt2, dt3);

		assertTrue(dt2.equals(dt3));

		log.info("Test end");
	}


}
