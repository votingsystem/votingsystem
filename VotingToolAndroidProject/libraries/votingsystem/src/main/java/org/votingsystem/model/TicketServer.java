package org.votingsystem.model;

import java.io.Serializable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketServer extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = "TicketServer";


    public String getDepositURL() {
        return getServerURL() + "/transaction/deposit";
    }

    public String getTicketRequestServiceURL() {
        return getServerURL() + "/ticket/request";
    }

    public String getTicketDepositServiceURL() {
        return getServerURL() + "/ticket/deposit";
    }

    public String getUserInfoServiceURL() {
        return getServerURL() + "/userVS/userInfo";
    }


}
