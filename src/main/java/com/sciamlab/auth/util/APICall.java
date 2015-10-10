package com.sciamlab.auth.util;

import java.util.Date;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.json.JSONObject;
import org.json.JSONString;

import com.sciamlab.common.util.SciamlabDateUtils;

public class APICall implements JSONString{
	
	private String shared_key;
	private Date   timestamp;
	private int    return_code;
	private String return_msg;
	private String method;
	private String media_type;
	private String application;
	private String version;
	private String resource;
	private MultivaluedMap<String, String> parameters = new MultivaluedHashMap<String, String>();
	private MultivaluedMap<String, String> header = new MultivaluedHashMap<String, String>();
	private String payload;
	
	

	public APICall(String shared_key, Date timestamp, int return_code,
			String return_msg, String method, String media_type, String application, String version,
			String resource, MultivaluedMap<String, String> parameters, MultivaluedMap<String, String> header, String payload) {
		super();
		this.shared_key = shared_key;
		this.timestamp = timestamp;
		this.return_code = return_code;
		this.return_msg = return_msg;
		this.method = method;
		this.media_type = media_type;
		this.application = application;
		this.version = version;
		this.resource = resource;
		this.parameters = parameters;
		this.header = header;
		this.payload = payload;
	}

	
	public String getShared_key() {
		return shared_key;
	}


	public void setShared_key(String shared_key) {
		this.shared_key = shared_key;
	}


	public Date getTimestamp() {
		return timestamp;
	}


	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}


	public String getMethod() {
		return method;
	}


	public void setMethod(String method) {
		this.method = method;
	}


	public int getReturn_code() {
		return return_code;
	}


	public void setReturn_code(int return_code) {
		this.return_code = return_code;
	}


	public String getReturn_msg() {
		return return_msg;
	}


	public String getMedia_type() {
		return media_type;
	}


	public void setMedia_type(String media_type) {
		this.media_type = media_type;
	}


	public void setReturn_msg(String return_msg) {
		this.return_msg = return_msg;
	}


	public String getApplication() {
		return application;
	}


	public void setApplication(String application) {
		this.application = application;
	}


	public String getVersion() {
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}


	public String getResource() {
		return resource;
	}


	public void setResource(String resource) {
		this.resource = resource;
	}


	public MultivaluedMap<String, String> getParameters() {
		return parameters;
	}


	public void setParameters(MultivaluedMap<String, String> parameters) {
		this.parameters = parameters;
	}


	public MultivaluedMap<String, String> getHeader() {
		return header;
	}


	public void setHeader(MultivaluedMap<String, String> header) {
		this.header = header;
	}


	public String getPayload() {
		return payload;
	}


	public void setPayload(String payload) {
		this.payload = payload;
	}


	public String toJSONString() {
		JSONObject json = new JSONObject();
		json.put("shared_key", shared_key);
		json.put("timestamp", SciamlabDateUtils.getDateAsIso8061String(timestamp));
		json.put("return_code", return_code);
		json.put("return_msg", return_msg);
		json.put("method", method);
		json.put("media_type", media_type);
		json.put("application", application);
		json.put("version", version);
		json.put("resource", resource);
		json.put("parameters", parameters);
		json.put("header",  header);
		json.put("payload", payload);
		return json.toString();
	}

}
