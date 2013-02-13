package org.sistemavotacion.smime;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle2.asn1.ASN1EncodableVector;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.cms.SignerInfo;
import org.bouncycastle2.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle2.asn1.smime.SMIMECapability;
import org.bouncycastle2.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle2.cert.jcajce.JcaCertStore;
import org.bouncycastle2.cms.SignerInfoGenerator;
import org.bouncycastle2.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle2.operator.OperatorCreationException;
import org.bouncycastle2.util.Store;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.seguridad.KeyStoreUtil;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class SignedMailGenerator {
    
    public enum Type {USER, ACESS_CONTROL, CONTROL_CENTER}
    
    public static final String NOMBRE_ARCHIVO_FIRMADO = "EventoEnviado";
    public static final String SIGN_PROVIDER = "BC";
    public static final String SIGNED_PART_EXTENSION = ".p7s";
    
    private SMIMESignedGenerator smimeSignedGenerator = null;
    // Get a Session object and create the mail message
    private static Properties props = System.getProperties();
    private static Session session = Session.getDefaultInstance(props, null);
    
    /*

    
    public SignedMailGenerator(InputStream keyStoreInputStream, String keyAlias, char[] password) throws Exception {
    	KeyStore keyStore = KeyStoreUtil.getKeyStoreFromStream(keyStoreInputStream, password);
        PrivateKey key = (PrivateKey)keyStore.getKey(keyAlias, password);
        Certificate[] chain = keyStore.getCertificateChain(keyAlias);
        init(key, chain);
    }
    
    public SignedMailGenerator(KeyStore keyStore, String keyAlias, char[] password) throws Exception {
        PrivateKey key = (PrivateKey)keyStore.getKey(keyAlias, password);
        Certificate[] chain = keyStore.getCertificateChain(keyAlias);
        init(key, chain);
    }
    
    public SignedMailGenerator(PrivateKey key, Certificate[] chain) throws Exception {
        init(key, chain);
    }*/
    public SignedMailGenerator(byte[] keyStoreBytes, String keyAlias, 
    		char[] password, String signMechanism) throws Exception {
    	KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
        PrivateKey key = (PrivateKey)keyStore.getKey(keyAlias, password);
        Certificate[] chain = keyStore.getCertificateChain(keyAlias);
        init(key, chain, signMechanism);
    }
    
    public SignedMailGenerator(PrivateKey privateKey,
			X509Certificate[] arrayCerts, String signMechanism) 
					throws CertificateEncodingException, OperatorCreationException {
    	init(privateKey, arrayCerts, signMechanism);
	}

	private void init (PrivateKey key, Certificate[] chain, String signatureMechanism) 
            throws CertificateEncodingException, OperatorCreationException {
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
        // add a signer to the generator - this specifies we are using SHA1 and
        // adding the smime attributes above to the signed attributes that
        // will be generated as part of the signature. The encryption algorithm
        // used is taken from the key - in this RSA with PKCS1Padding
        JcaSimpleSignerInfoGeneratorBuilder jcaSignerInfoGeneratorBuilder =  new JcaSimpleSignerInfoGeneratorBuilder();
        jcaSignerInfoGeneratorBuilder = jcaSignerInfoGeneratorBuilder.setProvider(SIGN_PROVIDER);
        jcaSignerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = jcaSignerInfoGeneratorBuilder.build(
        		signatureMechanism, key, (X509Certificate)chain[0]);

        smimeSignedGenerator.addSignerInfoGenerator(signerInfoGenerator);
        // add our pool of certs and cerls (if any) to go with the signature
        smimeSignedGenerator.addCertificates(certs);
    }
	
    
    public File genFile(String fromUser, String toUser, String textoAFirmar, 
            String asunto, Header header, Type signerType, File outputFile) throws Exception {
        MimeMessage body = gen(
                fromUser, toUser, textoAFirmar, asunto, header, signerType);
        body.writeTo(new FileOutputStream(outputFile));
        return outputFile;
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
    
    public MimeMessage gen(String fromUser, String toUser, String textoAFirmar, 
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
    
    public SMIMESignedGenerator getSMIMESignedGenerator() {
    	return smimeSignedGenerator;
    }
   
     public MimeMultipart genMimeMultipart(MimeBodyPart body, 
             SMIMEMessageWrapper dnieMimeMessage, Type type) throws Exception {
         smimeSignedGenerator.addSigners(dnieMimeMessage.getSmimeSigned().getSignerInfos());
         smimeSignedGenerator.addAttributeCertificates(dnieMimeMessage.getSmimeSigned().getAttributeCertificates());
         smimeSignedGenerator.addCertificates(dnieMimeMessage.getSmimeSigned().getCertificates());
         smimeSignedGenerator.addCRLs(dnieMimeMessage.getSmimeSigned().getCRLs());
         smimeSignedGenerator.getGeneratedDigests();
         //MimeMultipart mimeMultipart = smimeSignedGenerator.generate(body, type.toString() + SIGNED_PART_EXTENSION);
        // MimeMultipart mimeMultipart = smimeSignedGenerator.generate(
         //        dnieMimeMessage, SIGN_PROVIDER,
           //      type.toString() + SIGNED_PART_EXTENSION);
         MimeMultipart mimeMultipart = smimeSignedGenerator.generate(
        		 body, SIGN_PROVIDER, Aplicacion.SIGNATURE_ALGORITHM);
         return mimeMultipart;
     }
    
}