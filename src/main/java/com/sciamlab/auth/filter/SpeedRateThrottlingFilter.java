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
import com.sciamlab.auth.dao.UserValidator;
import com.sciamlab.auth.model.User;
import com.sciamlab.auth.util.AuthLibConfig;
import com.sciamlab.common.exception.BadRequestException;
import com.sciamlab.common.exception.InternalServerErrorException;
import com.sciamlab.common.exception.SciamlabWebApplicationException;
import com.sciamlab.common.exception.TooManyRequestsException;
import com.sciamlab.common.exception.UnauthorizedException;
import com.sciamlab.common.util.SciamlabStringUtils;

@Priority(Priorities.AUTHENTICATION)
public class SpeedRateThrottlingFilter implements ContainerRequestFilter {

	private static final Logger logger = Logger.getLogger(SpeedRateThrottlingFilter.class);

    //cache to store plans activities per user
    private Map<String,Cache<String, String>> speedRegisterCachesMap = new HashMap<String,Cache<String, String>>();
    
    private final String api_name;
    private final Map<String,Long> speed_limit_throttling_plans;
    private final UserValidator user_validator;
    
    private SpeedRateThrottlingFilter(SpeedRateThrottlingFilterBuilder builder) { 
    	logger.info("Initializing "+DailyRateThrottlingFilter.class.getSimpleName()+"...");
    	this.api_name = builder.api_name;
    	this.user_validator = builder.user_validator;
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
    	try{
			ContainerRequest request = (ContainerRequest) requestContext;
	    	String jwt = request.getHeaderString("Authorization");
	        if(jwt==null){
	        	List<String> key_params = request.getUriInfo().getQueryParameters().get("key");
	        	if(key_params!=null && !key_params.isEmpty())
	        		jwt = key_params.get(0);
	        }
	    	if(jwt==null){
	    		logger.warn("Missing key in filter "+this.getClass().getSimpleName());
	    		return;
	    	}
	        User user = user_validator.validate(jwt); 
	        if(user==null){
	    		logger.warn("No session found for user in filter "+this.getClass().getSimpleName());
	    		return;
	    	}
			String user_profile = user.getProfile(this.api_name);
	    	
	    	Cache<String, String> planCache = this.speedRegisterCachesMap.get(user_profile);
	    	if(planCache.getIfPresent(user.getApiKey())!=null)
	    		throw new TooManyRequestsException("Too many requests in the given time of "+this.speed_limit_throttling_plans.get(user_profile)+" millis");
	    	
	    	//reset the timer for next access (need to wait another second)
	    	planCache.put(user.getApiKey(), user.getApiKey());
	    	//add the plan to the header to be used in the parentResource class
	    	request.setProperty("plan", user_profile);
	    	return;
    	}catch(SciamlabWebApplicationException e){
			logger.error(e.getMessage());
			throw e;
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new InternalServerErrorException(e);
		}
    }
	
	public static class SpeedRateThrottlingFilterBuilder{
		
    	private final String api_name;
    	private final UserValidator user_validator;
    	private Map<String,Long> speed_limit_throttling_plans;
		
		public static SpeedRateThrottlingFilterBuilder init(String api_name, UserValidator user_validator){
			return new SpeedRateThrottlingFilterBuilder(api_name, user_validator);
		}
		
		private SpeedRateThrottlingFilterBuilder(String api_name, UserValidator user_validator) {
			super();
			this.user_validator = user_validator;
			this.api_name = api_name;
			this.speed_limit_throttling_plans = new HashMap<String, Long>(){{put(AuthLibConfig.API_BASIC_PROFILE,AuthLibConfig.API_BASIC_PROFILE_SPEED);}};
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
