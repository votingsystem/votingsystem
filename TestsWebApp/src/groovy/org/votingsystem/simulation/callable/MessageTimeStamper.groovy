package org.votingsystem.simulation.callable;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle.asn1.cms.Attribute;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.VotingSystemKeyGenerator;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.simulation.ApplicationContextHolder as ACH;
import org.apache.log4j.Logger;


/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MessageTimeStamper implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(MessageTimeStamper.class);
    
    private SMIMEMessageWrapper smimeMessage;
    private TimeStampToken timeStampToken;
    private TimeStampRequest timeStampRequest;
    private static final int numMaxAttempts = 3;
      
    public MessageTimeStamper (SMIMEMessageWrapper smimeMessage) throws Exception {
        this.smimeMessage = smimeMessage;
        this.timeStampRequest = smimeMessage.getTimeStampRequest();
    }
    
    public MessageTimeStamper (TimeStampRequest timeStampRequest) throws Exception {
        this.timeStampRequest = timeStampRequest;
    }
        
    @Override public ResponseVS call() throws Exception {
        int numAttemp = 0;
        AtomicBoolean done = new AtomicBoolean(false);
        ResponseVS responseVS = null;
        while(!done.get()) {
            responseVS = ACH.getSimulationContext().getHttpHelper().sendByteArray(
                timeStampRequest.getEncoded(), "timestamp-query", 
                ACH.getSimulationContext().getURLTimeStampServer());
            if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
                byte[] bytesToken = responseVS.getMessageBytes();
                timeStampToken = new TimeStampToken(
                        new CMSSignedData(bytesToken));
                X509Certificate timeStampCert = ACH.getSimulationContext().getTimeStampServerCert();
                SignerInformationVerifier timeStampSignerInfoVerifier = new 
                        JcaSimpleSignerInfoVerifierBuilder().build(timeStampCert); 
                timeStampToken.validate(timeStampSignerInfoVerifier);
                if(smimeMessage != null) smimeMessage.setTimeStampToken(timeStampToken);
                done.set(true);
            } else if(ResponseVS.SC_ERROR_TIMESTAMP == responseVS.getStatusCode()) {
                if(numAttemp < numMaxAttempts) {
                    ++numAttemp;
                    log.debug("Error getting timestamp - attemp: " + numAttemp);
                } else done.set(true);
            } else done.set(true);
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
        //log.debug(" digestTokenStr: " + digestTokenStr);
        return digestToken;
    }
    
    public SMIMEMessageWrapper getSmimeMessage() {
        return smimeMessage;
    }

}