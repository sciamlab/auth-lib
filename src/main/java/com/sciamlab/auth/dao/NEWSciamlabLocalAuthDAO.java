/**
 * Copyright 2014 Sciamlab s.r.l.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *    
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sciamlab.auth.dao;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jose4j.lang.JoseException;
import org.json.JSONObject;
import org.postgresql.util.PGobject;

import com.sciamlab.auth.model.Role;
import com.sciamlab.auth.model.User;
import com.sciamlab.auth.model.UserLocal;
import com.sciamlab.auth.model.UserSocial;
import com.sciamlab.auth.util.AuthLibConfig;
import com.sciamlab.common.dao.SciamlabDAO;
import com.sciamlab.common.exception.DAOException;
import com.sciamlab.common.util.SciamlabTokenUtils;

/**
 * 
 * @author SciamLab
 *
 */

public abstract class NEWSciamlabLocalAuthDAO extends SciamlabDAO implements SciamlabAuthDAO {
	
	private static final Logger logger = Logger.getLogger(NEWSciamlabLocalAuthDAO.class);
	private static Map<String, Map<String, User>> USERS_BY_TYPE = new HashMap<String, Map<String, User>>();
	private static Map<String, User> USERS_BY_ID = new HashMap<String, User>();
	private static Map<String, User> USERS_BY_API_KEY = new HashMap<String, User>();
	private static Map<String, User> LOGGED_USERS = new HashMap<String, User>();
	private static Map<String, String> LOGGED_USERS_REVERSE = new HashMap<String, String>();
	private static List<String> PRODUCTS = new ArrayList<String>();
	
	public void buildCache(){
		//caching products on startup
		getProductsList();
		logger.debug("PRODUCTS: "+PRODUCTS);
		//caching users on startup
		getUsersList();
		logger.debug("USERS:");
		for(User u : USERS_BY_ID.values())
			logger.debug(u.getType()+":\t"+u.getUserName());
	}
	
	private static User updateCache(User user){
		Map<String, User> map = USERS_BY_TYPE.get(user.getType());
		if(map==null){
			map = new HashMap<String, User>();
			USERS_BY_TYPE.put(user.getType(), map);
		}
		map.put(user.getUserName(), user);
		USERS_BY_ID.put(user.getId(), user);
		USERS_BY_API_KEY.put(user.getApiKey(), user);
		String jwt = LOGGED_USERS_REVERSE.get(user.getId());
		if(jwt!=null)
			LOGGED_USERS.put(jwt, user);
		return user;
	}
	
	private static void removeCache(String id){
		User user = USERS_BY_ID.remove(id);
		if(user!=null){
			USERS_BY_API_KEY.remove(user.getApiKey());
			Map<String, User> map = USERS_BY_TYPE.get(user.getType());
			if(map!=null)
				map.remove(user.getUserName());
//			String jwt = LOGGED_USERS_REVERSE.remove(user.getId());
//			if(jwt!=null)
//				LOGGED_USERS.remove(jwt);
		}
	}
	
	private static void addLoggedUser(String jwt, User user){
		LOGGED_USERS.put(jwt, user);
		LOGGED_USERS_REVERSE.put(user.getId(), jwt);
	}
	
	private static boolean removeLoggedUser(String jwt){
		User user = LOGGED_USERS.remove(jwt);
		if(user!=null){
			LOGGED_USERS_REVERSE.remove(user.getId());
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * get the list of USERS_BY_TYPE
	 * 
	 * @return the list of USERS_BY_TYPE
	 */
	public Collection<User> getUsersList() {
		List<Properties> result = this.execQuery(AuthLibConfig.GET_USERS_LIST); 
		for(Properties p : result){
			User u = null;
			if(User.LOCAL.equals(p.getProperty("type"))){
				u = new UserLocal(p.getProperty("id"), p.getProperty("api_key"));
				((UserLocal) u).setFirstName(p.getProperty("first_name"));
				((UserLocal) u).setLastName(p.getProperty("last_name"));
				((UserLocal) u).setEmail(p.getProperty("email"));
				((UserLocal) u).setPassword(p.getProperty("password"));
			}else{
				u = new UserSocial(p.getProperty("id"), p.getProperty("api_key"));
				((UserSocial) u).setSocialType(UserSocial.TYPES.get(p.getProperty("social")));
				((UserSocial) u).setSocialId(p.getProperty("social_id"));
				((UserSocial) u).setSocialUser(p.getProperty("user_name"));
				((UserSocial) u).setSocialDisplay(p.getProperty("display_name"));
				((UserSocial) u).setSocialDetails(new JSONObject(((PGobject) p.get("details")).getValue()));
			}
			u.setApiKey(p.getProperty("api_key"));
			u.setId(p.getProperty("id"));
			u.getRoles().clear();
			u.getRoles().addAll(this.getRolesByUserId(p.getProperty("id")));
			u.getProfiles().clear();
			u.getProfiles().putAll(this.getProfilesByUserId(p.getProperty("id")));
			logger.debug("User: "+u);
			updateCache(u);
		}
		return USERS_BY_ID.values();
	}
	/**
	 * get the list of PRODUCTS
	 * 
	 * @return the list of PRODUCTS
	 */
	public List<String> getProductsList() {
		List<Properties> result = this.execQuery(AuthLibConfig.GET_PRODUCTS_LIST); 
		for(Properties p : result){
			PRODUCTS.add(p.getProperty("product"));
		}
        return PRODUCTS;
	}
	
	/**
	 * get the user roles
	 * 
	 * @param id
	 * @return the list of user roles
	 */
	public List<Role> getRolesByUserId(final String id) {
		if(USERS_BY_ID.containsKey(id))
			return USERS_BY_ID.get(id).getRoles();
		List<Properties> result = this.execQuery(AuthLibConfig.GET_ROLES_BY_USER_ID, new ArrayList<Object>(){{ add(id); }});
		List<Role> roles = new ArrayList<Role>();
		if(result.size()==0) {
			roles.add(Role.anonymous);
			return roles;
		}
		for(Properties p : result){
			Role role = Role.valueOf(p.getProperty("role"));
			if(role!=null){
				roles.add(role);
				logger.debug("Role added: "+role);
			}else{
				logger.warn(p.getProperty("role")+" is not identified as a valid role!");
			}
		}
		if(roles.isEmpty())
			roles.add(Role.anonymous);
		return roles;
	}
	
	/**
	 * set the user roles
	 * 
	 * @param id, the id of the user
	 * @param role, the role to be associated to the user
	 * @return the updated user
	 */
	public User setUserRole(final String id, final Role role) {
		int result = this.execUpdate(AuthLibConfig.UPDATE_USER_ROLE,
				new ArrayList<Object>() {{ add(role.name()); add(new Timestamp(new Date().getTime())); add(id); add(role.name());}});
		if(result==0){
			//role not found for given product, need to insert
			result = this.execUpdate(AuthLibConfig.INSERT_USER_ROLE,
					new ArrayList<Object>() {{ add(id); add(role.name()); add(new Timestamp(new Date().getTime())); }});
		}else{
			logger.warn("role "+role+" already set for user "+id);
		}
		removeCache(id);
		return updateCache(getUserById(id));
	}
	
	/**
	 * delete the user role
	 * 
	 * @param id, the id of the user
	 * @param role, the role to be deleted
	 * @return the updated user
	 */
	public User deleteUserRole(final String id, final Role role) {
		int result = this.execUpdate(AuthLibConfig.DELETE_USER_ROLE,
				new ArrayList<Object>() {{ add(id); add(role.name());}});
		removeCache(id);
		return updateCache(getUserById(id));
	}
	
	/**
	 * get the user profiles for all the products
	 * 
	 * @param id
	 * @return a map of products, profiles
	 */
	public Map<String, String> getProfilesByUserId(final String id) {
		if(USERS_BY_ID.containsKey(id))
			return USERS_BY_ID.get(id).getProfiles();
		Map<String, Properties> result = this.execQuery(AuthLibConfig.GET_PROFILES_BY_USER_ID, new ArrayList<Object>(){{ add(id); }}, "product", new ArrayList<String>(){{ add("product"); }});
		Map<String, String> profiles = new HashMap<String, String>();
		for(String product : PRODUCTS){
			String profile = (result.containsKey(product)) ? result.get(product).getProperty("profile") : AuthLibConfig.API_BASIC_PROFILE;
			profiles.put(product, profile);
		}
		return profiles;
	}
	
	/**
	 * set the user profile on the given product
	 * 
	 * @param id, the id of the user
	 * @param product, the product to be updated
	 * @param profile, the profile to be associated to the product
	 * @return the updated user
	 */
	public User setUserProductProfile(final String id, final String product, final String profile) {
		int result = this.execUpdate(AuthLibConfig.UPDATE_USER_PRODUCT_PROFILE,
				new ArrayList<Object>() {{ add(profile); add(new Timestamp(new Date().getTime())); add(id); add(product);}});
		if(result==0){
			//profile not found for given product, need to insert
			result = this.execUpdate(AuthLibConfig.INSERT_USER_PRODUCT_PROFILE,
					new ArrayList<Object>() {{ add(id); add(product); add(profile); add(new Timestamp(new Date().getTime())); }});
		}
		removeCache(id);
		return updateCache(getUserById(id));
	}
	
	/**
	 * delete the user profile on the given product
	 * 
	 * @param id, the id of the user
	 * @param product, the product to be deleted
	 * @return the updated user
	 */
	public User deleteUserProductProfile(final String id, final String product) {
		int result = this.execUpdate(AuthLibConfig.DELETE_USER_PRODUCT_PROFILE,
				new ArrayList<Object>() {{ add(id); add(product);}});
		removeCache(id);
		return updateCache(getUserById(id));
	}
	
	@Override
	public User validate(String jwt){
		return this.LOGGED_USERS.get(jwt);
	}
	
	/**
	 * invalidate the user session related to the given token
	 * @param jwt
	 * @return true if there is an active session for the given token, false otherwise
	 */
	public boolean logout(String jwt){
		return removeLoggedUser(jwt);
	}
	
	/**
	 * create a user session
	 * @param user
	 * @return the token related to the session
	 */
	public String login(User user){
		try {
			String jwt = SciamlabTokenUtils.createJsonWebToken(user.toJSONString(), AuthLibConfig.JWT_KEY);
			addLoggedUser(jwt, user);
			return jwt;
		} catch (JoseException e) {
			throw new DAOException(e);
		}
	}
	
	/**
	 * get the user by id
	 * 
	 * @param id
	 * @return the user
	 */
	public User getUserById(final String id){
		if(USERS_BY_ID.containsKey(id))
			return USERS_BY_ID.get(id);
		List<Properties> result = this.execQuery(AuthLibConfig.GET_USER_BY_ID, new ArrayList<Object>(){{add(id);}}); 
		if(result.size()==0) return null;
		if(result.size()>1) 
			throw new DAOException("Multiple users retrieved for id "+id);
		User u = this.buildUserFromResultSet(result.get(0));
		logger.debug("User: "+u);
		return updateCache(u);
	}
	
	/**
	 * get the user by api key
	 * 
	 * @param username
	 * @param type
	 * @return the user
	 */
	public User getUserByApiKey(final String api_key){
		if(USERS_BY_API_KEY.containsKey(api_key))
			return USERS_BY_API_KEY.get(api_key);
		List<Properties> result = this.execQuery(AuthLibConfig.GET_USER_BY_API_KEY, new ArrayList<Object>(){{add(api_key);}}); 
		if(result.size()==0) return null;
		if(result.size()>1) 
			throw new DAOException("Multiple users retrieved for api key "+api_key);
		User u = this.buildUserFromResultSet(result.get(0));
		logger.debug("User: "+u);
		return updateCache(u);
	}
	
	private User buildUserFromResultSet(Properties p){
		User u = null;
		if(User.LOCAL.equals(p.getProperty("type"))){
			u = new UserLocal(p.getProperty("id"), p.getProperty("api_key"));
			((UserLocal) u).setFirstName(p.getProperty("first_name"));
			((UserLocal) u).setLastName(p.getProperty("last_name"));
			((UserLocal) u).setEmail(p.getProperty("email"));
			((UserLocal) u).setPassword(p.getProperty("password"));
		}else{
			u = new UserSocial(p.getProperty("id"), p.getProperty("api_key"));
			((UserSocial) u).setSocialType(UserSocial.TYPES.get(p.getProperty("social")));
			((UserSocial) u).setSocialId(p.getProperty("social_id"));
			((UserSocial) u).setSocialUser(p.getProperty("user_name"));
			((UserSocial) u).setSocialDisplay(p.getProperty("display_name"));
			((UserSocial) u).setSocialDetails(new JSONObject(((PGobject) p.get("details")).getValue()));
		}
		u.setApiKey(p.getProperty("api_key"));
		u.setId(p.getProperty("id"));
		u.getRoles().clear();
		u.getRoles().addAll(this.getRolesByUserId(p.getProperty("id")));
		u.getProfiles().clear();
		u.getProfiles().putAll(this.getProfilesByUserId(p.getProperty("id")));
		return u;
	}
	
	/**
	 * get the user by username and type
	 * 
	 * @param username
	 * @param type
	 * @return the user
	 */
	public User getUserByUserNameAndType(final String username, String type){
		return User.LOCAL.equals(type) ? getUserLocalByEmail(username) : getUserSocialByUserName(username, type);
	}
	
	/**
	 * get the local user by email
	 * 
	 * @param username
	 * @return the user
	 */
	public UserLocal getUserLocalByEmail(final String email){
		if(USERS_BY_TYPE.containsKey(UserLocal.LOCAL) && ((Map<String, User>)USERS_BY_TYPE.get(UserLocal.LOCAL)).containsKey(email))
			return (UserLocal) USERS_BY_TYPE.get(UserLocal.LOCAL).get(email);
		List<Properties> result = this.execQuery(AuthLibConfig.GET_USER_LOCAL, new ArrayList<Object>(){{ add(email); }});
		if(result.size()==0) return null;
		if(result.size()>1) 
			throw new DAOException("Multiple local users retrieved for email "+email);
		User u = this.buildUserFromResultSet(result.get(0));
		logger.debug("User: "+u);
		return (UserLocal) updateCache(u);
	}
	/**
	 * get the social user by username
	 * 
	 * @param username
	 * @return the user
	 */
	public UserSocial getUserSocialByUserName(final String username, String social){
		final String social_cleaned = UserSocial.TYPES.get(social);
		if(social_cleaned == null)
			throw new DAOException("Wrong value for social '"+social+"'. use one in "+UserSocial.TYPES.keySet());
		if(USERS_BY_TYPE.containsKey(social) && ((Map<String, User>)USERS_BY_TYPE.get(social)).containsKey(username))
			return (UserSocial)((Map<String, User>)USERS_BY_TYPE.get(social)).get(username);
		List<Properties> result = this.execQuery(AuthLibConfig.GET_USER_SOCIAL, new ArrayList<Object>(){{ add(username); add(social_cleaned);}});
		if(result.size()==0) return null;
		if(result.size()>1) 
			throw new DAOException("Multiple social users retrieved for username "+username+" on "+social_cleaned);
		User u = this.buildUserFromResultSet(result.get(0));
		logger.debug("User: "+u);
		return (UserSocial) updateCache(u);
	}
	
	/**
	 * insert the user
	 * @param user
	 * @return true if the insert is succeeded 
	 */
	public boolean addUser(final User user){
		LinkedHashMap<String, List<Object>> updates = new LinkedHashMap<String, List<Object>>();
		updates.put(AuthLibConfig.INSERT_USER, new ArrayList<Object>() {{ 
			add(user.getId()); add(user.getApiKey()); add(user.getType()); add(new Timestamp(new Date().getTime()));}});
		if(User.LOCAL.equals(user.getType())){
			updates.put(AuthLibConfig.INSERT_USER_LOCAL, new ArrayList<Object>() {{ 
				add(user.getId());
				add(((UserLocal)user).getFirstName());
				add(((UserLocal)user).getLastName());
				add(((UserLocal)user).getEmail());
				add(((UserLocal)user).getPassword());
			}});
		}else{
			updates.put(AuthLibConfig.INSERT_USER_SOCIAL, new ArrayList<Object>() {{
				add(user.getId());
				add(((UserSocial)user).getType());
				add(((UserSocial)user).getSocialId());
				add(((UserSocial)user).getSocialUser());
				add(((UserSocial)user).getSocialDisplay());
				add(((UserSocial)user).getSocialDetails().toString());
			}});
		}
		int count = this.execUpdate(updates);
		updateCache(user);
		//check with 2 since the list of updates contains 2 statements and we expect to update 1 row for each statement
		return count == 2;
	}
	
	/**
	 * update the user
	 * @param user
	 * @return true if the update is succeeded 
	 */
	public boolean updateUser(final User user){
		LinkedHashMap<String, List<Object>> updates = new LinkedHashMap<String, List<Object>>();
		if(User.LOCAL.equals(user.getType())){
			updates.put(AuthLibConfig.UPDATE_USER_LOCAL, new ArrayList<Object>() {{ 
				add(((UserLocal)user).getFirstName());
				add(((UserLocal)user).getLastName());
				add(((UserLocal)user).getEmail());
				add(((UserLocal)user).getPassword());
				add(user.getId());
			}});
		}else{
			updates.put(AuthLibConfig.UPDATE_USER_SOCIAL, new ArrayList<Object>() {{ 
				add(((UserSocial)user).getSocialUser());
				add(((UserSocial)user).getSocialDisplay());
				add(((UserSocial)user).getSocialDetails().toString());
				add(user.getId());
			}});
		}
		updates.put(AuthLibConfig.UPDATE_USER, new ArrayList<Object>() {{ add(new Timestamp(new Date().getTime())); add(user.getId()); }});
		int count = this.execUpdate(updates);
		updateCache(user);
		//check with 2 since the list of updates contains 2 statements and we expect to update 1 row for each statement
		return count == 2;
	}
	
	/**
	 * reactivate a deleted user
	 * 
	 * @param id
	 * @return the reactivated user
	 */
	public User reactivateUser(final String id){
		this.execUpdate(AuthLibConfig.UPDATE_USER, new ArrayList<Object>() {{ add(new Timestamp(new Date().getTime())); add(id); }});
		User user = this.getUserById(id);
		return updateCache(user);
	}
	
	/**
	 * check if a social user has been deleted from the system
	 * 
	 * @param username
	 * @param social
	 * @return the id of the user if present but deleted, false otherwise
	 */
	public String checkDeletedUserSocial(final String social_id, String social){
		final String social_cleaned = UserSocial.TYPES.get(social);
		if(social_cleaned == null)
			throw new DAOException("Wrong value for social '"+social+"'. use one in "+UserSocial.TYPES.keySet());
		List<Properties> result = this.execQuery(AuthLibConfig.CHECK_USER_SOCIAL_DELETED, new ArrayList<Object>(){{add(social_id);add(social_cleaned);}});
		if(result.size()==0)
			return null;
		return result.get(0).getProperty("id");
	}
	
	/**
	 * check if a local user has been deleted from the system
	 * 
	 * @param email
	 * @return the id of the user if present but deleted, false otherwise
	 */
	public String checkDeletedUserLocal(final String email){
		List<Properties> result = this.execQuery(AuthLibConfig.CHECK_USER_LOCAL_DELETED, new ArrayList<Object>(){{add(email);}});
		if(result.size()==0)
			return null;
		return result.get(0).getProperty("id");
	}
	
	/**
	 * delete the user
	 * @param user
	 * @return true if the delete is succeeded 
	 */
	public boolean deleteUser(final User user){
		int count = this.execUpdate(AuthLibConfig.DELETE_USER, new ArrayList<Object>() {{ add(new Timestamp(new Date().getTime())); add(user.getId()); }});
		removeCache(user.getId());
		return count==1;
	}
	
}