package org.votingsystem.model.voting;

import org.votingsystem.model.ActorVS;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@Table(name="AccessControlVS")
@DiscriminatorValue("AccessControlVS")
public class AccessControlVS extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public AccessControlVS() {
        setType(Type.ACCESS_CONTROL);
    }

    public AccessControlVS (ActorVS actorVS) throws Exception {
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
        return Type.ACCESS_CONTROL;
    }

    public String getAccessServiceURL() {
        return getServerURL() + "/accessRequestVS";
    }

    public String getAnonymousDelegationRequestServiceURL() {
        return getServerURL() + "/representative/anonymousDelegationRequest";
    }

    public String getAnonymousDelegationServiceURL() {
        return getServerURL() + "/rest/representative/anonymousDelegation";
    }

    public String getDelegationServiceURL() {
        return getServerURL() + "/rest/representative/delegation";
    }

    public String getBackupServiceURL() {
        return getServerURL() + "/rest/backupVS";
    }

    public String getCertRequestServiceURL() {
        return getServerURL() + "/certificateVS/certRequest.xhtml";
    }

    public String getDownloadServiceURL(String param) {
        return getServerURL() + "/rest/backupVS/download/" + param;
    }

    public String getPublishElectionURL() {
        return getServerURL() + "/rest/eventVSElection";
    }

    public String getEventURL(String eventId) {
        return getServerURL() + "/rest/eventVS/" + eventId;
    }

    public String getCancelEventServiceURL() {
        return getServerURL() + "/rest/eventVSElection/cancelled";
    }

    public String getServerSubscriptionServiceURL() {
        return getServerURL() + "/rest/subscriptionVS";
    }

    public String getRepresentativeServiceURL() {
        return getServerURL() + "/rest/representative/save";
    }

    public String getControlCenterCheckServiceURL(String controlCenterServerURL) {
        return getServerURL() + "/rest/subscriptionVS/checkControlCenter?serverURL=" + controlCenterServerURL;
    }

    public String getVoteCancelerServiceURL() {
        return getServerURL() + "/rest/voteVS/cancel";
    }

    public String getAnonymousDelegationCancelerServiceURL() {
        return getServerURL() + "/representative/cancelAnonymousDelegation";
    }

    public String getVoteStateServiceURL(String voteHashHex) {
        return getServerURL() + "/rest/voteVS/hash/" + voteHashHex;
    }
    public String getVoteServiceURL() {
        return getServerURL() + "/rest/voteVS";
    }

    public String getDashBoardURL(String menu, String locale) {
        String sufix = (menu != null || locale != null)? "?menu=" + menu + "&locale=" + locale : "";
        return getServerURL() + "/app/admin.xhtml" + sufix;
    }

    public String getVotingPageURL(String menu, String locale) {
        String sufix = (menu != null || locale != null)? "?menu=" + menu + "&locale=" + locale : "";
        return getServerURL() + "/rest/eventVSElection" + sufix;
    }

    public String getSelectRepresentativePageURL(String menu, String locale) {
        String sufix = (menu != null || locale != null)? "?menu=" + menu + "&locale=" + locale : "";
        return getServerURL() + "/rest/representative" + sufix;
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
        return getServerURL() + "/rest/userVS/nif/" + nif + "/representationState";
    }

    public String getRepresentativeByNifServiceURL (String nif) {
        return getServerURL() + "/rest/representative/nif/" + nif;
    }

}