package com.sciamlab.auth.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;

import com.sciamlab.auth.SciamlabSecurityContext;
import com.sciamlab.auth.model.Role;
import com.sciamlab.auth.model.User;
import com.sciamlab.auth.model.UserLocal;
import com.sciamlab.auth.model.UserSocial;
import com.sciamlab.common.exception.BadRequestException;
import com.sciamlab.common.exception.InternalServerErrorException;
import com.sciamlab.common.exception.UnauthorizedException;

@Priority(Priorities.AUTHENTICATION)
public class JWTSecurityFilter implements ContainerRequestFilter {

	private static final Logger logger = Logger.getLogger(JWTSecurityFilter.class);
	
	private final LocalSecurityDao dao;
	
	private JWTSecurityFilter(JWTSecurityFilterBuilder builder) { 
    	logger.info("Initializing "+JWTSecurityFilter.class.getSimpleName()+"...");
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
        if(!dao.getLoggedUsers().containsKey(key))
        	throw new UnauthorizedException("no session found for user");
        try {
//			User user = dao.getLocalUsers().get(dao.getLoggedUsers().get(key));
			request.setSecurityContext(new SciamlabSecurityContext(null));//user));
			return ;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new InternalServerErrorException(e);
		}
    }

    public static class JWTSecurityFilterBuilder{
		
    	private final LocalSecurityDao dao;
		
		public static JWTSecurityFilterBuilder newBuilder(){
			return new JWTSecurityFilterBuilder();
		}
		
		private JWTSecurityFilterBuilder() {
			super();
			this.dao = new LocalSecurityDao();
		}

		public JWTSecurityFilter build() {
			return new JWTSecurityFilter(this);
		}
    }
    
    public static class LocalSecurityDao{
    	
    	private static Map<String,UserLocal> users_local = new HashMap<String, UserLocal>();
    	private static Map<String,UserSocial> users_facebook = new HashMap<String, UserSocial>();
    	private static Map<String,UserSocial> users_google = new HashMap<String, UserSocial>();
    	private static Map<String,UserSocial> users_twitter = new HashMap<String, UserSocial>();
    	private static Map<String,UserSocial> users_github = new HashMap<String, UserSocial>();
    	private static Map<String, Map> users = new HashMap<String, Map>();
    	private static Map<String,User> logged_users = new HashMap<String, User>();
    	
    	static{
    		logger.debug("Starting LocalSecurityDao...");
    		UserLocal u = new UserLocal();
    		u.setFirstName("Paolo");
    		u.setLastName("Starace");
    		u.setEmail("paolo@sciamlab.com");
    		u.setPassword("1234");
    		u.getRoles().clear();
    		u.getRoles().add(Role.admin);
    		u.getRoles().add(Role.editor);
    		u.getProfiles().clear();
    		logger.debug("User: "+u);
    		users_local.put(u.getEmail(), u);
    		u = new UserLocal(); 
    		u.setFirstName("Alessio");
    		u.setLastName("Dragoni");
    		u.setEmail("ad@sciamlab.com");
    		u.setPassword("1234");
    		u.getRoles().clear();
    		u.getRoles().add(Role.editor);
    		u.getProfiles().clear();
    		logger.debug("User: "+u);
    		users_local.put(u.getEmail(), u);
    		users.put(UserSocial.FACEBOOK, users_facebook);
    		users.put(UserSocial.GOOGLE, users_google);
    		users.put(UserSocial.GITHUB, users_github);
    		users.put(UserSocial.TWITTER, users_twitter);
    		users.put(UserLocal.LOCAL, users_local);
    		logger.debug("[DONE]");
    	}
    	
    	public Map<String,UserLocal> getLocalUsers(){
    		return users_local;
    	}
    	public Map<String,UserSocial> getFacebookUsers(){
    		return users_facebook;
    	}
    	public Map<String,UserSocial> getGoogleUsers(){
    		return users_google;
    	}
    	public Map<String,UserSocial> getTwitterUsers(){
    		return users_twitter;
    	}
    	public Map<String,UserSocial> getGithubUsers(){
    		return users_github;
    	}
    	public Map<String,Map> getUsersMap(){
    		return users;
    	}
    	public Map<String,User> getLoggedUsers(){
    		return logged_users;
    	}
    }

}