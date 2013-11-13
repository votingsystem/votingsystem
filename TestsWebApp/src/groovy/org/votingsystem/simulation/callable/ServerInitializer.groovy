package org.votingsystem.simulation.callable;

import java.util.concurrent.Callable;
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.simulation.util.SimulationUtils
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.simulation.ContextService;

import org.votingsystem.simulation.ApplicationContextHolder as ACH;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ServerInitializer implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(ServerInitializer.class);
    
    private String accessControlURL = null;
    private ActorVS.Type serverType = null;
	private ContextService contextService = null;
    
    public ServerInitializer (String accessControlURL, ActorVS.Type serverType) 
            throws Exception {
        this.serverType = serverType;
        this.accessControlURL = accessControlURL;
		contextService = ACH.getSimulationContext();
    }
        
    @Override public ResponseVS call() throws Exception {
		String accessControlURL = StringUtils.checkURL(accessControlURL);
        String urlInfoServidor = contextService.getServerInfoURL(accessControlURL);
        log.debug("call - serverType: " + serverType.toString() + 
                " - serviceURL: " + urlInfoServidor);
		ResponseVS responseVS = contextService.getHttpHelper().getData(urlInfoServidor, null);
        String msg = null;
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            switch(serverType) {
                case ActorVS.Type.ACCESS_CONTROL:
                    ActorVS accessControl = ActorVS.populate(new JSONObject(responseVS.getMessage()));
                    ResponseVS checkResponse = SimulationUtils.checkActor(accessControl, 
                            ActorVS.Type.ACCESS_CONTROL);
                    if(ResponseVS.SC_OK == checkResponse.getStatusCode()) {
                        contextService.setAccessControl(accessControl);
                        byte[] rootCACertPEMBytes = CertUtil.fromX509CertToPEM (
							contextService.getRootCACert());
                        String rootCAServiceURL = contextService.
							getRootCAServiceURL(accessControl.getServerURL());
                            responseVS = contextService.getHttpHelper().sendByteArray(
								rootCACertPEMBytes, null, rootCAServiceURL);
                    } else log.error(msg);
                    break;
                case ActorVS.Type.CONTROL_CENTER:
                        ActorVS controlCenter = ActorVS.populate(new JSONObject(responseVS.getMessage()));
                        ResponseVS checkResponse = SimulationUtils.checkActor(
                                controlCenter, ActorVS.Type.CONTROL_CENTER);
                        if(ResponseVS.SC_OK == checkResponse.getStatusCode()) {
                            contextService.setControlCenter(controlCenter);
                            byte[] rootCACertPEMBytes = CertUtil.fromX509CertToPEM (
								contextService.getRootCACert());
                            String rootCAServiceURL = contextService.
                                    getRootCAServiceURL(controlCenter.getServerURL());
                            responseVS = contextService.getHttpHelper().sendByteArray(
								rootCACertPEMBytes, null, rootCAServiceURL);
                        } else log.error(msg);
                    break;
            }
        }
        return responseVS;
    }
     
}