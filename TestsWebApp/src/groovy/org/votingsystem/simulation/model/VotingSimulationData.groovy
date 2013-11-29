package org.votingsystem.simulation.model

import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.grails.web.json.JSONArray
import org.votingsystem.model.FieldEventVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.DateUtils

import java.util.concurrent.atomic.AtomicLong

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
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
        setUserBaseData(simulData.getUserBaseSimulationData());
        setMaxPendingResponses(simulData.getMaxPendingResponses());
        setMessage(simulData.getMessage());
        setBackupRequestEmail(simulData.getBackupRequestEmail());
        setNumHoursProjected(simulData.getNumHoursProjected());
        setNumMinutesProjected(simulData.getNumMinutesProjected());
        setNumRequestsProjected(simulData.getNumRequestsProjected());
        setNumSecondsProjected(simulData.getNumSecondsProjected());
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

    public Long getNumVotingRequestsColected() {
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

    public Long getNumAccessRequestsColected() {
        return (accessRequestERROR.get() + accessRequestOK.get());
    }

    public boolean waitingForVoteRequests() {
        return (maxPendingResponses < (getNumVotingRequests() - getNumVotingRequestsColected()));
    }

    public boolean hasPendingVotes() {
        return (getNumOfElectors() > getNumVotingRequestsColected());
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
        setStatusCode(Respuesta.SC_ERROR);
        this.error = error;
    }

    public static VotingSimulationData parse (String dataStr) throws Exception {
        log.debug("- parse");
        if(dataStr == null) return null;
        JSONObject dataJSON = new JSONObject(dataStr);
        return parse(dataJSON);
    }

    @Override public static VotingSimulationData parse (JSONObject dataJSON) throws Exception {
        log.debug("- parse - json ");
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