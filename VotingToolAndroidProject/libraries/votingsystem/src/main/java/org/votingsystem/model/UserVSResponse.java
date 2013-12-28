package org.votingsystem.model;

import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class UserVSResponse {
	
	public static final String TAG = "UserVSResponse";

    private Long numRepresentatives;
    private Long numTotalRepresentatives;
    
    private Long offset;
    private List<UserVS> users;

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Long getOffset() {
        return offset;
    }

    public List<UserVS> getUsers() {
        return users;
    }

    public void setUsers(List<UserVS> users) {
        this.users = users;
    }

    public Long getNumRepresentatives() {
        return numRepresentatives;
    }

    public void setNumRepresentatives(Long numRepresentatives) {
        this.numRepresentatives = numRepresentatives;
    }

    public Long getNumTotalRepresentatives() {
        return numTotalRepresentatives;
    }

    public void setNumTotalRepresentatives(Long numTotalRepresentatives) {
        this.numTotalRepresentatives = numTotalRepresentatives;
    }

	public static UserVSResponse parse(String requestStr) throws ParseException, JSONException {
    	Log.d(TAG + ".parse(...)", "parse(...)");
        UserVSResponse eventVSResponse = new UserVSResponse();
    	JSONObject requestJSON = new JSONObject(requestStr);
        List<UserVS> users = new ArrayList<UserVS>();
        JSONArray representativesArray = requestJSON.getJSONArray("representatives");
        if (representativesArray != null) {
            for (int i=0; i < representativesArray.length(); i++) {
                UserVS representative = UserVS.parse(representativesArray.getJSONObject(i));
                users.add(representative);
            }
        }
        eventVSResponse.setUsers(users);
        if(requestJSON.has("numRepresentatives"))
        	eventVSResponse.setNumRepresentatives(requestJSON.getLong("numRepresentatives"));
        if(requestJSON.has("numTotalRepresentatives"))
        	eventVSResponse.setNumTotalRepresentatives(requestJSON.getLong("numTotalRepresentatives"));
        if(requestJSON.has("offset"))
        	eventVSResponse.setOffset(requestJSON.getLong("offset"));
        return eventVSResponse;
    }

}
