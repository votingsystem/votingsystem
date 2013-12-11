package org.votingsystem.simulation.callable

import grails.converters.JSON
import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.ApplicationContextHolder as ACH

import java.util.concurrent.Callable
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
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
		ResponseVS responseVS =HttpHelper.getInstance().getData(ActorVS.getServerInfoURL(serverURL),ContentTypeVS.JSON);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            ActorVS actorVS = ActorVS.populate(new JSONObject(responseVS.getMessage()));
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
            }
            byte[] rootCACertPEMBytes = CertUtil.getPEMEncoded (ContextVS.getInstance().getRootCACert());
            responseVS = HttpHelper.getInstance().sendData(rootCACertPEMBytes, null,
                    actorVS.getRootCAServiceURL());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                if(actorVS instanceof AccessControlVS) ContextVS.getInstance().setAccessControl(actorVS);
                if(actorVS instanceof ControlCenterVS) ContextVS.getInstance().setControlCenter(actorVS);
                responseVS.setData(actorVS)
            }
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
            SMIMEMessageWrapper smimeDocument = ContextVS.getInstance().genTestSMIMEMessage(
                    ContextVS.getInstance().getAccessControl().getNameNormalized(), "${mapToSign as JSON}", msgSubject);
            SMIMESignedSender signedSender = new SMIMESignedSender(smimeDocument,
                    ContextVS.getInstance().getAccessControl().getServerSubscriptionServiceURL(),
                    ContentTypeVS.JSON_SIGNED, null, null);
            responseVS = signedSender.call();
        }
        return responseVS;
    }

}