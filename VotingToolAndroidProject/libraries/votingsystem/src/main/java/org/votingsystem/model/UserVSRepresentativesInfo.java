package org.votingsystem.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserVSRepresentativesInfo {
	
	public static final String TAG = UserVSRepresentativesInfo.class.getSimpleName();

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

	public static UserVSRepresentativesInfo parse(JSONObject userJSON) throws Exception {
    	Log.d(TAG + ".parse(...)", "parse(...)");
        UserVSRepresentativesInfo eventVSResponse = new UserVSRepresentativesInfo();
        List<UserVS> users = new ArrayList<UserVS>();
        JSONArray representativesArray = userJSON.getJSONArray("representatives");
        if (representativesArray != null) {
            for (int i=0; i < representativesArray.length(); i++) {
                UserVS representative = UserVS.parse(representativesArray.getJSONObject(i));
                users.add(representative);
            }
        }
        eventVSResponse.setUsers(users);
        if(userJSON.has("numRepresentatives"))
        	eventVSResponse.setNumRepresentatives(userJSON.getLong("numRepresentatives"));
        if(userJSON.has("numTotalRepresentatives"))
        	eventVSResponse.setNumTotalRepresentatives(userJSON.getLong("numTotalRepresentatives"));
        if(userJSON.has("offset"))
        	eventVSResponse.setOffset(userJSON.getLong("offset"));
        return eventVSResponse;
    }

}
