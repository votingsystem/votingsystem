package org.sistemavotacion.controlacceso

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.Properties;
import javax.mail.Header;
import javax.mail.Session;
import javax.mail.internet.MimeMultipart;
import org.sistemavotacion.util.*
import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.smime.SignedMailGenerator;
import org.sistemavotacion.smime.SignedMailGenerator.Type;
import org.springframework.context.ApplicationContext;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import java.security.KeyStore
import java.security.PrivateKey
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;
import java.security.cert.CertPathValidatorException
import java.security.cert.PKIXCertPathValidatorResult
import java.security.cert.TrustAnchor
import java.security.cert.X509Certificate;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo
import org.bouncycastle.asn1.x509.X509Extensions
import java.util.Locale;

class FirmaService {

	
	private SignedMailGenerator firmadoraValidaciones;
	static Set<X509Certificate> trustedCerts;
	static HashMap<Long, Certificado> trustedCertsHashMap;
	private static HashMap<Long, Set<X509Certificate>> eventTrustedCertsHashMap;
	def grailsApplication;
	def messageSource
	def csrService;
		
	public void inicializar() {
		log.debug "inicializar"
		eventTrustedCertsHashMap = new HashMap<Long, Set<X509Certificate>>()
		def rutaAlmacenClaves = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaAlmacenClaves}")
		File keyStore = new File(rutaAlmacenClaves);
		String aliasClaves = grailsApplication.config.SistemaVotacion.aliasClavesFirma
		String password = grailsApplication.config.SistemaVotacion.passwordClavesFirma
		firmadoraValidaciones = new SignedMailGenerator(FileUtils.getBytesFromFile(keyStore), 
			aliasClaves, password.toCharArray());
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(new FileInputStream(keyStore), password.toCharArray());
		java.security.cert.Certificate[] chain = ks.getCertificateChain(aliasClaves);
		byte[] pemCertsArray
		for (int i = 0; i < chain.length; i++) {
			log.debug " --- Importando certificado --- ${i} -- SubjectDN: ${chain[i].getSubjectDN()}"
			if(!pemCertsArray) pemCertsArray = CertUtil.fromX509CertToPEM (chain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtil.fromX509CertToPEM (chain[i]))
		}
		
		trustedCerts = new HashSet<X509Certificate>()
		X509Certificate localServerCertSigner = (X509Certificate) ks.getCertificate(aliasClaves);
		trustedCerts.add(localServerCertSigner)
		
		def rutaCadenaCertificacion = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion}")
		FileUtils.copyStreamToFile(new ByteArrayInputStream(pemCertsArray), new File(rutaCadenaCertificacion))
		inicializarAutoridadesCertificadoras();
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
				Certificado certificado = Certificado.findByNumeroSerie(
					certificate.getSerialNumber().longValue())
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
				} else log.debug " --certificado: ${certificado?.id}"
				trustedCertsHashMap.put(certificate?.getSerialNumber()?.longValue(), certificado)
			}
			log.debug("trustedCerts.size(): ${trustedCerts?.size()}")
			return new Respuesta(codigoEstado:200, mensaje:"Importadas Autoridades Certificadoras")
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:400, mensaje:ex.getMessage())
		}
	}
	
	def obtenerArchivoFirmado (String fromUser, String toUser, String textoAFirmar,
			String asunto, Header header, Type signerType) {
		log.debug "obtenerArchivoFirmado - textoAFirmar: ${textoAFirmar}"
		File resultado = firmadoraValidaciones.genFile(fromUser, toUser, textoAFirmar,
			asunto, header, SignedMailGenerator.Type.ACESS_CONTROL)
		return resultado
	}
	
	def obtenerCadenaFirmada (String fromUser, String toUser, String textoAFirmar,
			String asunto, Header header, Type signerType) {
		log.debug "obtenerCadenaFirmada - textoAFirmar: ${textoAFirmar}"
		String resultado = getFirmadoraValidaciones().genString(fromUser, toUser, textoAFirmar,
			asunto, header, SignedMailGenerator.Type.ACESS_CONTROL)
		return resultado
	}
			
	def obtenerCadenaFirmada (String textoAFirmar, String asunto) {
		log.debug "obtenerCadenaFirmada - textoAFirmar: ${textoAFirmar}"
		String resultado = obtenerCadenaFirmada(null, null, textoAFirmar,
			asunto, null, SignedMailGenerator.Type.ACESS_CONTROL)
		return resultado
	}
	
	def obtenerCadenaFirmada (String textoAFirmar, String asunto, Header header) {
		log.debug "obtenerCadenaFirmada - textoAFirmar: ${textoAFirmar}"
		String resultado = obtenerCadenaFirmada(null, null, textoAFirmar,
			asunto, header, SignedMailGenerator.Type.ACESS_CONTROL)
		return resultado
	}
	
	public synchronized MimeMessage generarMultifirma (
			final SMIMEMessageWrapper smimeMessage, String mailSubject) {
		log.debug("generarMultifirma - From:"  + smimeMessage.getFrom());
		MimeMessage multifirma = getFirmadoraValidaciones().genMultiSignedMessage(smimeMessage, mailSubject); 
		return multifirma
	}
			
	public Respuesta firmarCertificadoVoto (byte[] csr, Evento evento, Locale locale) {
		log.debug("firmarCertificadoVoto - evento: ${evento?.id}");
		Respuesta respuesta = csrService.validarCSRVoto(csr, evento, locale)
		if(200 != respuesta.codigoEstado) return respuesta
		AlmacenClaves almacenClaves = evento.getAlmacenClaves()
		//TODO
		KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(almacenClaves.bytes, 
			grailsApplication.config.SistemaVotacion.passwordClavesFirma.toCharArray());
		PrivateKey privateKeySigner = (PrivateKey)keyStore.getKey(almacenClaves.keyAlias, 
			grailsApplication.config.SistemaVotacion.passwordClavesFirma.toCharArray());
		X509Certificate certSigner = (X509Certificate) keyStore.getCertificate(almacenClaves.keyAlias);
		PKCS10WrapperServer pkcs10wrapper = new PKCS10WrapperServer(privateKeySigner, certSigner);
		byte[] certificadoFirmado = pkcs10wrapper.firmarValidandoCsr(csr, evento.fechaInicio, evento.fechaFin)
		if (!certificadoFirmado) {
			return new Respuesta(codigoEstado:400, tipo:Tipo.ERROR_VALIDANDO_CSR)	
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
				solicitudCSRVoto:solicitudCSR, tipo:Certificado.Tipo.VOTO,
				hashCertificadoVotoBase64:respuesta.hashCertificadoVotoBase64)
			certificado.save()
			return new Respuesta(codigoEstado:200,firmaCSR:certificadoFirmado)
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
		
		log.debug("certSigner:${certSigner}");
		
		PKCS10WrapperServer pkcs10wrapper = new PKCS10WrapperServer(privateKeySigner, certSigner);
		Date today = Calendar.getInstance().getTime();
		Calendar today_plus_year = Calendar.getInstance();
		today_plus_year.add(Calendar.YEAR, 1);
		byte[] certificadoFirmado = pkcs10wrapper.firmarValidandoCsr(
			solicitudCSR.contenido, today, today_plus_year.getTime())
		if (!certificadoFirmado) {
			return new Respuesta(codigoEstado:400, mensaje:Tipo.ERROR_VALIDANDO_CSR.toString())
		} else {
			X509Certificate certificate = getUserCert(certificadoFirmado)
			solicitudCSR.estado = SolicitudCSRUsuario.Estado.OK
			solicitudCSR.numeroSerie = certificate.getSerialNumber().longValue()
			solicitudCSR.save()
			Certificado certificado = new Certificado(numeroSerie:certificate.getSerialNumber()?.longValue(),
				contenido:certificate.getEncoded(), usuario:solicitudCSR.usuario, estado:Certificado.Estado.OK,
				solicitudCSRUsuario:solicitudCSR, tipo:Certificado.Tipo.USUARIO, valido:true)
			certificado.save()
			return new Respuesta(codigoEstado:200)
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
	
	private SignedMailGenerator getFirmadoraValidaciones() {
		if (firmadoraValidaciones == null) inicializar()
		return firmadoraValidaciones
	}
	
	/*
	 * Método para poder añadir certificados de confianza en las pruebas de carga.
	 * El procedimiento para añadir una autoridad certificadora consiste en 
	 * añadir el certificado en formato pem en el directorio ./WEB-INF/cms
	 */
	public Respuesta addCertificateAuthority (byte[] caPEM)  {
		log.debug("addCertificateAuthority");
		if(!caPEM) 
			return new Respuesta(codigoEstado:400, mensaje:"Certificado nulo")
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
			return new Respuesta(codigoEstado:200, 
				mensaje:"Certificado de CA de pruebas añadido a la lista de confianza")
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:400, mensaje:ex.getMessage())
		}
	}
	
	public Certificado getCertificadoCA(long numSerie) {
		log.debug("getCertificadoCA - numSerie: '${numSerie}'")
		return trustedCertsHashMap.get(numSerie)
	}
	
	public Respuesta validarCertificacionFirmantesVoto(
		SMIMEMessageWrapper messageWrapper, EventoVotacion evento) {
		Set<Usuario> firmantes = messageWrapper.getFirmantes();
		if(firmantes.size() == 0) return new Respuesta(
			codigoEstado:400, mensaje:"Documento sin firmantes")
		if(!eventTrustedCertsHashMap) inicializar();
		Set<X509Certificate> eventTrustedCerts = eventTrustedCertsHashMap.get(evento?.id)
		if(!eventTrustedCerts) {
			Certificado certificadoCAEvento = Certificado.findWhere(
				eventoVotacion:evento, estado:Certificado.Estado.OK,
				tipo:Certificado.Tipo.RAIZ_VOTOS)
			if(!certificadoCAEvento) return new Respuesta(codigoEstado:400,
				mensaje:"La Autoridad Certificadora del evento '${evento.id}' no esta dada de alta")				
			X509Certificate certCAEvento = CertUtil.loadCertificateFromStream (
				new ByteArrayInputStream(certificadoCAEvento.contenido))
			eventTrustedCerts = new HashSet<X509Certificate>()
			Collection<X509Certificate> centroControlCerts = CertUtil.
				fromPEMToX509CertCollection (evento.cadenaCertificacionCentroControl)
			eventTrustedCerts.addAll(centroControlCerts)
			eventTrustedCerts.add(certCAEvento)
			eventTrustedCertsHashMap.put(evento.id, eventTrustedCerts)
		}
		log.debug("validarCertificacionFirmantesVoto - firmantes.size():" + 
			" ${firmantes.size()} - eventTrustedCerts.size(): ${eventTrustedCerts.size()}")
		for(Usuario usuario: firmantes) {
			log.debug("Validando firmante ${usuario.getCertificate().getSubjectDN()}")
			try {
				PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
					usuario.getCertificate(), eventTrustedCerts, false)
				TrustAnchor ta = pkixResult.getTrustAnchor();
				X509Certificate certCaResult = ta.getTrustedCert();
				log.debug("certCaResult: " + certCaResult?.getSubjectDN()?.toString()+
						"- numserie: " + certCaResult?.getSerialNumber()?.longValue());
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex)
				return new Respuesta(codigoEstado:400, mensaje:
					"Error validando Certificación del certificado" + 
					" '${usuario.getCertificate().getSubjectDN()?.toString()}'")
			}
		}
		return new Respuesta(codigoEstado:200)
	}

	public Respuesta checkTimeStamps() {
		
	}
		
	public Respuesta validarCertificacionFirmantes(
			SMIMEMessageWrapper messageWrapper, Locale locale) {
		Set<Usuario> firmantes = messageWrapper.getFirmantes();
		inicializar()
		log.debug("*** validarCertificacionFirmantes - firmantes.size(): " +
			" ${firmantes.size()} - trustedCerts.size(): ${trustedCerts.size()}")
		if(firmantes.size() == 0) return new Respuesta(
			codigoEstado:400, mensaje:"Documento sin firmantes")
		for(Usuario usuario: firmantes) {			 
			try {
				PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
					usuario.getCertificate(), trustedCerts, false)
				TrustAnchor ta = pkixResult.getTrustAnchor();
				X509Certificate certCaResult = ta.getTrustedCert();
				usuario.certificadoCA = trustedCertsHashMap.get(certCaResult?.getSerialNumber()?.longValue())
				log.debug("Certificado de usuario emitido por: " + certCaResult?.getSubjectDN()?.toString()+
				        "- numserie: " + certCaResult?.getSerialNumber()?.longValue());
			} catch (CertPathValidatorException ex) {
				log.error(ex.getMessage(), ex)
				return new Respuesta(codigoEstado:400, mensaje:
					messageSource.getMessage('error.caUnknown', null, locale))
			} catch (Exception ex) {
				log.error(ex.getMessage(), ex)
				return new Respuesta(codigoEstado:400, mensaje:ex.getMessage())
			}
		}
		return new Respuesta(codigoEstado:200)
	}
			
	public Set<X509Certificate> getTrustedCerts(){
		return trustedCerts
	}

}