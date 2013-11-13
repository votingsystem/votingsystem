package org.votingsystem.simulation.callable;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.smime.SignedMailGenerator;

import org.votingsystem.model.ActorVS
import org.votingsystem.model.ResponseVS;
import org.apache.log4j.Logger;
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.util.StringUtils;
import org.votingsystem.simulation.ApplicationContextHolder as ACH;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class TimeStamperTest implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(TimeStamperTest.class);
    
    private SMIMEMessageWrapper documentSMIME;
    private String requestNIF;
    private String urlTimeStampService;
    private Long eventId;

    public TimeStamperTest (String requestNIF, 
            String urlTimeStampService, Long eventId) 
            throws Exception {
        this.requestNIF = requestNIF;
        this.urlTimeStampService = urlTimeStampService;
        this.eventId = eventId;
    }
        
    @Override
    public ResponseVS call() throws Exception {
        KeyStore mockDnie = SimulationContext.INSTANCE.crearMockDNIe(requestNIF);

        ActorVS controlAcceso = Contexto.INSTANCE.getAccessControl();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                SimulationContext.DEFAULTS.END_ENTITY_ALIAS, 
                SimulationContext.PASSWORD.toCharArray(),
                SimulationContext.VOTE_SIGN_MECHANISM);
        String subject = SimulationContext.INSTANCE.getString("timeStampMsgSubject");
        
        documentSMIME = signedMailGenerator.genMimeMessage(
                requestNIF, toUser, getRequestDataJSON(), subject , null);

        /*MessageTimeStamper timeStamper = new MessageTimeStamper(documentSMIME);
        ResponseVS respuesta = timeStamper.call();
        if(ResponseVS.SC_OK != respuesta.getStatusCode()) return respuesta;
        documentSMIME = timeStamper.getSmimeMessage();*/
        
        SMIMESignedSender signedSender = new SMIMESignedSender(null, documentSMIME, 
                urlTimeStampService, null, null);
        ResponseVS respuesta = signedSender.call();
        
        return respuesta;
    }
        
    public String getRequestDataJSON() {
        log.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "TIMESTAMP_TEST");
        map.put("UUID", UUID.randomUUID().toString());
        map.put("eventId", eventId);
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }

}