package com.sciamlab.auth.filter;

import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;

import com.sciamlab.auth.SciamlabSecurityContext;
import com.sciamlab.auth.dao.SciamlabAuthDAO;
import com.sciamlab.auth.model.User;
import com.sciamlab.common.exception.BadRequestException;
import com.sciamlab.common.exception.InternalServerErrorException;

@Priority(Priorities.AUTHENTICATION)
public class ApiKeySecurityFilter implements ContainerRequestFilter {

	private static final Logger logger = Logger.getLogger(ApiKeySecurityFilter.class);
	
	private final SciamlabAuthDAO dao;
	
	private ApiKeySecurityFilter(ApiKeySecurityFilterBuilder builder) { 
    	logger.info("Initializing "+ApiKeySecurityFilter.class.getSimpleName()+"...");
    	this.dao = builder.dao;
    	logger.info("[DONE]");
    }

	@Override
    public void filter(ContainerRequestContext requestContext) {
    	ContainerRequest request = (ContainerRequest) requestContext;
    	String key = request.getHeaderString("Authorization");
        if(key==null){
        	List<String> key_params = request.getUriInfo().getQueryParameters().get("key");
        	if(key_params!=null && !key_params.isEmpty())
        		key = key_params.get(0);
        }
        if(key==null)
        	throw new BadRequestException("Missing Authorization key");
        
        try {
			User user = dao.getUserByApiKey(key);
			request.setSecurityContext(new SciamlabSecurityContext(user));
			return ;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new InternalServerErrorException(e);
		}
    }

    public static class ApiKeySecurityFilterBuilder{
		
    	private final SciamlabAuthDAO dao;
		
		public static ApiKeySecurityFilterBuilder newBuilder(SciamlabAuthDAO dao){
			return new ApiKeySecurityFilterBuilder(dao);
		}
		
		private ApiKeySecurityFilterBuilder(SciamlabAuthDAO dao) {
			super();
			this.dao = dao;
		}

		public ApiKeySecurityFilter build() {
			return new ApiKeySecurityFilter(this);
		}
    }

}