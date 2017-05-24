package com.ric.web;

import org.springframework.beans.BeansException;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.view.jasperreports.JasperReportsMultiFormatView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsViewResolver;

@Configuration
@ComponentScan({"com.ric.st", "com.ric.bill.dao", "com.ric.bill.mm", "com.ric.bill", "com.ric.st.builder"}) // это нужно чтобы работали Unit-тесты! (по сути можно закомментить)
@ImportResource("spring.xml")
@EnableCaching
@EnableAsync
public class AppConfig  implements ApplicationContextAware {

	static ApplicationContext ctx = null;
	
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		ctx = context;
	}
	
	/**
	 * Note that this is a static method which expose ApplicationContext
	 **/
	public static ApplicationContext getContext(){
	      return ctx;
	}

	@Bean
	public JasperReportsViewResolver getJasperReportsViewResolver() {
	  JasperReportsViewResolver resolver = new JasperReportsViewResolver();
	  //resolver.setPrefix("classpath:/reports/");
	  resolver.setPrefix("classpath:/");
	  //resolver.setSuffix(".jasper");
	  resolver.setSuffix(".jrxml");
	  resolver.setReportDataKey("datasource");
	  resolver.setViewNames("rep*");
	  resolver.setViewClass(JasperReportsMultiFormatView.class);
	  resolver.setOrder(0);
	  return resolver;
	}  

/*	@Bean
	public InternalResourceViewResolver getInternalResourceViewResolver() {
	  InternalResourceViewResolver resolver = new InternalResourceViewResolver();
	  resolver.setPrefix("/WEB-INF/views/");
	  resolver.setSuffix(".jsp");
	  resolver.setOrder(1);
	  return resolver;
	}*/
	
}
