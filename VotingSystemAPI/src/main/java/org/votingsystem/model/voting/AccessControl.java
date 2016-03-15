package org.votingsystem.model.voting;

import org.votingsystem.model.Actor;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("AccessControl")
public class AccessControl extends Actor implements Serializable {

    public static final long serialVersionUID = 1L;

    public AccessControl() {
        setType(Type.ACCESS_CONTROL);
    }

    public AccessControl(Actor actor) throws Exception {
        setName(actor.getName());
        setX509Certificate(actor.getX509Certificate());
        setControlCenter(actor.getControlCenter());
        setEnvironmentVS(actor.getEnvironmentVS());
        setServerURL(actor.getServerURL());
        setState(actor.getState());
        setId(actor.getId());
        setTimeStampCert(actor.getTimeStampCert());
        setTrustAnchors(actor.getTrustAnchors());
    }

    @Override public Type getType() {
        return Type.ACCESS_CONTROL;
    }

    public String getAccessServiceURL() {
        return getServerURL() + "/accessRequest";
    }

    public String getAnonymousDelegationRequestServiceURL() {
        return getServerURL() + "/representative/anonymousDelegationRequest";
    }

    public String getAnonymousDelegationServiceURL() {
        return getServerURL() + "/rest/representative/anonymousDelegation";
    }

    public String getBackupServiceURL() {
        return getServerURL() + "/rest/backup";
    }

    public String getCertRequestServiceURL() {
        return getServerURL() + "/certificateVS/certRequest.xhtml";
    }

    public String getDownloadServiceURL(String param) {
        return getServerURL() + "/rest/backup/download/" + param;
    }

    public String getPublishElectionURL() {
        return getServerURL() + "/rest/eventElection";
    }

    public String getEventURL(String eventId) {
        return getServerURL() + "/rest/eventVS/" + eventId;
    }

    public String getCancelEventServiceURL() {
        return getServerURL() + "/rest/eventElection/cancelled";
    }

    public String getServerSubscriptionServiceURL() {
        return getServerURL() + "/rest/subscription";
    }

    public String getRepresentativeServiceURL() {
        return getServerURL() + "/rest/representative/save";
    }

    public String getControlCenterCheckServiceURL(String controlCenterServerURL) {
        return getServerURL() + "/rest/subscription/checkControlCenter?serverURL=" + controlCenterServerURL;
    }

    public String getVoteCancelerServiceURL() {
        return getServerURL() + "/rest/vote/cancel";
    }

    public String getAnonymousDelegationCancelerServiceURL() {
        return getServerURL() + "/representative/cancelAnonymousDelegation";
    }

    public String getVoteStateServiceURL(String voteHashHex) {
        return getServerURL() + "/rest/vote/hash/" + voteHashHex;
    }
    public String getVoteServiceURL() {
        return getServerURL() + "/rest/vote";
    }

    public String getDashBoardURL(String menu, String locale) {
        String sufix = (menu != null || locale != null)? "?menu=" + menu + "&locale=" + locale : "";
        return getServerURL() + "/spa.xhtml?menu=Admin" + sufix;
    }

    public String getVotingPageURL(String menu, String locale) {
        String sufix = (menu != null || locale != null)? "?menu=" + menu + "&locale=" + locale : "";
        return getServerURL() + "/spa.xhtml#!/eventElection" + sufix;
    }

    public String getSelectRepresentativePageURL(String menu, String locale) {
        String sufix = (menu != null || locale != null)? "?menu=" + menu + "&locale=" + locale : "";
        return getServerURL() + "/spa.xhtml#!/representative" + sufix;
    }

    public String getUserCSRServiceURL (Long csrRequestId) {
        return getServerURL() + "/rest/csr?csrRequestId=" + csrRequestId;
    }

    public String getUserCSRValidationServiceURL() {
        return getServerURL() + "/rest/csr/validate";
    }

    public String getUserCSRServiceURL () {
        return getServerURL() + "/rest/csr/request";
    }

    public String getRepresentationStateServiceURL (String nif) {
        return getServerURL() + "/rest/user/nif/" + nif + "/representationState";
    }

    public String getRepresentativeByNifServiceURL (String nif) {
        return getServerURL() + "/rest/representative/nif/" + nif;
    }

}