package com.sciamlab.auth.model;

import org.json.JSONObject;

public class UserLocal extends User {
	
    private String firstname;
    private String lastname;
    private String email;
    private String password;
    
    
    public UserLocal() {
        super();
    }
    
    public UserLocal(String id, String api_key) {
        super(id, api_key);
    }
    
    public String getType(){
    	return LOCAL;
    }
    
    public String getUserName(){
    	return this.email;
    }
    
    public String getDisplayName(){
    	String display = (this.firstname!=null) ? this.firstname : "";
    	display += (this.lastname!=null) ? " "+this.lastname : "";
    	if("".equals(display.trim()))
    		display = email;
    	return display;
    }
    
    public String getPassword() {
		return password;
	}
    
	public void setPassword(String password) {
		this.password = password;
	}
	
	public boolean checkPassword(String password) {
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
    
	public boolean equals(Object otherUser) {
        boolean response = false;

        if(otherUser == null) {
            response = false;
        }
        else if(! (otherUser instanceof User)) {
            response = false;
        }
        else {
            if(((UserLocal)otherUser).getEmail().equals(this.getEmail())) {
                response = true;
            }
        }

        return response;
    }

    public int hashCode() {
        return this.getEmail().hashCode();
    }

	@Override
	public String toString() {
		return super.toString() + " --> UserLocal [first_name=" + firstname + ", last_name=" + lastname
				+ ", email=" + email + ", password="	+ password + "]";
	}

	@Override
	public String toJSONString() {
		return toJSON().toString();
	}
	
	public JSONObject toJSON() {
		JSONObject result = super.toJSON();
        result.put("first_name", firstname);
        result.put("last_name", lastname);
        result.put("email", email);
		return result;
	}
	
}
