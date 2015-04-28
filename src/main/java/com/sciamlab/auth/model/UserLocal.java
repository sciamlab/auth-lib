package com.sciamlab.auth.model;

import org.json.JSONObject;

public class UserLocal extends User {
	
    private String firstname;
    private String lastname;
    private String email;
    private String hashedPassword;

    public UserLocal() {
        super();
    }
    
    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getHashedPassword() {
        return this.hashedPassword;
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
				+ ", email=" + email + ", password="	+ hashedPassword + "]";
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
