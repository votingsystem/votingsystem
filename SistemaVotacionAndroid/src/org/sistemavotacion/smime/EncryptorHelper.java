package org.sistemavotacion.smime;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.bouncycastle2.cms.CMSAlgorithm;
import org.bouncycastle2.cms.SignerInformation;
import org.bouncycastle2.cms.SignerInformationStore;
import org.bouncycastle2.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle2.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle2.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle2.util.Strings;
import org.bouncycastle2.util.encoders.Base64;
import org.sistemavotacion.android.Aplicacion;

import android.util.Log;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
public class EncryptorHelper {
	
	public static final String TAG = "SMIMEEncryptorHelper";

	public static File encryptSMIMEFile(File fileToEncrypt, 
			X509Certificate receiverCert) throws Exception {
		Log.d(TAG + ".encryptSMIMEFile(...) ", " *** encryptSMIMEFile ");
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
            Log.d(TAG + ".encryptMessage(...)", " - headerLine: " + headerLine);
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
		Log.d(TAG + ".encryptText(...) ", " *** encryptText ");
    	Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setContent(new String(text), "text/plain");
        // set the Date: header
        //mimeMessage.setSentDate(new Date());
    	SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
    	encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
    			Aplicacion.getControlAcceso().getCertificado()).setProvider("BC"));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encrypter.generate(mimeMessage,
                new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
        encryptedPart.writeTo(new FileOutputStream(encryptedFile));
		return encryptedFile;
	}
	
	
	public static File encryptFile(File fileToEncrypt, File encryptedFile,
			X509Certificate receiverCert) throws Exception {
		Log.d(TAG + ".encryptFile(...) ", " *** encryptFile ");
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
	
}
