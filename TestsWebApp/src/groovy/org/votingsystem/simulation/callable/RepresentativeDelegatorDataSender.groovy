package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator

import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.StringUtils

import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.concurrent.Callable

import static org.votingsystem.model.ContextVS.*
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class RepresentativeDelegatorDataSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(RepresentativeDelegatorDataSender.class);
    
    private String userNIF;
    private String representativeNIF;
    private String serviceURL = null;
    private ResponseVS reponseVS;
    
    public RepresentativeDelegatorDataSender(String userNIF, String representativeNIF, String serviceURL) throws Exception {
        this.userNIF = userNIF;
        this.serviceURL = serviceURL;
        this.representativeNIF = representativeNIF;
        logger.debug("NIF: " + userNIF + " - representativeNIF: " + representativeNIF + " - serviceURL: " + serviceURL);
    }
    
    @Override public ResponseVS call() throws Exception {
        KeyStore mockDnie = ContextVS.getInstance().generateKeyStore(userNIF);
        String delegationDataJSON = getDelegationDataJSON(representativeNIF);

        ActorVS accessRequest = ContextVS.getInstance().getAccessControl();
        String toUser = StringUtils.getNormalized(accessRequest.getName());
        SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie,
                END_ENTITY_ALIAS, PASSWORD.toCharArray(), DNIe_SIGN_MECHANISM);

        String msgSubject = ApplicationContextHolder.getInstance().getMessage("representativeDelegationMsgSubject", null);
                
        SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(
                userNIF, toUser, delegationDataJSON, msgSubject, null);        
        
        X509Certificate destinationCert = ContextVS.getInstance().getAccessControl().getX509Certificate();
        SMIMESignedSender senderSender = new SMIMESignedSender(smimeMessage, serviceURL,
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                ContentTypeVS.JSON_SIGNED, null, destinationCert);
        reponseVS = senderSender.call();
        if (ResponseVS.SC_OK == reponseVS.getStatusCode()) reponseVS.setMessage(userNIF);
        return reponseVS;
    }

    public static String getDelegationDataJSON(String representativeNif) {
        logger.debug("getRepresentativeDataJSOn - ");
        Map map = new HashMap();
        map.put("operation", "REPRESENTATIVE_SELECTION");
        map.put("representativeNif", representativeNif);
        map.put("UUID", UUID.randomUUID().toString());
        JSONObject jsonObject = new JSONObject(map);
        return jsonObject.toString();
    }

}