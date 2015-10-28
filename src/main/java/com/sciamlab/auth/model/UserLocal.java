package com.sciamlab.auth.model;

import org.json.JSONObject;

public class UserLocal extends User {
	
    private String firstname;
    private String lastname;
    private String username;
    private String email;
    private String password;
    
    public UserLocal(String username) {
        super();
        this.username = username;
    }
    
    public String getType(){
    	return LOCAL;
    }
    
    public String getUserName(){
    	return this.username;
    }
    
    public String getDisplayName(){
    	String display = (this.firstname!=null) ? this.firstname : "";
    	display += (this.lastname!=null) ? " "+this.lastname : "";
    	if("".equals(display.trim()))
    		display = username;
    	return display;
    }
    
    public String getEncodedPassword() {
		return password;
	}
    
	public void setPassword(String password) {
		//TODO encode
		this.password = password;
	}
	
	public boolean checkPassword(String password) {
		//TODO encode
		return this.password.equals(password);
	}

    public String getFirstName() {
        return firstname;
    }

	public void setFirstName(String firstName) {
        this.firstname = firstName;
    }

    public String getLastName() {
        return lastname;
    }

    public void setLastName(String lastName) {
        this.lastname = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public int hashCode() {
        return this.getEmail().hashCode();
    }

	@Override
	public String toString() {
		return super.toString() + " --> UserLocal [first_name=" + firstname + ", last_name=" + lastname
				+ ", email=" + email + ", password="	+ password + "]";
	}

	public JSONObject toJSON() {
		JSONObject result = super.toJSON();
        result.put("first_name", firstname);
        result.put("last_name", lastname);
        result.put("email", email);
		return result;
	}
	
}
