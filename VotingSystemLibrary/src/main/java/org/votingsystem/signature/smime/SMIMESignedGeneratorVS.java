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
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class SMIMESignedGeneratorVS {

    private static Logger log = Logger.getLogger(SMIMESignedGeneratorVS.class);
    
    private SMIMESignedGenerator smimeSignedGenerator = null;
    private PrivateKey key;
    private Certificate[] chain;
    private Store jcaCertStore;
    private SignerInfoGenerator signerInfoGenerator;
    
    public SMIMESignedGeneratorVS(byte[] keyStoreBytes, String keyAlias,
                                  char[] password, String signMechanism) throws Exception {
        KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
        init(keyStore, keyAlias, password, signMechanism);
    }
    
    public SMIMESignedGeneratorVS(KeyStore keyStore, String keyAlias,
                                  char[] password, String signMechanism) throws Exception {
    	init(keyStore, keyAlias, password, signMechanism);
    }
    
    private void init(KeyStore keyStore, String keyAlias, 
    		char[] password, String signMechanism) throws Exception {
    	log.debug("init");
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
    
    public SMIMESignedGeneratorVS(PrivateKey key, Certificate[] chain, String signatureMechanism)
            throws CertificateEncodingException, OperatorCreationException {
        this(key, Arrays.asList(chain), signatureMechanism);
    }

    public SMIMESignedGeneratorVS(PrivateKey key, List<Certificate> chain, String signatureMechanism)
            throws CertificateEncodingException, OperatorCreationException {
        log.debug("SignedMailGenerator");
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        //create some smime capabilities in case someone wants to respond
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        Store certs = new JcaCertStore(chain);
        smimeSignedGenerator = new SMIMESignedGenerator();
        SimpleSignerInfoGeneratorBuilder signerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();
        signerInfoGeneratorBuilder.setProvider(ContextVS.PROVIDER);
        signerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = signerInfoGeneratorBuilder.build(
                signatureMechanism, key, (X509Certificate)chain.iterator().next());
        smimeSignedGenerator.addSignerInfoGenerator(signerInfoGenerator);
        // add our pool of certs and cerls (if any) to go with the signature
        smimeSignedGenerator.addCertificates(certs);
    }

    public SMIMEMessage getSMIME(String fromUser, String toUser,
            String textToSign, String subject, Header... headers) throws Exception {
        if (subject == null) throw new IllegalArgumentException("Subject null");
        if (textToSign == null) throw new IllegalArgumentException("Content null");
        MimeBodyPart msg = new MimeBodyPart();
        msg.setText(textToSign);
        MimeMultipart mimeMultipart = smimeSignedGenerator.generate(
                msg,ContextVS.DEFAULT_SIGNED_FILE_NAME);
        SMIMEMessage body = new SMIMEMessage(ContextVS.MAIL_SESSION);
        if (headers != null) {
        	for(Header header : headers) {
        		 if (header != null) body.setHeader(header.getName(), header.getValue());
        	}
        }
        if (fromUser != null && !fromUser.trim().isEmpty()) {
        	Address fromUserAddress = new InternetAddress(fromUser);
        	body.setFrom(fromUserAddress);
        }
        if (toUser != null && !toUser.trim().isEmpty()) {
        	Address toUserAddress = new InternetAddress(toUser.replace(" ", ""));
        	body.setRecipient(Message.RecipientType.TO, toUserAddress);
        }
        body.setSubject(subject);
        body.setContent(mimeMultipart, mimeMultipart.getContentType());
        body.updateChanges();
        return body;
    }
     
     public synchronized SMIMEMessage getSMIMEMultiSigned(SMIMEMessage smimeMessage, String subject) throws Exception {
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
 		 multiSignedMessage.setSubject(subject);
 		 multiSignedMessage.setContent(newMimeMultipart, newMimeMultipart.getContentType());
 		 multiSignedMessage.saveChanges();
 		 return multiSignedMessage;*/
         if(subject != null) smimeMessage.setSubject(subject);
         smimeMessage.setContent(newMimeMultipart, newMimeMultipart.getContentType());
         smimeMessage.updateChanges();
         return smimeMessage;
     }

    
}
