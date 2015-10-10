package com.sciamlab.auth.dao;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.ws.rs.core.MultivaluedHashMap;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.sciamlab.auth.model.Role;
import com.sciamlab.auth.model.User;
import com.sciamlab.auth.model.UserLocal;
import com.sciamlab.auth.model.UserSocial;
import com.sciamlab.common.exception.DAOException;
import com.sciamlab.common.util.HTTPClient;
import com.sciamlab.common.util.SciamlabCollectionUtils;

/**
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

public class CKANUserRemoteValidator implements UserValidator{
	
	private static final Logger logger = Logger.getLogger(CKANUserRemoteValidator.class);
	
	private HTTPClient http = new HTTPClient();
	
	private final String API_KEY_VALIDATION_ENDPOINT;
	
	public CKANUserRemoteValidator(String API_KEY_VALIDATION_ENDPOINT) {
		this.API_KEY_VALIDATION_ENDPOINT = API_KEY_VALIDATION_ENDPOINT;
	}
	
	@Override
	public User validate(final String apikey){
		String result;
		try {
			logger.debug("Invoking remote auth service... ["+API_KEY_VALIDATION_ENDPOINT+"]");
			result = this.http.doGET(new URL(API_KEY_VALIDATION_ENDPOINT), null,
					new MultivaluedHashMap<String, String>(){{
						put("Authorization", new ArrayList<String>(){{ add(apikey); }});
					}}).readEntity(String.class);
		} catch (MalformedURLException e) {
			throw new DAOException(e);
		}
		JSONObject json = null;
		try {
			json = new JSONObject(result);
		} catch (Exception e) {
			throw new DAOException(json.toString());
		}
		if(json.has("error") || !json.optBoolean("success"))
			return null;
		json = json.getJSONObject("user"); 
		
        User u = null;
		if("local".equals(json.getString("user_type"))){
			u = new UserLocal(json.getString("id"), json.getString("api_key"));
			((UserLocal) u).setFirstName(json.optString("first_name"));
			((UserLocal) u).setEmail(json.getString("email"));
		}else{
			u = new UserSocial(json.getString("id"), json.getString("api_key"));
			((UserSocial) u).setSocialType(UserSocial.TYPES.get(json.getString("user_type")));
			((UserSocial) u).setSocialDetails(json.getJSONObject("social_details"));
		}
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