package org.sistemavotacion.controlacceso

import java.io.IOException;
import java.io.InputStream;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.controlacceso.modelo.*
import org.springframework.context.ApplicationContext;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.security.cert.X509Certificate;
import java.util.Set;

import grails.util.Environment
/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
class CertificadoController {
	
	def grailsApplication
	def firmaService

	def index = { }
	
	def cadenaCertificacion = {
		try {
			File cadenaCertificacion = grailsApplication.mainContext.getResource(
				grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion).getFile();
			response.status = 200
			response.outputStream << cadenaCertificacion.getBytes() // Performing a binary stream copy
			response.outputStream.flush()
			return
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			flash.respuesta = new Respuesta(mensaje:ex.getMessage(),
				codigoEstado:500, tipo: Tipo.ERROR_DE_SISTEMA)
			forward controller: "error500", action: "procesar"
		}
	}

	def certificadoDeVoto = {
		if (params.hashCertificadoVotoHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertificadoVotoBase64 = new String(
				hexConverter.unmarshal(params.hashCertificadoVotoHex))
			log.debug "hashCertificadoVotoBase64: ${hashCertificadoVotoBase64}"
			Certificado certificado;
			Certificado.withTransaction{
				certificado = Certificado.findWhere(hashCertificadoVotoBase64:
					hashCertificadoVotoBase64)
			}
			if (certificado) {
				response.status = 200
				ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
				response.setContentType("text/plain")
				response.contentLength = pemCert.length
				response.outputStream <<  pemCert
				response.outputStream.flush()
				return false
			}
			response.status = 404
			render message(code: 'certificado.certificadoHexNotFound',
				args:[params.hashCertificadoVotoHex])
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def certificadoUsuario = {
		def usuarioId
		if (params.long('usuarioId')) {
			Usuario usuario = Usuario.get(params.long('usuarioId'))
			if(!usuario) {
				response.status = 400
				render message(code: 'error.UsuarioNoEncontrado', args:[params.usuarioId])
				return false
			}
			Certificado certificado
			Certificado.withTransaction {
				//certificado = Certificado.findWhere(
				//	usuario:usuario, estado:Certificado.Estado.OK)
				certificado = Certificado.get(8)
			}
			if (certificado) {
				response.status = 200
				ByteArrayInputStream bais = new ByteArrayInputStream(certificado.contenido)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
				response.setContentType("text/plain")
				response.contentLength = pemCert.length
				response.outputStream <<  pemCert
				response.outputStream.flush()
				return false
			}
			response.status = 404
			render message(code: 'error.UsuarioSinCertificado',
				args:[params.usuarioId])
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def certificadoCA_DeEvento = {
		if (params.int('idEvento')) {
			EventoVotacion eventoVotacion;
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.get(params.int('idEvento'))
			}
			log.debug "certificadoCA_DeEvento - eventoVotacion: '${eventoVotacion.id}'"
			if (eventoVotacion) {
				Certificado certificadoCA
				EventoVotacion.withTransaction { 
					certificadoCA = Certificado.findWhere(
						eventoVotacion:eventoVotacion, tipo:Certificado.Tipo.RAIZ_VOTOS)
				}
				response.status = 200
				ByteArrayInputStream bais = new ByteArrayInputStream(certificadoCA.contenido)
				X509Certificate certX509 = CertUtil.loadCertificateFromStream (bais)
				byte[] pemCert = CertUtil.fromX509CertToPEM (certX509)
				response.setContentType("text/plain")
				response.contentLength = pemCert.length
				response.outputStream <<  pemCert
				response.outputStream.flush()
				return false
			}
			response.status = 404
			render message(code: 'evento.eventoNotFound', args:[params.idEvento])
			return false
		}
		response.status = 400
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def certificadoCA_Usuario = {
		if(!Environment.TEST.equals(Environment.current)) {
			def msg = message(code: "CertificadoController.certificadoCA_Usuario.msg")
			log.debug msg
			render msg
			return false
		} else {
			log.debug "Entorno de TEST"
		}
		def msg = message(code: "CertificadoController.certificadoCA_Usuario.msg")
		log.debug "${msg}"
		log.debug "Environment.current: " + Environment.current
		render Environment.current
		return false
	}

	def addCertificateAuthority = {
		if(!Environment.TEST.equals(Environment.current)) {
			def msg = message(code: "msg.servicioEntornoTest")
			log.debug msg
			render msg
			return false
		} else {
			log.debug "Entorno de TEST"
		}
		Respuesta respuesta = firmaService.addCertificateAuthority(
			params.archivoFirmado?.getBytes())
		log.debug("addCertificateAuthority - codigo estado: ${respuesta.codigoEstado} - mensaje: ${respuesta.mensaje}")
		response.status = respuesta.codigoEstado
		render respuesta.mensaje
		return false
	}

	def trustedCerts = {
		Set<X509Certificate> trustedCerts = firmaService.getTrustedCerts()
		log.debug("number trustedCerts: ${trustedCerts.size()}")
		response.setContentType("text/plain")
		for(X509Certificate cert : trustedCerts) {
			byte[] pemCert = CertUtil.fromX509CertToPEM (cert)
			response.outputStream <<  pemCert
		}
		response.outputStream.flush()
		return false
	}
}