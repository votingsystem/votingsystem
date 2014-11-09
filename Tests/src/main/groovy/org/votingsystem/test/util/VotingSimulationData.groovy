package org.votingsystem.test.util

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.votingsystem.model.FieldEventVS
import org.votingsystem.model.ResponseVS

import java.util.concurrent.atomic.AtomicLong

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class VotingSimulationData extends SimulationData {

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(SimulationData.class);

    public enum Error {ACCESS_REQUES, VOTING};

    private Error error;
    private String controlCenterURL;
    private AtomicLong numOfElectors;
    private UserBaseSimulationData userBaseData;

    private AtomicLong accessRequestERROR ;
    private AtomicLong accessRequestOK;

    private AtomicLong votingRequestERROR;
    private AtomicLong votingRequestOK;

    public VotingSimulationData() {}

    public VotingSimulationData(SimulationData simulData) throws Exception {
        setAccessControlURL(simulData.getAccessControlURL());
        setEventVS(simulData.getEventVS());
        init(simulData.getBegin());
        setEventStateWhenFinished(simulData.getEventStateWhenFinished());
        setUserBaseData(simulData.getUserBaseSimulationData());
        setMaxPendingResponses(simulData.getMaxPendingResponses());
        setMessage(simulData.getMessage());
        setBackupRequestEmail(simulData.getBackupRequestEmail());
        setDurationInMillis(simulData.getDurationInMillis())
        setNumRequestsProjected(simulData.getNumRequestsProjected());
    }

    @Override public void init(Long begin) {
        numOfElectors = new AtomicLong(0);
        accessRequestERROR = new AtomicLong(0);
        accessRequestOK = new AtomicLong(0);
        votingRequestERROR = new AtomicLong(0);
        votingRequestOK = new AtomicLong(0);
        super.init(begin);
    }

    @Override public Map getDataMap() {
        Map resultMap = super.getDataMap();
        resultMap.put("numAccessRequestsERROR", getNumAccessRequestsERROR());
        resultMap.put("numVotingRequestsERROR", getNumVotingRequestsERROR());
        resultMap.put("numAccessRequestsOK", getNumAccessRequestsOK())
        return resultMap;
    }

    public Long getNumVotingRequests() {
        return votingRequestERROR.get() + votingRequestOK.get();
    }

    public Long getNumVotingRequestsERROR() {
        return votingRequestERROR.get();
    }

    public Long getAndIncrementNumVotingRequestsERROR() {
        return votingRequestERROR.getAndIncrement();
    }

    public Long getNumVotingRequestsOK() {
        return votingRequestOK.get();
    }

    public Long getAndIncrementNumVotingRequestsOK() {
        return votingRequestOK.getAndIncrement();
    }

    public Long getNumVotingRequestsCollected() {
        return (votingRequestERROR.get() + votingRequestOK.get()
                + accessRequestERROR.get());
    }

    public Long getNumAccessRequests() {
        return accessRequestERROR.get() + accessRequestOK.get();
    }

    public Long getNumAccessRequestsERROR() {
        return accessRequestERROR.get();
    }

    public Long getAndIncrementNumAccessRequestsERROR() {
        return accessRequestERROR.getAndIncrement();
    }

    public Long getNumAccessRequestsOK() {
        return accessRequestOK.get();
    }

    public Long getAndIncrementNumAccessRequestsOK() {
        return accessRequestOK.getAndIncrement();
    }

    public Long getNumAccessRequestsCollected() {
        return (accessRequestERROR.get() + accessRequestOK.get());
    }

    public boolean waitingForVoteRequests() {
        log.debug("waitingForVoteRequests - collected: " + getNumVotingRequestsCollected() + " of " + getNumVotingRequests())
        return (maxPendingResponses < (getNumVotingRequests() - getNumVotingRequestsCollected()));
    }

    public boolean hasPendingVotes() {
        log.debug("hasPendingVotes - NumOfElectors: " + getNumOfElectors() + " - NumVotingRequestsCollected: " +
                getNumVotingRequestsCollected())
        return (getNumOfElectors() > getNumVotingRequestsCollected());
    }

    public String getControlCenterURL() {
        return controlCenterURL;
    }

    /**
     * @param controlCenterURL the controlCenterURL to set
     */
    public void setControlCenterURL(String controlCenterURL) {
        this.controlCenterURL = controlCenterURL;
    }

    /**
     * @return the numOfElectors
     */
    public Long getNumOfElectors() {
        return numOfElectors.get();
    }

    /**
     * @param numOfElectors the numOfElectors to set
     */
    public void setNumOfElectors(Long numOfElectors) {
        this.numOfElectors.set(numOfElectors)
    }

    /**
     * @return the userBaseData
     */
    public UserBaseSimulationData getUserBaseData() {
        return userBaseData;
    }

    /**
     * @param userBaseData the userBaseData to set
     */
    public void setUserBaseData(UserBaseSimulationData userBaseData) {
        this.userBaseData = userBaseData;
    }

    /**
     * @return the error
     */
    public Error getError() {
        return error;
    }

    /**
     * @param error the error to set
     */
    public void setError(Error error) {
        setStatusCode(ResponseVS.SC_ERROR);
        this.error = error;
    }

    @Override public static VotingSimulationData parse (JSONObject dataJSON) throws Exception {
        VotingSimulationData simulationData = new VotingSimulationData(SimulationData.parse(dataJSON));
        if (dataJSON.containsKey("controlCenterURL")) {
            simulationData.setControlCenterURL(dataJSON.getString("controlCenterURL"));
        }
        if (dataJSON.containsKey("options")) {
            Set<FieldEventVS> eventOptions = new HashSet<FieldEventVS>();
            JSONArray jsonArray = dataJSON.getJSONArray("options");
            for (int i = 0; i< jsonArray.size(); i++) {
                FieldEventVS optionVS = new FieldEventVS();
                optionVS.setContent(jsonArray.getString(i));
                eventOptions.add(optionVS);
            }
            simulationData.getEventVS().setFieldsEventVS(eventOptions)
        }
        return simulationData;
    }

}
