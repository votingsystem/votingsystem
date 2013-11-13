package org.votingsystem.controlcenter.service

import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.votingsystem.controlcenter.model.*
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;

import java.security.cert.CertPathValidatorException
import java.security.cert.PKIXCertPathValidatorResult
import java.security.cert.TrustAnchor
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
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

import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.CertUtil;
import org.votingsystem.signature.smime.*;
import org.votingsystem.util.*;
import org.springframework.context.*

import java.util.Locale;
import grails.util.Metadata

class FirmaService {
	
	static transactional = false
	
	def grailsApplication;
	def messageSource;
	def subscripcionService
	def timeStampService
	private File certChainFile;
	
	private SignedMailGenerator signedMailGenerator;
	private static Set<X509Certificate> trustedCerts;
	private static HashMap<Long, Certificado> trustedCertsHashMap;
	private static HashMap<Long, Set<X509Certificate>> eventTrustedCertsHashMap = 
			new HashMap<Long, Set<X509Certificate>>();
	private static HashMap<Long, Set<X509Certificate>> eventValidationTrustedCertsHashMap =
			new HashMap<Long, Set<X509Certificate>>();
			

	public void inicializar() {
		log.debug "inicializar"
		File keyStore = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.keyStorePath).getFile()
		String aliasClaves = grailsApplication.config.VotingSystem.signKeysAlias
		String password = grailsApplication.config.VotingSystem.signKeysPassword
		signedMailGenerator = new SignedMailGenerator(FileUtils.getBytesFromFile(keyStore), 
			aliasClaves, password.toCharArray(), ContextVS.SIGN_MECHANISM);
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
		certChainFile = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.certChainPath)?.getFile();
		certChainFile.createNewFile()
		certChainFile.setBytes(pemCertsArray)
		inicializarAutoridadesCertificadoras();
	}
	
	
	public ResponseVS deleteTestCerts () {
		log.debug(" - deleteTestCerts - ")
		def certificadosTest = null
		Certificado.withTransaction {
			certificadosTest = Certificado.findAllWhere(type:Certificado.Type.AUTORIDAD_CERTIFICADORA_TEST);
			certificadosTest.each {
				it.delete()
			}
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK)
	}
	
	/*
	 * Método para poder añadir certificados de confianza en las pruebas de carga.
	 * El procedimiento para añadir una autoridad certificadora consiste en
	 * añadir el certificado en formato pem en el directorio ./WEB-INF/cms
	 */
	public ResponseVS addCertificateAuthority (byte[] caPEM, Locale locale)  {
		log.debug("addCertificateAuthority");
		if(grails.util.Environment.PRODUCTION  ==  grails.util.Environment.current) {
			log.debug(" ### ADDING CERTS NOT ALLOWED IN PRODUCTION ENVIRONMENTS ###")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message: messageSource.getMessage('serviceDevelopmentModeMsg', null, locale))
		}
		if(!caPEM) return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
			message: messageSource.getMessage('error.nullCertificate', null, locale))
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
							type:Certificado.Type.AUTORIDAD_CERTIFICADORA_TEST,
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
			return new ResponseVS(statusCode:ResponseVS.SC_OK,
				message:messageSource.getMessage('cert.newCACertMsg', null, locale))
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
		}
	}
	
	def inicializarAutoridadesCertificadoras() {
		try {

			trustedCertsHashMap = new HashMap<Long, Certificado>();
			File directory=  grailsApplication.mainContext.getResource(
				grailsApplication.config.VotingSystem.certAuthoritiesDirPath).getFile()
			File[] acFiles = directory.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String fileName) {
					return fileName.startsWith("AC_") && fileName.endsWith(".pem");
				}
			  });
			for(File caFile:acFiles) {
				trustedCerts.addAll(CertUtil.fromPEMToX509CertCollection(
					FileUtils.getBytesFromFile(caFile)));
			}
			Certificado.withTransaction {
				def criteria = Certificado.createCriteria()
				def trustedCertsDB = criteria.list {
					eq("estado", Certificado.Estado.OK)
					or {
						eq("type",	Certificado.Type.AUTORIDAD_CERTIFICADORA)
						if(grails.util.Environment.PRODUCTION  !=  grails.util.Environment.current) {
							eq("type", Certificado.Type.AUTORIDAD_CERTIFICADORA_TEST)
						}
					}
				}
				trustedCertsDB.each { certificado ->
					ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
					X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
					trustedCerts.add(certX509)
				}
			}
			
			for(X509Certificate certificate:trustedCerts) {
				log.debug " ---Added trustedCert authority -- SubjectDN: ${certificate?.getSubjectDN()} - número serie:${certificate?.getSerialNumber()?.longValue()}"
				Certificado certificado
				Certificado.withTransaction {
					certificado = Certificado.findByNumeroSerie(
						certificate.getSerialNumber().longValue())
				}
				if(!certificado) {
					boolean esRaiz = CertUtil.isSelfSigned(certificate)
					certificado = new Certificado(esRaiz:esRaiz, 
						type:Certificado.Type.AUTORIDAD_CERTIFICADORA,
						estado:Certificado.Estado.OK,
						contenido:certificate.getEncoded(),
						numeroSerie:certificate.getSerialNumber().longValue(),
						validoDesde:certificate.getNotBefore(),
						validoHasta:certificate.getNotAfter())
					Certificado.withTransaction {
						certificado.save()
					}
					log.debug " -- Added to database CA con id:'${certificado?.id}'"
				} else log.debug " -- CA: ${certificate.getSerialNumber().longValue()} ---database id: ${certificado?.id}"
				trustedCertsHashMap.put(certificate?.getSerialNumber()?.longValue(), certificado)
			}			
			log.debug("Número de Autoridades Certificadoras en sistema: ${trustedCerts?.size()}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Importadas Autoridades Certificadoras")
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
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
	
	public ResponseVS validateSMIMEVote(
		SMIMEMessageWrapper messageWrapper, Locale locale) {
		log.debug("validateSMIMEVote -")
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIMEVote - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		return validateVoteCerts(messageWrapper, locale)
	}
		
	public ResponseVS validateSMIMEVoteCancelation(String url,
		SMIMEMessageWrapper messageWrapper, Locale locale) {
		log.debug("validateSMIMEVoteCancelation -")
		EventoVotacion evento = null
		if(url) {
			EventoVotacion.withTransaction {
				evento = EventoVotacion.findByUrl(url)
			}
		}
		if(!evento) {
			String msg = messageSource.getMessage('evento.eventoNotFound',
				[url].toArray(), locale)
			log.error("validateSMIMEVoteCancelation - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		MessageSMIME messageSMIME = MessageSMIME.findWhere(
			base64ContentDigest:messageWrapper.getContentDigestStr())
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIMEVoteCancelation - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		return validateVoteValidationCerts(messageWrapper,	evento, locale)
	}

	public ResponseVS validateVoteCerts(SMIMEMessageWrapper smimeMessageReq, Locale locale) {
		String msg
		ResponseVS respuesta
		EventoVotacion evento
		InformacionVoto infoVoto = smimeMessageReq.informacionVoto
		Set<Usuario> votantes = null
		if(!infoVoto || !infoVoto.getCertificadoVoto()) {
			msg = messageSource.getMessage('error.documentWithoutSigners', null, locale)
			log.error ("validateVoteCerts - ERROR SIGNERS - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				type:TypeVS.VOTE_ERROR, message:msg)
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
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
				type:TypeVS.VOTE_ERROR)
		} 
		EventoVotacion.withTransaction {
			evento = EventoVotacion.findWhere(controlAcceso:controlAcceso,
				eventoVotacionId:infoVoto.getEventoId())
		}
		if (!evento) {
			msg = messageSource.getMessage('validacionVoto.convocatoriaDesconocida', null, locale)
			log.error ("validateVoteCerts - ERROR EVENT NOT FOUND - Event '${infoVoto?.getEventoId()}' " + 
				"voteAccessControlURL: '${controlAccesoURL}'")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
				type:TypeVS.VOTE_ERROR)
		}
		if(evento.estado != EventoVotacion.Estado.ACTIVO) {
			msg = messageSource.getMessage('validacionVoto.eventClosed', [evento.asunto].toArray(), locale)
			log.error ("validateVoteCerts - ERROR EVENT '${evento.id}' STATE -> ${evento.estado}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.VOTE_ERROR)
		}
		Certificado certificado = Certificado.findWhere(
			hashCertificadoVotoBase64:infoVoto.hashCertificadoVotoBase64)
		if (certificado) {
			log.error("validateVoteCerts - repeated vote - hashCertificadoVotoBase64:${infoVoto.hashCertificadoVotoBase64}")
			Voto voto = Voto.findWhere(certificado:certificado)
			msg = messageSource.getMessage('voteRepeatedErrorMsg', [voto.id].toArray(), locale)
			log.error("validateVoteCerts - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_VOTE_REPEATED,
				evento:evento, message:msg)
		}
		Set<X509Certificate> eventTrustedCerts = eventTrustedCertsHashMap.get(evento?.id)
		if(!eventTrustedCerts) {
			Certificado eventCACert
			Certificado.withTransaction {
				eventCACert = Certificado.findWhere(
					eventoVotacion:evento, estado:Certificado.Estado.OK,
					type:Certificado.TypeVS.RAIZ_VOTOS)
			}
			if(!eventCACert) {
				msg = messageSource.getMessage('certificado.caEventoNotFound', null, locale)
				log.error ("validateVoteCerts - Event '${evento.id}' without CA cert")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg)
			} 
			ByteArrayInputStream bais =	new ByteArrayInputStream(eventCACert.contenido)
			X509Certificate certCAEvento = CertUtil.loadCertificateFromStream (bais)
			eventTrustedCerts = new HashSet<X509Certificate>()
			eventTrustedCerts.add(certCAEvento)
			eventTrustedCertsHashMap.put(evento.id, eventTrustedCerts)
		}
		respuesta = timeStampService.validateToken(
				infoVoto.getVoteTimeStampToken(), evento, locale)
		if(ResponseVS.SC_OK != respuesta.statusCode) {
			respuesta.type = TypeVS.VOTE_ERROR
			respuesta.eventVS = evento
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
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, evento:evento, 
			smimeMessage:smimeMessageReq, usuarios:votantes)
	}
			
	public ResponseVS validateVoteValidationCerts(SMIMEMessageWrapper smimeMessageReq,
		EventoVotacion evento, Locale locale) {
		log.debug("validateVoteValidationCerts");
		Set<Usuario> firmantes = smimeMessageReq.getFirmantes();
		String msg
		if(firmantes.isEmpty()) {
			return new ResponseVS(
				statusCode:ResponseVS.SC_ERROR_REQUEST, message:messageSource.
					getMessage('error.documentWithoutSigners', null, locale))
		}
		Set<X509Certificate> eventTrustedCerts = eventValidationTrustedCertsHashMap.get(evento?.id)
		if(!eventTrustedCerts) {
			eventTrustedCerts = new HashSet<X509Certificate>()
			Collection<X509Certificate> controlAccesoCerts = CertUtil.
				fromPEMToX509CertCollection (evento.cadenaCertificacionControlAcceso)
			Collection<X509Certificate> centroControlCerts = CertUtil.
				fromPEMToX509CertCollection (certChainFile.getBytes())
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
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
			}
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, evento:evento,
			smimeMessage:smimeMessageReq)
	}
		
	public ResponseVS validateSMIME(
		SMIMEMessageWrapper smimeMessageReq, Locale locale) {
		log.debug("validateSMIME -")
		MessageSMIME messageSMIME = MessageSMIME.findWhere(base64ContentDigest:smimeMessageReq.getContentDigestStr())
		if(messageSMIME) {
			String msg = messageSource.getMessage('smimeDigestRepeatedErrorMsg',
				[smimeMessageReq.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIME - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg)
		}
		return validateSignersCerts(smimeMessageReq, locale)
	}
	
		
	public ResponseVS validateSignersCerts(
			SMIMEMessageWrapper smimeMessageReq, Locale locale) {
		Set<Usuario> firmantes = smimeMessageReq.getFirmantes();
		if(firmantes.isEmpty()) return new ResponseVS(
			statusCode:ResponseVS.SC_ERROR_REQUEST, message:
			messageSource.getMessage('error.documentWithoutSigners', null, locale))
		log.debug("validateSignersCerts - number of signers: ${firmantes.size()}")
		Set<Usuario> firmantesDB = new HashSet<Usuario>()
		for(Usuario usuario: firmantes) {
			try {
				PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
					usuario.getCertificate(), getTrustedCerts(), false)
				TrustAnchor ta = pkixResult.getTrustAnchor();
				X509Certificate certCaResult = ta.getTrustedCert();
				usuario.certificadoCA = trustedCertsHashMap.get(
					certCaResult?.getSerialNumber()?.longValue())
				log.debug("Certificado de usuario emitido por: " +
						certCaResult?.getSubjectDN()?.toString() +
						"- numserie: " + certCaResult?.getSerialNumber()?.longValue());
				ResponseVS respuesta = subscripcionService.checkUser(usuario, locale)
				if(ResponseVS.SC_OK != respuesta.statusCode) return respuesta
				else {
					Usuario usuarioDB = respuesta.usuario
					usuarioDB.setCertificate(usuario.getCertificate())
					firmantesDB.add(usuarioDB)
				}
			} catch (CertPathValidatorException ex) {
				log.error(ex.getMessage(), ex)
				log.error(" --- Error with certificate: ${usuario.getCertificate().getSubjectDN()}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:messageSource.getMessage('error.caUnknown', null, locale))
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex)
				return new ResponseVS(
					statusCode:ResponseVS.SC_ERROR_REQUEST, message:ex.getMessage())
			}
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK,
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

		
	public synchronized SMIMEMessageWrapper getMultiSignedMimeMessage (
		String fromUser, String toUser,	final SMIMEMessageWrapper smimeMessage, String subject) {
		log.debug("getMultiSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'");
		if(fromUser) {
			fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setFrom(new InternetAddress(fromUser))
		}
		if(toUser) {
			toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
			smimeMessage.setHeader("To", toUser)
		}
		SMIMEMessageWrapper multifirma = getSignedMailGenerator().
				genMultiSignedMessage(smimeMessage, subject);
		return multifirma
	}
	
	public Set<X509Certificate> getTrustedCerts() {
		if(!trustedCerts || trustedCerts.isEmpty()) {
			inicializar()
		}
		return trustedCerts;
	}
	
	public File getCadenaCertificacion() {
		if(!certChainFile) inicializar()
		return certChainFile;
	}
	
	private SignedMailGenerator getSignedMailGenerator() {
		return signedMailGenerator
	}
	
}