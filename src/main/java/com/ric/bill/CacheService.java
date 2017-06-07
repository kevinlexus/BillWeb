package com.ric.bill;

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
	      
	    }

	  }
	  
}
