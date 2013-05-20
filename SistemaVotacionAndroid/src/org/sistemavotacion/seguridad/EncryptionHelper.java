package org.sistemavotacion.seguridad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.crypto.Cipher;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle2.cms.CMSAlgorithm;
import org.bouncycastle2.cms.Recipient;
import org.bouncycastle2.cms.RecipientId;
import org.bouncycastle2.cms.RecipientInformation;
import org.bouncycastle2.cms.RecipientInformationStore;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.SignerInformationStore;
import org.bouncycastle2.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle2.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle2.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle2.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle2.mail.smime.SMIMEEnveloped;
import org.bouncycastle2.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle2.mail.smime.SMIMEUtil;
import org.bouncycastle2.util.Strings;
import org.bouncycastle2.util.encoders.Base64;
import org.sistemavotacion.modelo.EncryptedBundle;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.smime.SMIMEMessageWrapper;

import android.util.Log;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class EncryptionHelper {
	
	public static final String TAG = "EncryptionHelper";

	private  EncryptionHelper() {	}
	
	public static File encryptSMIMEFile(File fileToEncrypt, 
			X509Certificate receiverCert) throws Exception {
		Log.d(TAG + ".encryptSMIMEFile(...) ", " #### encryptSMIMEFile ");
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
            Log.d(TAG + ".encryptSMIMEFile(...)", " - headerLine: " + headerLine);
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
        encryptedMessage.writeTo(new FileOutputStream(fileToEncrypt));
		return fileToEncrypt;
	}
	
	public static File encryptText(byte[] text, File encryptedFile, 
			X509Certificate receiverCert) throws Exception {
		Log.d(TAG + ".encryptText(...) ", " #### encryptText ");
    	Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setContent(new String(text), "text/plain");
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
	
	public static byte[] encryptText(byte[] text, 
			X509Certificate receiverCert) throws Exception {
		Log.d(TAG + ".encryptText(...) ", " #### encryptText ");
    	Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setContent(new String(text), "text/plain");
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
		return baos.toByteArray();
	}
	
	
	public static File encryptFile(File fileToEncrypt, File encryptedFile, 
			X509Certificate receiverCert) throws Exception {
		Log.d(TAG + ".encryptFile(...) ", " #### encryptFile ");
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
	
	/**
	 * Method to decrypt files attached to SMIME (not signed) messages 
	 */
	public static byte[] decryptFile (byte[] encryptedFile, 
    		X509Certificate receiverCert, PrivateKey receiverPrivateKey) throws Exception {
		Log.d(TAG + ".decryptFile(...) ", " #### decryptFile ");
        RecipientId recId = null;
        if(receiverCert != null) 
            recId = new JceKeyTransRecipientId(receiverCert);
		Recipient recipient = new JceKeyTransEnvelopedRecipient(
				receiverPrivateKey).setProvider("BC");
		MimeMessage msg = new MimeMessage(
				null, new ByteArrayInputStream(encryptedFile));
		SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);

        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation        recipientInfo = null;
        if(recId != null) recipientInfo = recipients.get(recId);
        if(recipientInfo == null && recipients.getRecipients().size() == 1) {
            recipientInfo = (RecipientInformation) 
                recipients.getRecipients().iterator().next();
        }
        
		/*RecipientId recipientId = null;
		if(recipientInfo.getRID() != null) {
			recipientId = recipientInfo.getRID();
			Log.d(TAG + ".decryptFile(...) ", " #### recipientId.getSerialNumber(): " 
					+ recipientId.getSerialNumber());
		}*/
		MimeBodyPart res = SMIMEUtil.toMimeBodyPart(recipientInfo.getContent(recipient));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		res.writeTo(baos);
		MimeMessage mimeMessage = new MimeMessage(null,
			new ByteArrayInputStream(baos.toByteArray()));
		return (byte[])mimeMessage.getContent();
	}
	
	public static EncryptedBundle decryptEncryptedBundle(EncryptedBundle encryptedBundle, 
			X509Certificate receiverCert, PrivateKey receiverPrivateKey) throws Exception {
		byte[] messageBytes = null;
		switch(encryptedBundle.getType()) {
			case FILE:
				messageBytes = decryptFile(encryptedBundle.getEncryptedMessageBytes(), 
						receiverCert, receiverPrivateKey);
				encryptedBundle.setStatusCode(Respuesta.SC_OK);
				encryptedBundle.setDecryptedMessageBytes(messageBytes);
				break;
			case SMIME_MESSAGE:
				SMIMEMessageWrapper smimeMessageWrapper = decryptSMIMEMessage(
						encryptedBundle.getEncryptedMessageBytes(), 
						receiverCert, receiverPrivateKey);
				encryptedBundle.setStatusCode(Respuesta.SC_OK);
				encryptedBundle.setDecryptedSMIMEMessage(smimeMessageWrapper);
				break;
			case TEXT:
				messageBytes = decryptFile(encryptedBundle.getEncryptedMessageBytes(), 
						receiverCert, receiverPrivateKey);
				encryptedBundle.setStatusCode(Respuesta.SC_OK);
				encryptedBundle.setDecryptedMessageBytes(messageBytes);
				break;
		}
		return encryptedBundle;
	}
	
	public List<EncryptedBundle> decryptEncryptedBundleList(
			List<EncryptedBundle> encryptedBundleList, X509Certificate receiverCert,
			PrivateKey receiverPrivateKey) throws Exception {
		List<EncryptedBundle> result = new ArrayList<EncryptedBundle>();
		for(EncryptedBundle encryptedBundle:encryptedBundleList) {
			result.add(decryptEncryptedBundle(encryptedBundle, 
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
