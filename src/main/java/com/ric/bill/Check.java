package com.ric.bill;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

//@Service
//@RequestScope
@Component
//@Scope(value="request", proxyMode=ScopedProxyMode.TARGET_CLASS)
@Scope("prototype")
public class Check {

	public void checkMe(int i) {
		System.out.println("checkMe:"+i);
		
	}
}
