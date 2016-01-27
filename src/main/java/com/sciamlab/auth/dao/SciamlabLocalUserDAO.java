package com.sciamlab.auth.dao;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
import com.sciamlab.common.util.SciamlabDateUtils;
import com.sciamlab.common.util.SciamlabTokenUtils;

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

public abstract class SciamlabLocalUserDAO extends SciamlabDAO implements UserValidator, UserDAO {
	
	private static final Logger logger = Logger.getLogger(SciamlabLocalUserDAO.class);
	private static Map<String, Map<String, User>> USERS_BY_TYPE_USERNAME = new HashMap<String, Map<String, User>>();
	private static Map<String, User> USERS_BY_ID = new HashMap<String, User>();
	private static Map<String, User> USERS_BY_API_KEY = new HashMap<String, User>();
	private static Map<String, Role> ROLES_BY_NAME = new HashMap<String, Role>();
	private static Map<String, User> LOGGED_USERS = new HashMap<String, User>();
	private static Map<String, String> LOGGED_USERS_REVERSE = new HashMap<String, String>();
	private static List<String> PRODUCTS = new ArrayList<String>();
	
//	public void buildCache() {
//		//caching products on startup
//		initCacheProductsList();
//		//caching products on startup
//		initCacheRolesList();
//		//caching users on startup
//		initCacheUsersList();
//	}
	
	protected static User updateCachedUser(User user){
		Map<String, User> map = USERS_BY_TYPE_USERNAME.get(user.getType());
		if(map==null){
			map = new HashMap<String, User>();
			USERS_BY_TYPE_USERNAME.put(user.getType(), map);
		}
		map.put(user.getUserName(), user);
		USERS_BY_ID.put(user.id(), user);
		USERS_BY_API_KEY.put(user.getApiKey(), user);
		String jwt = LOGGED_USERS_REVERSE.get(user.id());
		if(jwt!=null)
			LOGGED_USERS.put(jwt, user);
		return user;
	}
	
	public static void removeCachedUser(String id){
		User user = USERS_BY_ID.remove(id);
		if(user!=null){
			USERS_BY_API_KEY.remove(user.getApiKey());
			Map<String, User> map = USERS_BY_TYPE_USERNAME.get(user.getType());
			if(map!=null)
				map.remove(user.getUserName());
//			String jwt = LOGGED_USERS_REVERSE.remove(user.getId());
//			if(jwt!=null)
//				LOGGED_USERS.remove(jwt);
		}
	}
	
	public static User getCachedUserByTypeAndUsername(String type, String username){
		if(USERS_BY_TYPE_USERNAME.containsKey(type))
			return USERS_BY_TYPE_USERNAME.get(type).get(username);
		else
			return null;
	}
	public static User getCachedUserByUsername(String username){
		for(Map<String, User> map : USERS_BY_TYPE_USERNAME.values()){
			if(map.containsKey(username))
				return map.get(username);
		}
		return null;
	}
	public static User getCachedUserByID(String id){
		return USERS_BY_ID.get(id);
	}
	public static User getCachedUserByApiKey(String key){
		return USERS_BY_API_KEY.get(key);
	}
	
	protected static void addLoggedUser(String jwt, User user){
		LOGGED_USERS.put(jwt, user);
		LOGGED_USERS_REVERSE.put(user.id(), jwt);
	}
	
	protected static boolean removeLoggedUserByJWT(String jwt){
		User user = LOGGED_USERS.remove(jwt);
		if(user!=null){
			LOGGED_USERS_REVERSE.remove(user.id());
			removeCachedUser(user.id());
			return true;
		}else{
			return false;
		}
	}
	
	protected static boolean removeLoggedUserByID(String id){
		String jwt = LOGGED_USERS_REVERSE.remove(id);
		if(jwt!=null){
			User user = LOGGED_USERS.remove(jwt);
			if(user!=null)
				removeCachedUser(user.id());
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * init the cache map containing the list of roles
	 * 
	 * @return the list of roles
	 */
	public Collection<Role> initRolesCache() {
		List<Properties> result = this.execQuery(AuthLibConfig.GET_ROLES_LIST);
		for(Properties p : result){
			Role role = new Role(p.getProperty("role"));
			ROLES_BY_NAME.put(role.getName().toUpperCase(), role);
		}
		result = this.execQuery(AuthLibConfig.GET_ROLE_TO_ROLE_LIST);
		for(Properties p : result){
			Role role = ROLES_BY_NAME.get(p.getProperty("role_child").toUpperCase());
			role.addRole(ROLES_BY_NAME.get(p.getProperty("role_parent").toUpperCase()));
		}
		for(Role r : ROLES_BY_NAME.values())
			logger.debug("ROLE: "+r);
		return ROLES_BY_NAME.values();
	}
	
	/**
	 * init the cache map containing the list of users
	 * 
	 * @return the list of users
	 */
	protected Collection<User> initUsersCache() {
		List<Properties> result = this.execQuery(AuthLibConfig.GET_USERS_LIST); 
		logger.debug("USERS:");
		for(Properties p : result){
			User u = this.buildUserFromResultSet(p);
			updateCachedUser(u);
			logger.debug(u.getType()+":\t"+u.getUserName());
		}
		return USERS_BY_ID.values();
	}
	
	protected User buildUserFromResultSet(Properties p){
		User u = null;
		if(User.LOCAL.equals(p.getProperty("type"))){
			u = new UserLocal(p.getProperty("email"));
			((UserLocal) u).setFirstName(p.getProperty("first_name"));
			((UserLocal) u).setLastName(p.getProperty("last_name"));
			((UserLocal) u).setEmail(p.getProperty("email"));
			((UserLocal) u).setPassword(p.getProperty("password"));
		}else{
			u = new UserSocial(p.getProperty("social_id"), UserSocial.TYPES.get(p.getProperty("social")));
			((UserSocial) u).setSocialUser(p.getProperty("user_name"));
			((UserSocial) u).setSocialDisplay(p.getProperty("display_name"));
			((UserSocial) u).setSocialDetails(new JSONObject(((PGobject) p.get("details")).getValue()));
		}
		u.setApiKey(p.getProperty("api_key"));
		u.setId(p.getProperty("id"));
		u.addRoles(this.getRolesByUserId(p.getProperty("id")));
		u.getProfiles().clear();
		u.getProfiles().putAll(this.getProfilesByUserId(p.getProperty("id")));
		return u;
	}
	
	
	/**
	 * init the cache map containing the list of products
	 * 
	 * @return the list of products
	 */
	public List<String> initProductsCache() {
		List<Properties> result = this.execQuery(AuthLibConfig.GET_PRODUCTS_LIST); 
		for(Properties p : result){
			PRODUCTS.add(p.getProperty("product"));
		}
		logger.debug("PRODUCTS: "+PRODUCTS);
        return PRODUCTS;
	}
	
	/**
	 * get the cached list of products
	 * 
	 * @return the list of products
	 */
	public Collection<String> getCachedProductsList() {
		return PRODUCTS;
	}
	/**
	 * get the cached list of users
	 * 
	 * @return the list of users
	 */
	public Collection<User> getCachedUsersList() {
		return USERS_BY_ID.values();
	}
	/**
	 * get the cached list of roles
	 * 
	 * @return the list of roles
	 */
	public Collection<Role> getCachedRolesList() {
		return ROLES_BY_NAME.values();
	}
	
	/**
	 * get the role by name
	 * 
	 * @param name
	 * @return the role
	 */
	public Role getRoleByName(final String name){
		if(ROLES_BY_NAME.containsKey(name.toUpperCase()))
			return ROLES_BY_NAME.get(name.toUpperCase());
		List<Properties> result = this.execQuery(AuthLibConfig.GET_ROLE_BY_NAME, new ArrayList<Object>(){{add(name);}}); 
		if(result.size()==0) return null;
		if(result.size()>1) 
			throw new DAOException("Multiple roles retrieved for name "+name);
		Role role = new Role(name).description(result.get(0).getProperty("description"));
		logger.debug("Role: "+role);
		ROLES_BY_NAME.put(name.toUpperCase(), role);
		return role;
	}
	
	/**
	 * get the user roles
	 * 
	 * @param id
	 * @return the list of user roles
	 */
	public List<Role> getRolesByUserId(final String id) {
		Set<Role> roles = new HashSet<Role>();
		User cached = getCachedUserByID(id);
		if(cached!=null){
			roles.addAll(cached.getAllRoles());
			return new ArrayList<Role>(roles);
		}
		List<Properties> result = this.execQuery(AuthLibConfig.GET_ROLES_BY_USER_ID, new ArrayList<Object>(){{ add(id); }});
		roles.add(Role.ANONYMOUS);
		if(result.size()==0) 
			return new ArrayList<Role>(roles);
		for(Properties p : result){
			Role role = getRoleByName(p.getProperty("role").toUpperCase());
			if(role!=null){
				roles.add(role);
				logger.debug("Role added: "+role);
			}else{
				logger.warn(p.getProperty("role")+" is not identified as a valid role!");
			}
		}
		return new ArrayList<Role>(roles);
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
				new ArrayList<Object>() {{ add(role.getName()); add(new Timestamp(new Date().getTime())); add(id); add(role.getName());}});
		if(result==0){
			//role not found for given product, need to insert
			result = this.execUpdate(AuthLibConfig.INSERT_USER_ROLE,
					new ArrayList<Object>() {{ add(id); add(role.getName()); add(new Timestamp(new Date().getTime())); }});
		}else{
			logger.warn("role "+role+" already set for user "+id);
		}
		removeCachedUser(id);
		return updateCachedUser(getUserById(id));
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
				new ArrayList<Object>() {{ add(id); add(role.getName());}});
		removeCachedUser(id);
		return updateCachedUser(getUserById(id));
	}
	
	/**
	 * get the user profiles for all the products
	 * 
	 * @param id
	 * @return a map of products, profiles
	 */
	public Map<String, String> getProfilesByUserId(final String id) {
		User cached = getCachedUserByID(id);
		if(cached!=null)
			return cached.getProfiles();
		List<Properties> result = this.execQuery(AuthLibConfig.GET_PROFILES_BY_USER_ID, new ArrayList<Object>(){{ add(id); }});
		Map<String, String> profiles = new HashMap<String, String>();
		for(String product : PRODUCTS){
			boolean found = false;
			for(Properties p : result){
				if(product.equals(p.getProperty("product"))){
					found = true;
					//Day, Week, SemiMonth, Month, Year
					String period = p.getProperty("period");
					int gregorian_calendar_field = -1;
					int multiplier = 1;
					if("Day".equals(period))
						gregorian_calendar_field = GregorianCalendar.DAY_OF_YEAR;
					else if("Week".equals(period))
						gregorian_calendar_field = GregorianCalendar.WEEK_OF_YEAR;
					else if("SemiMonth".equals(period)){
						gregorian_calendar_field = GregorianCalendar.WEEK_OF_YEAR;
						multiplier = 2;
					}else if("Month".equals(period))
						gregorian_calendar_field = GregorianCalendar.MONTH;
					else if("Year".equals(period))
						gregorian_calendar_field = GregorianCalendar.YEAR;
					if(gregorian_calendar_field==-1)
						throw new DAOException("period not valid: "+period);
					
					int duration = Integer.parseInt(p.getProperty("duration"));
					Calendar start = new GregorianCalendar();
					start.setTime(SciamlabDateUtils.getDateFromIso8061DateString(p.getProperty("start")));
					start.add(gregorian_calendar_field, duration);
					String wired_profile_to_be_replaced_by_dynamic_parameter = "gold";
					//TODO CONTINUA DA QUA!!
					String profile = (start.after(new GregorianCalendar())) ? wired_profile_to_be_replaced_by_dynamic_parameter : AuthLibConfig.API_BASIC_PROFILE;
					profiles.put(product, profile);
					break;
				}
			}
			if(!found){
				profiles.put(product, AuthLibConfig.API_BASIC_PROFILE);
			}
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
		removeCachedUser(id);
		return updateCachedUser(getUserById(id));
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
		removeCachedUser(id);
		return updateCachedUser(getUserById(id));
	}
	
	@Override
	public User validate(String jwt){
		User u = LOGGED_USERS.get(jwt);
		return u;
	}
	
	/**
	 * invalidate the user session related to the given token
	 * @param jwt
	 * @return true if there is an active session for the given token, false otherwise
	 */
	public boolean logoutByJWT(String jwt){
		return removeLoggedUserByJWT(jwt);
	}
	
	/**
	 * invalidate the user session related to the given token
	 * @param jwt
	 * @return true if there is an active session for the given token, false otherwise
	 */
	public boolean logoutByID(String id){
		return removeLoggedUserByID(id);
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
		User cached = getCachedUserByID(id);
		if(cached!=null)
			return cached;
		List<Properties> result = this.execQuery(AuthLibConfig.GET_USER_BY_ID, new ArrayList<Object>(){{add(id);}}); 
		if(result.size()==0) return null;
		if(result.size()>1) 
			throw new DAOException("Multiple users retrieved for id "+id);
		User u = this.buildUserFromResultSet(result.get(0));
		logger.debug("User: "+u);
		return updateCachedUser(u);
	}
	
	/**
	 * get the user by api key
	 * 
	 * @param username
	 * @param type
	 * @return the user
	 */
	public User getUserByApiKey(final String api_key){
		User cached = getCachedUserByApiKey(api_key);
		if(cached!=null)
			return cached;
		List<Properties> result = this.execQuery(AuthLibConfig.GET_USER_BY_API_KEY, new ArrayList<Object>(){{add(api_key);}}); 
		if(result.size()==0) return null;
		if(result.size()>1) 
			throw new DAOException("Multiple users retrieved for api key "+api_key);
		User u = this.buildUserFromResultSet(result.get(0));
		logger.debug("User: "+u);
		return updateCachedUser(u);
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
		UserLocal cached = (UserLocal) getCachedUserByTypeAndUsername(UserLocal.LOCAL, email);
		if(cached!=null)
			return cached;
		List<Properties> result = this.execQuery(AuthLibConfig.GET_USER_LOCAL, new ArrayList<Object>(){{ add(email); }});
		if(result.size()==0) return null;
		if(result.size()>1) 
			throw new DAOException("Multiple local users retrieved for email "+email);
		User u = this.buildUserFromResultSet(result.get(0));
		logger.debug("User: "+u);
		return (UserLocal) updateCachedUser(u);
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
		UserSocial cached = (UserSocial) getCachedUserByTypeAndUsername(social, username);
		if(cached!=null)
			return cached;
		List<Properties> result = this.execQuery(AuthLibConfig.GET_USER_SOCIAL, new ArrayList<Object>(){{ add(username); add(social_cleaned);}});
		if(result.size()==0) return null;
		if(result.size()>1) 
			throw new DAOException("Multiple social users retrieved for username "+username+" on "+social_cleaned);
		User u = this.buildUserFromResultSet(result.get(0));
		logger.debug("User: "+u);
		return (UserSocial) updateCachedUser(u);
	}
	
	/**
	 * insert the user
	 * @param user
	 * @return true if the insert is succeeded 
	 */
	public boolean addUser(final User user){
		LinkedHashMap<String, List<Object>> updates = new LinkedHashMap<String, List<Object>>();
		updates.put(AuthLibConfig.INSERT_USER, new ArrayList<Object>() {{ 
			add(user.id()); add(user.getApiKey()); add(user.getType()); add(new Timestamp(new Date().getTime()));}});
		if(User.LOCAL.equals(user.getType())){
			updates.put(AuthLibConfig.INSERT_USER_LOCAL, new ArrayList<Object>() {{ 
				add(user.id());
				add(((UserLocal)user).getFirstName());
				add(((UserLocal)user).getLastName());
				add(((UserLocal)user).getEmail());
				add(((UserLocal)user).getEncodedPassword());
			}});
		}else{
			updates.put(AuthLibConfig.INSERT_USER_SOCIAL, new ArrayList<Object>() {{
				add(user.id());
				add(((UserSocial)user).getType());
				add(((UserSocial)user).getSocialId());
				add(((UserSocial)user).getSocialUser());
				add(((UserSocial)user).getSocialDisplay());
				add(((UserSocial)user).getSocialDetails().toString());
			}});
		}
		int count = this.execUpdate(updates);
		updateCachedUser(user);
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
				add(((UserLocal)user).getEncodedPassword());
				add(user.id());
			}});
		}else{
			updates.put(AuthLibConfig.UPDATE_USER_SOCIAL, new ArrayList<Object>() {{ 
				add(((UserSocial)user).getSocialUser());
				add(((UserSocial)user).getSocialDisplay());
				add(((UserSocial)user).getSocialDetails().toString());
				add(user.id());
			}});
		}
		updates.put(AuthLibConfig.UPDATE_USER, new ArrayList<Object>() {{ add(new Timestamp(new Date().getTime())); add(user.id()); }});
		int count = this.execUpdate(updates);
		updateCachedUser(user);
		//check with 2 since the list of updates contains 2 statements and we expect to update 1 row for each statement
		return count == 2;
	}
	
	/**
	 * reactivate a deleted user
	 * 
	 * @param id
	 * @return the reactivated user
	 */
	public User reactivateUser(final String id, final String type){
		this.execUpdate(AuthLibConfig.UPDATE_USER, new ArrayList<Object>() {{ add(new Timestamp(new Date().getTime())); add(id); }});
		User user = this.getUserById(id);
		return updateCachedUser(user);
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
		int count = this.execUpdate(AuthLibConfig.DELETE_USER, new ArrayList<Object>() {{ add(new Timestamp(new Date().getTime())); add(user.id()); }});
		removeCachedUser(user.id());
		return count==1;
	}
	
	/**
	 * create or update a role
	 * 
	 * @param role, the role to be created
	 * @return the created role
	 */
	public boolean upsertRole(final Role role) {
		int result = this.execUpdate(AuthLibConfig.UPDATE_ROLE,
				new ArrayList<Object>() {{ add(new Timestamp(new Date().getTime())); add(role.description()); add(role.getName());}});
		if(result==0){
			//role not found for given role, need to insert
			result = this.execUpdate(AuthLibConfig.INSERT_ROLE,
					new ArrayList<Object>() {{ add(role.getName()); add(role.description()); add(new Timestamp(new Date().getTime())); }});
		}
		ROLES_BY_NAME.put(role.getName().toUpperCase(), role);
		return result==1;
	}
	
	/**
	 * add a role parent to a role child
	 * 
	 * @param role, the role child
	 * @param role, the role parent
	 * @return true if successful
	 */
	public boolean addRoleToRole(final Role child, final Role parent) {
		boolean result = ROLES_BY_NAME.get(child.getName().toUpperCase()).addRole(parent);
		if(result)
			this.execUpdate(AuthLibConfig.INSERT_ROLE_TO_ROLE,
				new ArrayList<Object>() {{ add(child.getName()); add(parent.getName()); add(new Timestamp(new Date().getTime())); }});
		return result;
	}
	
	/**
	 * delete a role parent to a role child
	 * 
	 * @param role, the role child
	 * @param role, the role parent
	 * @return true if successful
	 */
	public boolean deleteRoleToRole(final Role child, final Role parent) {
		boolean result = ROLES_BY_NAME.get(child.getName().toUpperCase()).removeRole(parent);
		if(result)
			this.execUpdate(AuthLibConfig.DELETE_ROLE_TO_ROLE,
				new ArrayList<Object>() {{ add(child.getName()); add(parent.getName());}});
		return result;
	}
	
	/**
	 * delete the role
	 * 
	 * @param role, the role to be deleted
	 * @return boolean
	 */
	public boolean deleteRole(final Role role) {
		ROLES_BY_NAME.remove(role.getName().toUpperCase());
		for(Role r : ROLES_BY_NAME.values())
			if(r.hasRole(role))
				r.removeRole(role);
		for(User u : getCachedUsersList())
			if(u.hasRole(role)){
				removeCachedUser(u.id());
				updateCachedUser(getUserById(u.id()));
			}
		int result = this.execUpdate(AuthLibConfig.DELETE_ROLE,
				new ArrayList<Object>() {{ add(role.getName()); }});
		return result == 1;
	}

}
