package org.sistemavotacion.test.simulation.callable;

import java.util.concurrent.Callable;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.test.util.SimulationUtils;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.worker.DocumentSenderWorker;
import org.sistemavotacion.worker.InfoGetterWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessControlInitializer implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(
            AccessControlInitializer.class);
    
    private String accessControlURL = null;
    
    public AccessControlInitializer (String accessControlURL) 
            throws Exception {
        this.accessControlURL = accessControlURL;
    }
        
    @Override
    public Respuesta call() throws Exception {
        logger.debug("call");
        String urlInfoServidor = ContextoPruebas.getURLInfoServidor(
                StringUtils.prepararURL(accessControlURL));
        InfoGetterWorker worker = new InfoGetterWorker(
                null, urlInfoServidor, null, null);
        worker.execute();
        Respuesta respuesta = worker.get();
        if(Respuesta.SC_OK == worker.getStatusCode()) {
            ActorConIP accessControl = ActorConIP.parse(worker.getMessage());
            String msg = SimulationUtils.checkActor(
                    accessControl, ActorConIP.Tipo.CONTROL_ACCESO);
            if(msg == null) {
                ContextoPruebas.INSTANCE.setControlAcceso(accessControl);
                byte[] rootCACertPEMBytes = CertUtil.fromX509CertToPEM (
                ContextoPruebas.INSTANCE.getRootCACert());
                String rootCAServiceURL = ContextoPruebas.INSTANCE.
                        getAccessControlRootCAServiceURL();
                DocumentSenderWorker docSenderWorker = new DocumentSenderWorker(
                        null, rootCACertPEMBytes, null, rootCAServiceURL, null);
                docSenderWorker.execute();
                respuesta = docSenderWorker.get();
            } 
        }
        return respuesta;
    }
        

}