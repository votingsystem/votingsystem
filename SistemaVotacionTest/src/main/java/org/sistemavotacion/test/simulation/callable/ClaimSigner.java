package org.sistemavotacion.test.simulation.callable;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.ActorConIP;
import org.sistemavotacion.modelo.Operacion;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.util.StringUtils;
import org.sistemavotacion.callable.SMIMESignedSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ClaimSigner implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(
            ClaimSigner.class);
    
    private SMIMEMessageWrapper smimeMessage;
    private String nif;
    private String submitClaimsURL = null;
    private Long eventId = null;
    private Respuesta respuesta;
        
    public ClaimSigner (String nif, Long eventId)  throws Exception {
        this.nif = nif;
        this.eventId = eventId;
        submitClaimsURL = ContextoPruebas.INSTANCE.getUrlSubmitClaims();
    }
    
    @Override public Respuesta call() throws Exception {
        try {
            KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(nif);

            ActorConIP controlAcceso = Contexto.INSTANCE.getAccessControl();
            String toUser = StringUtils.getCadenaNormalizada(controlAcceso.getNombre());

            String claimDataStr = getClaimDataJSON(eventId);
            String subject = ContextoPruebas.INSTANCE.getString("claimMsgSubject");
            
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(
                    mockDnie, ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS, 
                    ContextoPruebas.PASSWORD.toCharArray(),
                    ContextoPruebas.DNIe_SIGN_MECHANISM);
            smimeMessage = signedMailGenerator.genMimeMessage(
                    nif, toUser, claimDataStr, subject, null);
            X509Certificate destinationCert = ContextoPruebas.INSTANCE.
                    getAccessControl().getCertificate();  
            SMIMESignedSender worker = new SMIMESignedSender(
                    null, smimeMessage, submitClaimsURL, 
                    null, destinationCert);
            respuesta = worker.call();
            if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                respuesta.setMensaje(nif);
            } else respuesta.appendErrorMessage(nif);
        } catch(Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta.appendErrorMessage(ex.getMessage());
        }
        return respuesta;
    }
    
    public static String getClaimDataJSON(Long eventId) {
        Map map = new HashMap();
        map.put("operation", Operacion.Tipo.FIRMA_RECLAMACION_SMIME.toString());
        map.put("id", eventId);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(map);
        return jsonObject.toString();
    }

}