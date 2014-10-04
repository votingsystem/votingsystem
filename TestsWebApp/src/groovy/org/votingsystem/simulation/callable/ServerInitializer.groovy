package org.votingsystem.simulation.callable

import grails.converters.JSON
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.simulation.SignatureVSService
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.HttpHelper

import java.security.cert.X509Certificate
import java.util.concurrent.Callable
/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class ServerInitializer implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(ServerInitializer.class);
    
    private String serverURL = null;
    private ActorVS.Type serverType = null;
    
    public ServerInitializer (String serverURL, ActorVS.Type serverType) throws Exception {
        this.serverType = serverType;
        this.serverURL = serverURL;
    }

    @Override public ResponseVS call() throws Exception {
        logger.debug("call - serverType: " + serverType.toString());
        ResponseVS responseVS = null;
		responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(serverURL),ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            ActorVS actorVS = ActorVS.parse(new JSONObject(responseVS.getMessage()));
            if(actorVS.getEnvironmentVS() == null || EnvironmentVS.DEVELOPMENT != actorVS.getEnvironmentVS()) {
                return new ResponseVS(ResponseVS.SC_ERROR, "SERVER NOT IN DEVELOPMENT MODE. Server mode:" +
                        actorVS.getEnvironmentVS());
            }
            switch(serverType) {
                case ActorVS.Type.ACCESS_CONTROL:
                    if(!(actorVS instanceof AccessControlVS)) {
                        return new ResponseVS(ResponseVS.SC_ERROR, serverURL + " IS NOT " + serverType.toString() +
                                " - serverType: " + actorVS.getType().toString());
                    }
                    break;
                case ActorVS.Type.CONTROL_CENTER:
                    if(actorVS instanceof ControlCenterVS) {
                        responseVS = checkControlCenter(actorVS.getServerURL());
                        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                    } else {
                        String msg = serverURL + " SERVER IS NOT " + serverType.toString() +
                                " - serverType: " + actorVS.getType().toString();
                        logger.debug(msg)
                        return new ResponseVS(ResponseVS.SC_ERROR, msg);
                    }
                    break;
                case ActorVS.Type.VICKETS:
                    responseVS = HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(
                            actorVS.getTimeStampServerURL()),ContentTypeVS.JSON);
                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
                    ActorVS timeStampServer = ActorVS.parse(new JSONObject(responseVS.getMessage()));
                    ContextVS.getInstance().setTimeStampServerCert(timeStampServer.getCertChain().iterator().next());
                    break;

            }
            if(actorVS instanceof AccessControlVS) ContextVS.getInstance().setAccessControl(actorVS);
            if(actorVS instanceof ControlCenterVS) ContextVS.getInstance().setControlCenter(actorVS);
            responseVS.setData(actorVS)
        }
        return responseVS;
    }

    private ResponseVS checkControlCenter(String serverURL) {
        logger.debug("checkControlCenter - serverURL: " + serverURL);
        String serviceURL = ContextVS.getInstance().getAccessControl().getControlCenterCheckServiceURL(serverURL);
        ResponseVS responseVS = HttpHelper.getInstance().getData(serviceURL, ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) return responseVS;
        else {//serverURL isn't associated
            logger.debug("Control Center isn't associated -> Matching serverURL: " + serverURL);
            Map mapToSign = ActorVS.getAssociationDocumentMap(serverURL);
            String msgSubject = ApplicationContextHolder.getInstance().getMessage("associateControlCenterMsgSubject");
            SMIMEMessage smimeDocument = ContextVS.getInstance().genTestSMIMEMessage(
                    ContextVS.getInstance().getAccessControl().getNameNormalized(), "${mapToSign as JSON}", msgSubject);
            SMIMESignedSender signedSender = new SMIMESignedSender(smimeDocument,
                    ContextVS.getInstance().getAccessControl().getServerSubscriptionServiceURL(),
                    ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                    ContentTypeVS.JSON_SIGNED, null, null);
            responseVS = signedSender.call();
        }
        return responseVS;
    }

    private ResponseVS addCertificateAuthority(ActorVS actorVS) {
        logger.debug("addCertificateAuthority");
        SignatureVSService signatureVSService = (SignatureVSService)ApplicationContextHolder.getBean("signatureVSService")
        X509Certificate serverCert = signatureVSService.getServerCert()
        byte[] rootCACertPEMBytes = CertUtil.getPEMEncoded (serverCert);
        //byte[] rootCACertPEMBytes = CertUtil.getPEMEncoded (ContextVS.getInstance().getRootCACert());
        Map requestMap = [operation: TypeVS.CERT_CA_NEW.toString(), certChainPEM: new String(rootCACertPEMBytes, "UTF-8"),
                          info: "Autority from Test Web App '${Calendar.getInstance().getTime()}'"]
        String requestStr = "${requestMap as JSON}".toString();
        String msgSubject = ApplicationContextHolder.getMessage("newCertificateAuthorityMsgSubject")

        String serverName = ApplicationContextHolder.getGrailsApplication().config.VotingSystem.serverName
        ResponseVS responseVS = signatureVSService.getTimestampedSignedMimeMessage(serverName,
                actorVS.getNameNormalized(), requestStr, msgSubject)
        if(ResponseVS.SC_OK == responseVS.statusCode) {
            responseVS = HttpHelper.getInstance().sendData(responseVS.getSmimeMessage().getBytes(),
                    ContentTypeVS.JSON_SIGNED, actorVS.getRootCAServiceURL())
        }
        return responseVS;
    }

}