package com.sciamlab.auth.model;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.json.JSONObject;
import org.json.JSONString;

public abstract class Member implements Principal, JSONString{
	
	protected String id;
	private Collection<Role> roleParents;
	
	public Member(String id){
		this.id = id;
		this.roleParents = new ArrayList<Role>();
	}
	public Member(){
		this(UUID.randomUUID().toString());
	}
	
	public String id() {
		return id;
	}
    
    @Override
    public String getName() {
        return id;
    }
    
    public void addRoles(Collection<Role> roles) {
    	for(Role role : roles)
    		this.addRole(role);
    }

    public boolean addRole(Role role) {
    	if(this.hasRole(role))
    		return false;
    	this.roleParents.add(role);
        this.addMemberToRole(role);
        return true;
    }
    protected abstract void addMemberToRole(Role role);
    
    public boolean removeRole(Role role) {
    	if(!this.roleParents.contains(role))
			return false;
        this.roleParents.remove(role);
        this.removeMemberFromRole(role);
        return true;
    }
    protected abstract void removeMemberFromRole(Role role);
    
    public boolean hasRole(Role role) {
        if(roleParents.contains(role))
        	return true;
        for(Role parent : roleParents)
        	if(parent.hasRole(role))
        		return true;
        return false;
    }
    
    public Set<Role> getAllRoles() {
    	Set<Role> roles = new HashSet<Role>();
        for(Role parent : roleParents){
        	roles.add(parent);
        	roles.addAll(parent.getAllRoles());
        }
        return roles;
    }
    
	@Override
	public String toString() {
		return "Member [id=" + id + "]";
	}
    
	@Override
	public String toJSONString(){
		return toJSON().toString();
	}
	
	public JSONObject toJSON(){
		return new JSONObject().put("id", id);
	}
    
	
}
