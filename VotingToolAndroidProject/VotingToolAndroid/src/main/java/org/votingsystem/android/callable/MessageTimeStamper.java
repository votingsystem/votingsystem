package org.votingsystem.android.callable;

import android.util.Log;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cms.CMSSignedData;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MessageTimeStamper implements Callable<ResponseVS> {
    
	public static final String TAG = "MessageTimeStamper";
    
    private SMIMEMessageWrapper smimeMessage;
    private static final int numMaxAttempts = 3;
    private TimeStampToken timeStampToken;
    private TimeStampRequest timeStampRequest;
    private AppContextVS contextVS;
      
    public MessageTimeStamper (SMIMEMessageWrapper smimeMessage,
            AppContextVS context) throws Exception {
        this.smimeMessage = smimeMessage;
        this.timeStampRequest = smimeMessage.getTimeStampRequest();
        this.contextVS = context;
    }
    
    public MessageTimeStamper (TimeStampRequest timeStampRequest,
            AppContextVS context) throws Exception {
        this.timeStampRequest = timeStampRequest;
        this.contextVS = context;
    }
        
    public MessageTimeStamper (String timeStampDigestAlgorithm, 
    		byte[] digestToTimeStamp, AppContextVS context) throws Exception {
    	TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
        this.timeStampRequest = reqgen.generate(
        		timeStampDigestAlgorithm, digestToTimeStamp);
        this.contextVS = context;
    }
    
        
    @Override public ResponseVS call() throws Exception {
        //byte[] base64timeStampRequest = Base64.encode(timeStampRequest.getEncoded());        
        AtomicInteger numAttemp = new AtomicInteger(0);
        AtomicBoolean done = new AtomicBoolean(false);
        ResponseVS responseVS = null;
        while(!done.get()) {
        	String timeStampServiceURL = contextVS.getAccessControl().
                    getTimeStampServiceURL();
            responseVS = HttpHelper.sendData(timeStampRequest.getEncoded(),
                    ContentTypeVS.TIMESTAMP_QUERY, timeStampServiceURL);
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                timeStampToken= new TimeStampToken(new CMSSignedData(responseVS.getMessageBytes()));
                X509Certificate timeStampCert = contextVS.getAccessControl().getTimeStampCert();
                /* -> Android project config problem
                 * SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(ContextVS.PROVIDER).build(timeStampCert);
                timeStampToken.validate(timeStampSignerInfoVerifier);*/
                timeStampToken.validate(timeStampCert, ContextVS.PROVIDER);/**/
                if(smimeMessage != null)
                	smimeMessage.setTimeStampToken(timeStampToken);
                done.set(true);
            } else if(ResponseVS.SC_ERROR_TIMESTAMP == responseVS.getStatusCode()) {
                if(numAttemp.getAndIncrement() < numMaxAttempts) {
                	Log.e(TAG + ".call(...)", "Error getting timestamp - attemp: " + numAttemp.get());
                } else done.set(true);
            } else {
            	done.set(true);
            }
        }
        return responseVS;
    }
    
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }
        
    public SMIMEMessageWrapper getSmimeMessage() {
        return smimeMessage;
    }

}