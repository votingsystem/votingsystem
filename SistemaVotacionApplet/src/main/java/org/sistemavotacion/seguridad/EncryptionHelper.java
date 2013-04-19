package org.sistemavotacion.seguridad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.Recipient;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class EncryptionHelper {

    private static Logger logger = LoggerFactory.getLogger(EncryptionHelper.class); 

    public static File encryptSMIMEFile(File fileToEncrypt, 
                    X509Certificate receiverCert) throws Exception {
        logger.debug(" - encryptSMIMEFile");
        /* Create the encrypter */
        SMIMEMessageWrapper msgToEncrypt = 
                new SMIMEMessageWrapper(null, fileToEncrypt);
        SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
        encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
                        receiverCert).setProvider("BC"));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encrypter.generate(msgToEncrypt,
                new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
        // Create a new MimeMessage that contains the encrypted and signed content
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encryptedPart.writeTo(out);

        MimeMessage encryptedMessage = new MimeMessage(null,
                new ByteArrayInputStream(out.toByteArray()));

        /* Set all original MIME headers in the encrypted message */
        Enumeration headers = msgToEncrypt.getAllHeaderLines();
        while (headers.hasMoreElements()) {
            String headerLine = (String)headers.nextElement();
            logger.debug("headerLine: " + headerLine);
            /* 
             * Make sure not to override any content-* headers from the
             * original message
             */
            if (!Strings.toLowerCase(headerLine).startsWith("content-")) {
                encryptedMessage.addHeaderLine(headerLine);
            }
        }
        SignerInformationStore  signers = 
                        msgToEncrypt.getSmimeSigned().getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        byte[] digestBytes = it.next().getContentDigest();//method can only be called after verify.
        String digestStr = new String(Base64.encode(digestBytes));
        encryptedMessage.addHeaderLine("SignedMessageDigest: " + digestStr);
        encryptedMessage.writeTo(new FileOutputStream(fileToEncrypt));
                return fileToEncrypt;
    }

    public static File encryptText(byte[] text, File encryptedFile, 
                    X509Certificate receiverCert) throws Exception {
        logger.debug(" - encryptText(...) - ");
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setText(new String(text));
        // set the Date: header
        //mimeMessage.setSentDate(new Date());
        SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
        encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
                receiverCert).setProvider("BC"));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encrypter.generate(mimeMessage,
                new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
        encryptedPart.writeTo(new FileOutputStream(encryptedFile));
                return encryptedFile;
    }


    public static File encryptFile(File fileToEncrypt, File encryptedFile,
                    X509Certificate receiverCert) throws Exception {
        logger.debug(" - encryptFile(...)");
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage mimeMessage = new MimeMessage(session);
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        FileDataSource fds = new FileDataSource(fileToEncrypt);
        mimeBodyPart.setDataHandler(new DataHandler(fds));
        mimeBodyPart.setFileName(fds.getName());
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);
        mimeMessage.setContent(multipart);
        // set the Date: header
        //mimeMessage.setSentDate(new Date());
        SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
        encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
                        receiverCert).setProvider("BC"));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encrypter.generate(mimeMessage,
                new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
        encryptedPart.writeTo(new FileOutputStream(encryptedFile));
                return encryptedFile;
    }
   	
    /**
    * Method to decrypt SMIME signed messages
    */
   public static SMIMEMessageWrapper decryptSMIMEMessage(
           byte[] encryptedMessageBytes,X509Certificate receiverCert, 
            PrivateKey receiverPrivateKey) throws Exception {
        logger.debug("decryptSMIMEMessage(...) ");
        InputStream inputStream = new ByteArrayInputStream(decryptMessage(
                encryptedMessageBytes, receiverCert, receiverPrivateKey));
        return new SMIMEMessageWrapper(null, inputStream, null);
   }
	
	
    /**
     * helper method to decrypt SMIME signed messages
     */
    public static byte[] decryptMessage(
            byte[] encryptedMessageBytes, X509Certificate receiverCert, 
            PrivateKey receiverPrivateKey) throws Exception {
        logger.debug("decryptMessage(...) ");
        RecipientId recId = new JceKeyTransRecipientId(receiverCert);
        Recipient recipient = new JceKeyTransEnvelopedRecipient(
                receiverPrivateKey).setProvider("BC");
        MimeMessage msg = new MimeMessage(null, 
                        new ByteArrayInputStream(encryptedMessageBytes));
        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);

        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation        recipientInfo = recipients.get(recId);

        /*RecipientId recipientRID = null;
        if(recipient.getRID() != null) {
                recipientRID = recipient.getRID();
                log.debug(" -- recipientRID.getSerialNumber(): " + recipientRID.getSerialNumber());
                if(recipient.getRID().getCertificate() != null) {
                        log.debug(" -- recipient: " + recipient.getRID().getCertificate().getSubjectDN().toString());
                } else log.debug(" -- recipient.getRID().getCertificate() NULL");
        } else log.debug(" -- getRID NULL");
        MimeBodyPart res = SMIMEUtil.toMimeBodyPart(
                 recipient.getContent(new JceKeyTransEnvelopedRecipient(serverPrivateKey).setProvider("BC")));*/
        byte[] messageBytes = recipientInfo.getContent(recipient);
        return messageBytes;
    }
	
    /**
     * Method to decrypt files attached to SMIME (not signed) messages 
     */
    public static byte[] decryptFile (byte[] encryptedFile, 
            X509Certificate decryptCert, PrivateKey decryptPrivateKey) 
            throws Exception {
        logger.debug("decryptFile(...) ");
        RecipientId recId = new JceKeyTransRecipientId(decryptCert);
        Recipient recipient = new JceKeyTransEnvelopedRecipient(
                        decryptPrivateKey).setProvider("BC");
        MimeMessage msg = new MimeMessage(
                        null, new ByteArrayInputStream(encryptedFile));
        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
        RecipientInformationStore recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation recipientInfo = recipients.get(recId);
        RecipientId recipientId = null;
        if(recipientInfo.getRID() != null) {
            recipientId = recipientInfo.getRID();
            logger.debug("decryptFile(...)  #### recipientId.getSerialNumber(): " 
                            + recipientId.getSerialNumber());
        }
        MimeBodyPart res = SMIMEUtil.toMimeBodyPart(recipientInfo.getContent(recipient));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        res.writeTo(baos);
        MimeMessage mimeMessage = new MimeMessage(null,
                new ByteArrayInputStream(baos.toByteArray()));
        return (byte[])mimeMessage.getContent();
    }
    
	
}
