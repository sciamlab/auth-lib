package com.sciamlab.auth.util;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jose4j.keys.AesKey;
import org.jose4j.lang.ByteUtil;
import org.json.JSONObject;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sciamlab.auth.model.Role;
import com.sciamlab.common.exception.InternalServerErrorException;
import com.sciamlab.common.util.SciamlabStreamUtils;

public class AuthLibConfig {
	
	private static final Logger logger = Logger.getLogger(AuthLibConfig.class);

	public static final String DEFAULT_PROPS_FILE = "auth.properties";

	public static String GET_USER_BY_ID;
	public static String GET_USER_BY_API_KEY;
    public static String GET_USERS_LIST;
    public static String GET_ROLES_BY_USER_ID;
    public static String GET_ROLE_BY_NAME;
    public static String GET_ROLES_LIST;
    public static String INSERT_ROLE;
    public static String UPDATE_ROLE;
    public static String DELETE_ROLE;
    public static String GET_ROLE_TO_ROLE_LIST;
    public static String INSERT_ROLE_TO_ROLE;
    public static String DELETE_ROLE_TO_ROLE;
    public static String GET_PRODUCTS_LIST;
    public static String GET_PROFILES_BY_USER_ID;
    public static String INSERT_USER_PRODUCT_PROFILE;
    public static String UPDATE_USER_PRODUCT_PROFILE;
    public static String DELETE_USER_PRODUCT_PROFILE;
    public static String INSERT_USER_ROLE;
    public static String UPDATE_USER_ROLE;
    public static String DELETE_USER_ROLE;
	public static String GET_USER_LOCAL;
	public static String GET_USER_SOCIAL;
	public static String DELETE_USER;
	public static String INSERT_USER;
	public static String INSERT_USER_LOCAL;
	public static String INSERT_USER_SOCIAL;
	public static String UPDATE_USER;
	public static String UPDATE_USER_LOCAL;
	public static String UPDATE_USER_SOCIAL;
	public static String CHECK_USER_LOCAL_DELETED;
	public static String CHECK_USER_SOCIAL_DELETED;

	public static final Key JWT_KEY = new AesKey(ByteUtil.randomBytes(16));
	public static String JWT_VALIDATION_ENDPOINT;
	public static String API_KEY_VALIDATION_ENDPOINT;
	
	public static int SESSION_DATE_OFFSET_IN_MINUTES = 15;
    public static int ACCESS_TOKEN_LIFETIME_IN_MILLIS = 5000;
    
    public static int THROTTLING_USERS_CACHE_LIFETIME_IN_MINUTES = 20;
    //cache to store users profiles (refreshed every 20 minutes)
    public static Cache<String, String> usersCache = CacheBuilder.newBuilder().expireAfterWrite(AuthLibConfig.THROTTLING_USERS_CACHE_LIFETIME_IN_MINUTES, TimeUnit.MINUTES).build();
    
    private static String PRODUCTS_FILE;
	public static JSONObject PRODUCTS; 

	public static final String API_BASIC_PROFILE = "basic";
    public static final Long API_BASIC_PROFILE_SPEED = 5000L;
    public static final Long API_BASIC_PROFILE_DAILY = 500L;
    public static List<String> API_PLANS = new ArrayList<String>(){{add(API_BASIC_PROFILE);}};
    
    public static Map<String, Long> SPEED_LIMIT_THROTTLING_PLANS = new HashMap<String, Long>(){{put(API_BASIC_PROFILE,API_BASIC_PROFILE_SPEED);}};
    public static Map<String, Long> DAILY_LIMIT_THROTTLING_PLANS = new HashMap<String, Long>(){{put(API_BASIC_PROFILE,API_BASIC_PROFILE_DAILY);}};
    
    /*
     * those are used only by CKANLocalUserDAO (to be deprecated)
     */
    public static String USERS_TABLE_NAME;
    public static String ROLES_TABLE_NAME;
    public static String PROFILES_TABLE_NAME;
    public static String USERS_SOCIAL_TABLE_NAME;
    
    /**
	 * that map represents the role available in CKAN
	 */
    public static Map<String, Role> CKAN_ROLES = new HashMap<String, Role>(){{
    	put("ADMIN", new Role("admin"));
    	put("READER", new Role("reader"));
    	put("EDITOR", new Role("editor")); 
    	put("ANONYMOUS", Role.ANONYMOUS); 
    }};

	public static void init(String module_name){
		//loading properties
		try {
			loadProps(module_name);
			logger.info("Properties loading completed");
		} catch (Exception e) {
			logger.error("Error loading properties", e);
			throw new RuntimeException(e);
		}
		//loading products
		if(PRODUCTS_FILE!=null){
			try{
				PRODUCTS = new JSONObject(SciamlabStreamUtils.convertStreamToString(SciamlabStreamUtils.getInputStream(PRODUCTS_FILE)));
				logger.info("Products loading completed");
			} catch (Exception e) {
				logger.error("Error loading products", e);
				throw new RuntimeException(e);
			}
		}
		logger.info("DONE");
	}

    public static void loadProps(String module_name) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		try (InputStream is = SciamlabStreamUtils.getInputStream(DEFAULT_PROPS_FILE);){
			Properties prop = new Properties();
			prop.load(is);
			
			for(Object k : prop.keySet()){
				logger.info(k+": "+prop.getProperty((String)k));
			}
			
			GET_USER_BY_API_KEY = prop.getProperty("get_user_by_apikey");
			GET_USER_BY_ID = prop.getProperty("get_user_by_id");
			GET_USERS_LIST = prop.getProperty("get_users_list");
			GET_ROLES_BY_USER_ID = prop.getProperty("get_roles_by_user_id");
			GET_ROLES_LIST = prop.getProperty("get_roles");
			GET_ROLE_BY_NAME = prop.getProperty("get_role_by_name");
			INSERT_ROLE = prop.getProperty("insert_role");
			UPDATE_ROLE = prop.getProperty("update_role");
			DELETE_ROLE = prop.getProperty("delete_role");
			GET_ROLE_TO_ROLE_LIST = prop.getProperty("get_role_to_role");
			INSERT_ROLE_TO_ROLE = prop.getProperty("insert_role_to_role");
			DELETE_ROLE_TO_ROLE = prop.getProperty("delete_role_to_role");
			GET_PRODUCTS_LIST = prop.getProperty("get_product_list");
			GET_PROFILES_BY_USER_ID = prop.getProperty("get_profiles_by_user_id");
			DELETE_USER_PRODUCT_PROFILE = prop.getProperty("delete_user_product_profile");
			UPDATE_USER_PRODUCT_PROFILE = prop.getProperty("update_user_product_profile");
			INSERT_USER_PRODUCT_PROFILE = prop.getProperty("insert_user_product_profile");
			UPDATE_USER_ROLE = prop.getProperty("update_user_role");
			DELETE_USER_ROLE = prop.getProperty("delete_user_role");
			INSERT_USER_ROLE = prop.getProperty("insert_user_role");
			GET_USER_LOCAL = prop.getProperty("get_user_local");
			GET_USER_SOCIAL = prop.getProperty("get_user_social");
			INSERT_USER = prop.getProperty("insert_user");
			INSERT_USER_LOCAL = prop.getProperty("insert_user_local");
			INSERT_USER_SOCIAL = prop.getProperty("insert_user_social");
			DELETE_USER = prop.getProperty("delete_user");
			UPDATE_USER = prop.getProperty("update_user");
			UPDATE_USER_LOCAL = prop.getProperty("update_user_local");
			UPDATE_USER_SOCIAL = prop.getProperty("update_user_social");
			CHECK_USER_LOCAL_DELETED = prop.getProperty("check_user_local_deleted");
			CHECK_USER_SOCIAL_DELETED = prop.getProperty("check_user_social_deleted");
			
			JWT_VALIDATION_ENDPOINT = prop.getProperty("jwt.validation.endpoint");
			API_KEY_VALIDATION_ENDPOINT = prop.getProperty("apikey.validation.endpoint");
			
			PRODUCTS_FILE = prop.getProperty("products");//, "products.json");
			
			if(prop.containsKey("api.plans")){
				API_PLANS = Arrays.asList(prop.getProperty("api.plans").split(","));
				for(String p : API_PLANS){
					//default to 5000
					SPEED_LIMIT_THROTTLING_PLANS.put(p,Long.parseLong((prop.containsKey(module_name+"."+p+".limit.speed"))?prop.getProperty(module_name+"."+p+".limit.speed"):prop.getProperty("api."+p+".limit.speed", "5000")));
					//default to 500
					DAILY_LIMIT_THROTTLING_PLANS.put(p,Long.parseLong((prop.containsKey(module_name+"."+p+".limit.daily"))?prop.getProperty(module_name+"."+p+".limit.daily"):prop.getProperty("api."+p+".limit.daily", "500")));
				}
				logger.info("SPEED_LIMIT_THROTTLING_PLANS: "+SPEED_LIMIT_THROTTLING_PLANS);
				logger.info("DAILY_LIMIT_THROTTLING_PLANS: "+DAILY_LIMIT_THROTTLING_PLANS);
			}
			
			USERS_TABLE_NAME = prop.getProperty("db.table.users");
			USERS_SOCIAL_TABLE_NAME = prop.getProperty("db.table.users_social");
			ROLES_TABLE_NAME = prop.getProperty("db.table.roles");
			PROFILES_TABLE_NAME = prop.getProperty("db.table.profiles");
			
		}
	}
}
