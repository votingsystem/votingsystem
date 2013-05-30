package org.sistemavotacion.centrocontrol

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Properties;
import javax.mail.Header;
import javax.mail.Session;
import javax.mail.internet.MimeMultipart;
import org.springframework.beans.factory.InitializingBean
import org.sistemavotacion.util.*
import org.sistemavotacion.centrocontrol.modelo.*
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.security.cert.CertPathValidatorException
import java.security.cert.PKIXCertPathValidatorResult
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.mail.smime.SMIMEEnveloped
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.asn1.x509.X509Extensions
import java.util.Locale;
import org.bouncycastle.cms.CMSException
import org.bouncycastle.cms.Recipient;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation
import org.bouncycastle.cms.RecipientInformationStore
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Base64;


/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
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
		def rutaAlmacenClaves = getAbsolutePath(
			"${grailsApplication.config.SistemaVotacion.rutaAlmacenClaves}")
		File keyStoreFile = new File(rutaAlmacenClaves);
		String aliasClaves = grailsApplication.config.SistemaVotacion.aliasClavesFirma
		String password = grailsApplication.config.SistemaVotacion.passwordClavesFirma
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
	public Respuesta decryptMessage (byte[] encryptedFile, Locale locale) {
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
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			res.writeTo(baos)
			MimeMessage mimeMessage = new MimeMessage(null,
				new ByteArrayInputStream(baos.toByteArray()));
			return new Respuesta(codigoEstado: Respuesta.SC_OK,
				messageBytes:mimeMessage.getContent())
		} catch(CMSException ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(mensaje:messageSource.getMessage(
				'encryptedMessageErrorMsg', null, locale),
				codigoEstado:Respuesta.SC_ERROR_PETICION)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado: Respuesta.SC_ERROR_PETICION,
				mensaje:ex.getMessage())
		}
	}
	
	public Respuesta encryptMessage(byte[] bytesToEncrypt,
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
			
			return new Respuesta(codigoEstado:Respuesta.SC_OK,
				messageBytes:result)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex);
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:ex.getMessage())
		}
	}
	
	/**
	 * Method to encrypt SMIME signed messages
	 */
	Respuesta encryptSMIMEMessage(byte[] bytesToEncrypt, 
		X509Certificate receiverCert, Locale locale) throws Exception {
		log.debug(" - encryptSMIMEMessage(...) ");
		try {
			SMIMEMessageWrapper msgToEncrypt = new SMIMEMessageWrapper(
					new ByteArrayInputStream(bytesToEncrypt));
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
				//log.debug(" - headerLine: ${headerLine}");
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
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			encryptedMessage.writeTo(baos);
			return new Respuesta(messageBytes:baos.toByteArray(),
				codigoEstado:Respuesta.SC_OK);
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(mensaje:ex.getMessage(),
				codigoEstado:Respuesta.SC_ERROR_PETICION)
		}
	}	
	
	/**
	 * Method to decrypt SMIME signed messages
	 */
	Respuesta decryptSMIMEMessage(byte[] encryptedMessageBytes, Locale locale) {
		log.debug(" - decryptSMIMEMessage - ")
		SMIMEMessageWrapper smimeMessageReq = null
		try {
			MimeMessage msg = new MimeMessage(getSession(), 
				new ByteArrayInputStream(encryptedMessageBytes));
			
			//String encryptedMessageBytesStr = new String(encryptedMessageBytes);
			//log.debug("decryptSMIMEMessage - encryptedMessageBytesStr: " + encryptedMessageBytesStr)
	
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
			
			smimeMessageReq = new SMIMEMessageWrapper(
					new ByteArrayInputStream(messageContentBytes));
		} catch(CMSException ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(mensaje:messageSource.getMessage(
				'encryptedMessageErrorMsg', null, locale),
				codigoEstado:Respuesta.SC_ERROR_PETICION)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(mensaje:ex.getMessage(),
				codigoEstado:Respuesta.SC_ERROR_PETICION)
		}
		return new Respuesta(smimeMessage:smimeMessageReq,
			codigoEstado:Respuesta.SC_OK)
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
	
	public String getAbsolutePath(String filePath){
		String prefijo = "${grailsApplication.mainContext.getResource('.')?.getFile()}"
		String sufijo =filePath.startsWith(File.separator)? filePath : File.separator + filePath;
		return "${prefijo}${sufijo}";
	}
}
