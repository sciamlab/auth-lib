package com.sciamlab.auth.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.sciamlab.auth.util.AuthLibConfig;
import com.sciamlab.common.exception.InternalServerErrorException;
import com.sciamlab.common.exception.SciamlabWebApplicationException;
import com.sciamlab.common.exception.TooManyRequestsException;
import com.sciamlab.common.util.SciamlabDateUtils;
import com.sciamlab.common.util.SciamlabStringUtils;

@Priority(Priorities.AUTHENTICATION)
public class DailyRateThrottlingFilter implements ContainerRequestFilter {

	private static final Logger logger = Logger.getLogger(DailyRateThrottlingFilter.class);

    private Map<String, Cache<String, String>> dailyRegisterCachesMap = new HashMap<String, Cache<String, String>>();
    
    private final String api_name;
    private final Map<String,Long> daily_limit_throttling_plans;
    private final SciamlabAuthDAO dao;
    
    private DailyRateThrottlingFilter(DailyRateThrottlingFilterBuilder builder) { 
    	logger.info("Initializing "+DailyRateThrottlingFilter.class.getSimpleName()+"...");
    	this.api_name = builder.api_name;
    	this.dao = builder.dao;
    	this.daily_limit_throttling_plans = builder.daily_limit_throttling_plans;
    	logger.info("[DONE]");
    }

	@Override
    public void filter(ContainerRequestContext requestContext) {
    	try {
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
			User user = dao.validate(jwt); 
			if(user==null){
				logger.warn("No session found for user in filter "+this.getClass().getSimpleName());
				return;
			}
			String user_profile = user.getProfile(this.api_name);
			
			Cache<String, String> tmpCache = this.dailyRegisterCachesMap.get(user.getApiKey());
			if(tmpCache==null){
				tmpCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();
				this.dailyRegisterCachesMap.put(user.getApiKey(), tmpCache);
			}
			tmpCache.put(SciamlabDateUtils.getCurrentDateAsIso8061String(), user.getApiKey());
			
			Set<String> copyCache = tmpCache.asMap().keySet();
			if(copyCache.size()>AuthLibConfig.DAILY_LIMIT_THROTTLING_PLANS.get(user_profile))
				throw new TooManyRequestsException("Exceeded limit of "+this.daily_limit_throttling_plans.get(user_profile)+" calls in the given time of one day");
			
			return;
    	}catch(SciamlabWebApplicationException e){
			logger.error(e.getMessage());
			throw e;
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new InternalServerErrorException(e);
		}
    }

	public static class DailyRateThrottlingFilterBuilder{
		
    	private final String api_name;
    	private final SciamlabAuthDAO dao;
    	private Map<String,Long> daily_limit_throttling_plans;
		
		public static DailyRateThrottlingFilterBuilder newBuilder(String api_name, SciamlabAuthDAO dao){
			return new DailyRateThrottlingFilterBuilder(api_name, dao);
		}
		
		private DailyRateThrottlingFilterBuilder(String api_name, SciamlabAuthDAO dao) {
			super();
			this.dao = dao;
			this.api_name = api_name;
			this.daily_limit_throttling_plans = new HashMap<String, Long>(){{put(AuthLibConfig.API_BASIC_PROFILE,AuthLibConfig.API_BASIC_PROFILE_DAILY);}};
		}
		
		public DailyRateThrottlingFilterBuilder plan(String profile, Long rate) {
			this.daily_limit_throttling_plans.put(profile, rate);
			return this;
		}
		
		public DailyRateThrottlingFilterBuilder plans(Map<String,Long> plans) {
			this.daily_limit_throttling_plans.putAll(plans);
			return this;
		}
		
		public DailyRateThrottlingFilter build() {
			return new DailyRateThrottlingFilter(this);
		}
    }

}
