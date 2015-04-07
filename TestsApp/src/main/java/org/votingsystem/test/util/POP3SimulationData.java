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

    public String getSmtpHostName() {
        return smtpHostName;
    }

    public void setSmtpHostName(String smtpHostName) {
        this.smtpHostName = smtpHostName;
    }

    public String getPop3HostName() {
        return pop3HostName;
    }

    public void setPop3HostName(String pop3HostName) {
        this.pop3HostName = pop3HostName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDomainname() {
        return domainName;
    }

    public void setDomainname(String domainName) {
        this.domainName = domainName;
    }

}