package org.votingsystem.callable;

import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContentTypeVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class MessageTimeStamper implements Callable<CMSSignedMessage> {
    
    private static Logger log = Logger.getLogger(MessageTimeStamper.class.getName());
    
    private CMSSignedMessage cmsMessage;
    private TimeStampToken timeStampToken;
    private TimeStampRequest timeStampRequest;
    private String timeStampServiceURL;
      
    public MessageTimeStamper (CMSSignedMessage cmsMessage, String timeStampServiceURL) throws Exception {
        this.cmsMessage = cmsMessage;
        this.timeStampRequest = cmsMessage.getTimeStampRequest();
        this.timeStampServiceURL = timeStampServiceURL;
    }
    
    public MessageTimeStamper (TimeStampRequest timeStampRequest, String timeStampServiceURL) throws Exception {
        this.timeStampRequest = timeStampRequest;
        this.timeStampServiceURL = timeStampServiceURL;
    }
        
    @Override public CMSSignedMessage call() throws Exception {
        ResponseVS responseVS = HttpHelper.getInstance().sendData(timeStampRequest.getEncoded(), ContentTypeVS.TIMESTAMP_QUERY,
                timeStampServiceURL);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] bytesToken = responseVS.getMessageBytes();
            timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
            X509Certificate timeStampCert = ContextVS.getInstance().getTimeStampServerCert();
            if(timeStampCert != null) {
                SignerInformationVerifier timeStampSignerInfoVerifier = new
                        JcaSimpleSignerInfoVerifierBuilder().build(timeStampCert);
                timeStampToken.validate(timeStampSignerInfoVerifier);
            } else log.info("TIMESTAMP RESPONSE NOT VALIDATED");
            if(cmsMessage != null) cmsMessage = CMSSignedMessage.addTimeStampToUnsignedAttributes(cmsMessage, timeStampToken);
            return cmsMessage;
        } else throw new ExceptionVS(responseVS.getMessage());
    }
    
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }
    
    public CMSSignedMessage getCMS() {
        return cmsMessage;
    }

}