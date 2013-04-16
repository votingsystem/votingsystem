package pruebas;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import javax.mail.BodyPart;
import javax.mail.Multipart;

import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.seguridad.KeyStoreUtil;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static pruebas.CreateEncryptedMessage.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class ReadEncryptedMessage {
    
    private static Logger logger = (Logger) LoggerFactory.getLogger(ReadEncryptedMessage.class);  
    
    
    public static void main(String args[]) throws Exception {
        Contexto.inicializar();
        //readEncryptedMessage();
        getPDFFromEncryptedMessage();
    }
    
     public static void readEncryptedMessage() throws Exception {         
        
        byte[] receiverKeyStoreBytes = FileUtils.getBytesFromInputStream(Thread.currentThread().getContextClassLoader()
               .getResourceAsStream(keyStoreFileName));
        KeyStore receiverKeyStore = KeyStoreUtil.getKeyStoreFromBytes(
               receiverKeyStoreBytes, keyStorePassword.toCharArray());
        java.security.cert.Certificate[] chain = 
                receiverKeyStore.getCertificateChain(receiverCertAlias);
        X509Certificate cert = (X509Certificate) chain[0];
        RecipientId     recId = new JceKeyTransRecipientId(cert);

        // Get a Session object with the default properties.
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);

        File encryptedMessageFile = new File(encryptedMessagePath);
        logger.debug(" ------ encryptedMessageFile path:" 
                + encryptedMessageFile.getAbsolutePath());
        MimeMessage msg = new MimeMessage(session, new FileInputStream(
                encryptedMessageFile));

        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
        
        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation        recipient = recipients.get(recId);

        //String contentDigest = new String(recipient.getContentDigest());
        RecipientId recipientRID = null;
        if(recipient.getRID() != null) {
            recipientRID = recipient.getRID();
            logger.debug(" -- recipientRID.getSerialNumber(): " + recipientRID.getSerialNumber());
            if(recipient.getRID().getCertificate() != null) {
                logger.debug(" -- recipient: " + recipient.getRID().getCertificate().getSubjectDN().toString());
            } else logger.debug(" -- recipient.getRID().getCertificate() NULL");
        } else logger.debug(" -- getRID NULL");
        
        MimeBodyPart res = SMIMEUtil.toMimeBodyPart(
                recipient.getContent(new JceKeyTransEnvelopedRecipient(
                (PrivateKey)receiverKeyStore.getKey(receiverCertAlias, 
                keyStorePassword.toCharArray())).setProvider("BC")));

        res.writeTo(System.out);
    }
     
    
    public static File getPDFFromEncryptedMessage() throws Exception {
      byte[] receiverKeyStoreBytes = FileUtils.getBytesFromInputStream(
              Thread.currentThread().getContextClassLoader()
              .getResourceAsStream(keyStoreFileName));
        KeyStore receiverKeyStore = KeyStoreUtil.getKeyStoreFromBytes(
               receiverKeyStoreBytes, keyStorePassword.toCharArray());
        java.security.cert.Certificate[] chain = 
                receiverKeyStore.getCertificateChain(receiverCertAlias);
        X509Certificate cert = (X509Certificate) chain[0];
        RecipientId     recId = new JceKeyTransRecipientId(cert);

        // Get a Session object with the default properties.
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);

        File encryptedMessageFile = new File(encryptedMessagePath);
        logger.debug(" ------ encryptedMessageFile path:" 
                + encryptedMessageFile.getAbsolutePath());
        MimeMessage msg = new MimeMessage(session, new FileInputStream(
                encryptedMessageFile));

        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
        
        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation        recipient = recipients.get(recId);

        //String contentDigest = new String(recipient.getContentDigest());
        RecipientId recipientRID = null;
        if(recipient.getRID() != null) {
            recipientRID = recipient.getRID();
            logger.debug(" -- recipientRID.getSerialNumber(): " + recipientRID.getSerialNumber());
            if(recipient.getRID().getCertificate() != null) {
                logger.debug(" -- recipient: " + recipient.getRID().getCertificate().getSubjectDN().toString());
            } else logger.debug(" -- recipient.getRID().getCertificate() NULL");
        } else logger.debug(" -- getRID NULL");
        
        
        MimeBodyPart res = SMIMEUtil.toMimeBodyPart(
                recipient.getContent(new JceKeyTransEnvelopedRecipient(
                (PrivateKey)receiverKeyStore.getKey(receiverCertAlias, 
                keyStorePassword.toCharArray())).setProvider("BC")));

        logger.debug("res.getContent(): " + res.getContent());
        
        

        
        //message.writeTo(System.out);
        

            InputStream stream = res.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));

            File pdfFile = new File("/home/jgzornoza/pdfFile.pdf");
            FileUtils.copyStreamToFile(stream, pdfFile);
        
        
        
        /*message.writeTo(new FileOutputStream(new File("/home/jgzornoza/emailPDF1")));
        
        
        logger.debug("message.getContent(): " + message.getContent().getClass());
        
        Multipart multipart = (Multipart) message.getContent();
        //logger.debug(multipart.getCount());

        for (int i = 0; i < multipart.getCount(); i++) {
            logger.debug("--------------------" + i);
            //logger.debug(multipart.getContentType());
            BodyPart bodyPart = multipart.getBodyPart(i);
            InputStream stream = bodyPart.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(stream));

            File pdfFile = new File("/home/jgzornoza/pdfFile.pdf");
            FileUtils.copyStreamToFile(stream, pdfFile);
        }*/
        
        /*logger.debug("Subject : " + res.getSubject());
        logger.debug("From : " + message.getFrom()[0]);
        logger.debug("--------------");
        logger.debug("Body : " +  message.getContent());*/
        return null;
    }
    
}
