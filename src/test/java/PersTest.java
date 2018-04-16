
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Date;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.TestSubject;
import org.easymock.Mock;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ric.bill.Calc;
import com.ric.bill.CntPers;
import com.ric.bill.RequestConfig;
import com.ric.bill.excp.EmptyStorable;
import com.ric.bill.mm.ParMng;
import com.ric.bill.mm.impl.KartMngImpl;
import com.ric.bill.model.tr.Serv;

@RunWith(EasyMockRunner.class)
public class PersTest extends EasyMockSupport {

	@TestSubject
	private KartMngImpl kartMng = new KartMngImpl();
	
	@Mock
	private ParMng parMng;
	
	@Test
	public void testMe() throws EmptyStorable {
		/*int rqn = -1;
		RequestConfig requestConfig = new RequestConfig();
		Calc calc = new Calc(requestConfig);
		Date genDt = new Date();
		Serv servChrg = new Serv(); 
		CntPers cntPers = null;
//		RegContains rc = ;
		
		// эмулировать, что вызов вернёт 6
//		expect(parMng.getDbl(rqn, servChrg, "Вариант расчета по общей площади-1")).andReturn(6D);
//		expect(kartMng.getCntPers(rqn, calc, rc, servChrg, cntPers, genDt)).andReturn(1D);
		
		replayAll();

		int tp = 0;
		// проверить что вернул 6
		assertEquals(kartMng.getStandartVol(rqn, calc, servChrg, cntPers , genDt, tp), 6);
		verifyAll();*/
		
	}
	
	
}