package org.vickets.simulation.model

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.DateUtils
import org.votingsystem.util.StringUtils

import java.util.concurrent.atomic.AtomicLong

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class SimulationData {

    private static Logger logger = Logger.getLogger(SimulationData.class);

    private Integer statusCode = ResponseVS.SC_PAUSED;
    private String message = null;
    private String serverURL = null;

    private Integer maxPendingResponses = 10; //default
    private Integer numRequestsProjected = null;

    private AtomicLong numRequests = new AtomicLong(0);
    private AtomicLong numRequestsOK = new AtomicLong(0);
    private AtomicLong numRequestsERROR = new AtomicLong(0);

    private Long begin = null;
    private Long finish = null;

    private boolean timerBased = false;
    private Integer numHoursProjected;
    private Integer numMinutesProjected;
    private Integer numSecondsProjected;
    private Long eventId;

    private String durationStr = null;
    private String subject = null;
    private String backupRequestEmail = null;
    private Long receptorId = null;
    private Long groupId = null;
    private BigDecimal transactionvsAmount;
    private String currencyCode;

    private UserBaseSimulationData userBaseSimulationData;

    private List<String> errorList = new ArrayList<String>();

    public SimulationData(int status, String message) {
        this.statusCode.set(status)
        this.message = message;
    }

    public SimulationData() {}

    public void setReceptorId(Long receptorId) {
        this.receptorId = receptorId;
    }

    public Long getReceptorId() {
        return receptorId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getGroupId() {
        return groupId;
    }


    public String getCurrency() {
        return currencyCode;
    }

    public void setCurrency(String currency) {
        this.currencyCode = currency;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public boolean isRunning() {
        return (ResponseVS.SC_PROCESSING == getStatusCode());
    }

    public static SimulationData parse (String dataStr) throws Exception {
        logger.debug("- parse");
        if(dataStr == null) return null;
        JSONObject dataJSON = new JSONObject(dataStr);
        return parse(dataJSON);
    }

    public Map getDataMap() {
        Map resultMap = new HashMap();
        String timeDurationStr = null;
        if(durationStr == null && begin != null) {
            long timeDuration = System.currentTimeMillis() - begin;
            timeDurationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(timeDuration);
        } else timeDurationStr = durationStr;
        resultMap.statusCode = getStatusCode();
        resultMap.errorList = errorList;
        resultMap.numRequestsProjected = numRequestsProjected?.longValue()
        resultMap.numRequests = numRequests?.longValue();
        resultMap.numRequestsOK = numRequestsOK?.longValue();
        resultMap.numRequestsERROR = numRequestsERROR?.longValue();
        resultMap.timeDuration = timeDurationStr;
        return resultMap;
    }

    public static SimulationData parse (JSONObject dataJSON) throws Exception {
        logger.debug(" ------ parse - json ");
        SimulationData simulationData = new SimulationData();
        if (dataJSON.containsKey("serverURL")) {
            simulationData.setServerURL(dataJSON.getString("serverURL"));
        }
        if(dataJSON.containsKey("userBaseData")) {
            simulationData.setUserBaseSimulationData(
                    UserBaseSimulationData.parseJSON(dataJSON.getJSONObject("userBaseData")));
        }
        if (dataJSON.containsKey("numRequestsProjected")) {
            simulationData.setNumRequestsProjected(dataJSON.getInt("numRequestsProjected"));
        }
        if (dataJSON.containsKey("maxPendingResponses")) {
            simulationData.setMaxPendingResponses(dataJSON.getInt("maxPendingResponses"));
        }
        if (!dataJSON.isNull("eventId")) {
            simulationData.setEventId(dataJSON.getLong("eventId"));
        }
        if (dataJSON.containsKey("transactionvsAmount")) {
            simulationData.setTransactionVSAmount(new BigDecimal(dataJSON.getLong("transactionvsAmount")));
        }
        if (dataJSON.containsKey("currency")) {
            simulationData.setCurrency(dataJSON.getString("currency").toUpperCase());
        }
        if (dataJSON.containsKey("subject")) {
            simulationData.setSubject(dataJSON.getString("subject"));
        }
        if (dataJSON.containsKey("receptorId")) {
            simulationData.setReceptorId(dataJSON.getLong("receptorId"));
        }
        if (dataJSON.containsKey("groupId")) {
            simulationData.setGroupId(dataJSON.getLong("groupId"));
        }

        if (!dataJSON.isNull("backupRequestEmail")) {
            String email = dataJSON.getString("backupRequestEmail");
            if(email != null && !email.trim().isEmpty()) simulationData.setBackupRequestEmail(email);
        }
        if(dataJSON.containsKey("timer")) {
            JSONObject timerJSONObject = dataJSON.getJSONObject("timer");
            if(timerJSONObject.containsKey("active")) {
                boolean timerBased = timerJSONObject.getBoolean("active");
                simulationData.setTimerBased(timerBased);
                if(timerBased) {
                    if(timerJSONObject.containsKey("time")) {
                        String timeStr = timerJSONObject.getString("time");
                        simulationData.setNumHoursProjected(Integer.valueOf(timeStr.split(":")[0]));
                        simulationData.setNumMinutesProjected(Integer.valueOf(timeStr.split(":")[1]));
                    } else {
                        if(timerJSONObject.containsKey("numHoursProjected")) {
                            simulationData.setNumHoursProjected(timerJSONObject.getInt("numHoursProjected"));
                        }
                        if(timerJSONObject.containsKey("numMinutesProjected")) {
                            simulationData.setNumMinutesProjected(timerJSONObject.getInt("numMinutesProjected"));
                        }
                        if(timerJSONObject.containsKey("numSecondsProjected")) {
                            simulationData.setNumSecondsProjected(timerJSONObject.getInt("numSecondsProjected"));
                        }
                    }
                }
            }
        }
        return simulationData;
    }

    public Long getNumRequestsColected() {
        return numRequestsOK.get() + numRequestsERROR.get();
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

    public void init(Long begin) {
        this.begin = begin;
    }

    public Long getFinish() {
        return finish;
    }

    public void finish(int statusCode, Long finish) throws Exception {
        setStatusCode(statusCode)
        if(begin != null && finish != null) {
            long duration = finish - begin;
            durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
            this.finish = finish;
        }
    }

    public String getDurationStr() {
        return durationStr;
    }

    public String getServerURL() {
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
        if(userBaseSimulationData != null) userBaseSimulationData.setMaxPendingResponses(maxPendingResponses)
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

    public Integer getNumHoursProjected() {
        return numHoursProjected;
    }

    public void setNumHoursProjected(Integer numHoursProjected) {
        this.numHoursProjected = numHoursProjected;
    }

    public Integer getNumMinutesProjected() {
        return numMinutesProjected;
    }

    public void setNumMinutesProjected(Integer numMinutesProjected) {
        this.numMinutesProjected = numMinutesProjected;
    }

    public Integer getNumSecondsProjected() {
        return numSecondsProjected;
    }

    public void setNumSecondsProjected(Integer numSecondsProjected) {
        this.numSecondsProjected = numSecondsProjected;
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


    public BigDecimal getTransactionVSAmount() {
        return transactionvsAmount;
    }

    public void setTransactionVSAmount(BigDecimal transactionvsAmount) {
        this.transactionvsAmount = transactionvsAmount;
    }
}
