package org.votingsystem.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
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
        return getServerURL() + "/backupVS";
    }

    public String getCertRequestServiceURL() {
        return getServerURL() + "/certificateVS/certRequest";
    }

    public String getDownloadServiceURL(String param) {
        return getServerURL() + "/backupVS/download/" + param;
    }

    public String getPublishManifestURL() {
        return getServerURL() + "/eventVSManifest";
    }


    public String getPublishClaimURL() {
        return getServerURL() + "/eventVSClaim";
    }

    public String getPublishElectionURL() {
        return getServerURL() + "/eventVSElection";
    }

    public String getManifestServiceURL(String eventId) {
        return getServerURL() + "/eventVSManifestCollector/" + eventId;
    }

    public String getClaimServiceURL() {
        return getServerURL() + "/eventVSClaimCollector";
    }

    public String getEventURL(String eventId) {
        return getServerURL() + "/eventVS/" + eventId;
    }

    public String getCancelEventServiceURL() {
        return getServerURL() + "/eventVS/cancelled";
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

    public String getVoteCancellerServiceURL() {
        return getServerURL() + "/voteVSCanceller";
    }

    public String getVoteStateServiceURL(String voteHashHex) {
        return getServerURL() + "/voteVS/hash/" + voteHashHex;
    }
    public String getVoteServiceURL() {
        return getServerURL() + "/voteVS";
    }

    public String getDashBoardURL() {
        return getServerURL() + "/app/admin?menu=admin";
    }

    public String getVotingPageURL() {
        return getServerURL() + "/eventVSElection/index?menu=user";
    }

    public String getSelectRepresentativePageURL() {
        return getServerURL() + "/representative/index?menu=user";
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
