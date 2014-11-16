package org.votingsystem.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.StringUtils;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class AccessControlVS extends ActorVS implements Serializable {

    public static final long serialVersionUID = 1L;

    public static final String TAG = "AccessControlVS";
    
    private EventVS eventVS;
    private ControlCenterVS controlCenter;

    public AccessControlVS() {}

    public AccessControlVS(ActorVS actorVS) {
        setCertificate(actorVS.getCertificate());
        setCertChain(actorVS.getCertChain());
        setCertificatePEM(actorVS.getCertificatePEM());
        setCertificateURL(actorVS.getCertificateURL());
        setDateCreated(actorVS.getDateCreated());
        setId(actorVS.getId());
        setState(actorVS.getState());
        setServerURL(actorVS.getServerURL());
        setLastUpdated(actorVS.getLastUpdated());
        setName(actorVS.getName());
    }

    public void setEventVS(EventVS eventVS) {
        this.eventVS = eventVS;
    }

    public EventVS getEventVS() {
        return eventVS;
    }
        
    @Override public Type getType() {
        return Type.CONTROL_CENTER;
    }

    public ControlCenterVS getControlCenter() {
        return controlCenter;
    }

    public void setControlCenter(ControlCenterVS controlCenter) {
        this.controlCenter = controlCenter;
    }

    public String getEventVSManifestCollectorURL (Long manifestId) {
        return getServerURL() + "/eventVSManifestCollector/" + manifestId;
    }

    public String getEventVSManifestURL (Long manifestId) {
        return getServerURL() + "/eventVSManifest/" + manifestId;
    }

    public String getCancelVoteServiceURL() {
        return getServerURL() + "/voteVSCanceller";
    }

    public String getEventVSClaimCollectorURL () {
        return getServerURL() + "/eventVSClaimCollector";
    }

    public String getSearchServiceURL (int offset, int max) {
        return getServerURL() + "/search/find?max=" + max + "&offset=" + offset;
    }

    public String getSearchServiceURL (Integer offset, Integer max, String searchText,
            EventVS.Type eventType, EventVS.State state) {
        String offsetStr = (offset != null? offset.toString():"");
        String maxStr = (max != null? max.toString():"");
        String stateStr = (state != null? state.toString():"");
        String typeStr = (eventType != null? eventType.toString():"");
        return getServerURL() + "/search/eventVS?max=" + maxStr + "&offset=" + offsetStr +
                "&eventVSState=" + stateStr + "&eventvsType=" + typeStr + "&searchText=" + searchText;
    }

    public String getEventVSURL () {
        return getServerURL() + "/eventVS";
    }

    public String getServerInfoURL () {
        return getServerURL() + "/serverInfo";
    }

    public String getRepresentativesURL (Long offset, Integer pageSize) {
        return getServerURL() + "/representative?offset=" + offset + "&max=" + pageSize;
    }

    public String getRepresentativeURL (Long representativeId) {
        return getServerURL() + "/representative/" + representativeId;
    }

    public String getRepresentativeURLByNif (String nif) {
        return getServerURL() + "/representative/nif/" + nif;
    }

    public String getRepresentativeImageURL (Long representativeId) {
        return getServerURL() + "/representative/" + representativeId + "/image";
    }

    public String getRepresentativeServiceURL () {
        return getServerURL() + "/representative";
    }

    public String getRepresentativeDelegationServiceURL () {
        return getServerURL() + "/representative/delegation";
    }

    public String getEventVSURL (EventVS.State eventState, int max, Long offset) {
        return getServerURL() + "/eventVSElection?max="+ max + "&offset=" + offset  +
                "&eventVSState=" + eventState.toString();
    }

    public String getPublishServiceURL() {
        return getServerURL() + "/eventVSElection";
    }

    public String getUserCSRServiceURL (Long csrRequestId) {
        return getServerURL() + "/csr?csrRequestId=" + csrRequestId;
    }

    public String getUserCSRServiceURL () {
        return getServerURL() + "/csr/request";
    }

    public String getCertificationCentersURL () {
        return getServerURL() + "/serverInfo/certificationCenters";
    }

    public String getAccessServiceURL () {
        return getServerURL() +  "/accessRequestVS";
    }

    public String getCheckDatesServiceURL (Long id) {
        return getServerURL() +  "/eventVS/checkDates?id=" + id;
    }

    public String getAnonymousDelegationRequestServiceURL() {
        return getServerURL() + "/representative/anonymousDelegationRequest";
    }

    public String getAnonymousDelegationServiceURL() {
        return getServerURL() + "/representative/anonymousDelegation";
    }

    public static AccessControlVS parse(String actorVSStr) throws Exception {
        JSONObject actorVSJSON = new JSONObject(actorVSStr);
        JSONObject jsonObject = null;
        JSONArray jsonArray;
        AccessControlVS actorVS = new AccessControlVS();
        if (actorVSJSON.getJSONObject("controlCenter") != null) {
            jsonObject = actorVSJSON.getJSONObject("controlCenter");
            ControlCenterVS controlCenter = new ControlCenterVS();
            controlCenter.setName(jsonObject.getString("name"));
            controlCenter.setServerURL(jsonObject.getString("serverURL"));
            controlCenter.setId(jsonObject.getLong("id"));
            controlCenter.setDateCreated(DateUtils.getDateFromString(jsonObject.getString("dateCreated")));
            if (jsonObject.getString("state") != null) {
                controlCenter.setState(State.valueOf(jsonObject.getString("state")));
            }
            ((AccessControlVS)actorVS).setControlCenter(controlCenter);
        }
        if (actorVSJSON.has("urlBlog"))
            actorVS.setUrlBlog(actorVSJSON.getString("urlBlog"));
        if (actorVSJSON.has("serverURL"))
            actorVS.setServerURL(actorVSJSON.getString("serverURL"));
        if (actorVSJSON.has("name"))
            actorVS.setName(actorVSJSON.getString("name"));
        if (actorVSJSON.has("certChainPEM")) {
            Collection<X509Certificate> certChain =
                    CertUtils.fromPEMToX509CertCollection(actorVSJSON.
                            getString("certChainPEM").getBytes());
            actorVS.setCertChain(certChain);
            X509Certificate serverCert = certChain.iterator().next();
            Log.d(TAG + ".parse(..) ", "actorVS cert: " + serverCert.getSubjectDN().toString());
            actorVS.setCertificate(serverCert);
        }
        if (actorVSJSON.has("timeStampCertPEM")) {
            actorVS.setTimeStampCertPEM(actorVSJSON.getString(
                    "timeStampCertPEM"));
        }
        if(actorVSJSON.has("urlTimeStampServer")) {
            String urlTimeStampServer = StringUtils.checkURL((String) actorVSJSON.get("urlTimeStampServer"));
            actorVS.setUrlTimeStampServer((String) actorVSJSON.get("urlTimeStampServer"));
        }
        return actorVS;
    }

    public String getRepresentativeRevokeServiceURL() {
        return getServerURL() + "/representative/revoke";
    }

}
