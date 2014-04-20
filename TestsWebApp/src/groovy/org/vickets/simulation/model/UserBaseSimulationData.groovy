package org.vickets.simulation.model

import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ResponseVS

import java.util.concurrent.atomic.AtomicLong
/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class UserBaseSimulationData extends SimulationData {

    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(UserBaseSimulationData.class);

    private int statusCode = ResponseVS.SC_ERROR;
    private String message = null;

    private Integer numUsers = 0;

    private AtomicLong numUserRequestsOK = new AtomicLong(0);
    private AtomicLong numUserRequestsERROR = new AtomicLong(0);

    private final AtomicLong representativeRequests = new AtomicLong(0);

    private AtomicLong userIndex = new AtomicLong(0);

    private List<String> representativeNifList = new ArrayList<String>();

    public Long getNumUserRequests() {
        return representativeRequests.get();
    }

    public Long getAndIncrementnumUserRequests() {
        return representativeRequests.getAndIncrement();
    }

    public Long getNumUserRequestsCollected() {
        return (numUserRequestsOK.get() + numUserRequestsERROR.get());
    }

    public Long getnumUserRequestsOK() {
        return numUserRequestsOK.get();
    }

    public boolean hasRepresesentativeRequestsPending() {
        return (numUsers > representativeRequests.get());
    }

    public boolean waitingForRepresesentativeRequests() {
        return (getMaxPendingResponses() < (representativeRequests.get() - getNumUserRequestsCollected()));
    }

    public Long getAndIncrementnumUserRequestsOK() {
        return numUserRequestsOK.getAndIncrement();
    }

    public Long getNumUserRequestsERROR() {
        return numUserRequestsERROR.get();
    }

    public Long getAndIncrementnumUserRequestsERROR() {
        return numUserRequestsERROR.getAndIncrement();
    }

    public UserBaseSimulationData(int status, String message) {
        this.statusCode = status;
        this.message = message;
    }

    public UserBaseSimulationData() {}

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getNumUsers() {
        return numUsers;
    }

    public void setnumUsers(Integer numUsers) {
        this.numUsers = numUsers;
    }

    public long getUserIndex() {
        return userIndex.get();
    }

    public long getAndIncrementUserIndex() {
        return userIndex.getAndIncrement();
    }

    public void setUserIndex(long userIndex) {
        this.userIndex = new AtomicLong(userIndex);
    }

    public List<String> getRepresentativeNifList() {
        return representativeNifList;
    }

    public void setRepresentativeNifList(List<String> representativeNifList) {
        this.representativeNifList = representativeNifList;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public static UserBaseSimulationData parse (String dataStr) {
        logger.debug("- parse");
        if(dataStr == null) return null;
        JSONObject dataJSON = new JSONObject(dataStr);
        return parse(dataJSON);
    }

    public static UserBaseSimulationData parseJSON (JSONObject dataJSON) {
        logger.debug("- parse - json "  + dataJSON.toString());
        if(dataJSON == null) return null;
        UserBaseSimulationData userBaseData = new UserBaseSimulationData();
        if (dataJSON.containsKey("userIndex")) {
            userBaseData.setUserIndex(dataJSON.getInt("userIndex"));
        }
        if (dataJSON.containsKey("numUsers")) {
            userBaseData.setnumUsers(dataJSON.getInt("numUsers"));
        }
        return userBaseData;
    }

}
