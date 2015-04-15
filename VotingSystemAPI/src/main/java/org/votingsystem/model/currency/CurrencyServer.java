package org.votingsystem.model.currency;

import org.votingsystem.model.ActorVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.StringUtils;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="CurrencyServer")
@DiscriminatorValue("CurrencyServer")
public class CurrencyServer extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public CurrencyServer() {}

    public CurrencyServer(ActorVS actorVS) throws Exception {
        setName(actorVS.getName());
        setX509Certificate(actorVS.getX509Certificate());
        setControlCenter(actorVS.getControlCenter());
        setEnvironmentVS(actorVS.getEnvironmentVS());
        setServerURL(actorVS.getServerURL());
        setState(actorVS.getState());
        setId(actorVS.getId());
        setTimeStampCert(actorVS.getTimeStampCert());
        setTrustAnchors(actorVS.getTrustAnchors());
    }

    @Override public Type getType() {
        return Type.CURRENCY;
    }

    public String getTransactionVSServiceURL() {
        return getServerURL() + "/rest/transactionVS";
    }

    public String getCurrencyRequestServiceURL() {
        return getServerURL() + "/rest/currency/request";
    }

    public String getCurrencyStateServiceURL(String hashCertVS) {
        return getServerURL() + "/rest/currency/" + StringUtils.toHex(hashCertVS) + "/state";
    }

    public String getCurrencyBundleStateServiceURL() {
        return getServerURL() + "/rest/currency/bundleState";
    }

    public String getCurrencyTransactionServiceURL() {
        return getServerURL() + "/rest/transactionVS/currency";
    }

    public String getUserDashBoardURL() {
        return getServerURL() + "/rest/app/userVS?menu=user";
    }

    public String getAdminDashBoardURL() {
        return getServerURL() + "/app/admin.xhtml?menu=admin";
    }

    public String getSaveBankServiceURL() {
        return getServerURL() + "/rest/userVS/newBankVS";
    }

    public String getSaveGroupVSServiceURL() {
        return getServerURL() + "/rest/groupVS/newGroup";
    }

    public String getGroupVSSubscriptionServiceURL(Long groupId) {
        return getServerURL() + "/rest/groupVS/id/" + String.valueOf(groupId) + "/subscribe";
    }

    public String getWalletURL() {
        return getServerURL() + "/rest/currency/wallet";
    }

    public String getGroupVSUsersServiceURL(Long groupId, Integer max, Integer offset,
                SubscriptionVS.State subscriptionState, UserVS.State userVSState) {
        return getServerURL() + "/rest/groupVS/id/" + String.valueOf(groupId) + "/listUsers" +
                "?max=" + ((max != null)?max:"") +
                "&offset=" + ((offset != null)?offset:"") +
                "&subscriptionState=" + ((subscriptionState != null)?subscriptionState.toString():"") +
                "&userVSState=" + ((userVSState != null)?userVSState.toString():"");
    }

    public String getGroupVSUsersActivationServiceURL() {
        return getServerURL() + "/rest/groupVS/activateUser";
    }

    public String getDeviceVSConnectedServiceURL(String nif) {
        return getServerURL() + "/rest/deviceVS/nif/" + nif + "/connected";
    }

}
