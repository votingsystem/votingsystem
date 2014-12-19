package org.votingsystem.model;

import org.votingsystem.util.DateUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinServer extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = CooinServer.class.getSimpleName();


    public String getTransactionVSServiceURL() {
        return getServerURL() + "/transactionVS";
    }

    public String getCooinTransactionServiceURL() {
        return getServerURL() + "/transactionVS/cooin";
    }

    public String getCooinRequestServiceURL() {
        return getServerURL() + "/cooin/request";
    }

    public String getCooinBatchCancellationServiceURL() {
        return getServerURL() + "/cooin/cancelBatch";
    }

    public String getCooinCancelServiceURL() {
        return getServerURL() + "/cooin/cancel";
    }

    public String getUserInfoServiceURL(String nif) {
        return getServerURL() + "/userVS/userInfo/" + nif;
    }

    public String getTagVSSearchServiceURL(String searchParam) {
        return getServerURL() + "/tagVS/index?tag=" + searchParam;
    }

    public String getDateUserInfoServiceURL(Date date) {
        return getServerURL() + "/userVS" + DateUtils.getPath(date);
    }

    public String getDeviceVSConnectedServiceURL(String nif) {
        return getServerURL() + "/deviceVS/" + nif + "/connected";
    }

    public String getSearchServiceURL(String searchText) {
        return getServerURL() + "/userVS/search?searchText=" + searchText;
    }

    public String getSearchServiceURL(String phone, String email) {
        return getServerURL() + "/userVS/searchByDevice?phone=" + phone + "&email=" + email;
    }
}
