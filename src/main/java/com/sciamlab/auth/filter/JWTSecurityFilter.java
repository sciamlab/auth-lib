package com.sciamlab.auth.filter;

import java.security.Key;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;

import org.apache.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.keys.AesKey;
import org.jose4j.lang.ByteUtil;
import org.jose4j.lang.JoseException;

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
			User user = dao.getLocalUsers().get(dao.getLoggedUsers().get(key));
			request.setSecurityContext(new SciamlabSecurityContext(user));
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
    	private static Map<String,String> logged_users = new HashMap<String, String>();
    	private static Key key = new AesKey(ByteUtil.randomBytes(16));
    	
    	static{
    		logger.debug("Starting LocalSecurityDao...");
    		logger.debug("KEY : "+key);
    		UserLocal u = new UserLocal();
    		u.setFirstName("Paolo");
    		u.setLastName("Starace");
    		u.setEmail("paolo@sciamlab.com");
    		u.setHashedPassword("1234");
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
    		u.setHashedPassword("1234");
    		u.getRoles().clear();
    		u.getRoles().add(Role.editor);
    		u.getProfiles().clear();
    		logger.debug("User: "+u);
    		users_local.put(u.getEmail(), u);
    		logger.debug("[DONE]");
    	}
    	
    	public static String createToken(String payload) throws JoseException{
    		JsonWebEncryption jwe = new JsonWebEncryption();
    		jwe.setPayload(payload);
    		jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A128KW);
    		jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_128_CBC_HMAC_SHA_256);
    		jwe.setKey(key);
    		return jwe.getCompactSerialization();
    	}
    	
    	public static String decodeToken(String serializedJwe) throws JoseException{
    		JsonWebEncryption jwe = new JsonWebEncryption();
    		jwe.setKey(key);
    		jwe.setCompactSerialization(serializedJwe);
    		return jwe.getPayload();
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
    	
    	public Map<String,String> getLoggedUsers(){
    		return logged_users;
    	}
    }

}