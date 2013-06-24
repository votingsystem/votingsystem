package org.sistemavotacion.callable;

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
import org.sistemavotacion.Contexto;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.modelo.Respuesta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class MessageTimeStamper implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(MessageTimeStamper.class);
    
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
        
    @Override public Respuesta call() throws Exception {
        int numAttemp = 0;
        AtomicBoolean done = new AtomicBoolean(false);
        Respuesta respuesta = null;
        while(!done.get()) {
            respuesta = Contexto.INSTANCE.getHttpHelper().sendByteArray(
                timeStampRequest.getEncoded(), "timestamp-query", 
                Contexto.INSTANCE.getURLTimeStampServer());
            if(Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                byte[] bytesToken = respuesta.getMessageBytes();
                timeStampToken = new TimeStampToken(
                        new CMSSignedData(bytesToken));
                X509Certificate timeStampCert = Contexto.INSTANCE.getTimeStampServerCert();
                SignerInformationVerifier timeStampSignerInfoVerifier = new 
                        JcaSimpleSignerInfoVerifierBuilder().build(timeStampCert); 
                timeStampToken.validate(timeStampSignerInfoVerifier);
                if(smimeMessage != null) smimeMessage.setTimeStampToken(timeStampToken);
                done.set(true);
            } else if(Respuesta.SC_ERROR_TIMESTAMP == respuesta.getCodigoEstado()) {
                if(numAttemp < numMaxAttempts) {
                    ++numAttemp;
                    logger.debug("Error getting timestamp - attemp: " + numAttemp);
                } else done.set(true);
            } else done.set(true);
        }
        return respuesta;
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
    
    public SMIMEMessageWrapper getSmimeMessage() {
        return smimeMessage;
    }

}