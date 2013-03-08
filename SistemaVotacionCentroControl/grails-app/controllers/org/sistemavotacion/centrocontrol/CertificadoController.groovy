package org.sistemavotacion.centrocontrol

import java.io.IOException;
import java.io.InputStream;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.centrocontrol.modelo.*
import org.springframework.context.ApplicationContext;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.security.cert.X509Certificate;
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
* */
class CertificadoController {

	def firmaService

	def index = { }

	def cadenaCertificacion = {
		try {
			response.outputStream << firmaService.getCadenaCertificacion().getBytes()
			response.outputStream.flush()
			return
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR_EJECUCION
			render ex.getMessage()
			return false 
		}
	}
	
	def certificadoDeVoto = {
		if (params.hashCertificadoVotoHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertificadoVotoBase64 = new String(
				hexConverter.unmarshal(params.hashCertificadoVotoHex))
			log.debug "hashCertificadoVotoBase64: ${hashCertificadoVotoBase64}"
			def certificado
			Certificado.withTransaction {
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
		render message(code: 'error.PeticionIncorrecta')
		return false
	}
	
	def certificadoUsuario = {
		def usuarioId
		if (params.int('usuarioId')) {
			Usuario usuario = Usuario.get(params.int('usuarioId'))
			if(!usuario) {
				response.status = 400
				render message(code: 'error.UsuarioNoEncontrado', args:[params.usuarioId])
				return false
			}
			def certificado
			Certificado.withTransaction {
				certificado = Certificado.findWhere(usuario:usuario, 
					estado:Certificado.Estado.OK)
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
		render message(code: 'error.PeticionIncorrecta')
		return false
	}
	
	def certificadoCA_DeEvento = {
		if (params.long('idEvento') && params.long('controlAccesoId')){
			log.debug "certificadoCA_DeEvento - idEvento: '${params.idEvento}' - controlAccesoId: '${params.controlAccesoId}'"
			ControlAcceso controlAcceso = ControlAcceso.get(params.controlAccesoId)
			if(!controlAcceso) {
				response.status = 404
				render message(code: 'controlAccesoNotFound', args:[params.controlAccesoId])
				return false 
			}
			EventoVotacion eventoVotacion
			EventoVotacion.withTransaction {
				eventoVotacion = EventoVotacion.get(params.idEvento)
			}
			if(!eventoVotacion) {
				response.status = 404
				render  message(code: 'eventoVotacion.eventoNotFound', args:[params.idEvento])
				return false
			}
			if (eventoVotacion && controlAcceso) {
				Certificado certificadoCA
				Certificado.withTransaction {
					certificadoCA = Certificado.findWhere(eventoVotacion:eventoVotacion,
						actorConIP:controlAcceso, esRaiz:true)
				}
				if(certificadoCA) {
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
			}
		}
		response.status = 400
		render (view:"index")
		return false
	}
}