package com.sciamlab.auth.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sciamlab.auth.dao.SciamlabAuthDAO;
import com.sciamlab.auth.model.User;
import com.sciamlab.auth.util.AppConfig;
import com.sciamlab.common.exception.InternalServerErrorException;
import com.sciamlab.common.exception.TooManyRequestsException;
import com.sciamlab.common.util.SciamlabStringUtils;

@Priority(Priorities.AUTHENTICATION)
public class SpeedRateThrottlingFilter implements ContainerRequestFilter {

	private static final Logger logger = Logger.getLogger(SpeedRateThrottlingFilter.class);

    //cache to store plans activities per user
    private Map<String,Cache<String, String>> speedRegisterCachesMap = new HashMap<String,Cache<String, String>>();
    
    private final String api_name;
    private final Map<String,Long> speed_limit_throttling_plans;
    private final SciamlabAuthDAO dao;
    
    private SpeedRateThrottlingFilter(SpeedRateThrottlingFilterBuilder builder) { 
    	logger.info("Initializing "+DailyRateThrottlingFilter.class.getSimpleName()+"...");
    	this.api_name = builder.api_name;
    	this.dao = builder.dao;
    	this.speed_limit_throttling_plans = builder.speed_limit_throttling_plans;
    	//initializing plans related caches
    	for(String plan : this.speed_limit_throttling_plans.keySet()){
    		Cache<String, String> tmpCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(this.speed_limit_throttling_plans.get(plan), TimeUnit.MILLISECONDS)
                    .build();
    		speedRegisterCachesMap.put(plan, tmpCache);
    	}
    	logger.info("[DONE]");
    }
	
	public boolean removeFromCacheMap(String key, String user_plan){
		logger.info("attempt to remove key "+key+" for plan "+user_plan);
		Cache<String, String> planCache = this.speedRegisterCachesMap.get(user_plan);
		if(planCache==null)
			throw new InternalServerErrorException("Plan cache not found for plan "+user_plan);
		logger.info("Found entry: "+planCache.getIfPresent(key));
		if(planCache.getIfPresent(key)!=null){
			planCache.invalidate(key);
			return true;
		}
		return false;
	}
    
	@Override
    public void filter(ContainerRequestContext requestContext) {
    	ContainerRequest request = (ContainerRequest) requestContext;
    	String key = request.getHeaderString(AppConfig.HEADER_AUTHORIZATION_FIELD);
    	if(key==null){
        	List<String> key_params = request.getUriInfo().getQueryParameters().get(AppConfig.QUERY_AUTHORIZATION_FIELD);
        	if(key_params!=null && !key_params.isEmpty())
        		key = key_params.get(0);
        }
    	User user;
		try {
			user = dao.getUserByApiKey(key);
		} catch (Exception e) {
			throw new InternalServerErrorException(SciamlabStringUtils.stackTraceToString(e));
		}
		String user_profile = user.getProfile(this.api_name);
    	
    	Cache<String, String> planCache = this.speedRegisterCachesMap.get(user_profile);
    	if(planCache.getIfPresent(key)!=null)
    		throw new TooManyRequestsException("Too many requests in the given time of "+this.speed_limit_throttling_plans.get(user_profile)+" millis");
    	
    	//reset the timer for next access (need to wait another second)
    	planCache.put(key, key);
    	//add the plan to the header to be used in the parentResource class
    	request.setProperty("plan", user_profile);
    	return;
    }
	
	public static class SpeedRateThrottlingFilterBuilder{
		
    	private final String api_name;
    	private final SciamlabAuthDAO dao;
    	private Map<String,Long> speed_limit_throttling_plans;
		
		public static SpeedRateThrottlingFilterBuilder newBuilder(String api_name, SciamlabAuthDAO dao){
			return new SpeedRateThrottlingFilterBuilder(api_name, dao);
		}
		
		private SpeedRateThrottlingFilterBuilder(String api_name, SciamlabAuthDAO dao) {
			super();
			this.dao = dao;
			this.api_name = api_name;
			this.speed_limit_throttling_plans = new HashMap<String, Long>(){{put(AppConfig.API_BASIC_PROFILE,AppConfig.API_BASIC_PROFILE_SPEED);}};
		}
		
		public SpeedRateThrottlingFilterBuilder plan(String profile, Long rate) {
			this.speed_limit_throttling_plans.put(profile, rate);
			return this;
		}
		
		public SpeedRateThrottlingFilterBuilder plans(Map<String,Long> plans) {
			this.speed_limit_throttling_plans.putAll(plans);
			return this;
		}
		
		public SpeedRateThrottlingFilter build() {
			return new SpeedRateThrottlingFilter(this);
		}
    }

}
