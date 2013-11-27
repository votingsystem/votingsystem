package org.votingsystem.applet.callable;

import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.applet.util.HttpHelper;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.PKCS10WrapperClient;

import javax.mail.Header;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.votingsystem.model.ContextVS.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestor implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(
            AccessRequestor.class);

    private EventVS eventVS;
    private SMIMEMessageWrapper smimeMessage;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private X509Certificate destinationCert = null;
 
    public AccessRequestor(SMIMEMessageWrapper smimeMessage,
            EventVS eventVS, X509Certificate destinationCert) throws Exception {
        this.smimeMessage = smimeMessage;
        this.eventVS = eventVS;
        this.destinationCert = destinationCert;
        this.pkcs10WrapperClient = new PKCS10WrapperClient(KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM, PROVIDER,
                eventVS.getAccessControlVS().getServerURL(), eventVS.getId().toString(),
                eventVS.getVoteVS().getHashCertVoteHex());
    }

    
    @Override public ResponseVS call() throws Exception {
        logger.debug("doInBackground - accessServiceURL: " +
                ((AccessControlVS)eventVS.getAccessControlVS()).getAccessServiceURL());
        TimeStampRequest timeStampRequest = smimeMessage.getTimeStampRequest();
        AccessControlVS accessControl = (AccessControlVS) ContextVS.getInstance().getAccessControl();
        ResponseVS responseVS = HttpHelper.getInstance().sendByteArray(
                timeStampRequest.getEncoded(), ContentTypeVS.TIMESTAMP_QUERY, accessControl.getTimeStampServerURL());
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] bytesToken = responseVS.getMessageBytes();
            TimeStampToken timeStampToken = new TimeStampToken(
                    new CMSSignedData(bytesToken));
            X509Certificate timeStampCert = ContextVS.getInstance().getTimeStampServerCert();
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().
                setProvider(ContextVS.PROVIDER).build(timeStampCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
            smimeMessage.setTimeStampToken(timeStampToken);

            Header header = new Header("votingSystemMessageType", "voteCsr");
            byte[] encryptedCSRBytes =Encryptor.encryptMessage(pkcs10WrapperClient.getCsrPEM(),
                    destinationCert, header);
            byte[] accessRequestEncryptedBytes = Encryptor.encryptSMIME(smimeMessage, destinationCert);
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" +  ContentTypeVS.ENCRYPTED;
            String accessRequestFileName = ContextVS.ACCESS_REQUEST_FILE_NAME + ":" + 
                    ContentTypeVS.SIGNED_AND_ENCRYPTED;
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, encryptedCSRBytes);
            mapToSend.put(accessRequestFileName, accessRequestEncryptedBytes);

            responseVS = HttpHelper.getInstance().sendObjectMap(mapToSend,
                    ((AccessControlVS)eventVS.getAccessControlVS()).getAccessServiceURL());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                byte[] encryptedData = responseVS.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, pkcs10WrapperClient.getPublicKey(),
                        pkcs10WrapperClient.getPrivateKey());
                pkcs10WrapperClient.initSigner(decryptedData);
            }
        }
        return responseVS;
    }

    public PKCS10WrapperClient getPKCS10WrapperClient() {
        return pkcs10WrapperClient;
    }

}