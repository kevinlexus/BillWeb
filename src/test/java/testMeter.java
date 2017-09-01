import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import lombok.extern.slf4j.Slf4j;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.ric.bill.mm.HouseMng;
import com.ric.bill.mm.MeterLogMng;
import com.ric.bill.model.tr.Serv;
import com.ric.web.AppConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes=AppConfig.class)
@Slf4j
public class testMeter {


	@Autowired
	private ApplicationContext ctx;
	@PersistenceContext
    private EntityManager em;
	@Autowired
	private MeterLogMng metMng;
	@Autowired
	private HouseMng houseMng;
	
    @Test
	public void mainWork() {
		log.info("Start");
		
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		
		//HouseMng houseMng = ctx.getBean(HouseMng.class);
		Serv serv = em.find(Serv.class, 71);
		
		houseMng.findAll2(null, 451, cal.getTime()).stream().forEach(t-> {
			System.out.println("houseId="+t.getId()+" "+t.getStreet().getName()+", "+t.getNd() );
			t.getKw().stream().forEach(k-> {
				k.getLsk().stream().forEach(s-> {
					if(!metMng.checkExsKartMet(1, s, serv, cal.getTime())) {
						System.out.println("lsk="+s.getLsk());
					}
				});
			});
		});
		
		//metMng.checkExsKartMet(1, kart, serv, genDt);
		
		
	}
    
/*    @Transactional(readOnly=false)
	public void work1() throws EmptyStorable, WrongSetMethod {
    	int rqn =-1;
		Obj obj = objMng.getByCD(-1, "Модуль начисления");

		Dw d = em.find(Dw.class, 8214820);
		Calendar calendar = new GregorianCalendar(2017, Calendar.FEBRUARY, 6);
		parMng.setDate(rqn, obj, "Начало расчетного периода", calendar.getTime());
		//d.setDts1(calendar.getTime());
		
		calendar = new GregorianCalendar(2017, Calendar.FEBRUARY, 8);
		parMng.setDate(rqn, obj, "Конец расчетного периода", calendar.getTime());
		
		log.info("Check ={}", obj.getId());
		log.info("Check dt1={} {}", parMng.getDate(-1, obj, "Начало расчетного периода"));
    	log.info("Check dt1={}", parMng.getDate(-1, obj, "Конец расчетного периода"));
    	
	}*/


}
