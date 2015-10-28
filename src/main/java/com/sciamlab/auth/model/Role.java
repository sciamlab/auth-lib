package com.sciamlab.auth.model;

import java.security.Principal;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import org.json.JSONArray;
import org.json.JSONObject;

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

public class Role extends Member {

	public static final Role ANONYMOUS = new Role("ANONYMOUS").description("Anonymous group");
	
	private String description;
	private Collection<Role> roleMembers;
	private Collection<User> userMembers;

	public Role(String id){
		super(id.toUpperCase());
		this.roleMembers = new ArrayList<Role>();
		this.userMembers = new ArrayList<User>();
	}
	
	public String description() {
		return description;
	}
	public Role description(String description) {
		this.description = description;
		return this;
	}
	
	@Override
    public boolean addRole(Role role) {
		if(this.equals(role))
			throw new RuntimeException("This role ["+this.id+"] is the same of the role ["+role.id+"] you are trying to add (so cannot be added as its parent!)");
		//check for cycles
        if(role.hasRole(this))
        	throw new RuntimeException("Circular dependency detected! This role ["+this.id+"] is already a parent of the role ["+role.id+"] (so cannot be added as its parent!)");
        return super.addRole(role);
    }
	@Override
	protected void addMemberToRole(Role role) {
		role.addMember(this);
	}
	@Override
	protected void removeMemberFromRole(Role role) {
        role.removeMember(this);
    }

	public boolean addMember(Role role) {
		if(this.equals(role))
			throw new RuntimeException("The role ["+role.id+"] you are trying to add is the same of this role ["+this.id+"] (so cannot be added as its member!)");
		if(this.hasMember(role))
			return false;
		//check for cycles
		if(this.hasRole(role))
			throw new RuntimeException("Circular dependency detected! The role ["+role.id+"] is already a parent of this role ["+this.id+"] (so cannot be added as its member!)");
		this.roleMembers.add(role);
		role.addRole(this);
		return true;
	}
	
	public boolean addMember(User user) {
		if(this.hasMember(user))
			return false;
		this.userMembers.add(user);
		user.addRole(this);
		return true;
	}
	
	public boolean removeMember(Role role) {
		if(!this.roleMembers.contains(role))
			return false;
		this.roleMembers.remove(role);
		role.removeRole(this);
		return true;
	}
	
	public boolean removeMember(User user) {
		if(!this.userMembers.contains(user))
			return false;
		this.userMembers.remove(user);
		user.removeRole(this);
		return true;
	}
	
	public boolean hasMember(Member member) {
		if(userMembers.contains(member))
			return true;
        if(roleMembers.contains(member))
        	return true;
        for(Role child : roleMembers)
        	if(child.hasMember(member))
        		return true;
        return false;
    }
	
	public Collection<Role> roleMembers() {
		return roleMembers;
	}
	
	public Collection<User> userMembers() {
		return userMembers;
	}

	@Override
	public String toString() {
		return "Role [id=" + id + ", description=" + description + "]";
	}
	
	@Override
	public JSONObject toJSON(){
		JSONArray json_roles = new JSONArray();
		for(Role r : getAllRoles())
			json_roles.put(r.getName());
		return super.toJSON().put("description", description).put("roles", json_roles);
	}
	
	
}

