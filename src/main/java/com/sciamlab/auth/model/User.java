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

package com.sciamlab.auth.model;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import com.sciamlab.auth.util.AppConfig;

public abstract class User implements JSONString, Principal{

    private String id;

    private List<Role> roles = new ArrayList<Role>();
    private Map<String, String> profiles = new HashMap<String, String>();

    private String apikey;

    public User() {
        this.id = UUID.randomUUID().toString();
        this.apikey = UUID.randomUUID().toString();
        addRole(Role.anonymous); //all users are anonymous until credentials are proved
    }
    
    public abstract String getUserName();
    public abstract String getDisplayName();
    public abstract String getUserType();
    
    public String getId() {
		return id;
	}
    
    public void setId(String id) {
		this.id = id;
	}
    
    public String getName() {
        return this.getId();
    }

	public List<Role> getRoles() {
        return roles;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
    
    public Map<String, String> getProfiles() {
		return profiles;
	}

	public void addProfile(String api, String profile) {
		this.profiles.put(api, profile);
	}
	
	public String getProfile(String api) {
		String profile = this.profiles.get(api);
		return (profile!=null)?profile:AppConfig.API_BASIC_PROFILE;
	}

	public boolean equals(Object otherUser) {
        boolean response = false;

        if(otherUser == null) {
            response = false;
        }
        else if(! (otherUser instanceof User)) {
            response = false;
        }
        else {
            if(((User)otherUser).getId().equals(this.getId())) {
                response = true;
            }
        }

        return response;
    }

    public int hashCode() {
        return getId().hashCode();
    }

    public void setApiKey(String apikey) {
    	this.apikey = apikey;
    }
    
    public String getApiKey() {
    	return this.apikey;
    }
    
	@Override
	public String toString() {
		return "User [id="+id+", user_name=" + getUserName() + ", display_name=" + getDisplayName() + ", user_type=" + getUserType() 
				+ ", api_key=" + apikey + ", roles=" + roles + ", profiles=" + profiles + "]";
	}

	@Override
	public String toJSONString() {
		return toJSON().toString();
	}
	
	public JSONObject toJSON() {
		JSONObject result = new JSONObject();
		result.put("id", id);
        result.put("user_name", getUserName());
        result.put("display_name", getDisplayName());
        result.put("user_type", getUserType());
        result.put("api_key", apikey);
        JSONArray json_roles = new JSONArray();
		for(Role r : roles){
			json_roles.put(r.name());
		}
		result.put("roles", json_roles);
		JSONArray json_profiles = new JSONArray();
		for(String api : profiles.keySet()){
			JSONObject profile = new JSONObject();
			profile.put("api", api);
			profile.put("profile", profiles.get(api));
			json_profiles.put(profile);
		}
		result.put("profiles", json_profiles);
		return result;
	}
	
//	public User fromJSON(JSONObject json){
//    	User u = new User();
////    	u.setId(json.getString("id"));
////		u.setApiKey(json.getString("api_key"));
//		u.getRoles().clear();
//		for(int i=0 ; i<json.getJSONArray("roles").length() ; i++){
//			u.getRoles().add(Role.valueOf(json.getJSONArray("roles").getString(i)));
//		}
//		u.getProfiles().clear();
//		for(int i=0 ; i<json.getJSONArray("profiles").length() ; i++){
//			JSONObject p = json.getJSONArray("profiles").getJSONObject(i);
//			u.getProfiles().put(p.getString("api"), p.getString("profile"));
//		}
//		return u;
//    }

}
