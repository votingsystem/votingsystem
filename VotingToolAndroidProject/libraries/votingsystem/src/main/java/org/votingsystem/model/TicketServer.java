package org.votingsystem.model;

import org.votingsystem.util.DateUtils;

import java.io.Serializable;
import java.util.Date;

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

    public String getTicketBatchCancellationServiceURL() {
        return getServerURL() + "/ticket/cancelBatch";
    }


    public String getTicketBatchServiceURL() {
        return getServerURL() + "/transaction/ticketBatch";
    }


    public String getUserInfoServiceURL() {
        return getServerURL() + "/userVS/userInfo";
    }


    public String getDateUserInfoServiceURL(Date date) {
        return getServerURL() + "/userVS" + DateUtils.getURLPath(date);
    }
}
