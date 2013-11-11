package org.votingsystem.applet.callable;

import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVSBase;
import org.votingsystem.model.ResponseVS;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.mail.Header;
import org.apache.log4j.Logger;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.applet.util.HttpHelper;
import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.VOTE_SIGN_MECHANISM;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.PKCS10WrapperClient;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestor implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(
            AccessRequestor.class);

    private EventVSBase evento;   
    private SMIMEMessageWrapper smimeMessage;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private X509Certificate destinationCert = null;
 
    public AccessRequestor(SMIMEMessageWrapper smimeMessage,
            EventVSBase evento, X509Certificate destinationCert) throws Exception {
        this.smimeMessage = smimeMessage;
        this.evento = evento;
        this.destinationCert = destinationCert;
        this.pkcs10WrapperClient = new PKCS10WrapperClient(
                KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM, PROVIDER, 
                evento.getControlAcceso().getServerURL(), 
                evento.getEventoId().toString(), 
                evento.getHashCertificadoVotoHex());
    }

    
    @Override public ResponseVS call() throws Exception {
        logger.debug("doInBackground - urlSolicitudAcceso: " + 
                evento.getUrlSolicitudAcceso());
        TimeStampRequest timeStampRequest = smimeMessage.getTimeStampRequest();
        ResponseVS responseVS = HttpHelper.INSTANCE.sendByteArray(
                timeStampRequest.getEncoded(), "timestamp-query", 
                ContextVS.INSTANCE.getURLTimeStampServer());
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] bytesToken = responseVS.getMessageBytes();
            TimeStampToken timeStampToken = new TimeStampToken(
                    new CMSSignedData(bytesToken));
            X509Certificate timeStampCert = ContextVS.INSTANCE.getTimeStampServerCert();
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().
                setProvider(ContextVS.PROVIDER).build(timeStampCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
            smimeMessage.setTimeStampToken(timeStampToken);
            

            Header header = new Header("votingSystemMessageType", "voteCsr");
            byte[] encryptedCSRBytes =Encryptor.encryptMessage(
                    pkcs10WrapperClient.getPEMEncodedRequestCSR(), 
                    destinationCert, header);
        
            byte[] accessRequestEncryptedBytes = Encryptor.encryptSMIME(
                    smimeMessage, destinationCert);

            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + 
                    ContextVS.ENCRYPTED_CONTENT_TYPE;
        
            String accessRequestFileName = ContextVS.ACCESS_REQUEST_FILE_NAME + ":" + 
                    ContextVS.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, encryptedCSRBytes);
            mapToSend.put(accessRequestFileName, accessRequestEncryptedBytes);

            responseVS = HttpHelper.INSTANCE.
                    sendObjectMap(mapToSend, evento.getUrlSolicitudAcceso());
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                byte[] encryptedData = responseVS.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, 
                        pkcs10WrapperClient.getPublicKey(), 
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