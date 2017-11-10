package com.ric.web;

import java.util.concurrent.TimeUnit;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.TouchedExpiryPolicy;

import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.stereotype.Component;

/**
 * Набор кэшей для Ehcache
 * @author lev
 *
 */
public class CacheService {

	
	  @Component
	  public static class CachingSetup implements JCacheManagerCustomizer
	  {

	 	/*Внимание! текущие настройки показали стабильную работу JVM, без зависаний, замедлений процесса начисления (без распр.объемов)
	 	безконтрольного увеличения памяти ред. 01.10.2017*/
		@Override
	    public void customize(CacheManager cacheManager)
	    {
	      cacheManager.createCache("rrr1", new MutableConfiguration<>()  
	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60)))
	        .setStoreByValue(false)
	        .setStatisticsEnabled(false));
	      cacheManager.createCache("TarifMngImpl.getOrg", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.getOrg", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.getServ", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.getServAll", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.getCapPrivs", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.getServPropByCD", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.getStandartVol", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.getCntPers", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.checkPersNullStatus", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.checkPersStatusExt", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.checkPersStatus", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ObjDAOImpl.getByCD", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("LstDAOImpl.getByCD", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 6000))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("MeterLogDAOImpl.getKart", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("OrgDAOImpl.getByKlsk", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ParDAOImpl.getByCd", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ParDAOImpl.checkPar", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ServDAOImpl.findMain", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ServDAOImpl.getByCD", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("DistGen.findLstCheck", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("MeterLogMngImpl.getAllMetLogByServTp", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("MeterLogMngImpl.checkExsKartMet", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("MeterLogMngImpl.checkExsMet", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("MeterLogMngImpl.getVolPeriod1", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("MeterLogMngImpl.getVolPeriod2", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("MeterLogMngImpl.getLinkedNode", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("MeterLogMngImpl.getKart", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ParMngImpl.isExByCd", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ParMngImpl.getBool1", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ParMngImpl.getBool2", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ParMngImpl.getDbl1", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ParMngImpl.getDbl2", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ParMngImpl.getDate", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ParMngImpl.getStr1", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ParMngImpl.getStr2", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("TarifMngImpl.getProp", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("TarifDAOImpl.getPropByCD", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("VsecDAOImpl.getPrivByUserRoleAct", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("LstMngImpl.getByCD", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ServMngImpl.getUpper", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 600))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("ServMngImpl.getUpperTree", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("MeterLogMngImpl.delNodeVol", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("VsecDAOImpl.getUserByCd", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      	      
	    }

	  }
	  
}
