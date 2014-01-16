package org.votingsystem.model;

import java.io.Serializable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketProviderVS extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = "TicketProviderVS";

    public String getTicketRequestServiceURL() {
        return getServerURL() + "/ticket/request";
    }

    public String getTicketDepositServiceURL() {
        return getServerURL() + "/ticket/deposit";
    }

}
