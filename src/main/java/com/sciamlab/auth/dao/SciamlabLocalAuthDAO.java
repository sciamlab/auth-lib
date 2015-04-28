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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.sciamlab.auth.model.Role;
import com.sciamlab.auth.model.User;
import com.sciamlab.auth.model.UserLocal;
import com.sciamlab.auth.util.AppConfig;
import com.sciamlab.common.exception.DAOException;

/**
 * 
 * @author SciamLab
 *
 */

public abstract class SciamlabLocalAuthDAO extends SciamlabAuthDAO{
	
	private static final Logger logger = Logger.getLogger(SciamlabLocalAuthDAO.class);
	
	@Override
	protected User getUser(String col_key, final String col_value) {
		List<Properties> map = this.execQuery("SELECT * FROM \"user\" WHERE "+col_key+" = ?", 
				new ArrayList<Object>(){{ add(col_value); }},
				new ArrayList<String>(){{ add("id"); add("name"); add("fullname"); add("email"); add("apikey"); }}); 
		if(map.size()==0) return null;
		if(map.size()>1) 
			throw new DAOException("Multiple users retrieved using "+col_key+": "+col_value);
		Properties p = map.get(0);
		UserLocal u = new UserLocal();
		u.setFirstName(p.getProperty("fullname"));
		u.setEmail(p.getProperty("email"));
		u.setApiKey(p.getProperty("apikey"));
		u.setId(p.getProperty("id"));
		u.getRoles().clear();
		u.getRoles().addAll(this.getRolesByUserId(p.getProperty("id")));
		u.getProfiles().clear();
		u.getProfiles().putAll(this.getProfilesByUserId(p.getProperty("id")));
		logger.debug("User: "+u);
        return u;
	}
	
	@Override
	public List<Role> getRolesByUserId(final String id) {
		Map<String, Properties> map = this.execQuery("SELECT DISTINCT role FROM user_object_role WHERE user_id = ?",
				new ArrayList<Object>(){{ add(id); }}, "role", new ArrayList<String>());
		List<Role> roles = new ArrayList<Role>();
		if(map.size()==0) {
			roles.add(Role.anonymous);
			return roles;
		}
		
		for(String r : map.keySet()){
			Role role = Role.valueOf(r);
			if(role!=null){
				roles.add(role);
				logger.debug("Role added: "+r);
			}else{
				logger.warn(r+"is not identified as a valid role!");
			}
		}
		if(roles.isEmpty())
			roles.add(Role.anonymous);
		return roles;
	}
	
	/**
	 * get the user profiles on all the apis
	 * 
	 * @param user_id
	 * @return a map where the keys are the api names and the values are the related profiles
	 */
	@Override
	public Map<String, String> getProfilesByUserId(final String user_id) {
		Map<String, Properties> result = this.execQuery("SELECT DISTINCT api,profile FROM user_api_profiles WHERE user_id = ?", 
				new ArrayList<Object>(){{ add(user_id); }}, "api", new ArrayList<String>(){{ add("api"); add("profile"); }});
		Map<String, String> profiles = new HashMap<String, String>();
		for(String api : AppConfig.API_LIST){
			String profile = (result.containsKey(api)) ? result.get(api).getProperty("profile") : AppConfig.API_BASIC_PROFILE;
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
	@Override
	public int setUserAPIProfile(final String user_id, final String api, final String profile) {
		int result = this.execUpdate("UPDATE user_api_profiles SET profile = ?, modified = ? WHERE user_id = ? AND api = ?",
				new ArrayList<Object>() {{ add(profile); add(new java.sql.Date(new Date().getTime())); add(user_id); add(api);}});
		if(result==0){
			//profile not found for given api, need to insert
			result = this.execUpdate("INSERT INTO user_api_profiles VALUES (?, ?, ?, ? ,?)",
					new ArrayList<Object>() {{ add(user_id); add(api); add(profile); add(new java.sql.Date(new Date().getTime())); add(new java.sql.Date(new Date().getTime())); }});
		}
		return result;
	}
	
	/**
	 * delete the user profile on the given api
	 * 
	 * @param user_id
	 * @param api
	 * @param profile
	 * @return number of updated records
	 */
	@Override
	public int deleteUserAPIProfile(final String user_id, final String api) {
		int result = this.execUpdate("DELETE FROM user_api_profiles WHERE user_id = ? AND api = ?",
				new ArrayList<Object>() {{ add(user_id); add(api); }});
		return result;
	}
	
}
