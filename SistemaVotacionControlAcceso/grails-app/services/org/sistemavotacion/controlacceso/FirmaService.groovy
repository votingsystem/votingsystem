package org.sistemavotacion.controlacceso

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Properties;
import javax.mail.Header;
import javax.mail.Session;
import javax.mail.internet.MimeMultipart;
import org.springframework.beans.factory.InitializingBean
import org.sistemavotacion.util.*
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.security.cert.CertPathValidatorException
import java.security.cert.PKIXCertPathValidatorResult
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMESigned
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.asn1.x509.X509Extensions
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSVerifierCertificateNotValidException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationVerifier
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.util.Store;

import java.util.Locale;

//class FirmaService implements InitializingBean {
class FirmaService {

	private static final String BC = BouncyCastleProvider.PROVIDER_NAME;
	
	private SignedMailGenerator signedMailGenerator;
	static Set<X509Certificate> trustedCerts;
	static HashMap<Long, Certificado> trustedCertsHashMap;
	private X509Certificate localServerCertSigner;
	private static HashMap<Long, Set<X509Certificate>> eventTrustedCertsHashMap = 
		new HashMap<Long, Set<X509Certificate>>();
	private static HashMap<Long, Set<X509Certificate>> eventValidationTrustedCertsHashMap =
		new HashMap<Long, Set<X509Certificate>>();
	def grailsApplication;
	def messageSource
	def csrService;
	def encryptionService;
	def subscripcionService
	def timeStampService
	boolean testMode = false
	
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
	
	//@Override
	public void afterPropertiesSet() throws Exception {
		log.debug(" - afterPropertiesSet - ")
		def rutaAlmacenClaves = getAbsolutePath(
			"${grailsApplication.config.SistemaVotacion.rutaAlmacenClaves}")
		File keyStore = new File(rutaAlmacenClaves);
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
		
		localServerCertSigner = (X509Certificate) ks.getCertificate(aliasClaves);
		trustedCerts.add(localServerCertSigner)
		
		def rutaCadenaCertificacion = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion}")
		new File(rutaCadenaCertificacion).setBytes(pemCertsArray)
		inicializarAutoridadesCertificadoras();
	}
	
	public boolean isSystemSignedMessage(Set<Usuario> signers) {
		boolean result = false
		log.debug "isSystemSignedMessage - localServerCert num. serie: ${localServerCertSigner.getSerialNumber().longValue()}"
		signers.each {
			long signerId = ((Usuario)it).getCertificate().getSerialNumber().longValue()
			log.debug " --- num serie signer: ${signerId}"
			if(signerId == localServerCertSigner.getSerialNumber().longValue()) result = true;
		}
		return result
	}
	
	public Set<X509Certificate> getTrustedCerts() {
		if(!trustedCerts || trustedCerts.isEmpty()) {
			afterPropertiesSet()
		}
		return trustedCerts;
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
		   Set<X509Certificate> fileSystemCerts = new HashSet<X509Certificate>()
			for(File caFile:acFiles) {
				fileSystemCerts.addAll(CertUtil.fromPEMToX509CertCollection(
					FileUtils.getBytesFromFile(caFile)));
			}
			for(X509Certificate certificate:fileSystemCerts) {
				long numSerie = certificate.getSerialNumber().longValue()
				log.debug " --- Importado certificado -- SubjectDN: ${certificate?.getSubjectDN()} --- número serie:${numSerie}"
				Certificado certificado = null
				Certificado.withTransaction {
					certificado = Certificado.findWhere(numeroSerie:numSerie)
				}
				if(!certificado) {
					boolean esRaiz = CertUtil.isSelfSigned(certificate)
					certificado = new Certificado(esRaiz:esRaiz, tipo:Certificado.Tipo.AUTORIDAD_CERTIFICADORA,
						estado:Certificado.Estado.OK,
						contenido:certificate.getEncoded(),
						numeroSerie:certificate.getSerialNumber().longValue(),
						validoDesde:certificate.getNotBefore(),
						validoHasta:certificate.getNotAfter())
					certificado.save()
					log.debug " -- Almacenada CA con id:'${certificado?.id}'"
				} else {
					if(Certificado.Estado.ANULADO == certificado.estado) {
						log.debug "El certificado.id '${certificado.id}' ${certificate.subjectDN} " + 
							" - Pasa de ANULADO a OK" 
						certificado.estado = Certificado.Estado.OK;
						certificado.save()
						//grailsApplication.mainContext.close()
					}
				}
				
			}
			
			Certificado.withTransaction {
				def criteria = Certificado.createCriteria()
				def trustedCertsDB = criteria.list {
					eq("estado", Certificado.Estado.OK)
					or {
						eq("tipo",	Certificado.Tipo.AUTORIDAD_CERTIFICADORA)
						eq("tipo", Certificado.Tipo.AUTORIDAD_CERTIFICADORA_TEST)
					}
				}
				trustedCertsDB.each { certificado ->
					ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
					X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
					
					trustedCerts.add(certX509)
					trustedCertsHashMap.put(certX509?.getSerialNumber()?.longValue(), certificado)
				}
			}

			log.debug("trustedCerts.size(): ${trustedCerts?.size()}")
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:"Importadas Autoridades Certificadoras")
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:ex.getMessage())
		}
	}
	
	def checkCancelledCerts () {
		log.debug "checkCancelledCerts - checkCancelledCerts"
		String caDirPath = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaDirectorioArchivosCA}")
		String cancelSufix = "${grailsApplication.config.SistemaVotacion.cancelSufix}".toUpperCase()
		log.debug ("caDirPath: ${caDirPath} - cancelSufix: ${cancelSufix}")
		new File(caDirPath).eachFile() { file ->
			String fileName = file.getName().toUpperCase()
			if(fileName.endsWith(cancelSufix)) {
				int idx = fileName.indexOf(cancelSufix)
				fileName = fileName.substring(0, idx);
				if(fileName.endsWith("JKS")) {
					log.debug ("--- cancelando JKS -> " + fileName)
					KeyStore ks = KeyStore.getInstance("JKS");
					String password = grailsApplication.config.SistemaVotacion.passwordClavesFirma
					String aliasClaves = grailsApplication.config.SistemaVotacion.aliasClavesFirma
					ks.load(new FileInputStream(file), password.toCharArray());
					java.security.cert.Certificate[] chain = ks.getCertificateChain(aliasClaves);				
					for (int i = 0; i < chain.length; i++) {
						X509Certificate cert = chain[i]
						cancelCert(cert.getSerialNumber().longValue())
					}
					file.delete();
				} else if (fileName.endsWith("PEM")) {
					log.debug ("--- cancelando PEM -> " + fileName)
					Collection<X509Certificate> certificates = CertUtil.fromPEMToX509CertCollection(
						FileUtils.getBytesFromFile(file))
					for (X509Certificate cert :certificates) {
						cancelCert(cert.getSerialNumber().longValue())
					}
					file.delete();
				}
			}
		}
	}
	
	private void cancelCert(long numSerieCert) {
		log.debug "cancelCert - numSerieCert: ${numSerieCert}"
		Certificado.withTransaction {
			Certificado certificado = Certificado.findWhere(numeroSerie:numSerieCert)
			if(certificado) {
				log.debug "Comprobando certificado.id '${certificado?.id}'  --- "
				if(Certificado.Estado.OK == certificado.estado) {
					certificado.cancelDate = new Date(System.currentTimeMillis());
					certificado.estado = Certificado.Estado.ANULADO;
					certificado.save()
					log.debug "cancelado certificado '${certificado?.id}'"
				} else log.debug "El certificado.id '${certificado?.id}' ya estaba cancelado"
			} else log.debug "No hay ningún certificado con num. serie '${numSerieCert}'"
		}
	}
	
	public File obtenerArchivoFirmado (String fromUser, String toUser,
		String textToSign, String subject, Header header) {
		log.debug "obtenerArchivoFirmado - textoAFirmar: ${textToSign}"
		MimeMessage mimeMessage = getSignedMailGenerator().genMimeMessage(
			fromUser, toUser, textToSign, subject, header)
		File resultFile = File.createTempFile("smime", "p7m");
		resultFile.deleteOnExit();
		mimeMessage.writeTo(new FileOutputStream(resultFile));
		return resultFile
	}
		
	public byte[] getSignedMimeMessage (String fromUser, String toUser,
		String textToSign, String subject, Header header) {
		log.debug "getSignedMimeMessage - subject '${subject}' - fromUser '${fromUser}' to user '${toUser}'"
		if(fromUser) {
			fromUser = fromUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		}
		if(toUser) {
			toUser = toUser?.replaceAll(" ", "_").replaceAll("[\\/:.]", "")
		}
		MimeMessage mimeMessage = getSignedMailGenerator().
			genMimeMessage(fromUser, toUser, textToSign, subject, header)
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
			smimeMessage.setTo(toUser)
		}
		SMIMEMessageWrapper multifirma = getSignedMailGenerator().
				genMultiSignedMessage(smimeMessage, subject);
		return multifirma
	}

	public Respuesta firmarCertificadoVoto (byte[] csr, Evento evento, 
		Usuario representative, Locale locale) {
		log.debug("firmarCertificadoVoto - evento: ${evento?.id}");
		Respuesta respuesta = csrService.validarCSRVoto(csr, evento, locale)
		if(Respuesta.SC_OK != respuesta.codigoEstado) return respuesta
		PublicKey requestPublicKey = (PublicKey)respuesta.data
		AlmacenClaves almacenClaves = evento.getAlmacenClaves()
		//TODO ==== vote keystore -- this is for developement
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(almacenClaves.bytes, 
			grailsApplication.config.SistemaVotacion.passwordClavesFirma.toCharArray());
		PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(almacenClaves.keyAlias, 
			grailsApplication.config.SistemaVotacion.passwordClavesFirma.toCharArray());
		X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(almacenClaves.keyAlias);
		String representativeURL = null
		if(representative && representative.type == Usuario.Type.REPRESENTATIVE) 
			representativeURL = "OU=RepresentativeURL:http://localhost:8080/SistemaVotacionControlAcceso/representative/${representative.id}"
		
		//representativeNIF = "OU=RepresentativeURL:${representative.nif}"
		byte[] certificadoFirmado = PKCS10WrapperServer.firmarValidandoCsr(
			csr, representativeURL, privateKeySigner, certSigner, 
			evento.fechaInicio, evento.fechaFin)
		if (!certificadoFirmado) {
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, tipo:Tipo.ERROR_VALIDANDO_CSR)	
		} else {
		    X509Certificate certificate = getVoteCert(certificadoFirmado)
			SolicitudCSRVoto solicitudCSR = new SolicitudCSRVoto(
				numeroSerie:certificate.getSerialNumber().longValue(),
				contenido:csr, eventoVotacion:evento, 
				estado:SolicitudCSRVoto.Estado.OK,
				hashCertificadoVotoBase64:respuesta.hashCertificadoVotoBase64)
			solicitudCSR.save()					
			Certificado certificado = new Certificado(numeroSerie:certificate.getSerialNumber().longValue(),
				contenido:certificate.getEncoded(), eventoVotacion:evento, estado:Certificado.Estado.OK,
				solicitudCSRVoto:solicitudCSR, tipo:Certificado.Tipo.VOTO, usuario:representative,
				hashCertificadoVotoBase64:respuesta.hashCertificadoVotoBase64)
			certificado.save()
			return new Respuesta(codigoEstado:Respuesta.SC_OK, data:requestPublicKey,
				firmaCSR:certificadoFirmado, certificado:certificate)
		}
	}
	
	public Respuesta firmarCertificadoUsuario (SolicitudCSRUsuario solicitudCSR, Locale locale) {
		log.debug("firmarCertificadoUsuario");
		def rutaAlmacenClaves = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaAlmacenClaves}")
		File keyStoreFile = new File(rutaAlmacenClaves);
		String aliasClaves = grailsApplication.config.SistemaVotacion.aliasClavesFirma
		String password = grailsApplication.config.SistemaVotacion.passwordClavesFirma		
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(
			FileUtils.getBytesFromFile(keyStoreFile), password.toCharArray());
		PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(aliasClaves, password.toCharArray());
		X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(aliasClaves);
		
		log.debug("firmarCertificadoUsuario - certSigner:${certSigner}");

		Date today = Calendar.getInstance().getTime();
		Calendar today_plus_year = Calendar.getInstance();
		today_plus_year.add(Calendar.YEAR, 1);
		byte[] certificadoFirmado = PKCS10WrapperServer.firmarValidandoCsr(
				solicitudCSR.contenido, null, privateKeySigner, 
				certSigner, today, today_plus_year.getTime())
		if (!certificadoFirmado) {
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:Tipo.ERROR_VALIDANDO_CSR.toString())
		} else {
			X509Certificate certificate = getUserCert(certificadoFirmado)
			solicitudCSR.estado = SolicitudCSRUsuario.Estado.OK
			solicitudCSR.numeroSerie = certificate.getSerialNumber().longValue()
			solicitudCSR.save()
			Certificado certificado = new Certificado(numeroSerie:certificate.getSerialNumber()?.longValue(),
				contenido:certificate.getEncoded(), usuario:solicitudCSR.usuario, estado:Certificado.Estado.OK,
				solicitudCSRUsuario:solicitudCSR, tipo:Certificado.Tipo.USUARIO, valido:true)
			certificado.save()
			return new Respuesta(codigoEstado:Respuesta.SC_OK)
		}
	}
	
	public String getAbsolutePath(String filePath){
		String prefijo = "${grailsApplication.mainContext.getResource('.')?.getFile()}"
		String sufijo =filePath.startsWith(File.separator)? filePath : File.separator + filePath;
		return "${prefijo}${sufijo}";
	}
	
	public X509Certificate getUserCert(byte[] csrFirmada) throws Exception {
		Collection<X509Certificate> certificados = 
			CertUtil.fromPEMToX509CertCollection(csrFirmada);
		X509Certificate userCert
		for (X509Certificate certificate : certificados) {
			if (certificate.subjectDN.toString().contains("OU=deviceId:")) {
				userCert = certificate
			}
		}
		return userCert
	}
	
	public X509Certificate getVoteCert(byte[] csrFirmada) throws Exception {
		Collection<X509Certificate> certificados = 
			CertUtil.fromPEMToX509CertCollection(csrFirmada);
		X509Certificate userCert
		for (X509Certificate certificate : certificados) {
			if (certificate.subjectDN.toString().contains("OU=hashCertificadoVotoHEX:")) {
				userCert = certificate
			}
		}
		return userCert
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
	
	public Certificado getCertificadoCA(long numSerie) {
		log.debug("getCertificadoCA - numSerie: '${numSerie}'")
		return trustedCertsHashMap.get(numSerie)
	}
		
	public Respuesta validateSMIME(
		SMIMEMessageWrapper messageWrapper, Locale locale) {
		log.debug("validateSMIME -")
		log.debug("validateSMIME ======================================")
		/*MensajeSMIME mensajeSMIME = MensajeSMIME.findWhere(base64ContentDigest:messageWrapper.getContentDigestStr())
		if(mensajeSMIME) {
			String message = messageSource.getMessage('smimeDigestRepeatedErrorMsg', 
				[messageWrapper.getContentDigestStr()].toArray(), locale)
			log.error("validateSMIME - ${message}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:message)
		} */
		return validateSignersCertificate(messageWrapper, locale)
	}
		
	public Respuesta validateSignersCertificate(
			SMIMEMessageWrapper messageWrapper, Locale locale) {
		Set<Usuario> firmantes = messageWrapper.getFirmantes();
		if(firmantes.isEmpty()) return new Respuesta(
			codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:
			messageSource.getMessage('error.documentWithoutSigners', null, locale))
		log.debug("validateSignersCertificate - number of signers: ${firmantes.size()}")
		Set<Usuario> checkedSigners = new HashSet<Usuario>()
		for(Usuario usuario: firmantes) {			 
			try {
				PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
					usuario.getCertificate(), getTrustedCerts(), false)
				TrustAnchor ta = pkixResult.getTrustAnchor();
				X509Certificate certCaResult = ta.getTrustedCert();
				usuario.certificadoCA = trustedCertsHashMap.get(
					certCaResult?.getSerialNumber()?.longValue())
				log.debug("validateSignersCertificate - Certificado de usuario emitido por: " + 
						certCaResult?.getSubjectDN()?.toString() +
				        "- numserie: " + certCaResult?.getSerialNumber()?.longValue());
				Respuesta respuesta = subscripcionService.checkUser(usuario, locale)
				if(Respuesta.SC_OK != respuesta.codigoEstado) return respuesta
				if(usuario.getTimeStampToken() != null) {
					log.debug("validateSignersCertificate - signature with timestamp")
					Respuesta timestampValidationResp = timeStampService.validate(
						usuario.getTimeStampToken(), locale)
					log.debug("validateSignersCertificate - timestampValidationResp - codigoEstado:${timestampValidationResp.codigoEstado} - mensaje:${timestampValidationResp.mensaje}")
					if(Respuesta.SC_OK != timestampValidationResp.codigoEstado) {
						log.error("validateSignersCertificate - TIMESTAMP ERROR - ${timestampValidationResp.mensaje}")
						return timestampValidationResp
					}
				}
				checkedSigners.add(respuesta.usuario)
			} catch (CertPathValidatorException ex) {
				log.error(ex.getMessage(), ex)
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:
					messageSource.getMessage('error.caUnknown', null, locale))
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex)
				return new Respuesta(
					codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:ex.getMessage())
			}
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK,
			smimeMessage:messageWrapper, usuarios:checkedSigners)
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
		Set<Usuario> firmantes = smimeMessageReq.getFirmantes();
		String msg
		Respuesta respuesta
		EventoVotacion evento
		if(firmantes.isEmpty()) {
			msg = messageSource.getMessage('error.documentWithoutSigners', null, locale)
			log.error ("validateVoteCerts - ERROR SIGNERS - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:msg, tipo:Tipo.VOTO_CON_ERRORES)
		}
		InformacionVoto infoVoto = smimeMessageReq.informacionVoto
		String localServerURL = grailsApplication.config.grails.serverURL
		String voteAccessControlURL = infoVoto.controlAccesoURL
		while(voteAccessControlURL.endsWith("/")) {
			voteAccessControlURL = voteAccessControlURL.substring(0, requestURL.length() - 1)
		}
		if (!localServerURL.equals(voteAccessControlURL)) {
			msg = messageSource.getMessage('validacionVoto.errorCert', 
				[voteAccessControlURL, localServerURL].toArray(), locale)
			log.error ("validateVoteCerts - ERROR SERVER URL - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg, 
				tipo:Tipo.VOTO_CON_ERRORES)
		}
		evento = EventoVotacion.get(Long.valueOf(infoVoto.getEventoId()))
		if (!evento)  {
			msg = messageSource.getMessage('validacionVoto.eventoNotFound', 
				[infoVoto.getEventoId()].toArray(), locale)
			log.error ("validateVoteCerts - ERROR EVENT NOT FOUND - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg,
				tipo:Tipo.VOTO_CON_ERRORES)
		}
		if(evento.estado != Evento.Estado.ACTIVO) {
			msg = messageSource.getMessage('validacionVoto.eventClosed', 
				[evento.asunto].toArray(), locale)
			log.error ("validateVoteCerts - ERROR EVENT '${evento.id}' STATE -> ${evento.estado}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg,
				tipo:Tipo.VOTO_CON_ERRORES, evento:evento)
		}
		Certificado certificado = Certificado.findWhere(
			hashCertificadoVotoBase64:infoVoto.hashCertificadoVotoBase64,
			estado:Certificado.Estado.OK)
		if (!certificado) {
			msg = messageSource.getMessage(
				'validacionVoto.errorHash', [infoVoto.hashCertificadoVotoBase64].toArray(), locale)
			log.error ("validateVoteCerts - ERROR CERT '${msg}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg,
				tipo:Tipo.VOTO_CON_ERRORES, evento:evento)
		}
		smimeMessageReq.informacionVoto.setCertificado(certificado)
		Set<X509Certificate> eventTrustedCerts = eventTrustedCertsHashMap.get(evento?.id)
		if(!eventTrustedCerts) {
			Certificado certificadoCAEvento = Certificado.findWhere(
				eventoVotacion:evento, estado:Certificado.Estado.OK,
				tipo:Certificado.Tipo.RAIZ_VOTOS)
			if(!certificadoCAEvento) {
				msg = messageSource.getMessage('eventWithoutCAErrorMsg',
					[evento.id].toArray(), locale)
				log.error ("validateVoteCerts - ERROR EVENT CA CERT -> '${msg}'")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg, tipo:Tipo.VOTO_CON_ERRORES, evento:evento)
			} 
			X509Certificate certCAEvento = CertUtil.loadCertificateFromStream (
				new ByteArrayInputStream(certificadoCAEvento.contenido))
			eventTrustedCerts = new HashSet<X509Certificate>()
			eventTrustedCerts.add(certCAEvento)
			eventTrustedCertsHashMap.put(evento.id, eventTrustedCerts)
		}
		//Vote validation
		PKIXCertPathValidatorResult pkixResult;
		TrustAnchor ta;
		X509Certificate certCaResult;
		X509Certificate checkedCert = infoVoto.getCertificadoVoto()
		try {
			pkixResult = CertUtil.verifyCertificate(
				checkedCert, eventTrustedCerts, false)
			ta = pkixResult.getTrustAnchor();
			certCaResult = ta.getTrustedCert();
			log.debug("validateVoteCerts - vote cert -> CA Result: " + certCaResult?.getSubjectDN()?.toString()+
					"- numserie: " + certCaResult?.getSerialNumber()?.longValue());
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('certValidationErrorMsg',
					[checkedCert.getSubjectDN()?.toString()].toArray(), locale)
			log.error ("validateVoteCerts - ERROR VOTE CERT VALIDATION -> '${msg}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:msg, tipo:Tipo.VOTO_CON_ERRORES, evento:evento)
		}
		Respuesta timestampValidationResp = timeStampService.validate(
			infoVoto.getVoteTimeStampToken(), locale)
		if(Respuesta.SC_OK != timestampValidationResp.codigoEstado) {
			log.error("validateVoteCerts - ERROR TIMESTAMP VOTE VALIDATION -> '${timestampValidationResp.mensaje}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:timestampValidationResp.mensaje, 
				tipo:Tipo.VOTO_CON_ERRORES, evento:evento)
		}
		Date timestampDate = infoVoto.getVoteTimeStampToken().getTimeStampInfo().getGenTime()
		if(!timestampDate.after(evento.fechaInicio) &&
			!timestampDate.before(evento.fechaFin)) {
			String dateRangeStr = "[${eventoVotacion.fechaInicio} - ${eventoVotacion.fechaFin}]"
			msg = messageSource.getMessage('timestampDateErrorMsg',
				[timestampDate, dateRangeStr].toArray(), locale)
			log.debug("validateVoteCerts - ERROR TIMESTAMP DATE - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:msg, tipo:Tipo.VOTO_CON_ERRORES, evento:evento)
		}
		//Control Center cert validation
		eventTrustedCerts = eventValidationTrustedCertsHashMap.get(evento?.id)
		if(!eventTrustedCerts) {
			eventTrustedCerts = new HashSet<X509Certificate>()
			Collection<X509Certificate> centroControlCerts = CertUtil.
				fromPEMToX509CertCollection (evento.cadenaCertificacionCentroControl)
			eventTrustedCerts.addAll(centroControlCerts)
			eventValidationTrustedCertsHashMap.put(evento.id, eventTrustedCerts)
		}
		checkedCert = infoVoto.getServerCerts()?.iterator()?.next()
		try {
			pkixResult = CertUtil.verifyCertificate(
				checkedCert, eventTrustedCerts, false)
			ta = pkixResult.getTrustAnchor();
			certCaResult = ta.getTrustedCert();
			log.debug("validateVoteCerts - Control Center cert -> CA Result: " + certCaResult?.getSubjectDN()?.toString() +
					"- numserie: " + certCaResult?.getSerialNumber()?.longValue());
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('certValidationErrorMsg',
					[checkedCert.getSubjectDN()?.toString()].toArray(), locale)
			log.error ("validateVoteCerts - ERROR CONTROL CENTER CERT VALIDATION -> '${msg}'")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:msg, tipo:Tipo.VOTO_CON_ERRORES, evento:evento)
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento,
			smimeMessage:smimeMessageReq, tipo:Tipo.VOTO_VALIDADO_CENTRO_CONTROL)
	}
	
	private SignedMailGenerator getSignedMailGenerator() {
		if(signedMailGenerator == null) afterPropertiesSet()
		return signedMailGenerator
	}
}