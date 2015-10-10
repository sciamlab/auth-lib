package com.sciamlab.auth.filter;

import java.util.Date;
import java.util.List;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseFilter;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;

import com.sciamlab.auth.util.APICall;

@Priority(Priorities.AUTHENTICATION)
public class UsageLoggingContextFilter implements ContainerRequestFilter {

	private static final Logger logger = Logger.getLogger(UsageLoggingContextFilter.class);
	private static Logger usage_logger = Logger.getLogger("USAGE");

    private static volatile UsageLoggingContextFilter instance = null;
	 
	public static UsageLoggingContextFilter getInstance() {
		if (instance == null) {
			synchronized (UsageLoggingContextFilter.class) {
				if (instance == null) 
					instance = new UsageLoggingContextFilter();
			}
		}
		return instance;
	}
    
	private UsageLoggingContextFilter() { 
    	logger.info("Initializing "+UsageLoggingContextFilter.class.getSimpleName()+"...");
    	//noop
    	logger.info("[DONE]");
    }

	@Override
    public void filter(ContainerRequestContext requestContext) {
    	ContainerRequest request = (ContainerRequest) requestContext;
		usage_logger.info(this.getAPICall(request).toJSONString());        
        return;
    }

    private APICall getAPICall(ContainerRequest request) {
    	String key = request.getHeaderString("Authorization");
    	if(key==null){
        	List<String> key_params = request.getUriInfo().getQueryParameters().get("key");
        	if(key_params!=null && !key_params.isEmpty())
        		key = key_params.get(0);
        }
    	APICall call = new APICall(
    			key, 
    			new Date(), 
    			-1, null,
    			request.getMethod(),
    			(request.getMediaType()!=null)?request.getMediaType().toString():null,
    			request.getBaseUri().toString(), 
    			null, 
    			request.getRequestUri().toString(), 
    			null, request.getRequestHeaders(), null);//request.readEntity(String.class));
		logger.debug("CALL: "+call.toJSONString());
		return call;
	}

	public ContainerRequestFilter getRequestFilter() {
        return this;
    }

    public ContainerResponseFilter getResponseFilter() {
        return null;
    }

}
