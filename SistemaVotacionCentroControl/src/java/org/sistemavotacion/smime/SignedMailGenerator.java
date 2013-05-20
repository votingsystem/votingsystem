package org.sistemavotacion.smime;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.util.Store;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class SignedMailGenerator {

    private static Logger logger = LoggerFactory.getLogger(SignedMailGenerator.class);
    
    public static final String NOMBRE_ARCHIVO_FIRMADO = "EventoEnviado";
    public static final String NOMBRE_ARCHIVO_MULTIFIRMA = "MultiFirma";
    public static final String SIGN_MECHANISM = "SHA256withRSA";
    public static final String PROVIDER = "BC";
    public static final String SIGNED_PART_EXTENSION = ".p7s";
    
    private SMIMESignedGenerator smimeSignedGenerator = null;
    // Get a Session object and create the mail message
    private static Properties props = System.getProperties();
    private static Session session = Session.getDefaultInstance(props, null);
    
    private PrivateKey key;
    private Certificate[] chain;
    private Store jcaCertStore;
    private SignerInfoGenerator signerInfoGenerator;
    
    public SignedMailGenerator(byte[] keyStoreBytes, String keyAlias, char[] password) throws Exception {
    	logger.debug("SignedMailGenerator");                                
        KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
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
        JcaSimpleSignerInfoGeneratorBuilder jcaSignerInfoGeneratorBuilder =  new JcaSimpleSignerInfoGeneratorBuilder();
        jcaSignerInfoGeneratorBuilder = jcaSignerInfoGeneratorBuilder.setProvider(PROVIDER);
        jcaSignerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        signerInfoGenerator = jcaSignerInfoGeneratorBuilder.build(SIGN_MECHANISM, key, (X509Certificate)chain[0]);
        smimeSignedGenerator = new SMIMESignedGenerator();
        smimeSignedGenerator.addSignerInfoGenerator(signerInfoGenerator);
        // add our pool of certs and cerls (if any) to go with the signature
        smimeSignedGenerator.addCertificates(jcaCertStore);
    }

    public MimeMessage genMimeMessage(String fromUser, String toUser, 
    		String textoAFirmar, String asunto, Header header) throws Exception {
        if (asunto == null) asunto = "";
        if (textoAFirmar == null) textoAFirmar = "";
        MimeBodyPart msg = new MimeBodyPart();
        msg.setText(textoAFirmar);
        MimeMultipart mimeMultipart = smimeSignedGenerator.generate(msg, "smime.p7s");
        MimeMessage body = new MimeMessage(session);
        if (header != null) body.setHeader(header.getName(), header.getValue());
        if (fromUser != null && !"".equals(fromUser)) {
        	Address fromUserAddress = new InternetAddress(fromUser);
        	body.setFrom(fromUserAddress);
        }
        if (toUser != null && !"".equals(toUser)) {
        	Address toUserAddress = new InternetAddress(toUser.replace(" ", ""));
        	body.setRecipient(Message.RecipientType.TO, toUserAddress);
        }
        body.setSubject(asunto);
        body.setContent(mimeMultipart, mimeMultipart.getContentType());
        body.saveChanges();
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
         MimeMultipart newMimeMultipart = smimeSignedGenerator.generate(bodyPart, NOMBRE_ARCHIVO_MULTIFIRMA);
         
 		 //Properties props = System.getProperties();
 		 //Session session = Session.getDefaultInstance(props, null);
 		 //MimeMessage multiSignedMessage = new MimeMessage(session);
         /*Session session = Session.getDefaultInstance(null, null);
         MimeMessage multiSignedMessage = new MimeMessage(session);
 		 multiSignedMessage.setSubject(mailSubject);
 		 multiSignedMessage.setContent(newMimeMultipart, newMimeMultipart.getContentType());
 		 multiSignedMessage.saveChanges();
 		 return multiSignedMessage;*/
         smimeMessage.setSubject(mailSubject);
         smimeMessage.setContent(newMimeMultipart, newMimeMultipart.getContentType());
         smimeMessage.saveChanges();
         return smimeMessage;
     }

    
}