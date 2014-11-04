package org.votingsystem.android.callable;

import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.cms.CMSSignedData;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class MessageTimeStamper implements Callable<ResponseVS> {
    
	public static final String TAG = MessageTimeStamper.class.getSimpleName();
    
    private SMIMEMessage smimeMessage;
    private TimeStampToken timeStampToken;
    private TimeStampRequest timeStampRequest;
    private AppContextVS contextVS;
    private String timeStampServiceURL;
      
    public MessageTimeStamper (SMIMEMessage smimeMessage,
            AppContextVS context) throws Exception {
        this.smimeMessage = smimeMessage;
        this.timeStampRequest = smimeMessage.getTimeStampRequest();
        this.contextVS = context;
    }

    public MessageTimeStamper (SMIMEMessage smimeMessage, String timeStampServiceURL,
               AppContextVS context) throws Exception {
        this.smimeMessage = smimeMessage;
        this.timeStampRequest = smimeMessage.getTimeStampRequest();
        this.contextVS = context;
        this.timeStampServiceURL = timeStampServiceURL;
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
        if(timeStampServiceURL == null) timeStampServiceURL =
                contextVS.getTimeStampServiceURL();
        ResponseVS responseVS = HttpHelper.sendData(timeStampRequest.getEncoded(),
                ContentTypeVS.TIMESTAMP_QUERY, timeStampServiceURL);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            timeStampToken= new TimeStampToken(new CMSSignedData(responseVS.getMessageBytes()));
            X509Certificate timeStampCert = contextVS.getTimeStampCert();
                /* -> Android project config problem
                 * SignerInformationVerifier timeStampSignerInfoVerifier = new JcaSimpleSignerInfoVerifierBuilder().
                    setProvider(ContextVS.PROVIDER).build(timeStampCert);
                timeStampToken.validate(timeStampSignerInfoVerifier);*/
            timeStampToken.validate(timeStampCert, ContextVS.PROVIDER);/**/
            if(smimeMessage != null) smimeMessage.setTimeStampToken(timeStampToken);
        }
        return responseVS;
    }
    
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }
        
    public SMIMEMessage getSMIME() {
        return smimeMessage;
    }

}