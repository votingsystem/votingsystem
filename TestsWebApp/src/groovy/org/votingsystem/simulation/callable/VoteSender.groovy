package org.votingsystem.simulation.callable

import org.apache.log4j.Logger
import org.votingsystem.callable.AccessRequestDataSender
import org.votingsystem.callable.SMIMESignedSender
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.VoteVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.smime.SignedMailGenerator
import org.votingsystem.signature.util.PKCS10WrapperClient

import org.votingsystem.util.ApplicationContextHolder

import java.security.KeyPair
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.util.concurrent.Callable
import grails.converters.JSON
import static org.votingsystem.model.ContextVS.*

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class VoteSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(VoteSender.class);
   
    private VoteVS voteVS;
    private UserVS userVS;
        
    public VoteSender(VoteVS voteVS, UserVS userVS) throws Exception {
        this.voteVS = voteVS;
        this.userVS = userVS;
    }
    
    @Override public ResponseVS call() {
        try {
            KeyStore mockDnie = ContextVS.getInstance().generateKeyStore(userVS.getNif());
            String msgSubject = ApplicationContextHolder.getInstance().getMessage("accessRequestMsgSubject",
                    voteVS.getEventVS().getId().toString());
            SignedMailGenerator signedMailGenerator = new SignedMailGenerator(mockDnie, END_ENTITY_ALIAS,
                    PASSWORD.toCharArray(), DNIe_SIGN_MECHANISM);
            String accessRequestStr = new JSON(voteVS.getAccessRequestDataMap()).toString();

            SMIMEMessageWrapper smimeMessage = signedMailGenerator.genMimeMessage(userVS.getNif(),
                    ContextVS.getInstance().getAccessControl().getNameNormalized(), accessRequestStr, msgSubject, null);
            AccessRequestDataSender accessRequestDataSender = new AccessRequestDataSender(smimeMessage, voteVS);
            ResponseVS responseVS = accessRequestDataSender.call();
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                PKCS10WrapperClient wrapperClient = responseVS.getData();
                String votoStr = new JSON(voteVS.getVoteDataMap()).toString();
                String subject = ContextVS.getInstance().getMessage("voteVSMsgSubject",
                        voteVS.getEventVS()?.getId()?.toString());
                smimeMessage = wrapperClient.genMimeMessage(voteVS.getHashCertVoteBase64(),
                        ContextVS.getInstance().getAccessControl().getNameNormalized(), votoStr, subject, null);
                SMIMESignedSender sender = new SMIMESignedSender(smimeMessage,
                        ContextVS.getInstance().getControlCenter().getVoteServiceURL(),
                        ContentTypeVS.VOTE, wrapperClient.getKeyPair(),
                        ContextVS.getInstance().getControlCenter().getX509Certificate());
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