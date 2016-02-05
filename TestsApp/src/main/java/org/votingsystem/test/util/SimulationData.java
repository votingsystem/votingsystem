package org.votingsystem.test.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.voting.EventVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SimulationData {

    private static Logger log = Logger.getLogger(SimulationData.class.getName());

    private Integer statusCode = ResponseVS.SC_PAUSED;
    private String message = null;
    private String accessControlURL = null;
    private String serverURL = null;

    private Integer maxPendingResponses = 10; //default
    private Integer numRequestsProjected = null;

    private AtomicLong numRequests = new AtomicLong(0);
    private AtomicLong numRequestsOK = new AtomicLong(0);
    private AtomicLong numRequestsERROR = new AtomicLong(0);

    private EventVS eventVS = null;

    private Long begin = null;
    private Long finish = null;

    private boolean timerBased = false;
    private Long durationInMillis;
    private Long eventId;
    private Long groupId;

    private String durationStr = null;
    private String backupRequestEmail = null;

    private UserBaseSimulationData userBaseSimulationData;
    private EventVS.State eventStateWhenFinished;
    private Map timerMap;
    private List<String> errorList = new ArrayList<String>();

    public SimulationData(int status, String message) {
        this.statusCode = status;
        this.message = message;
    }

    public SimulationData() {
        this.begin = System.currentTimeMillis();
    }

    public Long getDurationInMillis() {
        return durationInMillis;
    }

    public Long setDurationInMillis(Long durationInMillis) {
        return this.durationInMillis = durationInMillis;
    }

    public EventVS.State getEventStateWhenFinished() {
        return eventStateWhenFinished;
    }

    public void setEventStateWhenFinished(EventVS.State nextState) {
        eventStateWhenFinished = nextState;
    }

    public boolean isRunning() {
        return (ResponseVS.SC_PROCESSING == getStatusCode());
    }

    public Long getNumRequestsCollected() {
        return numRequestsOK.get() + numRequestsERROR.get();
    }

    public boolean hasPendingRequest() {
        return (getNumRequestsProjected() > getNumRequestsCollected());
    }

    public Long getNumRequests() {
        return numRequests.get();
    }

    public Long getAndIncrementNumRequests() {
        return numRequests.getAndIncrement();
    }

    public Long getAndAddNumRequestsOK(long delta) {
        return numRequestsOK.getAndAdd(delta);
    }

    public Long getNumRequestsOK() {
        return numRequestsOK.get();
    }

    public Long getAndIncrementNumRequestsOK() {
        return numRequestsOK.getAndIncrement();
    }

    public Long getNumRequestsERROR() {
        return numRequestsERROR.get();
    }

    public Long getAndIncrementNumRequestsERROR() {
        return numRequestsERROR.getAndIncrement();
    }


    public void setUserBaseSimulationData(UserBaseSimulationData userBaseSimulationData) {
        this.userBaseSimulationData = userBaseSimulationData;
    }

    public UserBaseSimulationData getUserBaseSimulationData() {
        return userBaseSimulationData;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public synchronized int getStatusCode() {
        return statusCode;
    }

    public synchronized void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Long getBegin() {
        return begin;
    }

    public Date getBeginDate() {
        if(begin == null) return null;
        else return new Date(begin);
    }

    public void init() {
        this.begin = System.currentTimeMillis();
    }

    public Long getFinish() {
        return finish;
    }

    public void finish(int statusCode, Long finish) throws Exception {
        setStatusCode(statusCode);
        if(begin != null && finish != null) {
            long duration = finish - begin;
            durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
            this.finish = finish;
        }
    }

    public void finishAndExit(int statusCode, String resultMessage) throws Exception {
        finish(ResponseVS.SC_OK, System.currentTimeMillis());
        log.info("----- finish - " + toString());
        if(resultMessage != null) log.info(resultMessage);
        System.exit(0);
    }

    public String getDurationStr() {
        return durationStr;
    }

    public String getAccessControlURL() {
        return accessControlURL;
    }

    public void setAccessControlURL(String accessControlURL) {
        this.accessControlURL = accessControlURL;
    }

    public String getServerURL() {
        if(serverURL == null) return accessControlURL;
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = StringUtils.checkURL(serverURL);
    }

    public Integer getMaxPendingResponses() {
        return maxPendingResponses;
    }

    public void setMaxPendingResponses(Integer maxPendingResponses) {
        this.maxPendingResponses = maxPendingResponses;
        if(userBaseSimulationData != null) userBaseSimulationData.setMaxPendingResponses(maxPendingResponses);
    }

    public Integer getNumRequestsProjected() {
        return numRequestsProjected;
    }

    public void setNumRequestsProjected(Integer numRequestsProjected) {
        this.numRequestsProjected = numRequestsProjected;
    }

    public boolean isTimerBased() {
        return timerBased;
    }

    public void setTimerBased(boolean timerBased) {
        this.timerBased = timerBased;
    }

    public EventVS getEventVS() {
        return eventVS;
    }

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public String getBackupRequestEmail() {
        return backupRequestEmail;
    }

    public void setBackupRequestEmail(String backupRequestEmail) {
        this.backupRequestEmail = backupRequestEmail;
    }

    public List<String> getErrorList() {
        return errorList;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Map getTimerMap() {
        return timerMap;
    }

    public void setTimerMap(Map timerMap) {
        this.timerMap = timerMap;
        if(timerMap.containsKey("active")) {
            timerBased = (boolean) timerMap.get("active");
            if(timerBased) {
                if(timerMap.containsKey("time")) {
                    String timeStr = (String) timerMap.get("time");
                    Long hoursMillis = 1000 * 60 * 60 * new Long(Integer.valueOf(timeStr.split(":")[0]));
                    Long minutesMillis = 1000 * 60 * new Long(Integer.valueOf(timeStr.split(":")[1]));
                    Long secondMillis = 1000 * new Long(Integer.valueOf(timeStr.split(":")[2]));
                    setDurationInMillis(hoursMillis + minutesMillis + secondMillis);
                }
            }
        }
    }

    @Override
    public String toString() {
        return MessageFormat.format("statusCode: {0} - message: {1} - serverURL: {2} - numRequestsProjected: {3} - " +
                "numRequests: {4} - numRequestsOK: {5} - numRequestsERROR: {6} - begin: {7} - duration: {8} - errors: {9}",
                statusCode, message, serverURL, numRequestsProjected, numRequests, numRequestsOK, numRequestsERROR,
                DateUtils.getDateStr(getBeginDate()), durationStr, errorList);
    }
}
