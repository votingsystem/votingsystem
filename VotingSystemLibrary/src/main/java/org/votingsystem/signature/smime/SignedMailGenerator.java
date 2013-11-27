package org.votingsystem.signature.smime;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.util.KeyStoreUtil;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignedMailGenerator {

    private static Logger logger = Logger.getLogger(SignedMailGenerator.class);
    
    private SMIMESignedGenerator smimeSignedGenerator = null;
    private PrivateKey key;
    private Certificate[] chain;
    private Store jcaCertStore;
    private SignerInfoGenerator signerInfoGenerator;
    
    public SignedMailGenerator(byte[] keyStoreBytes, String keyAlias, 
    		char[] password, String signMechanism) throws Exception {                               
        KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
        init(keyStore, keyAlias, password, signMechanism);
    }
    
    public SignedMailGenerator(KeyStore keyStore, String keyAlias, 
    		char[] password, String signMechanism) throws Exception {                             
    	init(keyStore, keyAlias, password, signMechanism);
    }
    
    private void init(KeyStore keyStore, String keyAlias, 
    		char[] password, String signMechanism) throws Exception {
    	logger.debug("init");                                
        key = (PrivateKey)keyStore.getKey(keyAlias, password);
        chain = keyStore.getCertificateChain(keyAlias);
        jcaCertStore = new JcaCertStore( Arrays.asList(chain));
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        //create some smime capabilities in case someone wants to respond        
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));        
        // add a signer to the generator - this specifies we are using SHA1 and
        // adding the smime attributes above to the signed attributes that
        // will be generated as part of the signature. The encryption algorithm
        // used is taken from the key - in this RSA with PKCS1Padding
        JcaSimpleSignerInfoGeneratorBuilder jcaSignerInfoGeneratorBuilder =  
        		new JcaSimpleSignerInfoGeneratorBuilder();
        jcaSignerInfoGeneratorBuilder = jcaSignerInfoGeneratorBuilder.setProvider(
        		ContextVS.PROVIDER);
        jcaSignerInfoGeneratorBuilder.setSignedAttributeGenerator(
        		new AttributeTable(signedAttrs));
        signerInfoGenerator = jcaSignerInfoGeneratorBuilder.build(
        		signMechanism, key, (X509Certificate)chain[0]);
        smimeSignedGenerator = new SMIMESignedGenerator();
        smimeSignedGenerator.addSignerInfoGenerator(signerInfoGenerator);
        // add our pool of certs and cerls (if any) to go with the signature
        smimeSignedGenerator.addCertificates(jcaCertStore);
    }
    
    public SignedMailGenerator (PrivateKey key, Certificate[] chain, String signatureMechanism) 
            throws CertificateEncodingException, OperatorCreationException {
        logger.debug("SignedMailGenerator");                                
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        //create some smime capabilities in case someone wants to respond        
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        List certList = Arrays.asList(chain);
        Store certs = new JcaCertStore(certList);
        smimeSignedGenerator = new SMIMESignedGenerator();
        SimpleSignerInfoGeneratorBuilder signerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();
        signerInfoGeneratorBuilder.setProvider(ContextVS.PROVIDER);
        signerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = signerInfoGeneratorBuilder.build(
                signatureMechanism, key, (X509Certificate)chain[0]);
        smimeSignedGenerator.addSignerInfoGenerator(signerInfoGenerator);
        // add our pool of certs and cerls (if any) to go with the signature
        smimeSignedGenerator.addCertificates(certs);
    }

    public SMIMEMessageWrapper genMimeMessage(String fromUser, String toUser, 
            String textToSign, String subject, Header... headers) throws Exception {
        if (subject == null) throw new Exception("Subject null");
        if (textToSign == null) throw new Exception("Content null");
        MimeBodyPart msg = new MimeBodyPart();
        msg.setText(textToSign);
        MimeMultipart mimeMultipart = smimeSignedGenerator.generate(
                msg,ContextVS.DEFAULT_SIGNED_FILE_NAME);
        SMIMEMessageWrapper body = new SMIMEMessageWrapper(ContextVS.MAIL_SESSION);
        if (headers != null) {
        	for(Header header : headers) {
        		 if (header != null) body.setHeader(header.getName(), header.getValue());
        	}
        }
        if (fromUser != null && !"".equals(fromUser)) {
        	Address fromUserAddress = new InternetAddress(fromUser);
        	body.setFrom(fromUserAddress);
        }
        if (toUser != null && !"".equals(toUser)) {
        	Address toUserAddress = new InternetAddress(toUser.replace(" ", ""));
        	body.setRecipient(Message.RecipientType.TO, toUserAddress);
        }
        body.setSubject(subject);
        body.setContent(mimeMultipart, mimeMultipart.getContentType());
        body.updateChanges();
        return body;
    }
     
     public synchronized SMIMEMessageWrapper genMultiSignedMessage(
    		 SMIMEMessageWrapper smimeMessage, String mailSubject) throws Exception {
 		 MimeMultipart mimeMultipart = (MimeMultipart)smimeMessage.getContent();
 		 MimeBodyPart bodyPart = (MimeBodyPart) mimeMultipart.getBodyPart(0);    	 
    	 SMIMESignedGenerator smimeSignedGenerator = new SMIMESignedGenerator();
         smimeSignedGenerator.addSignerInfoGenerator(signerInfoGenerator);
         // add our pool of certs and cerls (if any) to go with the signature
         smimeSignedGenerator.addCertificates(jcaCertStore);
         smimeSignedGenerator.addSigners(smimeMessage.getSmimeSigned().getSignerInfos());
         smimeSignedGenerator.addAttributeCertificates(smimeMessage.getSmimeSigned().getAttributeCertificates());
         smimeSignedGenerator.addCertificates(smimeMessage.getSmimeSigned().getCertificates());
         smimeSignedGenerator.addCRLs(smimeMessage.getSmimeSigned().getCRLs());
         smimeSignedGenerator.getGeneratedDigests();
         MimeMultipart newMimeMultipart = smimeSignedGenerator.generate(bodyPart, ContextVS.MULTISIGNED_FILE_NAME);
         /*MimeMessage multiSignedMessage = new MimeMessage(ContextVS.MAIL_SESSION);
 		 multiSignedMessage.setSubject(mailSubject);
 		 multiSignedMessage.setContent(newMimeMultipart, newMimeMultipart.getContentType());
 		 multiSignedMessage.saveChanges();
 		 return multiSignedMessage;*/
         smimeMessage.setSubject(mailSubject);
         smimeMessage.setContent(newMimeMultipart, newMimeMultipart.getContentType());
         smimeMessage.updateChanges();
         return smimeMessage;
     }

    
}
