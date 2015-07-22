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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.MultivaluedHashMap;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.sciamlab.auth.model.Role;
import com.sciamlab.auth.model.User;
import com.sciamlab.auth.model.UserLocal;
import com.sciamlab.auth.model.UserSocial;
import com.sciamlab.auth.util.AuthLibConfig;
import com.sciamlab.common.dao.SciamlabDAO;
import com.sciamlab.common.exception.DAOException;
import com.sciamlab.common.exception.InternalServerErrorException;
import com.sciamlab.common.exception.UnauthorizedException;
import com.sciamlab.common.util.HTTPClient;
import com.sciamlab.common.util.SciamlabCollectionUtils;
import com.sciamlab.common.util.SciamlabStringUtils;

/**
 * 
 * @author SciamLab
 *
 */

public abstract class SciamlabRemoteAuthDAO extends SciamlabDAO implements SciamlabAuthDAO{
	
	private static final Logger logger = Logger.getLogger(SciamlabRemoteAuthDAO.class);
	
	private HTTPClient http = new HTTPClient();
	
	@Override
	public User getUserByApiKey(final String apikey) {
		String result;
		try {
			logger.info("Invoking remote auth service... ["+AuthLibConfig.API_KEY_VALIDATION_ENDPOINT+"]");
			result = this.http.doGET(new URL(AuthLibConfig.API_KEY_VALIDATION_ENDPOINT), 
					new MultivaluedHashMap<String, String>(){{
						put("key", new ArrayList<String>(){{ add(apikey); }});
					}},null).readEntity(String.class);
		} catch (MalformedURLException e) {
			throw new DAOException(e);
		}
		JSONObject json = null;
		try {
			json = new JSONObject(result);
		} catch (Exception e) {
			throw new DAOException(json.toString());
		}
		if(!json.has("success"))
			throw new DAOException(json.toString());
		if(!json.getBoolean("success"))
			return null;
		json = json.getJSONObject("user"); 
		User u = null;
		if("local".equals(json.getString("user_type"))){
			u = new UserLocal();
			((UserLocal) u).setFirstName(json.getString("first_name"));
			((UserLocal) u).setEmail(json.getString("email"));
		}else{
			u = new UserSocial();
			((UserSocial) u).setUserType(UserSocial.SOCIAL_TYPES.get(json.getString("user_type")));
			((UserSocial) u).setSocialDetails(json.getJSONObject("social_details"));
		}
		u.setApiKey(json.getString("api_key"));
		u.setId(json.getString("id"));
		u.getRoles().clear();
		for(String r : SciamlabCollectionUtils.asStringList(json.getJSONArray("roles"))){
			u.getRoles().add(Role.valueOf(r));
		}
		u.getProfiles().clear();
		for(Object p : SciamlabCollectionUtils.asList(json.getJSONArray("profiles"))){
			u.getProfiles().put(((JSONObject)p).getString("api"), ((JSONObject)p).getString("profile"));
		}
		logger.debug("User: "+u);
        return u;
	}
	
}