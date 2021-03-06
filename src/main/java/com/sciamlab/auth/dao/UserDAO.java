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
import java.util.List;
import java.util.Map;

import com.sciamlab.auth.model.Role;
import com.sciamlab.auth.model.User;

/**
 * 
 * @author SciamLab
 *
 */

public interface UserDAO {
	
//	public User getUserByName(String name);
	
	public User getUserByApiKey(String apikey);
	
//	public User getUser(String col_key, final String col_value);
	
//	public List<User> getUserList();
	
	public List<Role> getRolesByUserId(final String id);
	
	/**
	 * get the user profiles on all the apis
	 * 
	 * @param user_id
	 * @return a map where the keys are the api names and the values are the related profiles
	 */
	public Map<String, String> getProfilesByUserId(final String user_id);
	
	/**
	 * set the user profile on the given api
	 * 
	 * @param user_id
	 * @param api
	 * @param profile
	 * @return number of updated records
	 */
	public User setUserProductProfile(final String user_id, final String api, final String profile);
	
	/**
	 * delete the user profile on the given api
	 * 
	 * @param user_id
	 * @param api
	 * @param profile
	 * @return number of updated records
	 */
	public User deleteUserProductProfile(final String user_id, final String api);
}
