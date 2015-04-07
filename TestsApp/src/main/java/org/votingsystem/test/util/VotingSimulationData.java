package org.votingsystem.test.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.FieldEventVS;
import org.votingsystem.model.ResponseVS;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VotingSimulationData extends SimulationData {

    private static Logger log = Logger.getLogger(SimulationData.class.getSimpleName());

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
        setDurationInMillis(simulData.getDurationInMillis());
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
        resultMap.put("numAccessRequestsOK", getNumAccessRequestsOK());
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
        log.info("waitingForVoteRequests - collected: " + getNumVotingRequestsCollected() + " of " + getNumVotingRequests());
        return (getMaxPendingResponses() < (getNumVotingRequests() - getNumVotingRequestsCollected()));
    }

    public boolean hasPendingVotes() {
        log.info("hasPendingVotes - NumOfElectors: " + getNumOfElectors() + " - NumVotingRequestsCollected: " +
                getNumVotingRequestsCollected());
        return (getNumOfElectors() > getNumVotingRequestsCollected());
    }

    public String getControlCenterURL() {
        return controlCenterURL;
    }

    public void setControlCenterURL(String controlCenterURL) {
        this.controlCenterURL = controlCenterURL;
    }

    public Long getNumOfElectors() {
        return numOfElectors.get();
    }

    public void setNumOfElectors(Long numOfElectors) {
        this.numOfElectors.set(numOfElectors);
    }

    public UserBaseSimulationData getUserBaseData() {
        return userBaseData;
    }

    public void setUserBaseData(UserBaseSimulationData userBaseData) {
        this.userBaseData = userBaseData;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        setStatusCode(ResponseVS.SC_ERROR);
        this.error = error;
    }

}
