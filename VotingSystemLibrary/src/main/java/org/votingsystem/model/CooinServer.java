package org.votingsystem.model;

import org.votingsystem.util.StringUtils;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="CooinServer")
@DiscriminatorValue("CooinServer")
public class CooinServer extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public CooinServer() {}

    public CooinServer(ActorVS actorVS) {
        setName(actorVS.getName());
        setX509Certificate(actorVS.getX509Certificate());
        setControlCenters(actorVS.getControlCenters());
        setEnvironmentVS(actorVS.getEnvironmentVS());
        setServerURL(actorVS.getServerURL());
        setState(actorVS.getState());
        setId(actorVS.getId());
        setTimeStampCert(actorVS.getTimeStampCert());
        setTrustAnchors(actorVS.getTrustAnchors());
    }

    @Override public Type getType() {
        return Type.COOINS;
    }

    public String getTransactionVSServiceURL() {
        return getServerURL() + "/transactionVS";
    }

    public String getCooinRequestServiceURL() {
        return getServerURL() + "/cooin/request";
    }

    public String getCooinStateServiceURL(String hashCertVS) {
        return getServerURL() + "/cooin/" + StringUtils.toHex(hashCertVS) + "/state";
    }

    public String getCooinBundleStateServiceURL() {
        return getServerURL() + "/cooin/bundleState";
    }

    public String getCooinTransactionServiceURL() {
        return getServerURL() + "/transactionVS/cooin";
    }

    public String getUserDashBoardURL() {
        return getServerURL() + "/app/userVS?menu=user";
    }

    public String getAdminDashBoardURL() {
        return getServerURL() + "/app/admin?menu=admin";
    }

    public String getSaveBankServiceURL() {
        return getServerURL() + "/userVS/newBankVS";
    }

    public String getSaveGroupVSServiceURL() {
        return getServerURL() + "/groupVS/newGroup";
    }

    public String getGroupVSSubscriptionServiceURL(Long groupId) {
        return getServerURL() + "/groupVS/" + String.valueOf(groupId) + "/subscribe";
    }

    public String getWalletURL() {
        return getServerURL() + "/cooin/wallet";
    }

    public String getGroupVSUsersServiceURL(Long groupId, Integer max, Integer offset,
                SubscriptionVS.State subscriptionState, UserVS.State userVSState) {
        return getServerURL() + "/groupVS/" + String.valueOf(groupId) + "/users" +
                "?max=" + ((max != null)?max:"") +
                "&offset=" + ((offset != null)?offset:"") +
                "&subscriptionState=" + ((subscriptionState != null)?subscriptionState.toString():"") +
                "&userVSState=" + ((userVSState != null)?userVSState.toString():"");
    }

    public String getGroupVSUsersActivationServiceURL() {
        return getServerURL() + "/groupVS/activateUser";
    }

    public String getDeviceVSConnectedServiceURL(String nif) {
        return getServerURL() + "/deviceVS/" + nif + "/connected";
    }

}
