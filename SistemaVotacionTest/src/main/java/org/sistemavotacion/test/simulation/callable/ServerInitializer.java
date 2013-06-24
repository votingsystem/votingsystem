package org.sistemavotacion.test.simulation.callable;

import java.util.concurrent.Callable;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.test.util.SimulationUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.callable.InfoSender;
import org.sistemavotacion.callable.InfoGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ServerInitializer implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(
            ServerInitializer.class);
    
    private String accessControlURL = null;
    private ActorConIP.Tipo serverType = null;
    
    public ServerInitializer (String accessControlURL, ActorConIP.Tipo serverType) 
            throws Exception {
        this.serverType = serverType;
        this.accessControlURL = accessControlURL;
    }
        
    @Override
    public Respuesta call() throws Exception {
        logger.debug("call - serverType: " + serverType.toString());
        String urlInfoServidor = ContextoPruebas.getURLInfoServidor(
                StringUtils.prepararURL(accessControlURL));
        InfoGetter worker = new InfoGetter(null, urlInfoServidor, null);
        Respuesta respuesta = worker.call();
        String msg = null;
        if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
            switch(serverType) {
                case CONTROL_ACCESO:
                    ActorConIP accessControl = ActorConIP.parse(respuesta.getMensaje());
                    msg = SimulationUtils.checkActor(accessControl, 
                            ActorConIP.Tipo.CONTROL_ACCESO);
                    if(msg == null) {
                        ContextoPruebas.INSTANCE.setControlAcceso(accessControl);
                        byte[] rootCACertPEMBytes = CertUtil.fromX509CertToPEM (
                        ContextoPruebas.INSTANCE.getRootCACert());
                        String rootCAServiceURL = ContextoPruebas.INSTANCE.
                                getAccessControlRootCAServiceURL();
                        InfoSender docSenderWorker = new InfoSender(
                                null, rootCACertPEMBytes, null, rootCAServiceURL);
                        respuesta = docSenderWorker.call();
                    } else logger.error(msg);
                    break;
                case CENTRO_CONTROL:
                        ActorConIP controlCenter = ActorConIP.parse(
                                respuesta.getMensaje());
                        msg = SimulationUtils.checkActor(
                                controlCenter, ActorConIP.Tipo.CENTRO_CONTROL);
                        if(msg == null) {
                            ContextoPruebas.INSTANCE.setControlCenter(controlCenter);
                            byte[] rootCACertPEMBytes = CertUtil.fromX509CertToPEM (
                            ContextoPruebas.INSTANCE.getRootCACert());
                            String rootCAServiceURL = ContextoPruebas.INSTANCE.
                                    getControlCenterRootCAServiceURL();
                            InfoSender docSenderWorker = new InfoSender(
                                    null, rootCACertPEMBytes, null, rootCAServiceURL);
                            respuesta = docSenderWorker.call();
                        } else logger.error(msg);
                    break;
            }

        }
        return respuesta;
    }
        

}