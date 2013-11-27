package org.votingsystem.controlcenter.service

import org.bouncycastle.cms.*
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator
import org.bouncycastle.mail.smime.SMIMEEnveloped
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator
import org.bouncycastle.mail.smime.SMIMEUtil
import org.bouncycastle.util.Strings
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper

import javax.mail.Session
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.cert.X509Certificate
/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class EncryptionService {
	
	def grailsApplication;
	def messageSource
	private X509Certificate serverCert;
	private PrivateKey serverPrivateKey;
	private RecipientId recId;
	private Session session
	private Recipient recipient;
	
	//@Override
	public void afterPropertiesSet() throws Exception {
		log.debug(" - afterPropertiesSet - ")
		File keyStoreFile =  grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		KeyStore keyStore = KeyStore.getInstance("JKS");
		keyStore.load(new FileInputStream(keyStoreFile), password.toCharArray());
		java.security.cert.Certificate[] chain = keyStore.getCertificateChain(aliasClaves);
		serverCert = (X509Certificate)chain[0]
		recId = new JceKeyTransRecipientId(serverCert);
		serverPrivateKey = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray())
		recipient = new JceKeyTransEnvelopedRecipient(serverPrivateKey).setProvider("BC")
		Properties props = System.getProperties();
		// Get a Session object with the default properties.
		session = Session.getDefaultInstance(props, null);
	}
	
	/**
	 * Method to decrypt files attached to SMIME (not signed) messages
	 */
	public ResponseVS decryptMessage (byte[] encryptedFile, Locale locale) {
		log.debug " - decryptMessage - "
		//log.debug "decryptMessage - encryptedFile: ${new String(encryptedFile)} "
		try {
			MimeMessage msg = new MimeMessage(getSession(), 
				new ByteArrayInputStream(encryptedFile));
			SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
			RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
			RecipientInformation        recipientInfo = recipients.get(getRecipientId());
			RecipientId recipientId = null;
			if(recipientInfo.getRID() != null) {
				recipientId = recipientInfo.getRID();
				log.debug(" -- recipientId.getSerialNumber(): " + recipientId.getSerialNumber());
			}
			MimeBodyPart res = SMIMEUtil.toMimeBodyPart(recipientInfo.getContent(getRecipient()));
			return new ResponseVS(statusCode: ResponseVS.SC_OK,
				messageBytes:res.getContent())
		} catch(CMSException ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(message:messageSource.getMessage(
				'encryptedMessageErrorMsg', null, locale),
				statusCode:ResponseVS.SC_ERROR_REQUEST)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
				message:ex.getMessage())
		}
	}
	
	public ResponseVS encryptMessage(byte[] bytesToEncrypt,
		PublicKey publicKey) throws Exception {
				log.debug("--- - encryptMessage(...) - ");
		try {
			MimeBodyPart mimeMessage = new MimeBodyPart();
			mimeMessage.setText(new String(bytesToEncrypt));

			// set the Date: header
			//mimeMessage.setSentDate(new Date());
			SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
			encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
					"".getBytes(), publicKey).setProvider(BC));
			/* Encrypt the message */
			MimeBodyPart encryptedPart = encrypter.generate(mimeMessage,
					new JceCMSContentEncryptorBuilder(
					CMSAlgorithm.DES_EDE3_CBC).setProvider(BC).build());
			ByteArrayOutputStream baos = new ByteArrayOutputStream()
			encryptedPart.writeTo(baos);
			byte[] result = baos.toByteArray()
			baos.close();
			
			return new ResponseVS(statusCode:ResponseVS.SC_OK,
				messageBytes:result)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex);
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message:ex.getMessage())
		}
	}
		
	/**
	 * Method to encrypt SMIME signed messages
	 */
	ResponseVS encryptSMIMEMessage(byte[] bytesToEncrypt, X509Certificate receiverCert, Locale locale) throws Exception {
		log.debug(" - encryptSMIMEMessage(...) ");
		//If the message isn't recreated there can be problems with
		//multipart boundaries. TODO
		SMIMEMessageWrapper msgToEncrypt = new SMIMEMessageWrapper(new ByteArrayInputStream(bytesToEncrypt));
		try {
			SMIMEEnvelopedGenerator encrypter = new SMIMEEnvelopedGenerator();
			encrypter.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(receiverCert).setProvider("BC"));
			/* Encrypt the message */
			MimeBodyPart encryptedPart = encrypter.generate(msgToEncrypt,
				new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).setProvider("BC").build());
			// Create a new MimeMessage that contains the encrypted and signed content
			/* Set all original MIME headers in the encrypted message */
			Enumeration headers = msgToEncrypt.getAllHeaderLines();
			while (headers.hasMoreElements()) {
				String headerLine = (String)headers.nextElement();
				//log.debug(" - headerLine: ${headerLine}");
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
			return new ResponseVS(messageBytes:baos.toByteArray(), statusCode:ResponseVS.SC_OK);
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(message:ex.getMessage(),
				statusCode:ResponseVS.SC_ERROR_REQUEST)
		}	
		return encryptSMIMEMessage(msgToEncrypt, receiverCert, locale)
	}
	
	
	/**
	 * Method to decrypt SMIME signed messages
	 */
	ResponseVS decryptSMIMEMessage(byte[] encryptedMessageBytes, Locale locale) {
		log.debug(" - decryptSMIMEMessage - ")
		SMIMEMessageWrapper smimeMessageReq = null
		try {
			MimeMessage msg = new MimeMessage(getSession(), new ByteArrayInputStream(encryptedMessageBytes));
			
			//String encryptedMessageBytesStr = new String(encryptedMessageBytes);
			//log.debug("- decryptSMIMEMessage - encryptedMessageBytesStr: " + encryptedMessageBytesStr)
	
			SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
			RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
			RecipientInformation        recipientInfo = recipients.get(getRecipientId());
	
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
			byte[] messageContentBytes =  recipientInfo.getContent(getRecipient())
			//log.debug(" ------- Message Contents: ${new String(messageContentBytes)}");
			
			smimeMessageReq = new SMIMEMessageWrapper(new ByteArrayInputStream(messageContentBytes));
		} catch(CMSException ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(message:messageSource.getMessage(
				'encryptedMessageErrorMsg', null, locale),
				statusCode:ResponseVS.SC_ERROR_REQUEST)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(message:ex.getMessage(),
				statusCode:ResponseVS.SC_ERROR_REQUEST)
		}
		return new ResponseVS(smimeMessage:smimeMessageReq,
			statusCode:ResponseVS.SC_OK)
	}
	
	
	private Session getSession() {
		return session
	}
	
	private Recipient getRecipient() {
		return recipient;	
	}
	
	private RecipientId getRecipientId() {
		if(recId == null) afterPropertiesSet()
		return recId;
	}

}
