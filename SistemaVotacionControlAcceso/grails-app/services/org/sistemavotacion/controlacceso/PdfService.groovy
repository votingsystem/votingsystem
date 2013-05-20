package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*

import org.bouncycastle.util.encoders.Base64;
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement
import com.itextpdf.text.pdf.PdfReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.*
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

import com.itextpdf.text.Element;
import com.itextpdf.text.Font
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Phrase
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfContentByte
import com.itextpdf.text.pdf.PdfCopyFields;
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPKCS7;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfEncryptor;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.draw.LineSeparator
import com.itextpdf.text.*
import org.bouncycastle.cms.RecipientId
import org.bouncycastle.cms.RecipientInformation
import org.bouncycastle.cms.RecipientInformationStore
import org.bouncycastle.cms.SignerId
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId
import org.bouncycastle.mail.smime.SMIMEEnveloped
import org.bouncycastle.ocsp.BasicOCSPResp
import org.bouncycastle.tsp.TimeStampToken
import org.bouncycastle.tsp.TimeStampTokenInfo
import org.sistemavotacion.seguridad.*
import org.springframework.context.ApplicationContext;
import org.sistemavotacion.util.*;
import java.util.Locale;

import javax.mail.Session;
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.springframework.beans.factory.InitializingBean


//class PdfService implements InitializingBean {
class PdfService {
	
	
	def grailsApplication
	def eventoFirmaService
	def eventoReclamacionService
	def eventoVotacionService
	def mailSenderService
	def firmaService
	def messageSource
	def subscripcionService
	def encryptionService
	private KeyStore trustedCertsKeyStore
	private PrivateKey key;
	private Certificate[] chain;
	private Session session
	
	
	//@Override 
	public void afterPropertiesSet() throws Exception {
		log.debug "afterPropertiesSet - afterPropertiesSet - afterPropertiesSet"
		def rutaAlmacenClaves = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaAlmacenClaves}")
		File keyStoreFile = new File(rutaAlmacenClaves);
		String aliasClaves = grailsApplication.config.SistemaVotacion.aliasClavesFirma
		String password = grailsApplication.config.SistemaVotacion.passwordClavesFirma
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
			FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
		key = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray());
		chain = keyStore.getCertificateChain(aliasClaves);
		log.debug "aliasClaves: ${aliasClaves} - chain.length:${chain.length}"
		trustedCertsKeyStore = KeyStore.getInstance("JKS");
		trustedCertsKeyStore.load(null, null);
		Set<X509Certificate> trustedCertsSet = firmaService.getTrustedCerts()
		log.debug "trustedCerts.size: ${trustedCertsSet.size()}"
		for(X509Certificate certificate:trustedCertsSet) {
			trustedCertsKeyStore.setCertificateEntry(
				certificate.getSubjectDN().toString(), certificate);
		}
		Properties props = System.getProperties();
		// Get a Session object with the default properties.
		session = Session.getDefaultInstance(props, null);
	}
	
	public Respuesta checkSignature (byte[] signedPDF, Locale locale) {
		log.debug "checkSiganture - signedPDF.length: ${signedPDF.length}"
		Respuesta respuesta = null;
		PdfReader reader = new PdfReader(signedPDF);
		Documento documento;
		AcroFields acroFields = reader.getAcroFields();
		ArrayList<String> names = acroFields.getSignatureNames();
		respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
			mensaje:messageSource.getMessage('error.documentWithoutSigners', null, locale));
		for (String name : names) {
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK);
			log.debug("checkSignature - Signature name: " + name + " - covers whole document:" +
				acroFields.signatureCoversWholeDocument(name));
			PdfPKCS7 pk = acroFields.verifySignature(name, "BC");
			log.debug("checkSignature - Hash verified -> ${pk.verify()}");
			if(!pk.verify()) {
				log.debug("checkSignature - VERIFICATION FAILED!!!");
				respuesta = new Respuesta (codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:messageSource.getMessage('pdfSignedDocumentError', null, locale))
			}
			X509Certificate signingCert = pk.getSigningCertificate();
			Usuario usuario = Usuario.getUsuario(signingCert);
			log.debug("checkSignature - Subject: " + PdfPKCS7.getSubjectFields(pk.getSigningCertificate()));
			Calendar signDate = pk.getSignDate();
			X509Certificate[] pkc = (X509Certificate[])pk.getSignCertificateChain();
			TimeStampToken timeStampToken = pk.getTimeStampToken();
			if(!trustedCertsKeyStore) afterPropertiesSet();
			Object[] fails = PdfPKCS7.verifyCertificates(pkc, getTrustedCertsKeyStore(), null, signDate);
			if(fails != null) {
				log.debug("checkSignature - fails - Cert '${signingCert.getSerialNumber()?.longValue()}' has fails: ${fails[1]}" );
				for(X509Certificate cert:pkc) {
					String notAfter = DateUtils.getStringFromDate(cert.getNotAfter())
					String notBefore = DateUtils.getStringFromDate(cert.getNotBefore())
					log.debug("checkSignature - fails - Cert: ${cert.getSubjectDN()} - NotBefore: ${notBefore} - NotAfter: ${notAfter}")
				}
				return new Respuesta (codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:
					messageSource.getMessage('error.caUnknown', null, locale))	
			}
			Certificado certificado = Certificado.findWhere(numeroSerie:signingCert.getSerialNumber()?.longValue())
			if (!certificado) {
				String subject = PdfPKCS7.getSubjectFields(pk.getSigningCertificate())
				Certificado certificadoCA
				for(X509Certificate certificate : pkc) {
					log.debug("checkSignature - checking document cert '${certificate?.getSerialNumber()?.longValue()}'")
					if(signingCert.getSerialNumber()?.longValue() !=
						certificate.getSerialNumber()?.longValue()) {
						log.debug("checkSignature - CA: '${certificate?.getSerialNumber()?.longValue()}' - ${certificate.getSubjectDN().toString()}")
						certificadoCA = firmaService.getCertificadoCA(certificate.getSerialNumber()?.longValue())
						log.debug("checkSignature - CA id: ${certificadoCA?.id}")
						usuario.setCertificadoCA(certificadoCA);
					}
				}
				Respuesta respuestaValidacionUsu = subscripcionService.checkUser(usuario, locale);
				if(Respuesta.SC_OK != respuestaValidacionUsu.codigoEstado) return respuestaValidacionUsu;
				usuario = respuestaValidacionUsu.usuario;
				certificado = respuestaValidacionUsu.certificadoDB;
			} else usuario = certificado.usuario;
			if (timeStampToken != null) {
				boolean impr = pk.verifyTimestampImprint();
				signDate= pk.getTimeStampDate();
				log.debug("checkSignature - timeStampToken - verifyTimestampImprint: ${impr} - signDate:${signDate.getTime()}" );
				TimeStampTokenInfo tsInfo= timeStampToken.getTimeStampInfo();
				SignerId signer_id = timeStampToken.getSID();
				BigInteger cert_serial_number = signer_id.getSerialNumber();
				log.debug("checkSignature - timeStampToken - Generation time " + tsInfo.getGenTime());
				log.debug("checkSignature - timeStampToken - Signer ID serial " + signer_id.getSerialNumber());
				log.debug("checkSignature - timeStampToken - Signer ID issuer " + signer_id.getIssuerAsString());
			}
			documento = new Documento(pdf:signedPDF, usuario:usuario, timeStampToken:timeStampToken,
				signDate:signDate?.getTime(), estado:Documento.Estado.VALIDADO)
			documento.save()
			respuesta.documento = documento
			/*BasicOCSPResp ocsp = pk.getOcsp();
			if (ocsp != null) {
				// Get a trusted certificate (could have come from a certificate store)
				InputStream inStream = new FileInputStream("responder.cer");
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
				inStream.close();
				log.debug("OCSP signature verifies: " + ocsp.verify(cert.getPublicKey(), "BC"));
				log.debug("OCSP revocation refers to this certificate: " + pk.isRevocationValid());
			}*/
		}
		log.debug "checkSiganture - DOCUMENT OK"
		return respuesta;
	}
		
	public Respuesta checkTimeStampToken(TimeStampToken timeStampToken) {
		TimeStampTokenInfo tokenInfo = timeStampToken.timeStampInfo
		log.debug(" -TimeStampToken Serial Number: " + tokenInfo.getSerialNumber());
	}

	public Respuesta firmar(PdfReader reader, String reason, 
		String location, Documento documento) throws Exception {
		Respuesta respuesta
		try {
			File file = File.createTempFile("pdfFirmadoServidor", ".pdf")
			file.deleteOnExit();
			FileOutputStream outputStream = new FileOutputStream(file)
			PdfStamper stp = PdfStamper.createSignature(reader, outputStream, '\0' as char, null, true);
			PdfSignatureAppearance signatureAppearance = stp.getSignatureAppearance();
			signatureAppearance.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
			signatureAppearance.setReason(reason);
			signatureAppearance.setLocation(location);
			signatureAppearance.setVisibleSignature(new Rectangle(330, 40, 580, 140), 1, null);
			log.debug("firmar - stp.hasSignature: " + stp.hasSignature)
			if (stp != null) stp.close();
			documento.pdf = file.getBytes()
			documento.save()
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK, file:file)
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:ex.getMessage())
		}
		return respuesta
	}
	
	public Respuesta firmarBloquear(PdfReader reader, String reason, 
			String location, Documento documento) throws Exception {
		Respuesta respuesta
		try {
			File file = File.createTempFile("pdfFirmadoServidor", ".pdf")
			file.deleteOnExit();
			FileOutputStream outputStream = new FileOutputStream(file)
			PdfStamper stp = PdfStamper.createSignature(
				reader, outputStream, '\0' as char, null, true);
			stp.setEncryption(null, null,PdfWriter.ALLOW_PRINTING, false);
			PdfSignatureAppearance signatureAppearance = stp.getSignatureAppearance();
			signatureAppearance.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
			signatureAppearance.setReason(reason);
			signatureAppearance.setLocation(location);
			signatureAppearance.setVisibleSignature(new Rectangle(330, 40, 580, 140), 1, null);
			log.debug("firmarBloquear - stp.hasSignature: " + stp.hasSignature)
			if (stp != null) stp.close();
			documento.pdf = file.getBytes()
			documento.save()
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK, file:file)
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:ex.getMessage())
		}
		return respuesta
	}
	
    public KeyStore getTrustedCertsKeyStore() {
		return trustedCertsKeyStore;
	}
			
	public String getAbsolutePath(String filePath){
		log.debug "getAbsolutePath - filePath: ${filePath}"
		//def resources = grailsApplication.mainContext.getResource('/WEB-INF/resources').file
		"${grailsApplication.mainContext.getResource(filePath).getFile()}"
	}
	
	public static void concatenate2PDF(PdfReader reader1, PdfReader reader2,
		FileOutputStream outputStream) throws Exception {
		//Document document = new Document();
		//document.addHeader(null, null);
		PdfCopyFields copy = new PdfCopyFields(outputStream);
		copy.addDocument(reader1);
		copy.addDocument(reader2);
		copy.close();
	}
}