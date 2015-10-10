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
import com.sciamlab.auth.model.User;

/**
 * 
 * @author SciamLab
 *
 */

public interface UserValidator {
	
	/**
	 * checks if the given token is bound to an active user session
	 * @param token
	 * @return the logged user, if any, or null if the given token doesn't refer to any user
	 */
	public User validate(String token);
}
