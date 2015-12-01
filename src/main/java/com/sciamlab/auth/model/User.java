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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

import com.sciamlab.auth.util.AuthLibConfig;

public abstract class User extends Member {
	
	public static final String LOCAL 	= "local";
	public static final String FACEBOOK = "facebook";
	public static final String GPLUS 	= "gplus";
	public static final String GITHUB 	= "github";
	public static final String TWITTER 	= "twitter";
	
	public static final Map<String,String> TYPES = new HashMap<String,String>(){{
		put(LOCAL,LOCAL); put(FACEBOOK,FACEBOOK); put(GPLUS,GPLUS); put(GITHUB,GITHUB); put(TWITTER,TWITTER);
	}};
	
//    private List<Role> roles = new ArrayList<Role>();
    private Map<String, String> profiles = new HashMap<String, String>();

    private String apikey;

    public User() {
    	super();
        this.apikey = UUID.randomUUID().toString();
//        this.addRole(Role.ANONYMOUS); //all users are anonymous until credentials are proved
    }
    
    public User(String id) {
        super(id);
//		this.addRole(Role.ANONYMOUS); //all users are anonymous until credentials are proved
    }
    
    public abstract String getUserName();
    public abstract String getDisplayName();
    public abstract String getType();
    
    public void setId(String id){
    	this.id = id;
    }
    
    @Override
    protected void addMemberToRole(Role role) {
        role.addMember(this);
    }
    @Override
	protected void removeMemberFromRole(Role role) {
        role.removeMember(this);
    }
    
    public Map<String, String> getProfiles() {
		return profiles;
	}

	public void addProfile(String api, String profile) {
		this.profiles.put(api, profile);
	}
	
	public String getProfile(String api) {
		String profile = this.profiles.get(api);
		return (profile!=null)?profile:AuthLibConfig.API_BASIC_PROFILE;
	}

	@Override
    public int hashCode() {
        return id.hashCode();
    }

    public void setApiKey(String apikey) {
    	this.apikey = apikey;
    }
    
    public String getApiKey() {
    	return this.apikey;
    }
    
    public boolean equals(Object otherUser) {
        if(otherUser == null) 
        	return false;
        else if(! (otherUser instanceof User))
        	return false;
        else if(((User)otherUser).getUserName().equals(this.getUserName()))
        	return true;
        return false;
    }
    
	@Override
	public String toString() {
		return "User [id="+id+", user_name=" + getUserName() + ", display_name=" + getDisplayName() + ", user_type=" + getType() 
				+ ", api_key=" + apikey + ", roles=" + getAllRoles() + ", profiles=" + profiles + "]";
	}

	@Override
	public JSONObject toJSON() {
		JSONObject result = super.toJSON();
        result.put("user_name", getUserName());
        result.put("display_name", getDisplayName());
        result.put("user_type", getType());
        result.put("api_key", apikey);
        JSONArray json_roles = new JSONArray();
		for(Role r : getAllRoles()){
			json_roles.put(r.getName());
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
	
}
