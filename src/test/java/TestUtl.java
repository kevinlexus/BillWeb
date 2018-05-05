import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
import com.ric.bill.Config;
import com.ric.bill.RequestConfig;
import com.ric.bill.Utl;
import com.ric.bill.excp.EmptyStorable;
import com.ric.bill.excp.WrongDate;
import com.ric.bill.excp.WrongExpression;
import com.ric.bill.mm.ObjMng;
import com.ric.bill.mm.ParMng;
import com.ric.bill.mm.PayordMng;
import com.ric.bill.mm.SecMng;
import com.ric.bill.model.exs.UlistTp;
import com.ric.web.AppConfig;

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
