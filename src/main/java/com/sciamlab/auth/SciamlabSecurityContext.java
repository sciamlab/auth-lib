package com.sciamlab.auth;

import java.security.Principal;

import javax.ws.rs.core.SecurityContext;

import org.apache.log4j.Logger;

import com.sciamlab.auth.model.Role;
import com.sciamlab.auth.model.User;
import com.sciamlab.common.exception.ForbiddenException;

public class SciamlabSecurityContext implements SecurityContext {
	
	private static final Logger logger = Logger.getLogger(SciamlabSecurityContext.class);

    private final User user;

    public SciamlabSecurityContext(User user) {
        this.user = user;
//        logger.debug(SciamlabSecurityContext.class.getSimpleName()+" created for user: "+user.toString());
    }
    
    @Override
    public Principal getUserPrincipal() {
        return user;
    }

    @Override
    public boolean isUserInRole(String role) {
    	if(role.equalsIgnoreCase(Role.ANONYMOUS.getName()))
             return true;

    	if(user == null)
            throw new ForbiddenException();

    	for(Role r : user.getAllRoles()){
        	if(r.getName().equalsIgnoreCase(role))
        		return true;
        }
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return SecurityContext.BASIC_AUTH;
    }
}
