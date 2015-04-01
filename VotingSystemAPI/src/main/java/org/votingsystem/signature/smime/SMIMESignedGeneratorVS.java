package org.votingsystem.signature.smime;

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
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.StringUtils;

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
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class SMIMESignedGeneratorVS {

    private static Logger log = Logger.getLogger(SMIMESignedGeneratorVS.class.getSimpleName());
    
    private SMIMESignedGenerator smimeSignedGenerator = null;
    private PrivateKey key;
    private Certificate[] chain;
    private Store jcaCertStore;
    private SignerInfoGenerator signerInfoGenerator;
    private JcaSimpleSignerInfoGeneratorBuilder jcaSignerInfoGeneratorBuilder;
    private String signMechanism;
    
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
    	log.info("init");
        this.signMechanism = signMechanism;
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
        jcaSignerInfoGeneratorBuilder = new JcaSimpleSignerInfoGeneratorBuilder().setProvider(ContextVS.PROVIDER);
        jcaSignerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        signerInfoGenerator = jcaSignerInfoGeneratorBuilder.build(signMechanism, key, (X509Certificate)chain[0]);
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
        log.info("SignedMailGenerator");
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

    public SMIMEMessage getSMIME(String fromUser, String toUser, String textToSign, String subject,
             Header... headers) throws Exception {
        if (subject == null) throw new IllegalArgumentException("Subject null");
        if (textToSign == null) throw new IllegalArgumentException("Content null");
        MimeBodyPart msg = new MimeBodyPart();
        msg.setText(textToSign);
        MimeMultipart mimeMultipart = smimeSignedGenerator.generate(msg,ContextVS.DEFAULT_SIGNED_FILE_NAME);
        SMIMEMessage smimeMessage = new SMIMEMessage(mimeMultipart, headers);
        fromUser = StringUtils.getNormalized(fromUser);
        toUser = StringUtils.getNormalized(toUser);
        if(fromUser != null) smimeMessage.setFrom(new InternetAddress(fromUser));
        if(toUser != null) smimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(toUser));
        smimeMessage.setSubject(subject);
        return smimeMessage;
    }

    public synchronized SMIMEMessage getSMIMEMultiSigned(String fromUser, String toUser,
              SMIMEMessage smimeMessage, String subject) throws Exception {
 		 MimeMultipart mimeMultipart = (MimeMultipart)smimeMessage.getContent();
 		 MimeBodyPart bodyPart = (MimeBodyPart) mimeMultipart.getBodyPart(0);    	 
    	 SMIMESignedGenerator smimeSignedGenerator = new SMIMESignedGenerator();
         smimeSignedGenerator.addSignerInfoGenerator(jcaSignerInfoGeneratorBuilder.build(
                 signMechanism, key, (X509Certificate)chain[0]));
         // add our pool of certs and cerls (if any) to go with the signature
         smimeSignedGenerator.addCertificates(jcaCertStore);
         smimeSignedGenerator.addSigners(smimeMessage.getSmimeSigned().getSignerInfos());
         smimeSignedGenerator.addAttributeCertificates(smimeMessage.getSmimeSigned().getAttributeCertificates());
         smimeSignedGenerator.addCertificates(smimeMessage.getSmimeSigned().getCertificates());
         smimeSignedGenerator.addCRLs(smimeMessage.getSmimeSigned().getCRLs());
         smimeSignedGenerator.getGeneratedDigests();
         mimeMultipart = smimeSignedGenerator.generate(bodyPart, ContextVS.MULTISIGNED_FILE_NAME);
         smimeMessage = new SMIMEMessage(mimeMultipart);
         if(subject != null) smimeMessage.setSubject(subject);
         fromUser = StringUtils.getNormalized(fromUser);
         toUser = StringUtils.getNormalized(toUser);
         if(fromUser != null) smimeMessage.setFrom(new InternetAddress(fromUser));
         if(toUser != null) smimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(toUser));
         return smimeMessage;
    }
    
}
