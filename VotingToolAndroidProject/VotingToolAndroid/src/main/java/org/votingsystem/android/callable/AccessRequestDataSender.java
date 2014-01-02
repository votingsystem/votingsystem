package org.votingsystem.android.callable;

import android.content.Context;
import android.util.Log;

import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EventVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.mail.Header;

import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.VOTE_SIGN_MECHANISM;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestDataSender implements Callable<ResponseVS> {
    
	public static final String TAG = "AccessRequestDataSender";

    private SMIMEMessageWrapper accessRequets;
    private CertificationRequestVS certificationRequest;
    private X509Certificate destinationCert = null;
    private String serviceURL = null;
    private Context context = null;

    public AccessRequestDataSender(SMIMEMessageWrapper accessRequets, EventVS eventVS,
            X509Certificate destinationCert, String serviceURL, Context context) throws Exception {
        this.accessRequets = accessRequets;
        this.serviceURL = serviceURL;
        this.destinationCert = destinationCert;
        this.context = context;
        this.certificationRequest = CertificationRequestVS.getVoteRequest(KEY_SIZE, SIG_NAME,
               VOTE_SIGN_MECHANISM, PROVIDER, eventVS.getAccessControl().getServerURL(),
               eventVS.getEventVSId().toString(), eventVS.getHashCertVSBase64());
    }
    
    @Override public ResponseVS call() {
        Log.d(TAG + ".call", " - urlAccessRequest: " + serviceURL);
        try {
            MessageTimeStamper timeStamper = new MessageTimeStamper(accessRequets, context);
            ResponseVS responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            accessRequets = timeStamper.getSmimeMessage();

            Header header = new Header("votingSystemMessageType", "voteCsr");
            byte[] csrEncryptedBytes = Encryptor.encryptMessage(certificationRequest.
            		getCsrPEM(), destinationCert, header);

            byte[] csrEncryptedAccessRequestBytes = Encryptor.encryptSMIME(
                    accessRequets, destinationCert);
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.ENCRYPTED.getName();

            String accessRequestFileName = ContextVS.ACCESS_REQUEST_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED.getName();
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, csrEncryptedBytes);
            mapToSend.put(accessRequestFileName, csrEncryptedAccessRequestBytes);

            responseVS = HttpHelper.sendObjectMap(mapToSend, serviceURL);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                byte[] encryptedData = responseVS.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, 
                        certificationRequest.getPublicKey(),
                        certificationRequest.getPrivateKey());
                certificationRequest.initSigner(decryptedData);
            }
            return responseVS;
        } catch(Exception ex) {
            ex.printStackTrace();
            return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
    }
    
    public CertificationRequestVS getPKCS10WrapperClient() {
        return certificationRequest;
    }
    
}