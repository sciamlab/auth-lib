package com.sciamlab.auth.model;

import org.json.JSONObject;

public class UserSocial extends User {
	
	private JSONObject socialDetails;
    private String socialId;
    private String socialUser;
    private String socialDisplay;
    private String socialType;

    public UserSocial() {
        super();
    }
    
    public UserSocial(String id, String api_key) {
        super(id, api_key);
    }
    
    public String getUserName(){
    	return this.socialUser;
    }
    
    public String getDisplayName(){
    	return (this.socialDisplay!=null) ? this.socialDisplay : this.socialUser;
    }
    
	public String getSocialUser() {
		return socialUser;
	}

	public void setSocialUser(String socialUser) {
		this.socialUser = socialUser;
	}

	public String getSocialDisplay() {
		return socialDisplay;
	}

	public String getType() {
		return socialType;
	}

	public void setSocialType(String userType) {
		this.socialType = userType;
	}

	public void setSocialDisplay(String socialDisplay) {
		this.socialDisplay = socialDisplay;
	}

	public JSONObject getSocialDetails() {
		return socialDetails;
	}

	public Object getSocialDetail(String key) {
		return socialDetails.opt(key);
	}

	public void setSocialDetails(JSONObject social_details) {
		this.socialDetails = social_details;
	}

	public String getSocialId() {
		return socialId;
	}

	public void setSocialId(String social_id) {
		this.socialId = social_id;
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
            if(((UserSocial)otherUser).getSocialId().equals(this.getSocialId())) {
                response = true;
            }
        }

        return response;
    }

    public int hashCode() {
        return this.getSocialId().hashCode();
    }

	@Override
	public String toString() {
		return super.toString() + " --> UserSocial [social_id=" + socialId + ", social_user=" + socialUser 
				+ ", social_display=" + socialDisplay + ", social_type=" + socialType + ", social_details=" + socialDetails + "]";
	}

	@Override
	public String toJSONString() {
		return toJSON().toString();
	}
	
	public JSONObject toJSON() {
		JSONObject result = super.toJSON();
        result.put("social_id", socialId);
        result.put("social_user", socialUser);
        result.put("social_display", socialDisplay);
        result.put("social_type", socialType);
        result.put("social_details", socialDetails);
		return result;
	}

}
