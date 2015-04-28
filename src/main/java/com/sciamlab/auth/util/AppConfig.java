package com.sciamlab.auth.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class AppConfig {

    public static String API_KEY_VALIDATION_ENDPOINT = null;
	public static String API_KEY_INTERNAL = null;
	public static int SESSION_DATE_OFFSET_IN_MINUTES = 15;
    public static int ACCESS_TOKEN_LIFETIME_IN_MILLIS = 5000;
    
    public static Map<String, Long> SPEED_LIMIT_THROTTLING_PLANS = new HashMap<String, Long>(){{put(API_BASIC_PROFILE,API_BASIC_PROFILE_SPEED);}};
    public static Map<String, Long> DAILY_LIMIT_THROTTLING_PLANS = new HashMap<String, Long>(){{put(API_BASIC_PROFILE,API_BASIC_PROFILE_DAILY);}};
    
    public static int THROTTLING_USERS_CACHE_LIFETIME_IN_MINUTES = 20;
    //cache to store users profiles (refreshed every 20 minutes)
    public static Cache<String, String> usersCache = CacheBuilder.newBuilder().expireAfterWrite(AppConfig.THROTTLING_USERS_CACHE_LIFETIME_IN_MINUTES, TimeUnit.MINUTES).build();
    
    public static String API_NAME;
    public static List<String> API_LIST = new ArrayList<String>(){{
    	add("ckan");
    	add("entilocali");
    	add("eurovoc");
    	add("indicepa");
    }};
    public static String API_BASIC_PROFILE = "basic";
    public static Long API_BASIC_PROFILE_SPEED = 5000L;
    public static Long API_BASIC_PROFILE_DAILY = 500L;
    
//    public static String USERS_TABLE_NAME = null;//"public.\"user\"";
//    public static String ROLES_TABLE_NAME = null;//"public.\"user_object_role\"";
//    public static String PROFILES_TABLE_NAME = null;//"public.\"user_api_profiles\"";
//    public static String LOG_TABLE_NAME = "db.table.log";
//    public static boolean LOG_ENABLED = true;
    public static String OAUTH_DATASOURCE_LOCATION = null;//"java:/comp/env/jdbc/oauth";
    
    
    public static String HEADER_AUTHORIZATION_FIELD = "Authorization";
    public static String QUERY_AUTHORIZATION_FIELD = "key";
    
    
    public static String INTERNAL_SHARED_KEY;
    public static String INTERNAL_USER_ID;
    public static String ACCESS_TOKEN_VALIDATION_ENDPOINT;
}
