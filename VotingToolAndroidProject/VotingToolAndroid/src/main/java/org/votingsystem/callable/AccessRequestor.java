package org.votingsystem.callable;

import android.content.Context;
import android.util.Log;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.android.model.ContextVSAndroid;
import org.votingsystem.android.model.EventVSAndroid;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.PKCS10WrapperClient;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.android.util.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.mail.Header;
import org.votingsystem.model.ContentTypeVS;
import static org.votingsystem.android.model.ContextVSAndroid.KEY_SIZE;
import static org.votingsystem.android.model.ContextVSAndroid.PROVIDER;
import static org.votingsystem.android.model.ContextVSAndroid.SIG_NAME;
import static org.votingsystem.android.model.ContextVSAndroid.VOTE_SIGN_MECHANISM;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class AccessRequestor implements Callable<ResponseVS> {
    
	public static final String TAG = "AccessRequestor";

    private SMIMEMessageWrapper accessRequets;
    private PKCS10WrapperClient pkcs10WrapperClient;
    private X509Certificate destinationCert = null;
    private String serviceURL = null;
    private Context context = null;

    public AccessRequestor (SMIMEMessageWrapper accessRequets,
            EventVSAndroid eventVSAndroid, X509Certificate destinationCert,
            String serviceURL, Context context) throws Exception {
        this.accessRequets = accessRequets;
        this.serviceURL = serviceURL;
        this.destinationCert = destinationCert;
        this.context = context;
        this.pkcs10WrapperClient = new PKCS10WrapperClient(
                KEY_SIZE, SIG_NAME, VOTE_SIGN_MECHANISM, PROVIDER, 
                eventVSAndroid.getAccessControl().getServerURL(),
                eventVSAndroid.getEventoId().toString(),
                eventVSAndroid.getHashCertificadoVotoHex());
    }
    
    @Override public ResponseVS call() {
        Log.d(TAG + ".call", " - urlAccessRequest: " + serviceURL);

        try {
            MessageTimeStamper timeStamper = new MessageTimeStamper(accessRequets, context);
            ResponseVS responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
            accessRequets = timeStamper.getSmimeMessage();

            Header header = new Header("votingSystemMessageType", "voteCsr");
            byte[] csrEncryptedBytes = Encryptor.encryptMessage(pkcs10WrapperClient.
            		getPEMEncodedRequestCSR(), destinationCert, header);

            byte[] csrEncryptedAccessRequestBytes = Encryptor.encryptSMIME(
                    accessRequets, destinationCert);
            String csrFileName = ContextVSAndroid.CSR_FILE_NAME + ":" +
            		ContentTypeVS.ENCRYPTED;

            String accessRequestFileName = ContextVSAndroid.ACCESS_REQUEST_FILE_NAME + ":" +
                    ContentTypeVS.SIGNED_AND_ENCRYPTED;
            Map<String, Object> mapToSend = new HashMap<String, Object>();
            mapToSend.put(csrFileName, csrEncryptedBytes);
            mapToSend.put(accessRequestFileName, csrEncryptedAccessRequestBytes);

            responseVS = HttpHelper.sendObjectMap(mapToSend, serviceURL);
            if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                byte[] encryptedData = responseVS.getMessageBytes();
                byte[] decryptedData = Encryptor.decryptFile(encryptedData, 
                        pkcs10WrapperClient.getPublicKey(), 
                        pkcs10WrapperClient.getPrivateKey());
                pkcs10WrapperClient.initSigner(decryptedData);
            }
            
            return responseVS;
        } catch(Exception ex) {
            ex.printStackTrace();
            return new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        }
    }
    
    public PKCS10WrapperClient getPKCS10WrapperClient() {
        return pkcs10WrapperClient;
    }
    
}