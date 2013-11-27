package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VoteVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.PKCS10WrapperClient
import org.votingsystem.simulation.ContextService
import org.votingsystem.util.ApplicationContextHolder

import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.util.concurrent.Callable
import grails.converters.JSON
import static org.votingsystem.simulation.ContextService.*

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VoteSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(VoteSender.class);
   
    private VoteVS voteVS;
	private ContextService contextService ;
    private UserVS userVS;
        
    public VoteSender(VoteVS voteVS, UserVS userVS) throws Exception {
        this.voteVS = voteVS;
        this.userVS = userVS;
        this.contextService = ApplicationContextHolder.getSimulationContext();
    }
    
    @Override public ResponseVS call() {
        try {
            KeyStore mockDnie = contextService.generateTestDNIe(userVS.getNif());
            String msgSubject = contextService.getMessage("accessRequestMsgSubject",
                    voteVS.getEventVS().getId().toString());
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, END_ENTITY_ALIAS,
                    PASSWORD.toCharArray(), DNIe_SIGN_MECHANISM);
            String accessRequestStr = new JSON(voteVS.getAccessRequestDataMap()).toString();

            SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(userVS.getNif(),
                    contextService.getAccessControl().getNameNormalized(), accessRequestStr, msgSubject, null);
            AccessRequestDataSender accessRequestor = new AccessRequestDataSender(smimeMessage, voteVS);
            ResponseVS responseVS = accessRequestor.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                PKCS10WrapperClient wrapperClient = accessRequestor.getPKCS10WrapperClient();
                String votoStr = new JSON(voteVS.getVoteDataMap()).toString();
                String subject = contextService.getMessage("voteVSMsgSubject",voteVS.getEventVS()?.getId()?.toString());
                smimeMessage = wrapperClient.genMimeMessage(voteVS.getHashCertVoteBase64(),
                        contextService.getAccessControl().getNameNormalized(), votoStr, subject, null);
                SMIMESignedSender sender = new SMIMESignedSender(smimeMessage,
                        contextService.getControlCenter().getVoteServiceURL(), wrapperClient.getKeyPair(), null);
                responseVS = sender.call();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    SMIMEMessageWrapper voteReceipt = responseVS.getSmimeMessage();
                    voteVS.setReceipt(voteReceipt);
                    //_ TODO _ validate receipt
                }
            }
            responseVS.setData([voteVS:voteVS, userVS:userVS])
            return responseVS;
        } catch(Exception ex) {
            String msg = ex.getMessage() + " - user nif: " + userVS.getNif() +
                    " - hashCertVoteBase64: " + voteVS.getHashCertVoteBase64();
            logger.error(msg , ex);
            return new ResponseVS(ResponseVS.SC_ERROR, msg);
        }  
    }

    
}