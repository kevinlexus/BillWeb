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

		@Override
	    public void customize(CacheManager cacheManager)
	    {
	      cacheManager.createCache("rrr1", new MutableConfiguration<>()  
	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300)))
	        .setStoreByValue(false)
	        .setStatisticsEnabled(false));
	      cacheManager.createCache("rrr2", new MutableConfiguration<>()  
	  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
	  	        .setStoreByValue(false)
	  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("rrr3", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("rrr4", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("wipein1min", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 60))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("neverWipe", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 3000000))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("TarifMngImpl.getOrg", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.getOrg", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.getServ", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      cacheManager.createCache("KartMngImpl.getServAll", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));

	      cacheManager.createCache("KartMngImpl.getCapPrivs", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      
	      cacheManager.createCache("KartMngImpl.getServPropByCD", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));

	      cacheManager.createCache("KartMngImpl.getStandartVol", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));

	      cacheManager.createCache("KartMngImpl.getCntPers", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));

	      cacheManager.createCache("KartMngImpl.checkPersNullStatus", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));

	      cacheManager.createCache("KartMngImpl.checkPersStatusExt", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	      
	      cacheManager.createCache("KartMngImpl.checkPersStatus", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));

	      cacheManager.createCache("ObjDAOImpl.getByCD", new MutableConfiguration<>()  
		  	        .setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 300))) 
		  	        .setStoreByValue(false)
		  	        .setStatisticsEnabled(false));
	     
	      
	     
	    }

	  }
	  
}
