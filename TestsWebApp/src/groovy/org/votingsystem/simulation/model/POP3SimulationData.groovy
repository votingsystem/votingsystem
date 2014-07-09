package org.votingsystem.simulation.model

import org.codehaus.groovy.grails.web.json.JSONObject
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class POP3SimulationData extends SimulationData {

    private static Logger logger = LoggerFactory.getLogger(POP3SimulationData.class);

    private String smtpHostName = null;
    private String pop3HostName = null;
    private String userName = null;
    private String password = null;
    private String domainName = null;


    public POP3SimulationData() {}

    public POP3SimulationData(SimulationData simulData) throws Exception {
        setMaxPendingResponses(simulData.getMaxPendingResponses());
        setMessage(simulData.getMessage());
        setBackupRequestEmail(simulData.getBackupRequestEmail());
        setNumHoursProjected(simulData.getNumHoursProjected());
        setNumMinutesProjected(simulData.getNumMinutesProjected());
        setNumRequestsProjected(simulData.getNumRequestsProjected());
        setNumSecondsProjected(simulData.getNumSecondsProjected());
    }

    public static POP3SimulationData parse (JSONObject dataJSON) throws Exception {
        if(dataJSON == null) return null;
        POP3SimulationData simulationData = new POP3SimulationData(
                SimulationData.parse(dataJSON));
        if (dataJSON.containsKey("smtpHostName")) {
            simulationData.setSmtpHostName(dataJSON.getString("smtpHostName"));
        }
        if (dataJSON.containsKey("pop3HostName")) {
            simulationData.setPop3HostName(dataJSON.getString("pop3HostName"));
        }
        if (dataJSON.containsKey("userName")) {
            simulationData.setUserName(dataJSON.getString("userName"));
        }
        if (dataJSON.containsKey("password")) {
            simulationData.setPassword(dataJSON.getString("password"));
        }
        if (dataJSON.containsKey("domainName")) {
            simulationData.setDomainname(dataJSON.getString("domainName"));
        }
        return simulationData;
    }

    /**
     * @return the smtpHostName
     */
    public String getSmtpHostName() {
        return smtpHostName;
    }

    /**
     * @param smtpHostName the smtpHostName to set
     */
    public void setSmtpHostName(String smtpHostName) {
        this.smtpHostName = smtpHostName;
    }

    /**
     * @return the pop3HostName
     */
    public String getPop3HostName() {
        return pop3HostName;
    }

    /**
     * @param pop3HostName the pop3HostName to set
     */
    public void setPop3HostName(String pop3HostName) {
        this.pop3HostName = pop3HostName;
    }

    /**
     * @return the userName
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @param userName the userName to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the domainName
     */
    public String getDomainname() {
        return domainName;
    }

    /**
     * @param domainName the domainName to set
     */
    public void setDomainname(String domainName) {
        this.domainName = domainName;
    }


}