package org.votingsystem.model.currency;

import org.votingsystem.model.Actor;
import org.votingsystem.model.User;
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
public class CurrencyServer extends Actor implements Serializable {

    public static final long serialVersionUID = 1L;

    public CurrencyServer() {}

    public CurrencyServer(Actor actor) throws Exception {
        setName(actor.getName());
        setX509Certificate(actor.getX509Certificate());
        setControlCenter(actor.getControlCenter());
        setServerURL(actor.getServerURL());
        setState(actor.getState());
        setId(actor.getId());
        setTimeStampCert(actor.getTimeStampCert());
        setTrustAnchors(actor.getTrustAnchors());
    }

    @Override public Type getType() {
        return Type.CURRENCY;
    }

    public String getTransactionServiceURL() {
        return getServerURL() + "/rest/transaction";
    }

    public String getCurrencyRequestServiceURL() {
        return getServerURL() + "/currency/request";
    }

    public String getCurrencyStateServiceURL(String hashCertVS) {
        return getServerURL() + "/rest/currency/hash/" + StringUtils.toHex(hashCertVS) + "/state";
    }

    public String getCurrencyBundleStateServiceURL() {
        return getServerURL() + "/rest/currency/bundleState";
    }

    public String getCurrencyTransactionServiceURL() {
        return getServerURL() + "/rest/transaction/currency";
    }

    public String getGroupListURL(String menu, String locale) {
        String sufix = (menu != null || locale != null)? "?menu=" + menu + "&locale=" + locale : "";
        return getServerURL() + "/rest/group" + sufix;
    }

    public String getSaveBankServiceURL() {
        return getServerURL() + "/rest/user/newBank";
    }

    public String getSaveGroupServiceURL() {
        return getServerURL() + "/rest/group/saveGroup";
    }

    public String getGroupSubscriptionServiceURL(Long groupId) {
        return getServerURL() + "/rest/group/id/" + String.valueOf(groupId) + "/subscribe";
    }

    public String getTagVSSearchServiceURL(String searchParam) {
        return getServerURL() + "/rest/tagVS?tag=" + searchParam;
    }

    public String getTagVSServiceURL() {
        return getServerURL() + "/rest/tagVS/list";
    }

    public String getGroupUsersServiceURL(Long groupId, Integer max, Integer offset,
                                          Subscription.State subscriptionState, User.State userState) {
        return getServerURL() + "/rest/group/id/" + String.valueOf(groupId) + "/listUsers" +
                "?max=" + ((max != null)?max:"") +
                "&offset=" + ((offset != null)?offset:"") +
                "&subscriptionState=" + ((subscriptionState != null)?subscriptionState.toString():"") +
                "&userState=" + ((userState != null)?userState.toString():"");
    }

    public String getGroupUsersActivationServiceURL() {
        return getServerURL() + "/rest/group/activateUser";
    }

    public String getDeviceConnectedServiceURL(String nif) {
        return getServerURL() + "/rest/device/nif/" + nif + "/connected";
    }

    public String getDeviceByIdServiceURL(Long deviceId) {
        return getServerURL() + "/rest/device/id/" + deviceId;
    }

}
