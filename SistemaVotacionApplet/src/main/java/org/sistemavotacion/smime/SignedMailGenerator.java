package org.sistemavotacion.smime;

import static org.sistemavotacion.Contexto.*;

import java.io.ByteArrayOutputStream;
import java.security.cert.CertificateEncodingException;
import javax.mail.Header;
import org.bouncycastle.operator.OperatorCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.util.Store;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class SignedMailGenerator {

    private static Logger logger = LoggerFactory.getLogger(SignedMailGenerator.class);
    
    public enum Type {USER, ACESS_CONTROL, CONTROL_CENTER}
        
    private SMIMESignedGenerator smimeSignedGenerator = null;
    // Get a Session object and create the mail message
    private static Properties props = System.getProperties();
    private static Session session = Session.getDefaultInstance(props, null);
    
    public SignedMailGenerator(byte[] keyStoreBytes, String keyAlias, 
            char[] password, String signatureMechanism) throws Exception {
    	KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
        PrivateKey key = (PrivateKey)keyStore.getKey(keyAlias, password);
        Certificate[] chain = keyStore.getCertificateChain(keyAlias);
        init(key, chain, signatureMechanism);
    }
    
    public SignedMailGenerator(KeyStore keyStore, String keyAlias, 
            char[] password, String signatureMechanism) throws Exception {
        PrivateKey key = (PrivateKey)keyStore.getKey(keyAlias, password);
        Certificate[] chain = keyStore.getCertificateChain(keyAlias);
        init(key, chain, signatureMechanism);
    }
    
    public SignedMailGenerator(PrivateKey key, Certificate[] chain, 
            String signatureMechanism) throws Exception {
        init(key, chain, signatureMechanism);
    }
    
    private void init (PrivateKey key, Certificate[] chain, String signatureMechanism) 
            throws CertificateEncodingException, OperatorCreationException {
        logger.debug("init");                                
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
        signerInfoGeneratorBuilder.setProvider(SIGN_PROVIDER);
        signerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = signerInfoGeneratorBuilder.build(
                signatureMechanism, key, (X509Certificate)chain[0]);
        smimeSignedGenerator.addSignerInfoGenerator(signerInfoGenerator);
        // add our pool of certs and cerls (if any) to go with the signature
        smimeSignedGenerator.addCertificates(certs);
    }
    
    public File genFile(String fromUser, String toUser, String textoAFirmar, 
            String asunto, Header header, Type signerType, File output) throws Exception {
        MimeMessage body = gen(
                fromUser, toUser, textoAFirmar, asunto, header, signerType);
        body.writeTo(new FileOutputStream(output));
        return output;
    }
          
    public String genString(String fromUser, String toUser, String textoAFirmar, 
            String asunto, Header header, Type signerType) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MimeMessage body = gen(
                fromUser, toUser, textoAFirmar, asunto, header, signerType);
        body.writeTo(baos);
        return new String(baos.toByteArray());
    }
    
    public MimeMessage genMimeMessage(String fromUser, String toUser, String textoAFirmar, 
            String asunto, Header header, Type signerType) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MimeMessage body = gen(
                fromUser, toUser, textoAFirmar, asunto, header, signerType);
        return body;
    }
    
    private MimeMessage gen(String fromUser, String toUser, String textoAFirmar, 
            String asunto, Header header, Type signerType) throws Exception {
        if (asunto == null) asunto = "";
        if (textoAFirmar == null) textoAFirmar = "";
        MimeBodyPart msg = new MimeBodyPart();
        msg.setText(textoAFirmar);
        MimeMultipart mimeMultipart = smimeSignedGenerator.generate(msg, 
                signerType.toString() + ".p7s");
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
    

     public MimeMultipart genMimeMultipart(MimeBodyPart body, 
             SMIMEMessageWrapper dnieMimeMessage, Type type, String signatureMechanism) throws Exception {
         smimeSignedGenerator.addSigners(dnieMimeMessage.getSmimeSigned().getSignerInfos());
         smimeSignedGenerator.addAttributeCertificates(dnieMimeMessage.getSmimeSigned().getAttributeCertificates());
         smimeSignedGenerator.addCertificates(dnieMimeMessage.getSmimeSigned().getCertificates());
         smimeSignedGenerator.addCRLs(dnieMimeMessage.getSmimeSigned().getCRLs());
         smimeSignedGenerator.getGeneratedDigests();
         //MimeMultipart mimeMultipart = smimeSignedGenerator.generate(body, type.toString() + SIGNED_PART_EXTENSION);
        // MimeMultipart mimeMultipart = smimeSignedGenerator.generate(
         //        dnieMimeMessage, SIGN_PROVIDER,
           //      type.toString() + SIGNED_PART_EXTENSION);
         MimeMultipart mimeMultipart = smimeSignedGenerator.generate(body, SIGN_PROVIDER);
         //MimeMultipart mimeMultipart = smimeSignedGenerator.generate(body, SIGN_PROVIDER, signatureMechanism);
         return mimeMultipart;
     }
    
}