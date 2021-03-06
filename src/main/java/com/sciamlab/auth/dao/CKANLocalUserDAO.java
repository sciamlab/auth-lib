package com.sciamlab.auth.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.postgresql.util.PGobject;

import com.sciamlab.auth.model.Role;
import com.sciamlab.auth.model.User;
import com.sciamlab.auth.model.UserLocal;
import com.sciamlab.auth.model.UserSocial;
import com.sciamlab.auth.util.AuthLibConfig;
import com.sciamlab.common.dao.SciamlabDAO;
import com.sciamlab.common.exception.DAOException;
import com.sciamlab.common.util.SciamlabCollectionUtils;

public abstract class CKANLocalUserDAO extends SciamlabDAO implements UserValidator, UserDAO {
	
	private static final Logger logger = Logger.getLogger(CKANLocalUserDAO.class);

	public static Map<String,User> USERS_BY_NAME = new HashMap<String, User>();
	
	@Override
	public User validate(String apikey) {
		return this.getUserByApiKey(apikey);
	}
		 
	public User getUserByName(String name) {
		User u = USERS_BY_NAME.get(name);
		if(u==null){
			u = (User)this.getUser("name", name);
			// updating cache
			USERS_BY_NAME.put(u.getName(), u);
		}
		return u;
	}
	
	public User getUserByApiKey(String apikey) {
		return this.getUser("apikey", apikey);
	}
	
	public User getUser(String col_key, final String col_value) {
		List<Properties> map = this.execQuery("SELECT * FROM "+AuthLibConfig.USERS_TABLE_NAME+" u left join "+AuthLibConfig.USERS_SOCIAL_TABLE_NAME+" us on u.id=us.ckan_id WHERE "+col_key+" = ?", 
				new ArrayList<Object>(){{ add(col_value); }},
				new ArrayList<String>(){{ add("id"); add("name"); add("password"); add("fullname"); add("email"); add("apikey"); add("social"); add("details");  }}); 
		if(map.size()==0) return null;
		if(map.size()>1) 
			throw new DAOException("Multiple users retrieved using "+col_key+": "+col_value);
		Properties p = map.get(0);
        return buildUserFromResultSet(p);
	}
	
	public List<User> getUserList() {
		List<Properties> map = this.execQuery("SELECT * FROM "+AuthLibConfig.USERS_TABLE_NAME+" u left join "+AuthLibConfig.USERS_SOCIAL_TABLE_NAME+" us on u.id=us.ckan_id", 
				null,
				new ArrayList<String>(){{ add("id"); add("name"); add("password"); add("fullname"); add("email"); add("apikey"); add("social"); add("details"); }}); 
		List<User> users = new ArrayList<User>();
		for(Properties p : map)
			users.add(buildUserFromResultSet(p));
        return users;
	}
	
	private User buildUserFromResultSet(Properties p) {
		User u = null;
		if(p.containsKey("social")){
			u = new UserSocial(p.getProperty("id").substring(0, p.getProperty("id").indexOf("@")), UserSocial.TYPES.get(p.getProperty("social")));
			((UserSocial) u).setSocialUser(p.getProperty("name"));
			((UserSocial) u).setSocialDisplay("".equals(p.getProperty("fullname")) ? null : p.getProperty("fullname"));
			((UserSocial) u).setSocialDetails(new JSONObject(((PGobject) p.get("details")).getValue()));
		}else{
			u = new UserLocal(p.getProperty("name"));
			((UserLocal) u).setFirstName(p.getProperty("fullname"));
			((UserLocal) u).setEmail(p.getProperty("email"));
			((UserLocal) u).setPassword(p.getProperty("password"));
		}
		u.setApiKey(p.getProperty("apikey"));
		u.setId(p.getProperty("id"));
		u.addRoles(this.getRolesByUserId(p.getProperty("id")));
		u.getProfiles().clear();
		u.getProfiles().putAll(this.getProfilesByUserId(p.getProperty("id")));
		logger.debug("User: "+u);
		return u;
	}
	
	public List<Role> getRolesByUserId(final String id) {
		Map<String, Properties> map = this.execQuery("SELECT DISTINCT role FROM "+AuthLibConfig.ROLES_TABLE_NAME+" WHERE user_id = ?",
				new ArrayList<Object>(){{ add(id); }}, "role", new ArrayList<String>());
		List<Role> roles = new ArrayList<Role>();
		roles.add(Role.ANONYMOUS);
		if(map.size()==0)
			return roles;
		for(String r : map.keySet()){
			if(AuthLibConfig.CKAN_ROLES.containsKey(r.toUpperCase())){
				roles.add(AuthLibConfig.CKAN_ROLES.get(r.toUpperCase()));
				logger.debug("Role added: "+r.toUpperCase());
			}else{
				logger.warn(r+"is not identified as a valid role!");
			}
		}
		return roles;
	}
	
	/**
	 * get the user profiles on all the apis
	 * 
	 * @param user_id
	 * @return a map where the keys are the api names and the values are the related profiles
	 */
	public Map<String, String> getProfilesByUserId(final String user_id) {
		Map<String, Properties> result = this.execQuery("SELECT DISTINCT api,profile FROM "+AuthLibConfig.PROFILES_TABLE_NAME+" WHERE user_id = ?", 
				new ArrayList<Object>(){{ add(user_id); }}, "api", new ArrayList<String>(){{ add("api"); add("profile"); }});
		Map<String, String> profiles = new HashMap<String, String>();
		for(String api : SciamlabCollectionUtils.asJSONObjectMap(AuthLibConfig.PRODUCTS).keySet()){
			String profile = (result.containsKey(api)) ? result.get(api).getProperty("profile") : AuthLibConfig.API_BASIC_PROFILE;
			profiles.put(api, profile);
		}
		return profiles;
	}
	
	/**
	 * set the user profile on the given api
	 * 
	 * @param user_id
	 * @param api
	 * @param profile
	 * @return number of updated records
	 */
	public User setUserProductProfile(final String user_id, final String api, final String profile) {
		int result = this.execUpdate("UPDATE "+AuthLibConfig.PROFILES_TABLE_NAME+" SET profile = ?, modified = ? WHERE user_id = ? AND api = ?",
				new ArrayList<Object>() {{ add(profile); add(new java.sql.Date(new Date().getTime())); add(user_id); add(api);}});
		if(result==0){
			//profile not found for given api, need to insert
			result = this.execUpdate("INSERT INTO "+AuthLibConfig.PROFILES_TABLE_NAME+" VALUES (?, ?, ?, ? ,?)",
					new ArrayList<Object>() {{ add(user_id); add(api); add(profile); add(new java.sql.Date(new Date().getTime())); add(new java.sql.Date(new Date().getTime())); }});
		}
		if(result==0)
			throw new DAOException("Error updating profile: "+profile);
		return getUser("id", user_id);
	}
	
	/**
	 * delete the user profile on the given api
	 * 
	 * @param user_id
	 * @param api
	 * @param profile
	 * @return number of updated records
	 */
	public User deleteUserProductProfile(final String user_id, final String api) {
		int result = this.execUpdate("DELETE FROM "+AuthLibConfig.PROFILES_TABLE_NAME+" WHERE user_id = ? AND api = ?",
				new ArrayList<Object>() {{ add(user_id); add(api); }});
		if(result==0)
			throw new DAOException("Error deleting profile for api: "+api);
		return getUser("id", user_id);
	}

}