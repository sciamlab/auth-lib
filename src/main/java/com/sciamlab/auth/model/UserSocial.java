package com.sciamlab.auth.model;

import org.json.JSONObject;

public class UserSocial extends User {
	
	private JSONObject socialDetails;
    private String socialId;
    private String socialUser;
    private String socialDisplay;
    private String socialType;

    public UserSocial(String socialId, String socialType) {
        super();
        this.socialId = socialId;
        this.socialType = socialType;
    }
    
    public String getUserName(){
    	return this.socialId;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((socialType == null) ? 0 : socialType.hashCode());
		result = prime * result
				+ ((socialUser == null) ? 0 : socialUser.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserSocial other = (UserSocial) obj;
		if (socialType == null) {
			if (other.socialType != null)
				return false;
		} else if (!socialType.equals(other.socialType))
			return false;
		if (socialUser == null) {
			if (other.socialUser != null)
				return false;
		} else if (!socialUser.equals(other.socialUser))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return super.toString() + " --> UserSocial [social_id=" + socialId + ", social_user=" + socialUser 
				+ ", social_display=" + socialDisplay + ", social_type=" + socialType + ", social_details=" + socialDetails + "]";
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
