package org.votingsystem.model;

import org.votingsystem.util.DateUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class VicketServer extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = VicketServer.class.getSimpleName();


    public String getTransactionVSServiceURL() {
        return getServerURL() + "/transactionVS";
    }

    public String getVicketTransactionServiceURL() {
        return getServerURL() + "/transactionVS/vicket";
    }

    public String getVicketRequestServiceURL() {
        return getServerURL() + "/vicket/request";
    }

    public String getVicketBatchCancellationServiceURL() {
        return getServerURL() + "/vicket/cancelBatch";
    }

    public String getVicketCancelServiceURL() {
        return getServerURL() + "/vicket/cancel";
    }

    public String getUserInfoServiceURL() {
        return getServerURL() + "/userVS/userInfo";
    }


    public String getDateUserInfoServiceURL(Date date) {
        return getServerURL() + "/userVS" + DateUtils.getPath(date);
    }
}
