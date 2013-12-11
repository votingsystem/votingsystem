package org.votingsystem.signature.util;

import android.util.Log;
import org.bouncycastle2.cms.*;
import org.bouncycastle2.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle2.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle2.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle2.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle2.mail.smime.SMIMEEnveloped;
import org.bouncycastle2.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle2.util.Strings;
import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.model.EncryptedBundleVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import javax.crypto.Cipher;
import javax.mail.Header;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class Encryptor {
	
	public static final String TAG = "EncryptionHelper";

	private  Encryptor() {	}
	
	public static byte[] encryptSMIME(SMIMEMessageWrapper msgToEncrypt, 
			X509Certificate receiverCert) throws Exception {
		Log.d(TAG + ".encryptSMIMEFile(...) ", " #### encryptSMIMEFile ");
    	/* Create the encrypter */
    	SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
    	encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
    			receiverCert).setProvider("BC"));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encrypter.generate(msgToEncrypt,
                new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
        /* Set all original MIME headers in the encrypted message */
        Enumeration headers = msgToEncrypt.getAllHeaderLines();
        while (headers.hasMoreElements()) {
            String headerLine = (String)headers.nextElement();
            Log.d(TAG + ".encryptSMIMEFile(...)", " - headerLine: " + headerLine);
            /* 
             * Make sure not to override any content-* headers from the
             * original message
             */
            if (!Strings.toLowerCase(headerLine).startsWith("content-")) {
            	encryptedPart.addHeaderLine(headerLine);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        byte[] result = baos.toByteArray();
        baos.close();
		return result;
	}
	
    public static byte[] encryptMessage(byte[] text, 
            X509Certificate receiverCert, Header... headers) throws Exception {
    	Log.d(TAG + ".encryptMessage(...) ", " #### encryptFile ");
		Properties props = System.getProperties();
		Session session = Session.getDefaultInstance(props, null);
		MimeMessage mimeMessage = new MimeMessage(session);
		mimeMessage.setText(new String(text));
        for(Header header:headers) {
            mimeMessage.setHeader(header.getName(), header.getValue());
        }
		// set the Date: header
		//mimeMessage.setSentDate(new Date());
		SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
		encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
		        receiverCert).setProvider("BC"));
		/* Encrypt the message */
		MimeBodyPart encryptedPart = encrypter.generate(mimeMessage,
		        new JceCMSContentEncryptorBuilder(
		        CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
	}
	
    public static MimeBodyPart encryptBase64Message(byte[] fileBytes,
            X509Certificate receiverCert, Header... headers) throws Exception {
    	Log.d(TAG + ".encryptMessage(...) ", " - encryptFile(...)");
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage mimeMessage = new MimeMessage(session);
        byte[] base64EncodedFileBytes = Base64.encode(fileBytes);
        
        mimeMessage.setContent(base64EncodedFileBytes, 
        		"text/plain; charset=ISO-8859-1");
        mimeMessage.setHeader("Content-Transfer-Encoding", "BASE64");
        for(Header header:headers) {
            mimeMessage.setHeader(header.getName(), header.getValue());
        }
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
    
	/**
	 * Method to decrypt SMIME signed messages
	 */
	public static SMIMEMessageWrapper decryptSMIMEMessage(byte[] encryptedMessageBytes, 
			X509Certificate receiverCert, PrivateKey receiverPrivateKey) throws Exception {
		Log.d(TAG + ".decryptSMIMEMessage(...) ", " #### decryptSMIMEMessage ");
		byte[] messageContentBytes = decryptMessage(encryptedMessageBytes, 
				receiverCert, receiverPrivateKey);
		SMIMEMessageWrapper smimeMessage = new SMIMEMessageWrapper(null, 
				new ByteArrayInputStream(messageContentBytes), null);
		return smimeMessage;
	}
	
    /**
    * Method to decrypt SMIME signed messages
    */
   public static SMIMEMessageWrapper decryptSMIMEMessage(
           byte[] encryptedMessageBytes, PublicKey  publicKey, 
            PrivateKey receiverPrivateKey) throws Exception {
	   Log.d(TAG + ".decryptSMIMEMessage(...) ", "decryptSMIMEMessage(...) ");
	   InputStream inputStream = new ByteArrayInputStream(decryptMessage(
                encryptedMessageBytes, publicKey, receiverPrivateKey));
        return new SMIMEMessageWrapper(null, inputStream, null);
   }
   
   /**
    * helper method to decrypt SMIME signed messages
    */
   public static byte[] decryptMessage(byte[] encryptedMessageBytes, 
           PublicKey receiverPublicKey, PrivateKey receiverPrivateKey) throws Exception {
	   Log.d(TAG + ".decryptMessage(...) ", "decryptMessage(...) ");
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
       //if(recId != null) recipientInfo = recipients.get(recId);
       if(recipientInfo == null && recipients.getRecipients().size() == 1) {
           recipientInfo = (RecipientInformation) 
               recipients.getRecipients().iterator().next();
       }
       byte[] messageBytes = recipientInfo.getContent(recipient);
       return messageBytes;
   }
   
	
	/**
	 * helper method to decrypt SMIME signed messages
	 */
	public static byte[] decryptMessage(byte[] encryptedMessageBytes,
			X509Certificate receiverCert, PrivateKey receiverPrivateKey) throws Exception {
		Log.d(TAG + ".decryptMessage(...) ", " #### decryptMessage ");
        RecipientId recId = null;
        if(receiverCert != null) 
            recId = new JceKeyTransRecipientId(receiverCert);
		Recipient recipient = new JceKeyTransEnvelopedRecipient(receiverPrivateKey).setProvider("BC");
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
		return  recipientInfo.getContent(recipient);
    }
	
    public static byte[] decryptFile (byte[] encryptedFile, 
            PublicKey publicKey, PrivateKey receiverPrivateKey) 
            throws Exception {
		Log.d(TAG + ".decryptFile(...) ", " #### decryptFile ");
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
    
	
	public static EncryptedBundleVS decryptEncryptedBundle(EncryptedBundleVS encryptedBundleVS,
			X509Certificate receiverCert, PrivateKey receiverPrivateKey) throws Exception {
		byte[] messageBytes = null;
		switch(encryptedBundleVS.getType()) {
			case FILE:
				messageBytes = decryptFile(encryptedBundleVS.getEncryptedMessageBytes(),
						receiverCert.getPublicKey(), receiverPrivateKey);
				encryptedBundleVS.setStatusCode(ResponseVS.SC_OK);
				encryptedBundleVS.setDecryptedMessageBytes(messageBytes);
				break;
			case SMIME_MESSAGE:
				SMIMEMessageWrapper smimeMessageWrapper = decryptSMIMEMessage(
						encryptedBundleVS.getEncryptedMessageBytes(),
						receiverCert, receiverPrivateKey);
				encryptedBundleVS.setStatusCode(ResponseVS.SC_OK);
				encryptedBundleVS.setDecryptedSMIMEMessage(smimeMessageWrapper);
				break;
			case TEXT:
				messageBytes = decryptFile(encryptedBundleVS.getEncryptedMessageBytes(),
						receiverCert.getPublicKey(), receiverPrivateKey);
				encryptedBundleVS.setStatusCode(ResponseVS.SC_OK);
				encryptedBundleVS.setDecryptedMessageBytes(messageBytes);
				break;
		}
		return encryptedBundleVS;
	}
	
	public List<EncryptedBundleVS> decryptEncryptedBundleList(
			List<EncryptedBundleVS> encryptedBundleVSList, X509Certificate receiverCert,
			PrivateKey receiverPrivateKey) throws Exception {
		List<EncryptedBundleVS> result = new ArrayList<EncryptedBundleVS>();
		for(EncryptedBundleVS encryptedBundleVS : encryptedBundleVSList) {
			result.add(decryptEncryptedBundle(encryptedBundleVS,
					receiverCert, receiverPrivateKey));
		}
		return result;
	}
	
    public static byte[] encryptSymmetric(Key key, byte[] dataBytes) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(dataBytes);
        return encrypted;
	}
	
	public static byte[] decryptSymmetric(Key key, byte[] encryptedDataBytes) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedData = cipher.doFinal(encryptedDataBytes);
        return decryptedData;
	}

}
