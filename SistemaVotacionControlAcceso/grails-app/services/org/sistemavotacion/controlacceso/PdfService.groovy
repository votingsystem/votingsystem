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
import org.bouncycastle.cms.SignerId
import org.bouncycastle.ocsp.BasicOCSPResp
import org.bouncycastle.tsp.TimeStampToken
import org.bouncycastle.tsp.TimeStampTokenInfo
import org.sistemavotacion.seguridad.*
import org.springframework.context.ApplicationContext;
import org.sistemavotacion.util.*;
import java.util.Locale;
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
	private KeyStore trustedCertsKeyStore
	private PrivateKey key;
	private Certificate[] chain;
	
	
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
	}
	
	public Respuesta validarFirma (byte[] pdfFirmado, Evento evento, 
		Documento.Estado tipoDocumento, Locale locale) {
		log.debug "validarFirma - tipoDocumento: ${tipoDocumento.toString()} " + 
			"- pdfFirmado.length: ${pdfFirmado.length}"

		Date todayDate = DateUtils.getTodayDate()
		if(tipoDocumento.equals(Documento.Estado.FIRMA_DE_MANIFIESTO)) {
			if(todayDate.compareTo(evento.fechaFin) > 0)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:messageSource.getMessage('documentDateErrorMsg', 
				[DateUtils.getStringFromDate(todayDate), DateUtils.getStringFromDate(evento.fechaFin)].toArray(), locale))
		}
		PdfReader reader = new PdfReader(pdfFirmado);
		Documento documento;
		AcroFields acroFields = reader.getAcroFields();
		ArrayList<String> names = acroFields.getSignatureNames();
		Respuesta respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
			mensaje:messageSource.getMessage('error.documentWithoutSigners', null, locale));
		for (String name : names) {
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK);
			log.debug("Signature name: " + name + " - covers whole document:" + 
				acroFields.signatureCoversWholeDocument(name));
			//log.debug("Document revision: " + acroFields.getRevision(name) + 
			//" of " + acroFields.getTotalRevisions());
			// Start revision extraction
			/*FileOutputStream out = new FileOutputStream("revision_" + acroFields.getRevision(name) + ".pdf");
			byte buffer[] = new byte[8192];
			InputStream ip = acroFields.extractRevision(name);
			int n = 0;
			while ((n = ip.read(buffer)) > 0)
				out.write(buffer, 0, n);
			out.close();
			ip.close();*/
			PdfPKCS7 pk = acroFields.verifySignature(name, "BC");
			log.debug("Hash verified -> ${pk.verify()}");
			X509Certificate signingCert = pk.getSigningCertificate();
			Usuario usuario = Usuario.getUsuario(signingCert);
			log.debug("Subject: " + PdfPKCS7.getSubjectFields(pk.getSigningCertificate()));
			Calendar signDate = pk.getSignDate();
			X509Certificate[] pkc = (X509Certificate[])pk.getSignCertificateChain();
			TimeStampToken timeStampToken = pk.getTimeStampToken();
			if(!trustedCertsKeyStore) afterPropertiesSet();
			Object[] fails = PdfPKCS7.verifyCertificates(pkc, trustedCertsKeyStore, null, signDate);
			Certificado certificado = Certificado.findWhere(numeroSerie:signingCert.getSerialNumber()?.longValue())
			if (!certificado) {
				String subject = PdfPKCS7.getSubjectFields(pk.getSigningCertificate())
				Certificado certificadoCA
				for(X509Certificate certificate : pkc) {
					log.debug("Comprobando cadena cert - Num. serie: ${certificate?.getSerialNumber()?.longValue()}")
					if(signingCert.getSerialNumber()?.longValue() !=
						certificate.getSerialNumber()?.longValue()) {
						log.debug("Num. serie CA de pdf: ${certificate?.getSerialNumber()?.longValue()} - ${certificate.getSubjectDN().toString()}")
						certificadoCA = firmaService.getCertificadoCA(certificate.getSerialNumber()?.longValue())
						log.debug("CertificadoCA id: ${certificadoCA?.id}")
						usuario.setCertificadoCA(certificadoCA);
					}
				}
				Respuesta respuestaValidacionUsu = subscripcionService.guardarUsuario(usuario, locale);
				if(Respuesta.SC_OK != respuestaValidacionUsu.codigoEstado) return respuestaValidacionUsu;
				usuario = respuestaValidacionUsu.usuario;
				certificado = respuestaValidacionUsu.certificadoDB;
			} else usuario = certificado.usuario;
			String mensajeValidacionDocumento = messageSource.getMessage('signatureVerified', null, locale)
			switch(tipoDocumento) {
				case Documento.Estado.MANIFIESTO:
					documento = Documento.findWhere(evento:evento, estado:Documento.Estado.MANIFIESTO_VALIDADO)
					if(documento)
						mensajeValidacionDocumento = messageSource.getMessage('pdfManifestRepeated', 
							[evento.asunto, documento.usuario?.nif].toArray(), locale)
					else mensajeValidacionDocumento = messageSource.getMessage('pdfManifestOK', 
						[evento.asunto, usuario?.nif].toArray(), locale)
					break;
				case Documento.Estado.FIRMA_DE_MANIFIESTO:
					documento = Documento.findWhere(evento:evento, usuario:usuario, estado:Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
					if(documento)
						mensajeValidacionDocumento = messageSource.getMessage('pdfSignatureManifestRepeated',
						[usuario.nif, evento.asunto, DateUtils.getStringFromDate(documento.dateCreated)].toArray(), locale)
					else mensajeValidacionDocumento = messageSource.getMessage('pdfSignatureManifestOK',
						[evento.asunto, usuario.nif].toArray(), locale)
					break;
			}
			if(documento) {
				log.debug(mensajeValidacionDocumento)
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:mensajeValidacionDocumento);
			}
			if (fails == null) {
				certificado.estado = Certificado.Estado.OK
				certificado.save()
				log.debug("Certificado '${certificado.id}' verificado con KeyStore");
			} else {
				certificado.estado = Certificado.Estado.CON_ERRORES
				certificado.save()
				log.debug(" --- Certificado '${certificado.id}' con fallos: ${fails[1]}" );
				for(X509Certificate cert:pkc) {
					String notAfter = DateUtils.getStringFromDate(cert.getNotAfter())
					String notBefore = DateUtils.getStringFromDate(cert.getNotBefore())
					log.debug("Cert: ${cert.getSubjectDN()} - NotBefore: ${notBefore} - NotAfter: ${notAfter}")
				}
				return new Respuesta (codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:
					messageSource.getMessage('error.caUnknown', null, locale))
			}
			if (timeStampToken != null) {
				boolean impr = pk.verifyTimestampImprint();
				signDate= pk.getTimeStampDate();
				log.debug(" ---timeStampToken - verifyTimestampImprint: ${impr} - signDate:${signDate.getTime()}" );
				TimeStampTokenInfo tsInfo= timeStampToken.getTimeStampInfo();
				SignerId signer_id = timeStampToken.getSID();
				BigInteger cert_serial_number = signer_id.getSerialNumber();
				log.debug(" ---timeStampToken - Generation time " + tsInfo.getGenTime());
				log.debug(" ---timeStampToken - Signer ID serial " + signer_id.getSerialNumber());
				log.debug(" ---timeStampToken - Signer ID issuer " + signer_id.getIssuerAsString());
			}
			documento = new Documento(evento:evento, pdf:pdfFirmado, usuario:usuario, signDate:signDate?.getTime())
			if(pk.verify()) {
				log.debug("Documento sin modificaciones");
				switch(tipoDocumento) {
					case Documento.Estado.MANIFIESTO:
						documento.estado = Documento.Estado.MANIFIESTO_VALIDADO
						break;
					case Documento.Estado.FIRMA_DE_MANIFIESTO:
						documento.estado = Documento.Estado.FIRMA_MANIFIESTO_VALIDADA
						break;
				}
			} else {
				log.debug("Documento modificado");
				documento.estado = Documento.Estado.MODIFICADO
				respuesta = new Respuesta (codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:messageSource.getMessage('pdfSignedDocumentError', null, locale))
			}
			documento.save()
			
			/*log.debug("Keystore type : " + trustedCertsKeyStore.getType());
			Enumeration e = trustedCertsKeyStore.aliases();
			while(e.hasMoreElements()) {
				String alias = (String)e.nextElement();
				log.debug("alias : " + alias);	
			}*/
			

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
			if(Respuesta.SC_OK == respuesta.codigoEstado) {
				respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK, 
					mensaje:mensajeValidacionDocumento, documento:documento)
				if(Documento.Estado.MANIFIESTO.equals(tipoDocumento)) {
					log.debug "Salvando usuario en Evento"
					evento.estado = Evento.Estado.ACTIVO
					evento.usuario = usuario
					evento.save()
				} else log.debug "SIN SALVAR usuario en Evento"
			}
		}
		log.debug "Resultado validacion firmas: ${respuesta.mensaje}"
		return respuesta;
	}
		
	public Respuesta checkTimeStampToken(TimeStampToken timeStampToken) {
		TimeStampTokenInfo tokenInfo = timeStampToken.timeStampInfo
		log.debug(" -TimeStampToken Serial Number: " + tokenInfo.getSerialNumber());
	}
	
	public Respuesta validarSolicitudCopia (byte[] solicitudCopiaFirmada, Locale locale) {
		PdfReader reader = new PdfReader(solicitudCopiaFirmada);
		AcroFields form = reader.getAcroFields();
		String eventoId = form.getField("eventoId");
		if(!eventoId) return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
			mensaje:messageSource.getMessage('backupRequestEventWithoutIdErrorMsg', null, locale))
		def evento = Evento.get(new Long(eventoId))
		if(!evento) return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
			mensaje:messageSource.getMessage('backupRequestEventIdErrorMsg', [eventoId].toArray(), locale))
		if(!evento.copiaSeguridadDisponible) return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
			mensaje:messageSource.getMessage('eventWithoutBackup', [evento.asunto].toArray(), locale))
		String asunto = form.getField("asunto");
		String email = form.getField("email");
		if(!email) return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
			mensaje:messageSource.getMessage('backupRequestEmailMissingErrorMsg', null, locale))
		log.debug "eventoId: ${eventoId} - asunto: ${asunto} - email: ${email}"
		Documento documento;
		SolicitudCopia solicitudCopia;
		AcroFields acroFields = reader.getAcroFields();
		ArrayList<String> names = acroFields.getSignatureNames();
		Respuesta respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
			mensaje:messageSource.getMessage('error.documentWithoutSigners', null, locale));
		for (String name : names) {
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK);
			log.debug("Signature name: " + name + " - covers whole document:" + 
				acroFields.signatureCoversWholeDocument(name));
			PdfPKCS7 pk = acroFields.verifySignature(name);
			X509Certificate signingCert = pk.getSigningCertificate();
			Usuario usu = Usuario.getUsuario(signingCert);
			Usuario usuario = Usuario.findWhere(nif:usu.nif)
			if(!usuario) usuario = usu.save()
			log.debug("usuario: " + usuario.getDescription());
			log.debug("Subject: " + PdfPKCS7.getSubjectFields(pk.getSigningCertificate()));
			String mensajeValidacionDocumento = messageSource.getMessage('signatureVerified', null, locale)
			Calendar signDate = pk.getSignDate();
			X509Certificate[] pkc = (X509Certificate[])pk.getSignCertificateChain();
			TimeStampToken ts = pk.getTimeStampToken();
			if (ts != null) {
				boolean impr = pk.verifyTimestampImprint();
				signDate= pk.getTimeStampDate();
				log.debug("Timestamp imprint verifies: " + impr + " - Timestamp date: " + signDate);
			}
			documento = new Documento(evento:evento, pdf:solicitudCopiaFirmada, usuario:usuario, email:email)
			if(pk.verify()) {
				log.debug("Documento sin modificaciones");
				documento.estado = Documento.Estado.SOLICITUD_COPIA
			} else {
				log.debug("Documento modificado");
				documento.estado = Documento.Estado.MODIFICADO
				respuesta = new Respuesta (codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:messageSource.getMessage('pdfSignedDocumentError', null, locale))
			}
			documento.save()
			Object[] fails = PdfPKCS7.verifyCertificates(pkc, trustedCertsKeyStore, null, signDate);
			Certificado certificado = Certificado.findWhere(numeroSerie:signingCert.getSerialNumber().longValue())
			if (!certificado) {
				String subject = PdfPKCS7.getSubjectFields(pk.getSigningCertificate())
				certificado = new Certificado(numeroSerie:signingCert.getSerialNumber().longValue(),
					 contenido:signingCert.getEncoded(), usuario:usuario,  tipo:Certificado.Tipo.USUARIO)
			}
			if (fails == null) {
				certificado.estado = Certificado.Estado.OK
				certificado.save()
				log.debug("Certificado '${certificado.id}' verificado con KeyStore");
			} else {
				certificado.estado = Certificado.Estado.CON_ERRORES
				certificado.save()
				log.debug("Certificado '${certificado.id}' con fallos: ${fails[1]}");
				respuesta = new Respuesta (codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:
					messageSource.getMessage('error.caUnknown', null, locale))
			}
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
			if(Respuesta.SC_OK == respuesta.codigoEstado) {
				respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK,
					mensaje:messageSource.getMessage('backupRequestOKMsg', [email].toArray(), locale))
				runAsync { 
					Respuesta respuestaGeneracionBackup
					if(evento instanceof EventoFirma) {
						log.debug("---> EventoFirma")
						respuestaGeneracionBackup = eventoFirmaService.generarCopiaRespaldo((EventoFirma)evento, locale)
					} else if(evento instanceof EventoReclamacion) {
						log.debug("---> EventoReclamacion")
						respuestaGeneracionBackup = eventoReclamacionService.generarCopiaRespaldo((EventoReclamacion)evento, locale)
					} else if(evento instanceof EventoVotacion) {
						log.debug("---> EventoVotacion")
						respuestaGeneracionBackup = eventoVotacionService.generarCopiaRespaldo((EventoVotacion)evento, locale)
					}
					if(Respuesta.SC_OK == respuestaGeneracionBackup?.codigoEstado) {
						File archivoCopias = respuestaGeneracionBackup.file
						solicitudCopia = new SolicitudCopia(filePath:archivoCopias.getAbsolutePath(),
							documento:documento, email:email, numeroCopias:respuestaGeneracionBackup.cantidad)
						SolicitudCopia.withTransaction {
							solicitudCopia.save()
						}
						mailSenderService.sendInstruccionesDescargaCopiaSeguridad(solicitudCopia, locale)
					} else log.error("Error generando archivo de copias de respaldo");
				}
			}
		}
		log.debug "Resultado validacion firmas: ${respuesta.mensaje}"
		return respuesta;
	}
	
	public Respuesta firmar(PdfReader reader, String reason, String location, Documento documento) throws Exception {
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