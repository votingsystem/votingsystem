package org.sistemavotacion.seguridad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.crypto.SecretKey;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.cms.CMSEnvelopedDataGenerator;
import org.bouncycastle.cms.CMSEnvelopedDataParser;
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSTypedStream;
import org.bouncycastle.cms.KeyTransRecipientId;
import org.bouncycastle.cms.Recipient;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInfoGenerator;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKEKRecipientInfoGenerator;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.util.Strings;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class Encryptor {
 
    private static Logger logger = LoggerFactory.getLogger(Encryptor.class); 
    
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    public static MimeMessage encryptSMIME(SMIMEMessageWrapper msgToEncrypt, 
                    X509Certificate receiverCert) throws Exception {
        logger.debug(" - encryptSMIME");
        /* Create the encrypter */
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
        /*SignerInformationStore  signers = 
                        msgToEncrypt.getSmimeSigned().getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        byte[] digestBytes = it.next().getContentDigest();//method can only be called after verify.
        String digestStr = new String(Base64.encode(digestBytes));
        encryptedMessage.addHeaderLine("SignedMessageDigest: " + digestStr);*/
        return encryptedMessage;
    }
    
       	
    /**
    * Method to decrypt SMIME signed messages
    */
   public static SMIMEMessageWrapper decryptSMIMEMessage(
           byte[] encryptedMessageBytes, PublicKey  publicKey, 
            PrivateKey receiverPrivateKey) throws Exception {
        logger.debug("decryptSMIMEMessage(...) ");
        InputStream inputStream = new ByteArrayInputStream(decryptMessage(
                encryptedMessageBytes, publicKey, receiverPrivateKey));
        return new SMIMEMessageWrapper(null, inputStream, null);
   }

    public static File encryptMessage(byte[] text, File encryptedFile, 
                    X509Certificate receiverCert) throws Exception {
        logger.debug(" - encryptMessage(...) - ");
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
    	
    /**
     * helper method to decrypt SMIME signed messages
     */
    public static byte[] decryptMessage(byte[] encryptedMessageBytes, 
            PublicKey receiverPublicKey, PrivateKey receiverPrivateKey) throws Exception {
        logger.debug("decryptMessage(...) ");
        RecipientId recId = null;
        /*if(receiverCert != null) 
            recId = new JceKeyTransRecipientId(receiverCert);*/
        Recipient recipient = new JceKeyTransEnvelopedRecipient(
                receiverPrivateKey).setProvider("BC");
        MimeMessage msg = new MimeMessage(null, 
                        new ByteArrayInputStream(encryptedMessageBytes));
        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);

        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        
        RecipientInformation        recipientInfo = null;
        if(recId != null) recipientInfo = recipients.get(recId);
        if(recipientInfo == null && recipients.getRecipients().size() == 1) {
            recipientInfo = (RecipientInformation) 
                recipients.getRecipients().iterator().next();
        }
        byte[] messageBytes = recipientInfo.getContent(recipient);
        return messageBytes;
    }
    
    public static byte[] decryptCMS(SecretKey privateKey, 
            byte[] cmsEncryptedData) throws Exception {
        CMSEnvelopedData ed = new CMSEnvelopedData(cmsEncryptedData);
        RecipientInformationStore  recipients = ed.getRecipientInfos();
        //assertEquals(ed.getEncryptionAlgOID(), CMSEnvelopedDataGenerator.DES_EDE3_CBC);
        Collection  c = recipients.getRecipients();
        Iterator    it = c.iterator();
        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipientInfo = (RecipientInformation)it.next();
            //Recipient recipient;
            //recipient = new JceKeyTransEnvelopedRecipient(privateKey);
            result = recipientInfo.getContent(privateKey, BC);
            //assertEquals(recipient.getKeyEncryptionAlgOID(), NISTObjectIdentifiers.id_aes128_wrap.getId());
            //result = recipient.getContent(secretKey, BC);
            //recipient.getContent(Recipient recipient)
        }
        return result;
    }

    public static byte[] decryptCMSStream(
            PrivateKey privateKey, byte[] cmsEncryptedData) throws Exception {
        CMSEnvelopedDataParser     ep = new CMSEnvelopedDataParser(cmsEncryptedData);
        RecipientInformationStore  recipients = ep.getRecipientInfos();
        Collection                 c = recipients.getRecipients();
        Iterator                   it = c.iterator();

        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipient = (RecipientInformation)it.next();
            //assertEquals(recipient.getKeyEncryptionAlgOID(), PKCSObjectIdentifiers.rsaEncryption.getId());
            CMSTypedStream recData = recipient.getContentStream(new JceKeyTransEnvelopedRecipient(privateKey).setProvider(BC));
            InputStream           dataStream = recData.getContentStream();
            ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
            byte[]                buf = new byte[4096];
            int len = 0;
            while ((len = dataStream.read(buf)) >= 0) {
                    dataOut.write(buf, 0, len);
            }
            dataOut.close();
            result = dataOut.toByteArray();
            //assertEquals(true, Arrays.equals(data, dataOut.toByteArray()));
        }
        return result;
    }
        
    public static byte[] encryptCMS(byte[] dataToEncrypt, 
        X509Certificate reciCert) throws Exception {
        CMSEnvelopedDataStreamGenerator edGen = new CMSEnvelopedDataStreamGenerator();
        edGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(reciCert).setProvider(BC));
        ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
        OutputStream out = edGen.open(bOut, new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider(BC).build());
        out.write(dataToEncrypt);
        out.close();
        return bOut.toByteArray();
    }    
        
    public static byte[] encryptCMS(byte[] dataToEncrypt, 
            SecretKey secretKey) throws Exception {
        CMSEnvelopedDataGenerator edGen = new CMSEnvelopedDataGenerator();
        byte[]  kekId = new byte[] { 1, 2, 3, 4, 5 };
        RecipientInfoGenerator rig = new JceKEKRecipientInfoGenerator(kekId, secretKey) ;
        edGen.addRecipientInfoGenerator(rig);
        CMSEnvelopedData ed = edGen.generate(
                                new CMSProcessableByteArray(dataToEncrypt),
                                CMSEnvelopedDataGenerator.DES_EDE3_CBC, BC);
        byte[] result = ed.getEncoded();
        return result;
    }

    public static MimeBodyPart encryptFile(File fileToEncrypt,
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
        return encryptedPart;
    }
    
    public static byte[] decryptFile (byte[] encryptedFile, 
            PublicKey publicKey, PrivateKey receiverPrivateKey) 
            throws Exception {
        logger.debug("- decryptFile(...) ");
        RecipientId recId = new KeyTransRecipientId(publicKey.getEncoded());
        
        Recipient recipient = new JceKeyTransEnvelopedRecipient(
                receiverPrivateKey).setProvider("BC");
        
        MimeMessage msg = new MimeMessage(null, 
                new ByteArrayInputStream(encryptedFile));
        
        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
        
        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation        recipientInfo = null;
        recipientInfo = recipients.get(recId);
        if(recipientInfo == null && recipients.getRecipients().size() == 1) {
            recipientInfo = (RecipientInformation) 
                recipients.getRecipients().iterator().next();
        }
        RecipientId recipientId = null;
        if(recipientInfo.getRID() != null) {
            recipientId = recipientInfo.getRID();
        }
        byte[] result = recipientInfo.getContent(recipient);
        return result;
    }
    
}
