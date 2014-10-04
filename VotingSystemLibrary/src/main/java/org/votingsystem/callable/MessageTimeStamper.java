package org.votingsystem.callable;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.concurrent.Callable;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class MessageTimeStamper implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(MessageTimeStamper.class);
    
    private SMIMEMessage smimeMessage;
    private TimeStampToken timeStampToken;
    private TimeStampRequest timeStampRequest;
    private String timeStampServerURL;
      
    public MessageTimeStamper (SMIMEMessage smimeMessage, String timeStampServerURL) throws Exception {
        this.smimeMessage = smimeMessage;
        this.timeStampRequest = smimeMessage.getTimeStampRequest();
        this.timeStampServerURL = timeStampServerURL;
    }
    
    public MessageTimeStamper (TimeStampRequest timeStampRequest, String timeStampServerURL) throws Exception {
        this.timeStampRequest = timeStampRequest;
        this.timeStampServerURL = timeStampServerURL;
    }
        
    @Override public ResponseVS call() throws Exception {
        ResponseVS responseVS = HttpHelper.getInstance().sendData(timeStampRequest.getEncoded(), ContentTypeVS.TIMESTAMP_QUERY,
                timeStampServerURL);
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            byte[] bytesToken = responseVS.getMessageBytes();
            timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
            X509Certificate timeStampCert = ContextVS.getInstance().getTimeStampServerCert();
            if(timeStampCert != null) {
                SignerInformationVerifier timeStampSignerInfoVerifier = new
                        JcaSimpleSignerInfoVerifierBuilder().build(timeStampCert);
                timeStampToken.validate(timeStampSignerInfoVerifier);
            } else logger.debug("TIMESTAMP RESPONSE NOT VALIDATED");
            if(smimeMessage != null) smimeMessage.setTimeStampToken(timeStampToken);
            responseVS.setSmimeMessage(smimeMessage);
        }
        return responseVS;
    }
    
    public TimeStampToken getTimeStampToken() {
        return timeStampToken;
    }
        
    public byte[] getDigestToken() {
        if(timeStampToken == null) return null;
        CMSSignedData tokenCMSSignedData = timeStampToken.toCMSSignedData();		
        Collection signers = tokenCMSSignedData.getSignerInfos().getSigners();
        SignerInformation tsaSignerInfo = (SignerInformation)signers.iterator().next();
        AttributeTable signedAttrTable = tsaSignerInfo.getSignedAttributes();
        ASN1EncodableVector v = signedAttrTable.getAll(CMSAttributes.messageDigest);
        Attribute t = (Attribute)v.get(0);
        ASN1Set attrValues = t.getAttrValues();
        DERObject validMessageDigest = attrValues.getObjectAt(0).getDERObject();
        ASN1OctetString signedMessageDigest = (ASN1OctetString)validMessageDigest;			
        byte[] digestToken = signedMessageDigest.getOctets();  
        //String digestTokenStr = new String(Base64.encode(digestToken));
        //logger.debug(" digestTokenStr: " + digestTokenStr);
        return digestToken;
    }
    
    public SMIMEMessage getSmimeMessage() {
        return smimeMessage;
    }

}