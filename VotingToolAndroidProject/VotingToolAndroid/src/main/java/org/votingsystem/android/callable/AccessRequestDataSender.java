package org.votingsystem.android.callable;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.VoteVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.HttpHelper;
import org.votingsystem.util.ResponseVS;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.model.ContextVS.KEY_SIZE;
import static org.votingsystem.model.ContextVS.PROVIDER;
import static org.votingsystem.model.ContextVS.SIG_NAME;
import static org.votingsystem.model.ContextVS.VOTE_SIGN_MECHANISM;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class AccessRequestDataSender implements Callable<ResponseVS> {
    
	public static final String TAG = AccessRequestDataSender.class.getSimpleName();

    private SMIMEMessage accessRequest;
    private CertificationRequestVS certificationRequest;
    private X509Certificate destinationCert = null;
    private String serviceURL = null;
    private AppContextVS contextVS = null;

    public AccessRequestDataSender(SMIMEMessage accessRequest, VoteVS vote,
            X509Certificate destinationCert, String serviceURL,
            AppContextVS context) throws Exception {
        this.accessRequest = accessRequest;
        this.serviceURL = serviceURL;
        this.destinationCert = destinationCert;
        this.contextVS = context;
        this.certificationRequest = CertificationRequestVS.getVoteRequest(KEY_SIZE, SIG_NAME,
               VOTE_SIGN_MECHANISM, PROVIDER, vote.getEventVS().getAccessControl().getServerURL(),
                vote.getEventVS().getEventVSId().toString(), vote.getHashCertVSBase64());
    }
    
    @Override public ResponseVS call() {
        LOGD(TAG + ".call", " - urlAccessRequest: " + serviceURL);
        try {
            MessageTimeStamper timeStamper = new MessageTimeStamper(accessRequest, contextVS);
            ResponseVS responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            accessRequest = timeStamper.getSMIME();

            byte[] csrEncryptedBytes = Encryptor.encryptMessage(certificationRequest.
            		getCsrPEM(), destinationCert);

            byte[] csrEncryptedAccessRequestBytes = Encryptor.encryptSMIME(
                    accessRequest, destinationCert);
            String csrFileName = ContextVS.CSR_FILE_NAME + ":" + ContentTypeVS.ENCRYPTED.getName();

            String accessRequestFileName = ContextVS.ACCESS_REQUEST_FILE_NAME + ":" +
                    ContentTypeVS.JSON_SIGNED.getName();
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
    
    public CertificationRequestVS getCertificationRequest() {
        return certificationRequest;
    }
    
}