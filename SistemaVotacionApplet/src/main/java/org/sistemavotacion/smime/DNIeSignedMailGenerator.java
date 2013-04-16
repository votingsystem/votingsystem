package org.sistemavotacion.smime;

import java.io.ByteArrayInputStream;
import static org.sistemavotacion.Contexto.*;
import java.io.ByteArrayOutputStream;
import javax.mail.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
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
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.Strings;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.util.FileUtils;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class DNIeSignedMailGenerator {

    private static Logger logger = LoggerFactory.getLogger(DNIeSignedMailGenerator.class); 
          
    public static File genFile(String fromUser, String toUser, String textoAFirmar, 
            char[] password, String asunto, File resultado) throws Exception {
    	return genFile(fromUser, toUser, textoAFirmar, password, asunto, null, resultado);
    }
    
    public static File genFile(String fromUser, String toUser, String textoAFirmar, 
            char[] password, String asunto, Header header, File resultado) throws Exception {
        MimeMessage body = gen(fromUser, toUser, textoAFirmar,  password, asunto, header);
        
        body.writeTo(new FileOutputStream(resultado));
        

        
        SMIMEMessageWrapper smimeMessageReq = new SMIMEMessageWrapper(null, resultado);
        
        logger.debug("===== Encriptando mensaje");
        /* Create the encrypter */
        File controlAccesoPEMFile = new File("/home/jgzornoza/temp/controlAcceso.pem");
        byte[] pemBytes = FileUtils.getBytesFromFile(controlAccesoPEMFile);
        Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(pemBytes);
        X509Certificate cert = certChain.iterator().next();
        
        SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
        
        
        JceKeyTransRecipientInfoGenerator recipientInfoGenerator = new JceKeyTransRecipientInfoGenerator(cert);
        
        
        encrypter.addRecipientInfoGenerator(recipientInfoGenerator.setProvider("BC"));

        /* Encrypt the message */
        MimeBodyPart encryptedPart = encrypter.generate(smimeMessageReq,
            new JceCMSContentEncryptorBuilder(CMSAlgorithm.RC2_CBC).setProvider("BC").build());
        
        

        /*
         * Create a new MimeMessage that contains the encrypted and signed
         * content
         */
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encryptedPart.writeTo(out);

        MimeMessage encryptedMessage = new MimeMessage(null,
                new ByteArrayInputStream(out.toByteArray()));

        /* Set all original MIME headers in the encrypted message */
        Enumeration headers = body.getAllHeaderLines();
        while (headers.hasMoreElements()) {
            String headerLine = (String)headers.nextElement();
            /*
             * Make sure not to override any content-* headers from the
             * original message
             */
            if (!Strings.toLowerCase(headerLine).startsWith("content-"))  {
                encryptedMessage.addHeaderLine(headerLine);
            }
        }
        encryptedMessage.writeTo(new FileOutputStream(new File("/home/jgzornoza/temp/encripteMessage")));
        
        
        return resultado;
    }
          
    public static String genString(String fromUser, String toUser, String textoAFirmar, 
            char[] password, String asunto, Header header) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MimeMessage body = gen(fromUser, toUser, textoAFirmar,  password, asunto, header);
        body.writeTo(baos);
        return new String(baos.toByteArray());
    }
    
     private static MimeMessage gen(String fromUser, String toUser, String textoAFirmar, 
            char[] password, String asunto, Header header) throws Exception {
        if (asunto == null) asunto = "";
        if (textoAFirmar == null) textoAFirmar = "";
        ASN1EncodableVector         signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector       caps = new SMIMECapabilityVector();
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        caps.addCapability(SMIMECapability.dES_CBC);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        SMIMESignedGenerator gen = new SMIMESignedGenerator();
        DNIeContentSigner dnieContentSigner = new DNIeContentSigner(DNIe_SIGN_MECHANISM);
        try {
            dnieContentSigner.setPkcs11Session(DNIeSessionHelper.getSession(password, DNIe_SESSION_MECHANISM));
        } catch(Exception ex) {
            if ("CKR_DEVICE_ERROR".equals(ex.getMessage()) || 
                    "CKR_CRYPTOKI_ALREADY_INITIALIZED".equals(ex.getMessage())) {
                logger.debug("Nuevo intento de obtención de sesión PKCS11 con el DNI");
                Thread.sleep(3000);
                dnieContentSigner.setPkcs11Session(DNIeSessionHelper.getSession(password, DNIe_SESSION_MECHANISM));
            } else throw ex;
        }
        SimpleSignerInfoGeneratorBuilder dnieSignerInfoGeneratorBuilder =  new SimpleSignerInfoGeneratorBuilder();
        dnieSignerInfoGeneratorBuilder = dnieSignerInfoGeneratorBuilder.setProvider(PROVIDER);
        dnieSignerInfoGeneratorBuilder.setSignedAttributeGenerator(new AttributeTable(signedAttrs));
        SignerInfoGenerator signerInfoGenerator = dnieSignerInfoGeneratorBuilder.build(
                DNIe_SIGN_MECHANISM, null, DNIeSessionHelper.getCertificadoUsuario(), dnieContentSigner);
        List certList = new ArrayList();
        certList.add(DNIeSessionHelper.getCertificadoCA());
        certList.add(DNIeSessionHelper.getCertificadoIntermedio());            
        certList.add(DNIeSessionHelper.getCertificadoUsuario());
        gen.addSignerInfoGenerator(signerInfoGenerator);
        // create a CertStore containing the certificates we want carried
        // in the signature
        Store certs = new JcaCertStore(certList);
        // add our pool of certs and cerls (if any) to go with the signature
        gen.addCertificates(certs);
        // create the base for our message
        MimeBodyPart msg = new MimeBodyPart();
        msg.setText(textoAFirmar);
        // extract the multipart object from the SMIMESigned object.
        MimeMultipart mimeMultipart = gen.generate(msg, "");
        
        // Get a Session object and create the mail message
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        String usuario = null;
        if (Contexto.getUsuario() != null) usuario = Contexto.getUsuario().getNif();
        Address fromUserAddress = new InternetAddress(usuario);
        Address toUserAddress = new InternetAddress(toUser.replace(" ", ""));
        MimeMessage body = new MimeMessage(session);
        if (header != null) body.setHeader(header.getName(), header.getValue());
        body.setFrom(fromUserAddress);
        body.setRecipient(Message.RecipientType.TO, toUserAddress);
        body.setSubject(asunto);
        body.setContent(mimeMultipart, mimeMultipart.getContentType());
        body.saveChanges();
        //sessionHelper.closeSession();
        return body;
    }
}
