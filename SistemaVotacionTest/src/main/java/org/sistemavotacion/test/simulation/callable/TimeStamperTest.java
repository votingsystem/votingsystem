package org.sistemavotacion.test.simulation.callable;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.callable.MessageTimeStamper;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class TimeStamperTest implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(TimeStamperTest.class);
    
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
    public Respuesta call() throws Exception {
        KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(requestNIF);

        ActorConIP controlAcceso = Contexto.INSTANCE.getAccessControl();
        String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, 
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                ContextoPruebas.PASSWORD.toCharArray(),
                ContextoPruebas.VOTE_SIGN_MECHANISM);
        String subject = ContextoPruebas.INSTANCE.getString("timeStampMsgSubject");
        
        documentSMIME = signedMailGenerator.genMimeMessage(
                requestNIF, toUser, getRequestDataJSON(), subject , null);

        /*MessageTimeStamper timeStamper = new MessageTimeStamper(documentSMIME);
        Respuesta respuesta = timeStamper.call();
        if(Respuesta.SC_OK != respuesta.getCodigoEstado()) return respuesta;
        documentSMIME = timeStamper.getSmimeMessage();*/
        
        SMIMESignedSender signedSender = new SMIMESignedSender(null, documentSMIME, 
                urlTimeStampService, null, null);
        Respuesta respuesta = signedSender.call();
        
        return respuesta;
    }
        
    public String getRequestDataJSON() {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "TIMESTAMP_TEST");
        map.put("UUID", UUID.randomUUID().toString());
        map.put("eventId", eventId);
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }

}