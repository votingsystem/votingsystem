package org.votingsystem.model;

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

    public AccessControlVS() {}

    public AccessControlVS (ActorVS actorVS) {
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
        return Type.ACCESS_CONTROL;
    }

    public String getAccessServiceURL() {
        return getServerURL() + "/accessRequestVS";
    }

    public String getAnonymousDelegationRequestServiceURL() {
        return getServerURL() + "/representative/anonymousDelegationRequest";
    }

    public String getAnonymousDelegationServiceURL() {
        return getServerURL() + "/representative/anonymousDelegation";
    }

    public String getDelegationServiceURL() {
        return getServerURL() + "/representative/delegation";
    }

    public String getBackupServiceURL() {
        return getServerURL() + "/rest/backupVS";
    }

    public String getCertRequestServiceURL() {
        return getServerURL() + "/jsf/certificateVS/certRequest.jsp";
    }

    public String getDownloadServiceURL(String param) {
        return getServerURL() + "/backupVS/download/" + param;
    }

    public String getPublishClaimURL() {
        return getServerURL() + "/eventVSClaim";
    }

    public String getPublishElectionURL() {
        return getServerURL() + "/eventVSElection";
    }

    public String getClaimServiceURL() {
        return getServerURL() + "/rest/eventVSClaim/collect";
    }

    public String getEventURL(String eventId) {
        return getServerURL() + "/rest/eventVS/" + eventId;
    }

    public String getCancelEventServiceURL() {
        return getServerURL() + "/rest/eventVS/cancelled";
    }

    public String getServerSubscriptionServiceURL() {
        return getServerURL() + "/subscriptionVS";
    }

    public String getRepresentativeServiceURL() {
        return getServerURL() + "/representative";
    }

    public String getControlCenterCheckServiceURL(String controlCenterServerURL) {
        return getServerURL() + "/subscriptionVS/checkControlCenter?serverURL=" + controlCenterServerURL;
    }

    public String getVoteCancelerServiceURL() {
        return getServerURL() + "/voteVS/cancel";
    }

    public String getVoteStateServiceURL(String voteHashHex) {
        return getServerURL() + "/voteVS/hash/" + voteHashHex;
    }
    public String getVoteServiceURL() {
        return getServerURL() + "/rest/voteVS";
    }

    public String getDashBoardURL() {
        return getServerURL() + "/app/admin?menu=admin";
    }

    public String getVotingPageURL() {
        return getServerURL() + "/eventVSElection?menu=user";
    }

    public String getSelectRepresentativePageURL() {
        return getServerURL() + "/representative?menu=user";
    }

    public String getUserCSRServiceURL (Long csrRequestId) {
        return getServerURL() + "/csr?csrRequestId=" + csrRequestId;
    }

    public String getUserCSRServiceURL () {
        return getServerURL() + "/csr/request";
    }

    public String getRepresentationStateServiceURL (String nif) {
        return getServerURL() + "/representative/state/" + nif;
    }
}
