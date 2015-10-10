package com.sciamlab.auth.filter;

import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;

import com.sciamlab.auth.SciamlabSecurityContext;
import com.sciamlab.auth.dao.UserValidator;
import com.sciamlab.auth.model.User;
import com.sciamlab.common.exception.BadRequestException;
import com.sciamlab.common.exception.InternalServerErrorException;
import com.sciamlab.common.exception.SciamlabWebApplicationException;
import com.sciamlab.common.exception.UnauthorizedException;

@Priority(Priorities.AUTHENTICATION)
public class TokenBasedSecurityFilter implements ContainerRequestFilter {

	private static final Logger logger = Logger.getLogger(TokenBasedSecurityFilter.class);
	
	private final UserValidator user_validator;
	
	private TokenBasedSecurityFilter(TokenBasedSecurityFilterBuilder builder) { 
    	logger.info("Initializing "+TokenBasedSecurityFilter.class.getSimpleName()+"...");
    	this.user_validator = builder.user_validator;
    	logger.info("[DONE]");
    }

	@Override
    public void filter(ContainerRequestContext requestContext) {
		try {
			ContainerRequest request = (ContainerRequest) requestContext;
	    	String jwt = request.getHeaderString("Authorization");
	        if(jwt==null){
	        	List<String> key_params = request.getUriInfo().getQueryParameters().get("key");
	        	if(key_params!=null && !key_params.isEmpty())
	        		jwt = key_params.get(0);
	        }
	        if(jwt==null)
	        	throw new BadRequestException("Missing Authorization key");
	        User user = user_validator.validate(jwt); 
	        if(user==null)
	        	throw new UnauthorizedException("No session found for user");
			request.setSecurityContext(new SciamlabSecurityContext(user));
			return ;
		}catch(SciamlabWebApplicationException e){
			logger.error(e.getMessage());
			throw e;
		}catch(Exception e){
			logger.error(e.getMessage(), e);
			throw new InternalServerErrorException(e);
		}
    }

    public static class TokenBasedSecurityFilterBuilder{
		
    	private final UserValidator user_validator;
		
		public static TokenBasedSecurityFilterBuilder init(UserValidator user_validator){
			return new TokenBasedSecurityFilterBuilder(user_validator);
		}
		
		private TokenBasedSecurityFilterBuilder(UserValidator user_validator) {
			super();
			this.user_validator = user_validator;
		}

		public TokenBasedSecurityFilter build() {
			return new TokenBasedSecurityFilter(this);
		}
    }
}