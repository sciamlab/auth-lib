package com.sciamlab.auth.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.log4j.Logger;

import com.sciamlab.common.exception.UnauthorizedException;

@Priority(Priorities.AUTHORIZATION) 
public class RolesAllowedSecurityFilter  implements ContainerRequestFilter {
	private static final Logger logger = Logger.getLogger(RolesAllowedSecurityFilter.class);
	
    private final boolean denyAll;
    private final List<String> rolesAllowed;

    private RolesAllowedSecurityFilter(RolesAllowedSecurityFilterBuilder builder) { 
    	logger.info("Initializing "+RolesAllowedSecurityFilter.class.getSimpleName()+"...");
    	this.denyAll = builder.denyAll;
    	this.rolesAllowed = builder.rolesAllowed;
    	logger.info("[DONE]");
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (!denyAll) 
            for (String role : rolesAllowed) 
                if (requestContext.getSecurityContext().isUserInRole(role)) 
                    return;
        throw new UnauthorizedException("Request rejected due to an authorization failure");
    }
    
    public static class RolesAllowedSecurityFilterBuilder{
		
    	private boolean denyAll;
    	private List<String> rolesAllowed;
		
		public static RolesAllowedSecurityFilterBuilder newBuilder(){
			return new RolesAllowedSecurityFilterBuilder();
		}
		
		private RolesAllowedSecurityFilterBuilder() {
			super();
			this.denyAll = false;
			this.rolesAllowed = new ArrayList<String>();
		}
		
		public RolesAllowedSecurityFilterBuilder role(String role) {
			this.rolesAllowed.add(role);
			return this;
		}
		
		public RolesAllowedSecurityFilterBuilder roles(List<String> roles) {
			this.rolesAllowed.addAll(roles);
			return this;
		}
		
		public RolesAllowedSecurityFilterBuilder denyAll() {
			this.denyAll = true;
			return this;
		}

		public RolesAllowedSecurityFilter build() {
			return new RolesAllowedSecurityFilter(this);
		}
    }
}