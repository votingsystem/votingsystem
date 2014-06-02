package org.votingsystem.model;

import org.votingsystem.util.DateUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class VicketServer extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = "VicketServer";


    public String getDepositURL() {
        return getServerURL() + "/transaction/deposit";
    }

    public String getVicketRequestServiceURL() {
        return getServerURL() + "/model/request";
    }

    public String getVicketDepositServiceURL() {
        return getServerURL() + "/model/deposit";
    }

    public String getVicketBatchCancellationServiceURL() {
        return getServerURL() + "/model/cancelBatch";
    }

    public String getVicketCancelServiceURL() {
        return getServerURL() + "/model/cancel";
    }

    public String getVicketBatchServiceURL() {
        return getServerURL() + "/transaction/vicketBatch";
    }


    public String getUserInfoServiceURL() {
        return getServerURL() + "/userVS/userInfo";
    }


    public String getDateUserInfoServiceURL(Date date) {
        return getServerURL() + "/userVS" + DateUtils.getURLPath(date);
    }
}
