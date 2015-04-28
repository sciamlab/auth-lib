package com.sciamlab.auth.model;

import org.json.JSONObject;

public class UserSocial extends User {
	
	private JSONObject socialDetails;
    private String socialId;

    public UserSocial() {
        super();
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

	public void setSocial_id(String social_id) {
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
		return super.toString() + " --> UserSocial [socialId=" + socialId + ", socialDetails=" + socialDetails + "]";
	}

	@Override
	public String toJSONString() {
		return toJSON().toString();
	}
	
	public JSONObject toJSON() {
		JSONObject result = super.toJSON();
        result.put("socialId", socialId);
        result.put("socialDetails", socialDetails);
		return result;
	}

}
