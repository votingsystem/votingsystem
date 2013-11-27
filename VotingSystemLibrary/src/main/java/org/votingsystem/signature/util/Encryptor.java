package org.votingsystem.signature.util;

import org.apache.log4j.Logger;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKEKRecipientInfoGenerator;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.util.Strings;
import org.votingsystem.model.ContextVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.crypto.SecretKey;
import javax.mail.Header;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Encryptor {
 
    private static Logger logger = Logger.getLogger(Encryptor.class); 
    

    public static byte[] encryptSMIME(SMIMEMessageWrapper msgToEncrypt, 
                    X509Certificate receiverCert) throws Exception {
        logger.debug(" - encryptSMIME");
        /* Create the encrypter */
        SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
        encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
                        receiverCert).setProvider(ContextVS.PROVIDER));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encrypter.generate(msgToEncrypt,
                new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
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
                encryptedPart.addHeaderLine(headerLine);
            }
        }
        /*SignerInformationStore  signers = 
                        msgToEncrypt.getSmimeSigned().getSignerInfos();
        Iterator<SignerInformation> it = signers.getSigners().iterator();
        byte[] digestBytes = it.next().getContentDigest();//method can only be called after verify.*/
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
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
        return new SMIMEMessageWrapper(inputStream);
   }
    
    public static byte[] encryptMessage(byte[] text, X509Certificate receiverCert, 
            Header... headers) throws Exception {
        logger.debug(" - encryptMessage(...) - ");
        MimeMessage mimeMessage = new MimeMessage(ContextVS.MAIL_SESSION);
        mimeMessage.setText(new String(text));
        // set the Date: header
        //mimeMessage.setSentDate(new Date());
        SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
        encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
                receiverCert).setProvider(ContextVS.PROVIDER));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encrypter.generate(mimeMessage,
                new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
    }
    
    public static byte[] encryptFile(File fileToEncrypt,
            X509Certificate receiverCert) throws Exception {
        logger.debug(" - encryptFile(...)");
        MimeMessage mimeMessage = new MimeMessage(ContextVS.MAIL_SESSION);
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
                        receiverCert).setProvider(ContextVS.PROVIDER));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encrypter.generate(mimeMessage,
                new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
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
                receiverPrivateKey).setProvider(ContextVS.PROVIDER);
        MimeMessage msg = new MimeMessage(null, 
                        new ByteArrayInputStream(encryptedMessageBytes));
        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);

        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        
        RecipientInformation        recipientInfo = null;
        //if(recId != null) recipientInfo = recipients.get(recId);
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
            result = recipientInfo.getContent(privateKey, ContextVS.PROVIDER);
            //assertEquals(recipient.getKeyEncryptionAlgOID(), NISTObjectIdentifiers.id_aes128_wrap.getId());
            //result = recipient.getContent(secretKey, SMIMEContext.PROVIDER);
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
            CMSTypedStream recData = recipient.getContentStream(new JceKeyTransEnvelopedRecipient(privateKey).setProvider(ContextVS.PROVIDER));
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
        edGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
        		reciCert).setProvider(ContextVS.PROVIDER));
        ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
        OutputStream out = edGen.open(bOut, new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider(
        		ContextVS.PROVIDER).build());
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
                CMSEnvelopedDataGenerator.DES_EDE3_CBC, ContextVS.PROVIDER);
        byte[] result = ed.getEncoded();
        return result;
    }
    
    public static byte[] decryptFile (byte[] encryptedFile, 
            PublicKey publicKey, PrivateKey receiverPrivateKey) 
            throws Exception {
        logger.debug("- decryptFile(...) ");
        RecipientId recId = new KeyTransRecipientId(publicKey.getEncoded());
        
        Recipient recipient = new JceKeyTransEnvelopedRecipient(
                receiverPrivateKey).setProvider(ContextVS.PROVIDER);
        
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
