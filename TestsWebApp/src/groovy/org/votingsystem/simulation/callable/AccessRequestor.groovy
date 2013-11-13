package org.votingsystem.simulation.callable;


import org.votingsystem.model.EventVS
import org.votingsystem.model.ResponseVS;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import javax.mail.Header;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.PKCS10WrapperClient;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.apache.log4j.Logger;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestor implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(
            AccessRequestor.class);

    private EventVS evento;   
    private SMIMEMessageWrapper smimeMessage;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private X509Certificate destinationCert = null;
 
    public AccessRequestor(SMIMEMessageWrapper smimeMessage,
            EventVS evento, X509Certificate destinationCert) throws Exception {
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
        log.debug("doInBackground - urlSolicitudAcceso: " + 
                evento.getUrlSolicitudAcceso());
        TimeStampRequest timeStampRequest = smimeMessage.getTimeStampRequest();
        ResponseVS respuesta = Contexto.INSTANCE.getHttpHelper().sendByteArray(
                timeStampRequest.getEncoded(), "timestamp-query", 
                Contexto.INSTANCE.getURLTimeStampServer());
        if (ResponseVS.SC_OK == respuesta.getStatusCode()) {
            byte[] bytesToken = respuesta.getMessageBytes();
            TimeStampToken timeStampToken = new TimeStampToken(
                    new CMSSignedData(bytesToken));
            X509Certificate timeStampCert = Contexto.INSTANCE.getTimeStampServerCert();
            SignerInformationVerifier timeStampSignerInfoVerifier = new 
                    JcaSimpleSignerInfoVerifierBuilder().
                setProvider(Contexto.PROVIDER).build(timeStampCert); 
            timeStampToken.validate(timeStampSignerInfoVerifier);
            smimeMessage.setTimeStampToken(timeStampToken);
            

            Header header = new Header("votingSystemMessageType", "voteCsr");
            byte[] encryptedCSRBytes =Encryptor.encryptMessage(
                    pkcs10WrapperClient.getPEMEncodedRequestCSR(), 
                    destinationCert, header);
        
            byte[] accessRequestEncryptedBytes = Encryptor.encryptSMIME(
                    smimeMessage, destinationCert);

            String csrFileName = Contexto.CSR_FILE_NAME + ":" + 
                    Contexto.ENCRYPTED_CONTENT_TYPE;
        
            String accessRequestFileName = Contexto.ACCESS_REQUEST_FILE_NAME + ":" + 
                    Contexto.SIGNED_AND_ENCRYPTED_CONTENT_TYPE;
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, encryptedCSRBytes);
            mapToSend.put(accessRequestFileName, accessRequestEncryptedBytes);

            respuesta = Contexto.INSTANCE.getHttpHelper().
                    sendObjectMap(mapToSend, evento.getUrlSolicitudAcceso());
            if (ResponseVS.SC_OK == respuesta.getStatusCode()) {
                byte[] encryptedData = respuesta.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, 
                        pkcs10WrapperClient.getPublicKey(), 
                        pkcs10WrapperClient.getPrivateKey());
                pkcs10WrapperClient.initSigner(decryptedData);
            }
        }
        return respuesta;
    }

    public PKCS10WrapperClient getPKCS10WrapperClient() {
        return pkcs10WrapperClient;
    }

}