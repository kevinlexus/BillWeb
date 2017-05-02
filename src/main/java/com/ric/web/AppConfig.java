package com.ric.web;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.view.jasperreports.JasperReportsMultiFormatView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsViewResolver;

@Configuration
@ComponentScan({"com.ric.bill"})
@ImportResource("spring.xml")
@EnableCaching
@EnableAsync
public class AppConfig {
	
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
