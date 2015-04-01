package org.votingsystem.test.util;

import java.util.Map;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class POP3SimulationData extends SimulationData {


    private String smtpHostName = null;
    private String pop3HostName = null;
    private String userName = null;
    private String password = null;
    private String domainName = null;


    public POP3SimulationData() {}

    public POP3SimulationData(SimulationData simulationData) throws Exception {
        setMaxPendingResponses(simulationData.getMaxPendingResponses());
        setMessage(simulationData.getMessage());
        setBackupRequestEmail(simulationData.getBackupRequestEmail());
        setDurationInMillis(simulationData.getDurationInMillis());
        setNumRequestsProjected(simulationData.getNumRequestsProjected());
    }

    public static POP3SimulationData parse (Map dataMap) throws Exception {
        if(dataMap == null) return null;
        POP3SimulationData simulationData = new POP3SimulationData(
                SimulationData.parse(dataMap));
        if (dataMap.containsKey("smtpHostName")) {
            simulationData.setSmtpHostName((String) dataMap.get("smtpHostName"));
        }
        if (dataMap.containsKey("pop3HostName")) {
            simulationData.setPop3HostName((String) dataMap.get("pop3HostName"));
        }
        if (dataMap.containsKey("userName")) {
            simulationData.setUserName((String) dataMap.get("userName"));
        }
        if (dataMap.containsKey("password")) {
            simulationData.setPassword((String) dataMap.get("password"));
        }
        if (dataMap.containsKey("domainName")) {
            simulationData.setDomainname((String) dataMap.get("domainName"));
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