package org.sistemavotacion.centrocontrol

import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.sistemavotacion.centrocontrol.modelo.*

import java.security.cert.CertPathValidatorException
import java.security.cert.PKIXCertPathValidatorResult
import java.security.cert.TrustAnchor
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import java.security.PrivateKey;
import java.security.cert.PKIXCertPathValidatorResult
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate;
import javax.security.auth.x500.X500PrivateCredential;
import java.security.KeyStore;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import javax.mail.*;
import javax.mail.internet.*

import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.smime.*;
import org.sistemavotacion.util.*;
import org.springframework.context.*
import org.sistemavotacion.exception.*
import java.util.Locale;

class FirmaService {
	
	static transactional = false
	
	private static final String BC = org.bouncycastle.jce.provider.BouncyCastleProvider.PROVIDER_NAME;
	
	def grailsApplication;
	def messageSource;
	def subscripcionService
	def timeStampService
	private File cadenaCertificacion;
	
	private SignedMailGenerator signedMailGenerator;
	private static Set<X509Certificate> trustedCerts;
	private static HashMap<Long, Certificado> trustedCertsHashMap;
	private static HashMap<Long, Set<X509Certificate>> eventTrustedCertsHashMap = 
			new HashMap<Long, Set<X509Certificate>>();
	private static HashMap<Long, Set<X509Certificate>> eventValidationTrustedCertsHashMap =
			new HashMap<Long, Set<X509Certificate>>();
			

	public void inicializar() {
		log.debug "inicializar"
		cadenaCertificacion = grailsApplication.mainContext.getResource(
			grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion)?.getFile();
		File keyStore = grailsApplication.mainContext.getResource(
			grailsApplication.config.SistemaVotacion.rutaAlmacenClaves).getFile()
		String aliasClaves = grailsApplication.config.SistemaVotacion.aliasClavesFirma
		String password = grailsApplication.config.SistemaVotacion.passwordClavesFirma
		signedMailGenerator = new SignedMailGenerator(FileUtils.getBytesFromFile(keyStore), 
			aliasClaves, password.toCharArray());
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(new FileInputStream(keyStore), password.toCharArray());
		java.security.cert.Certificate[] chain = ks.getCertificateChain(aliasClaves);
		byte[] pemCertsArray
		trustedCerts = new HashSet<X509Certificate>()
		for (int i = 0; i < chain.length; i++) {
			log.debug " --- inicializar - Adding local kesystore cert '${i}' -> 'SubjectDN: ${chain[i].getSubjectDN()}'"
			trustedCerts.add(chain[i])
			if(!pemCertsArray) pemCertsArray = CertUtil.fromX509CertToPEM (chain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtil.fromX509CertToPEM (chain[i]))
		}
		def rutaCadenaCertificacion = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion}")
		new File(rutaCadenaCertificacion).setBytes(pemCertsArray)
		inicializarAutoridadesCertificadoras();
	}
	
	public Respuesta deleteTestCerts () {
		log.debug(" - deleteTestCerts - ")
		def certificadosTest = null
		Certificado.withTransaction {
			certificadosTest = Certificado.findAllWhere(tipo:Certificado.Tipo.AUTORIDAD_CERTIFICADORA_TEST);
			certificadosTest.each {
				it.delete()
			}
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK)
	}
	
	/*
	 * Método para poder añadir certificados de confianza en las pruebas de carga.
	 * El procedimiento para añadir una autoridad certificadora consiste en
	 * añadir el certificado en formato pem en el directorio ./WEB-INF/cms
	 */
	public Respuesta addCertificateAuthority (byte[] caPEM, Locale locale)  {
		log.debug("addCertificateAuthority");
		if(!caPEM) return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
			mensaje: messageSource.getMessage('error.nullCertificate', null, locale))
		try {
			Collection<X509Certificate> certX509CertCollection = CertUtil.fromPEMToX509CertCollection(caPEM)
			for(X509Certificate cert: certX509CertCollection) {
				log.debug(" ------- addCertificateAuthority - adding cert: ${cert.getSubjectDN()}" );
				Certificado certificado = null
				Certificado.withTransaction {
					certificado = Certificado.findByNumeroSerie(
						cert?.getSerialNumber()?.longValue())
					if(!certificado) {
						boolean esRaiz = CertUtil.isSelfSigned(cert)
						certificado = new Certificado(esRaiz:esRaiz,
							tipo:Certificado.Tipo.AUTORIDAD_CERTIFICADORA_TEST,
							estado:Certificado.Estado.OK,
							contenido:cert.getEncoded(),
							numeroSerie:cert.getSerialNumber()?.longValue(),
							validoDesde:cert.getNotBefore(),
							validoHasta:cert.getNotAfter())
						certificado.save()
						trustedCertsHashMap.put(cert?.getSerialNumber()?.longValue(), certificado)
					}
				}
				trustedCerts.addAll(certX509CertCollection)
				log.debug "Almacenada Autoridad Certificadora de pruebas con id:'${certificado?.id}'"
			}
			return new Respuesta(codigoEstado:Respuesta.SC_OK,
				mensaje:messageSource.getMessage('cert.newCACertMsg', null, locale))
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:ex.getMessage())
		}
	}
	
	def inicializarAutoridadesCertificadoras() {
		try {

			trustedCertsHashMap = new HashMap<Long, Certificado>();
			String rutaDirectorioArchivosCA = getAbsolutePath(
				"${grailsApplication.config.SistemaVotacion.rutaDirectorioArchivosCA}")
			log.debug("rutaDirectorioArchivosCA: ${rutaDirectorioArchivosCA}")
			File directory= new File(rutaDirectorioArchivosCA);
			File[] acFiles = directory.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String fileName) {
					return fileName.startsWith("AC_") && fileName.endsWith(".pem");
				}
			  });
			for(File caFile:acFiles) {
				trustedCerts.addAll(CertUtil.fromPEMToX509CertCollection(
					FileUtils.getBytesFromFile(caFile)));
			}
			for(X509Certificate certificate:trustedCerts) {
				log.debug " --- Importado certificado -- SubjectDN: ${certificate?.getSubjectDN()} - número serie:${certificate?.getSerialNumber()?.longValue()}"
				Certificado certificado
				Certificado.withTransaction {
					certificado = Certificado.findByNumeroSerie(
						certificate.getSerialNumber().longValue())
				}
				if(!certificado) {
					boolean esRaiz = CertUtil.isSelfSigned(certificate)
					certificado = new Certificado(esRaiz:esRaiz, 
						tipo:Certificado.Tipo.AUTORIDAD_CERTIFICADORA,
						estado:Certificado.Estado.OK,
						contenido:certificate.getEncoded(),
						numeroSerie:certificate.getSerialNumber().longValue(),
						validoDesde:certificate.getNotBefore(),
						validoHasta:certificate.getNotAfter())
					Certificado.withTransaction {
						certificado.save()
					}
					log.debug " -- Almacenada CA con id:'${certificado?.id}'"
				} else log.debug " -- Id de certificado: ${certificado?.id}"
				trustedCertsHashMap.put(certificate?.getSerialNumber()?.longValue(), certificado)
			}
			log.debug("Número de Autoridades Certificadoras en sistema: ${trustedCerts?.size()}")
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:"Importadas Autoridades Certificadoras")
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:ex.getMessage())
		}
	}
	
	public boolean isSignerCertificate(Set<Usuario> signers, X509Certificate cert) {
		boolean result = false
		log.debug "isSignerCertificate - cert num. serie: ${cert.getSerialNumber().longValue()}"
		signers.each {
			long signerId = ((Usuario)it).getCertificate().getSerialNumber().longValue()
			if(signerId == cert.getSerialNumber().longValue()) result = true;
		}
		return result
	}
	
	/*
	 * Método para poder añadir certificados de confianza en las pruebas de carga.
	 * El procedimiento para añadir una autoridad certificadora consiste en 
	 * añadir el certificado en formato pem en el directorio ./WEB-INF/cms
	 */
	public Respuesta addCertificateAuthority (byte[] caPEM)  {
		log.debug("addCertificateAuthority");
		if(!caPEM) 
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:"Certificado nulo")
		try {
			X509Certificate certificadoX509 = CertUtil.fromPEMToX509Cert(caPEM)
			trustedCerts.add(certificadoX509)
			Certificado certificado = Certificado.findByNumeroSerie(
				certificadoX509?.getSerialNumber()?.longValue())
			if(!certificado) {
				boolean esRaiz = CertUtil.isSelfSigned(certificadoX509)
				certificado = new Certificado(esRaiz:esRaiz, 
					tipo:Certificado.Tipo.AUTORIDAD_CERTIFICADORA_TEST,
					estado:Certificado.Estado.OK,
					contenido:certificadoX509.getEncoded(),
					numeroSerie:certificadoX509.getSerialNumber().longValue(),
					validoDesde:certificadoX509.getNotBefore(),
					validoHasta:certificadoX509.getNotAfter())
				certificado.save()
				trustedCertsHashMap.put(certificadoX509?.getSerialNumber()?.longValue(), certificado)
				log.debug "Almacenada CA de pruebas con id:'${certificado?.id}'"
			} else log.debug "CA de pruebas con id de certificado: ${certificado?.id}"
			return new Respuesta(codigoEstado:Respuesta.SC_OK, 
				mensaje:"Certificado de CA de pruebas añadido a la lista de confianza")
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:ex.getMessage())
		}
	}
	
	public Respuesta validateSMIMEVote(
		SMIMEMessageWrapper messageWrapper, Locale locale) {
		log.debug("validateSMIMEVote -")
		MensajeSMIME mensajeSMIME = MensajeSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(mensajeSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIMEVote - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
		}
		return validateVoteCerts(messageWrapper, locale)
	}

	public Respuesta validateVoteCerts(SMIMEMessageWrapper smimeMessageReq, Locale locale) {
		String msg
		Respuesta respuesta
		EventoVotacion evento
		InformacionVoto infoVoto = smimeMessageReq.informacionVoto
		Set<Usuario> votantes = null
		if(!infoVoto || !infoVoto.getCertificadoVoto()) {
			msg = messageSource.getMessage('error.documentWithoutSigners', null, locale)
			log.error ("validateVoteCerts - ERROR SIGNERS - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				tipo:Tipo.VOTO_CON_ERRORES, mensaje:msg)
		} 
		if (infoVoto.getRepresentativeURL()) {
			votantes = new HashSet<Usuario>();
			Usuario representative = Usuario.findWhere(url:infoVoto.getRepresentativeURL())
			if(!representative) {
				representative = new Usuario(
					url:infoVoto.getRepresentativeURL(), type:Usuario.Type.REPRESENTATIVE)
				representative.save()
			} 
			votantes.add(representative)
		}
		String controlAccesoURL = infoVoto.controlAccesoURL
		ControlAcceso controlAcceso = ControlAcceso.findWhere(serverURL:controlAccesoURL)
		if (!controlAcceso) {
			msg = messageSource.getMessage('validacionVoto.errorEmisorDesconocido', null, locale)
			log.error ("validateVoteCerts - ERROR SERVER URL - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg,
				tipo:Tipo.VOTO_CON_ERRORES)
		} 
		EventoVotacion.withTransaction {
			evento = EventoVotacion.findWhere(controlAcceso:controlAcceso,
				eventoVotacionId:infoVoto.getEventoId())
		}
		if (!evento) {
			msg = messageSource.getMessage('validacionVoto.convocatoriaDesconocida', null, locale)
			log.error ("validateVoteCerts - ERROR EVENT NOT FOUND - Event '${infoVoto?.getEventoId()}' " + 
				"voteAccessControlURL: '${controlAccesoURL}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg,
				tipo:Tipo.VOTO_CON_ERRORES)
		}
		if(evento.estado != EventoVotacion.Estado.ACTIVO) {
			msg = messageSource.getMessage('validacionVoto.eventClosed', [evento.asunto].toArray(), locale)
			log.error ("validateVoteCerts - ERROR EVENT '${evento.id}' STATE -> ${evento.estado}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg,
				tipo:Tipo.VOTO_CON_ERRORES)
		}
		Certificado certificado = Certificado.findWhere(
			hashCertificadoVotoBase64:infoVoto.hashCertificadoVotoBase64)
		if (certificado) {
			log.error("validateVoteCerts - repeated vote - hashCertificadoVotoBase64:${infoVoto.hashCertificadoVotoBase64}")
			Voto voto = Voto.findWhere(certificado:certificado)
			msg = messageSource.getMessage('voteRepeatedErrorMsg', [voto.id].toArray(), locale)
			log.error("validateVoteCerts - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_VOTO_REPETIDO,
				evento:evento, mensaje:msg)
		}
		Set<X509Certificate> eventTrustedCerts = eventTrustedCertsHashMap.get(evento?.id)
		if(!eventTrustedCerts) {
			Certificado eventCACert
			Certificado.withTransaction {
				eventCACert = Certificado.findWhere(
					eventoVotacion:evento, estado:Certificado.Estado.OK,
					tipo:Certificado.Tipo.RAIZ_VOTOS)
			}
			if(!eventCACert) {
				msg = messageSource.getMessage('certificado.caEventoNotFound', null, locale)
				log.error ("validateVoteCerts - Event '${evento.id}' without CA cert")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg)
			} 
			ByteArrayInputStream bais =	new ByteArrayInputStream(eventCACert.contenido)
			X509Certificate certCAEvento = CertUtil.loadCertificateFromStream (bais)
			eventTrustedCerts = new HashSet<X509Certificate>()
			eventTrustedCerts.add(certCAEvento)
			eventTrustedCertsHashMap.put(evento.id, eventTrustedCerts)
		}
		respuesta = timeStampService.validateToken(
			infoVoto.getVoteTimeStampToken(), evento, locale)
		if(Respuesta.SC_OK != respuesta.codigoEstado) {
			respuesta.tipo = Tipo.VOTO_CON_ERRORES
			return respuesta
		}
		X509Certificate checkedCert = infoVoto.getCertificadoVoto()
		log.debug("validateVoteCerts - validating vote: ${checkedCert.getSubjectDN()}")
		try {
			PKIXCertPathValidatorResult pkixResult = CertUtil.
				verifyCertificate(checkedCert, eventTrustedCerts, false)
			TrustAnchor ta = pkixResult.getTrustAnchor();
			X509Certificate certCaResult = ta.getTrustedCert();
			log.debug("validateVoteCerts - certCaResult: " + certCaResult?.getSubjectDN()?.toString()+
					"- numserie: " + certCaResult?.getSerialNumber()?.longValue());	
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('certValidationErrorMsg',
					[checkedCert.getSubjectDN()?.toString()].toArray(), locale)
			log.error ("validateVoteCerts - msg:{msg} - Event '${evento.id}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento, 
			smimeMessage:smimeMessageReq, usuarios:votantes)
	}
			
	public Respuesta validateVoteValidationCerts(SMIMEMessageWrapper smimeMessageReq,
		EventoVotacion evento, Locale locale) {
		log.debug("validateAccessControlVoteCerts");
		Set<Usuario> firmantes = smimeMessageReq.getFirmantes();
		String msg
		if(firmantes.isEmpty()) {
			return new Respuesta(
				codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:messageSource.
					getMessage('error.documentWithoutSigners', null, locale))
		}
		Set<X509Certificate> eventTrustedCerts = eventValidationTrustedCertsHashMap.get(evento?.id)
		if(!eventTrustedCerts) {
			eventTrustedCerts = new HashSet<X509Certificate>()
			Collection<X509Certificate> controlAccesoCerts = CertUtil.
				fromPEMToX509CertCollection (evento.cadenaCertificacionControlAcceso)
			Collection<X509Certificate> centroControlCerts = CertUtil.
				fromPEMToX509CertCollection (cadenaCertificacion.getBytes())
			eventTrustedCerts.addAll(controlAccesoCerts)
			eventTrustedCerts.addAll(centroControlCerts)
			eventTrustedCerts.addAll(eventTrustedCertsHashMap.get(evento?.id))
			eventTrustedCertsHashMap.put(evento.id, eventTrustedCerts)
		}
		log.debug("validateAccessControlVoteCerts - num. firmantes: ${firmantes.size()} " + 
			" - num. certs de confianza de evento: ${eventTrustedCerts.size()}")
		for(Usuario usuario: firmantes) {
			log.debug("validateAccessControlVoteCerts - validating signer: ${usuario.getCertificate().getSubjectDN()}")
			try {
				PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
					usuario.getCertificate(), eventTrustedCerts, false)
				TrustAnchor ta = pkixResult.getTrustAnchor();
				X509Certificate certCaResult = ta.getTrustedCert();
				log.debug("validateAccessControlVoteCerts - certCaResult: " + certCaResult?.getSubjectDN()?.toString()+
						"- numserie: " + certCaResult?.getSerialNumber()?.longValue());
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex)
				msg = messageSource.getMessage('certValidationErrorMsg',
						[usuario.getCertificate().getSubjectDN()?.toString()].toArray(), locale)
				log.error ("validateAccessControlVoteCerts - msg:{msg} - Event '${evento.id}'")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
			}
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento,
			smimeMessage:smimeMessageReq)
	}
		
	public Respuesta validateSMIME(
		SMIMEMessageWrapper smimeMessageReq, Locale locale) {
		log.debug("validateSMIME -")
		MensajeSMIME mensajeSMIME = MensajeSMIME.findWhere(base64ContentDigest:smimeMessageReq.getContentDigestStr())
		if(mensajeSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[smimeMessageReq.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIME - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg)
		}
		return validateSignersCerts(smimeMessageReq, locale)
	}
	
		
	public Respuesta validateSignersCerts(
			SMIMEMessageWrapper smimeMessageReq, Locale locale) {
		Set<Usuario> firmantes = smimeMessageReq.getFirmantes();
		if(firmantes.isEmpty()) return new Respuesta(
			codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:
			messageSource.getMessage('error.documentWithoutSigners', null, locale))
		log.debug("*** validateSignersCerts - number of signers: ${firmantes.size()}")
		Set<Usuario> firmantesDB = new HashSet<Usuario>()
		for(Usuario usuario: firmantes) {
			try {
				PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
					usuario.getCertificate(), trustedCerts, false)
				TrustAnchor ta = pkixResult.getTrustAnchor();
				X509Certificate certCaResult = ta.getTrustedCert();
				usuario.certificadoCA = trustedCertsHashMap.get(
					certCaResult?.getSerialNumber()?.longValue())
				log.debug("Certificado de usuario emitido por: " +
						certCaResult?.getSubjectDN()?.toString() +
						"- numserie: " + certCaResult?.getSerialNumber()?.longValue());
				Respuesta respuesta = subscripcionService.checkUser(usuario, locale)
				if(Respuesta.SC_OK != respuesta.codigoEstado) return respuesta
				else {
					Usuario usuarioDB = respuesta.usuario
					usuarioDB.setCertificate(usuario.getCertificate())
					firmantesDB.add(usuarioDB)
				}
			} catch (CertPathValidatorException ex) {
				log.error(ex.getMessage(), ex)
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:messageSource.getMessage('error.caUnknown', null, locale))
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex)
				return new Respuesta(
					codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:ex.getMessage())
			}
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK,
			smimeMessage:smimeMessageReq, usuarios:firmantesDB)
	}
		
	public File obtenerArchivoFirmado (String fromUser, String toUser, 
		String textToSign, String subject, Header header) {
		log.debug "obtenerArchivoFirmado - textoAFirmar: ${textToSign}"
		if(signedMailGenerator == null) inicializar()
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(
			fromUser, toUser, textToSign, subject, header)
		File resultFile = File.createTempFile("smime", "p7m");
		mimeMessage.writeTo(new FileOutputStream(resultFile));
		return resultFile
	}
		
	public byte[] getSignedMimeMessage (String fromUser, String toUser, 
		String textToSign, String subject, Header header) {
		log.debug "getSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(signedMailGenerator == null) inicializar()
		if(fromUser) {
			fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		}
		if(toUser) {
			toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		}
		fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(
			fromUser, toUser, textToSign, subject, header)
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		mimeMessage.writeTo(baos);
		baos.close();
		return baos.toByteArray();
	}

	public synchronized SMIMEMessageWrapper getMultiSignedMimeMessage (String fromUser,
		String toUser, SMIMEMessageWrapper smimeMessage, String subject) {
		log.debug("getMultiSignedMimeMessage- subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
		if(signedMailGenerator == null) inicializar()
		if(fromUser) {
			fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setFrom(new InternetAddress(fromUser))
		} 
		if(toUser) {
			toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setTo(toUser)
		}
		if(fromUser) smimeMessage.setFrom(new InternetAddress(fromUser))
		if(toUser) smimeMessage.setTo(toUser)
		MimeMessage multifirma = getSignedMailGenerator().
			genMultiSignedMessage(smimeMessage, subject); 
		return multifirma
	}
	
	public String getAbsolutePath(String filePath){
		String prefijo = "${grailsApplication.mainContext.getResource('.')?.getFile()}"
		String sufijo =filePath.startsWith(File.separator)? filePath : File.separator + filePath;
		return "${prefijo}${sufijo}";
	}
	
	public File getCadenaCertificacion() {
		if(!cadenaCertificacion) inicializar()
		return cadenaCertificacion;
	}
	
	private SignedMailGenerator getSignedMailGenerator() {
		return signedMailGenerator
	}
	
}