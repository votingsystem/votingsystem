package org.votingsystem.simulation.model

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSBase
import org.votingsystem.model.ResponseVS;
import org.votingsystem.simulation.ApplicationContextHolder;
import org.votingsystem.simulation.model.*
import org.votingsystem.util.DateUtils;

import groovy.json.JsonSlurper;

import org.codehaus.groovy.grails.web.json.JSONObject
import org.apache.log4j.Logger;


class SimulationData {
	
	 private static Logger log = Logger.getLogger(SimulationData.class);
	
	 private int statusCode = ResponseVS.SC_ERROR;
	 private String message = null;
	 private String accessControlURL = null;
	 private Integer maxPendingResponses = 10; //default
	 private Integer numRequestsProjected = null;
	
	 private AtomicLong numRequests = new AtomicLong(0);
	 private AtomicLong numRequestsOK = new AtomicLong(0);
	 private AtomicLong numRequestsERROR = new AtomicLong(0);
	 
	 private EventVS evento = null;
	 
	 private Long begin = null;
	 private Long finish = null;
	 
	 private boolean timerBased = false;
	 private Integer numHoursProjected;
	 private Integer numMinutesProjected;
	 private Integer numSecondsProjected;
	 private Long eventId;
	 
	 private String durationStr = null;
	 private String backupRequestEmail = null;
	 
	 private List<String> errorList = new ArrayList<String>();
	
	 public SimulationData(int status, String message) {
		 this.statusCode = status;
		 this.message = message;
	 }
	 
	 public SimulationData() {}
	 
		 
	 public static SimulationData parse (String dataStr) throws Exception {
		 log.debug("- parse");
		 if(dataStr == null) return null;
		 JSONObject dataJSON = new JSONObject(dataStr);
		 return parse(dataJSON);
	 }
	 
	 public Map getDataMap() {
		 Map resultMap = new HashMap();
		 resultMap.statusCode = statusCode;
		 resultMap.errorList = errorList;
		 resultMap.numRequests = numRequests.longValue();
		 resultMap.numRequestsOK = numRequestsOK.longValue();
		 resultMap.numRequestsERROR = numRequestsERROR.longValue();
		 return resultMap;
	 }
	 
	 public static SimulationData parse (JSONObject dataJSON) throws Exception {
		 log.debug(" --- parse - json ");
		 if(dataJSON == null) return null;
		 SimulationData simulationData = new SimulationData();
		 EventVSBase evento = new EventVSBase();
		 if (dataJSON.containsKey("accessControlURL")) {
			 simulationData.setAccessControlURL(dataJSON.getString("accessControlURL"));
		 }
		 if (dataJSON.containsKey("numRequestsProjected")) {
			 simulationData.setNumRequestsProjected(dataJSON.getInt("numRequestsProjected"));
		 }
		 if (dataJSON.containsKey("maxPendingResponses")) {
			 simulationData.setMaxPendingResponses(dataJSON.getInt("maxPendingResponses"));
		 }
		 if (dataJSON.containsKey("event")) {
			 evento = EventVSBase.populate(dataJSON.getJSONObject("event"));
		 }
		 if (!dataJSON.isNull("eventId")) {
			 simulationData.setEventId(dataJSON.getLong("eventId"));
		 }
		 
		 if (!dataJSON.isNull("backupRequestEmail")) {
			 String email = dataJSON.getString("backupRequestEmail");
			 if(!"".equals(email)) simulationData.setBackupRequestEmail(email);
		 }
		 if(dataJSON.containsKey("timer")) {
			 JSONObject timerJSONObject = dataJSON.getJSONObject("timer");
			 if(timerJSONObject.containsKey("active")) {
				 boolean timerBased = timerJSONObject.getBoolean("active");
				 simulationData.setTimerBased(timerBased);
				 if(timerBased) {
					  if(timerJSONObject.containsKey("numHoursProjected")) {
						  simulationData.setNumHoursProjected(
								  timerJSONObject.getInt("numHoursProjected"));
					  }
					 if(timerJSONObject.containsKey("numMinutesProjected")) {
						  simulationData.setNumMinutesProjected(
								  timerJSONObject.getInt("numMinutesProjected"));
					 }
					 if(timerJSONObject.containsKey("numSecondsProjected")) {
						  simulationData.setNumSecondsProjected(
								  timerJSONObject.getInt("numSecondsProjected"));
					 }
				 }
			 }
		 }
		 if (dataJSON.containsKey("whenFinishChangeEventStateTo")) {
			 try {
				 EventVSBase.Estado estado = EventVSBase.Estado.valueOf(
					 dataJSON.getString("whenFinishChangeEventStateTo"));
				 evento.setNextState(estado);
			 }catch(Exception ex) { }
	
		 }
		 simulationData.setEvento(evento);
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
	 
	 
	 /**
	  * @return the message
	  */
	 public String getMessage() {
		 return message;
	 }
	
	 /**
	  * @param message the message to set
	  */
	 public void setMessage(String message) {
		 this.message = message;
	 }
	
	 
	 /**
	  * @return the statusCode
	  */
	 public int getStatusCode() {
		 return statusCode;
	 }
	
	 /**
	  * @param statusCode the statusCode to set
	  */
	 public void setStatusCode(int statusCode) {
		 this.statusCode = statusCode;
	 }
	
	 /**
	  * @return the begin
	  */
	 public Long getBegin() {
		 return begin;
	 }
	
	 public Date getBeginDate() {
		 if(begin == null) return null;
		 else return new Date(begin);
	 }
	 
	 /**
	  * @param begin the begin to set
	  */
	 public void setBegin(Long begin) {
		 this.begin = begin;
	 }
	
	 /**
	  * @return the finish
	  */
	 public Long getFinish() {
		 return finish;
	 }
	
	 /**
	  * @param finish the finish to set
	  */
	 public void setFinish(Long finish) throws Exception {
		 if(begin != null) {
			 long duration = finish - begin;
			 durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
		 }
		 this.finish = finish;
	 }
	
	 public String getDurationStr() {
		 return durationStr;
	 }
	
	 
	
	 /**
	  * @return the accessControlURL
	  */
	 public String getAccessControlURL() {
		 return accessControlURL;
	 }
	
	 /**
	  * @param accessControlURL the accessControlURL to set
	  */
	 public void setAccessControlURL(String accessControlURL) {
		 this.accessControlURL = accessControlURL;
	 }
	
	 /**
	  * @return the maxPendingResponses
	  */
	 public Integer getMaxPendingResponses() {
		 return maxPendingResponses;
	 }
	
	 /**
	  * @param maxPendingResponses the maxPendingResponses to set
	  */
	 public void setMaxPendingResponses(Integer maxPendingResponses) {
		 this.maxPendingResponses = maxPendingResponses;
	 }
	
	 /**
	  * @return the numRequestsProjected
	  */
	 public Integer getNumRequestsProjected() {
		 return numRequestsProjected;
	 }
	
	 /**
	  * @param numRequestsProjected the numRequestsProjected to set
	  */
	 public void setNumRequestsProjected(Integer numRequestsProjected) {
		 this.numRequestsProjected = numRequestsProjected;
	 }
	
	 /**
	  * @return the timerBased
	  */
	 public boolean isTimerBased() {
		 return timerBased;
	 }
	
	 /**
	  * @param timerBased the timerBased to set
	  */
	 public void setTimerBased(boolean timerBased) {
		 this.timerBased = timerBased;
	 }
	
	 /**
	  * @return the numHoursProjected
	  */
	 public Integer getNumHoursProjected() {
		 return numHoursProjected;
	 }
	
	 /**
	  * @param numHoursProjected the numHoursProjected to set
	  */
	 public void setNumHoursProjected(Integer numHoursProjected) {
		 this.numHoursProjected = numHoursProjected;
	 }
	
	 /**
	  * @return the numMinutesProjected
	  */
	 public Integer getNumMinutesProjected() {
		 return numMinutesProjected;
	 }
	
	 /**
	  * @param numMinutesProjected the numMinutesProjected to set
	  */
	 public void setNumMinutesProjected(Integer numMinutesProjected) {
		 this.numMinutesProjected = numMinutesProjected;
	 }
	
	 /**
	  * @return the numSecondsProjected
	  */
	 public Integer getNumSecondsProjected() {
		 return numSecondsProjected;
	 }
	
	 /**
	  * @param numSecondsProjected the numSecondsProjected to set
	  */
	 public void setNumSecondsProjected(Integer numSecondsProjected) {
		 this.numSecondsProjected = numSecondsProjected;
	 }
	
	 /**
	  * @return the evento
	  */
	 public EventVS getEvento() {
		 return evento;
	 }
	
	 /**
	  * @param evento the evento to set
	  */
	 public void setEvento(EventVS evento) {
		 this.evento = evento;
	 }
	
	 /**
	  * @return the backupRequestEmail
	  */
	 public String getBackupRequestEmail() {
		 return backupRequestEmail;
	 }
	
	 /**
	  * @param backupRequestEmail the backupRequestEmail to set
	  */
	 public void setBackupRequestEmail(String backupRequestEmail) {
		 this.backupRequestEmail = backupRequestEmail;
	 }
	
	 /**
	  * @return the errorList
	  */
	 public List<String> getErrorList() {
		 return errorList;
	 }
	
	 /**
	  * @param errorList the errorList to set
	  */
	 public void seterrorList(List<String> errorList) {
		 this.errorList = errorList;
	 }
	
	 /**
	  * @return the eventId
	  */
	 public Long getEventId() {
		 return eventId;
	 }
	
	 /**
	  * @param eventId the eventId to set
	  */
	 public void setEventId(Long eventId) {
		 this.eventId = eventId;
	 }
}
