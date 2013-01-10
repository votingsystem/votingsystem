package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*
import java.io.File;
import java.io.FileInputStream;
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
import org.sistemavotacion.smime.SignedMailGenerator.Type;
import org.sistemavotacion.util.*;
import org.springframework.context.*
import org.sistemavotacion.exception.*
import java.util.Locale;

class FirmaService {
	
	static transactional = false
	
	def grailsApplication;
	def messageSource;
	private SignedMailGenerator signedMailGenerator;
	private static Set<X509Certificate> trustedCerts;
	private static HashMap<Long, Certificado> trustedCertsHashMap;
	private static HashMap<Long, X509Certificate> trustedPollingCertsHashMap;
	private static HashMap<Long, Set<X509Certificate>> eventTrustedCertsHashMap;
	private File cadenaCertificacion;
	
	
	public void inicializar() {
		log.debug "inicializar"
		trustedPollingCertsHashMap = new HashMap<Long, X509Certificate>()
		eventTrustedCertsHashMap = new HashMap<Long, Set<X509Certificate>>()
		cadenaCertificacion = grailsApplication.mainContext.getResource(
			grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion).getFile();
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
		for (int i = 0; i < chain.length; i++) {
			log.debug " --- Importando certificado --- ${i} -- SubjectDN: ${chain[i].getSubjectDN()}"
			if(!pemCertsArray) pemCertsArray = CertUtil.fromX509CertToPEM (chain[i])
			else pemCertsArray = FileUtils.concat(pemCertsArray, CertUtil.fromX509CertToPEM (chain[i]))
		}
		def rutaCadenaCertificacion = getAbsolutePath("${grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion}")
		FileUtils.copyStreamToFile(new ByteArrayInputStream(pemCertsArray), new File(rutaCadenaCertificacion))
		inicializarAutoridadesCertificadoras();
		//java.security.cert.Certificate[] chain = ks.getCertificateChain(aliasClaves);
	}
	
	def inicializarAutoridadesCertificadoras() {
		try {
			trustedCerts = new HashSet<X509Certificate>()
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
			return new Respuesta(codigoEstado:200, mensaje:"Importadas Autoridades Certificadoras")
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:400, mensaje:ex.getMessage())
		}
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
	
	public Respuesta validarCertificacionCertificadoVoto(
		X509Certificate certificadoVoto, EventoVotacion evento, Locale locale) {
		Set<X509Certificate> trustedPollingCerts = new HashSet<X509Certificate>()
		if(!trustedPollingCertsHashMap) inicializar();
		X509Certificate certCAEvento = trustedPollingCertsHashMap.get(evento?.id)
		if(!certCAEvento) {
			Certificado certificadoCAEvento = Certificado.findWhere(
				eventoVotacion:evento, estado:Certificado.Estado.OK, 
				tipo:Certificado.Tipo.RAIZ_VOTOS)
			if(!certificadoCAEvento) return new Respuesta(codigoEstado:400,
				mensaje:messageSource.getMessage('certificado.caEventoNotFound', null, locale))
			ByteArrayInputStream bais = 
				new ByteArrayInputStream(certificadoCAEvento.contenido)
			certCAEvento = CertUtil.loadCertificateFromStream (bais)
			trustedPollingCertsHashMap.put(evento.id, certCAEvento)
			Set<X509Certificate> eventTrustedCerts = new HashSet<X509Certificate>()
			Collection<X509Certificate> controlAccesoCerts = CertUtil.
				fromPEMToX509CertCollection (evento.cadenaCertificacionControlAcceso)
			Collection<X509Certificate> centroControlCerts = CertUtil.
				fromPEMToX509CertCollection (cadenaCertificacion.getBytes())
			eventTrustedCerts.addAll(controlAccesoCerts)
			eventTrustedCerts.addAll(centroControlCerts)
			eventTrustedCerts.add(certCAEvento)
			eventTrustedCertsHashMap.put(evento.id, eventTrustedCerts)
		}
		trustedPollingCerts.add(certCAEvento)
		try {
			PKIXCertPathValidatorResult pkixResult = CertUtil.verifyCertificate(
				certificadoVoto, trustedPollingCerts, false)
			TrustAnchor ta = pkixResult.getTrustAnchor();
			X509Certificate certCaResult = ta.getTrustedCert();
			log.debug("certCaResult: " + certCaResult?.getSubjectDN()?.toString()+
					"- numserie: " + certCaResult?.getSerialNumber()?.longValue());
			return new Respuesta(codigoEstado:200)
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:400, mensaje:ex.getMessage())
		}
	}
		
	public Respuesta validarCertificacionFirmantesVoto(
		SMIMEMessageWrapper messageWrapper, EventoVotacion evento) {
		log.debug("validarCertificacionFirmantesVoto");
		Set<Usuario> firmantes = messageWrapper.getFirmantes();
		if(firmantes.size() == 0) return new Respuesta(
			codigoEstado:400, mensaje:"Documento sin firmantes")
		Set<X509Certificate> eventTrustedCerts = eventTrustedCertsHashMap.get(evento?.id)
		log.debug("validarCertificacionFirmantes - firmantes.size(): ${firmantes.size()} - eventTrustedCerts.size(): ${eventTrustedCerts.size()}")
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
					"Error validando Certificación del certificado '${usuario.getCertificate().getSubjectDN()?.toString()}'")
			}
		}
		return new Respuesta(codigoEstado:200)
	}
		
	public File obtenerArchivoFirmado (String fromUser, String toUser, String textoAFirmar,
		String asunto, Header header, Type signerType) {
		log.debug "obtenerArchivoFirmado - textoAFirmar: ${textoAFirmar}"
		File resultado = getSignedMailGenerator().genFile(fromUser, toUser, textoAFirmar,
			asunto, header, SignedMailGenerator.Type.ACESS_CONTROL)
		return resultado
	}
	
	public File getCadenaCertificacion() {
		if(!cadenaCertificacion) inicializar();
		return cadenaCertificacion;
	}
		
	public String obtenerCadenaFirmada (String fromUser, String toUser, String textoAFirmar,
			String asunto, Header header, Type signerType) {
		log.debug "obtenerCadenaFirmada - textoAFirmar: ${textoAFirmar}"
		String resultado = signedMailGenerator.genString(fromUser, toUser, textoAFirmar,
			asunto, header, SignedMailGenerator.Type.ACESS_CONTROL)
		return resultado
	}
			
	public String obtenerCadenaFirmada (String textoAFirmar, String asunto) {
		log.debug "obtenerCadenaFirmada - textoAFirmar: ${textoAFirmar}"
		String resultado = obtenerCadenaFirmada(null, null, textoAFirmar,
			asunto, null, SignedMailGenerator.Type.ACESS_CONTROL)
		return resultado
	}
	
	public String obtenerCadenaFirmada (String textoAFirmar, String asunto, Header header) {
		log.debug "obtenerCadenaFirmada - textoAFirmar: ${textoAFirmar}"
		String resultado = obtenerCadenaFirmada(null, null, textoAFirmar,
			asunto, header, SignedMailGenerator.Type.ACESS_CONTROL)
		return resultado
	}
	
	public synchronized MimeMessage generarMultifirma (SMIMEMessageWrapper smimeMessage, String mailSubject) {
		log.debug("generarMultifirma "  + smimeMessage.getFrom());
		MimeMessage multifirma = getSignedMailGenerator().genMultiSignedMessage(smimeMessage, mailSubject); 
		return multifirma
	}
	
	public SignedMailGenerator getSignedMailGenerator () {
		if (signedMailGenerator == null) inicializar()
		return signedMailGenerator
	}
	
	public String getAbsolutePath(String filePath){
		String prefijo = "${grailsApplication.mainContext.getResource('.')?.getFile()}"
		String sufijo =filePath.startsWith(File.separator)? filePath : File.separator + filePath;
		return "${prefijo}${sufijo}";
	}
	
}