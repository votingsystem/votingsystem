package org.votingsystem.model;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.Serializable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
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

    @Transient public String getTimeStampServerURL() {
        return getServerURL() + "/timeStampVS";
    }

    @Transient public String getAccessServiceURL() {
        return getServerURL() + "/accessRequestVS";
    }

    @Transient public String getBackupServiceURL() {
        return getServerURL() + "/backupVS";
    }

    @Transient public String getDownloadServiceURL(String param) {
        return getServerURL() + "/backupVS/download/" + param;
    }

    @Transient public String getPublishManifestURL() {
        return getServerURL() + "/eventVSManifest";
    }


    @Transient public String getPublishClaimURL() {
        return getServerURL() + "/eventVSClaim";
    }

    @Transient public String getPublishElectionURL() {
        return getServerURL() + "/eventVSElection";
    }

    @Transient public String getManifestServiceURL(String eventId) {
        return getServerURL() + "/eventVSManifestCollector/" + eventId;
    }

    @Transient public String getClaimServiceURL() {
        return getServerURL() + "/eventVSClaimCollector";
    }

    @Transient public String getEventURL(String eventId) {
        return getServerURL() + "/eventVS/" + eventId;
    }

    @Transient public String getCancelEventServiceURL() {
        return getServerURL() + "/eventVS/cancelled";
    }

    @Transient public String getServerSubscriptionServiceURL() {
        return getServerURL() + "/subscriptionVS";
    }

    @Transient public String getRepresentativeServiceURL() {
        return getServerURL() + "/representative";
    }

    @Transient public String getControlCenterCheckServiceURL(String controlCenterServerURL) {
        return getServerURL() + "/subscriptionVS/checkControlCenter?serverURL=" + controlCenterServerURL;
    }

    @Transient public String getVoteCancellerServiceURL() {
        return getServerURL() + "/voteVSCanceller";
    }

    @Transient public String getVoteServiceURL() {
        return getServerURL() + "/voteVS";
    }


}
