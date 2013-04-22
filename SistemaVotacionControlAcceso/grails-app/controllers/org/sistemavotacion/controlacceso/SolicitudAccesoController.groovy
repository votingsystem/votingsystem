package org.sistemavotacion.controlacceso

import java.security.cert.X509Certificate;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.springframework.web.multipart.MultipartHttpServletRequest
import org.springframework.web.multipart.MultipartFile;
import grails.converters.JSON

/**
 * @infoController Solicitudes de acceso
 * @descController Servicios relacionados con las solicitudes de acceso recibidas en una votación.
 * 
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class SolicitudAccesoController {
    
    def solicitudAccesoService
	def firmaService
	def csrService
	def encryptionService

	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/solicitudAcceso'.
	 */
	def index() { 
		redirect action: "restDoc"
	}
	
	/**
	 * @httpMethod GET
	 * @param id Opcional. El identificador de la solicitud de acceso en la base de datos.
	 * @return <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Solicitud-de-acceso">
	 * 			La solicitud de acceso</a> solicitada.
	 */
    def obtener () {
        if (params.long('id')) {
			def solicitudAcceso
			SolicitudAcceso.withTransaction {
				solicitudAcceso = SolicitudAcceso.get(params.id)
			}
            if (solicitudAcceso) {
                    response.status = Respuesta.SC_OK
                    response.contentLength = solicitudAcceso.mensajeSMIME?.contenido?.length
                    response.setContentType("text/plain")
                    response.outputStream <<  solicitudAcceso.mensajeSMIME?.contenido
                    response.outputStream.flush()
                    return false
            }
            response.status = Respuesta.SC_NOT_FOUND
            render  message(code: 'anulacionVoto.errorSolicitudNoEncontrada')
            return false
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrecta')
        return false
    }

	/**
	 * Servicio que valida las <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Solicitud-de-acceso">
	 * solicitudes de acceso</a> recibidas en una votación.
	 *
	 * @httpMethod POST
	 * @param archivoFirmado Obligatorio. La solicitud de acceso.
	 * @param csr Obligatorio. La solicitud de certificado de voto.
	 * @return La solicitud de certificado de voto firmada.
	 */
    def procesar () { 
        String nombreEntidadFirmada = grailsApplication.config.SistemaVotacion.nombreEntidadFirmada;
		SolicitudAcceso solicitudAcceso;
		Respuesta respuesta;
        if (request instanceof MultipartHttpServletRequest) {
			try {
				Map multipartFileMap = ((MultipartHttpServletRequest) request)?.getFileMap()
				MultipartFile solicitudAccesoMultipartFile = multipartFileMap.remove(nombreEntidadFirmada)                                
				respuesta = solicitudAccesoService.validarSolicitud(
					solicitudAccesoMultipartFile.getBytes(), request.getLocale(), true)
				solicitudAcceso = respuesta.solicitudAcceso
				if (Respuesta.SC_OK == respuesta.codigoEstado) {
					MultipartFile solicitudCsrFile = multipartFileMap.remove(
						grailsApplication.config.SistemaVotacion.nombreSolicitudCSR)
					Respuesta respuestaValidacionCSR = firmaService.
							firmarCertificadoVoto(solicitudCsrFile.getBytes(), 
							respuesta.evento, request.getLocale(), true)
					if (Respuesta.SC_OK == respuestaValidacionCSR.codigoEstado) {
						respuesta = encryptionService.encryptText(
							respuestaValidacionCSR.firmaCSR, respuestaValidacionCSR.certificado)
						if (Respuesta.SC_OK != respuesta.codigoEstado) {
							response.status = respuesta?.codigoEstado
							render respuesta?.mensaje
							return false;
						}
						response.contentLength = respuesta.messageBytes.length
						response.setContentType("application/octet-stream")
						response.outputStream << respuesta.messageBytes
						response.outputStream.flush()
						return false
					} else {
						if (solicitudAcceso)
							solicitudAccesoService.rechazarSolicitud(solicitudAcceso)
					}
				} else {
					response.status = respuesta.codigoEstado
					render respuesta?.mensaje
					return false;
				}
			} catch (Exception ex) {
				log.error (ex.getMessage(), ex)
				if (solicitudAcceso)
					solicitudAccesoService.rechazarSolicitud(solicitudAcceso)
				String mensaje = ex.getMessage();
				if(!mensaje || "".equals(mensaje)) {
					mensaje = message(code: 'error.PeticionIncorrecta')
				}
				response.status = Respuesta.SC_ERROR_EJECUCION
				render mensaje
				return false;
			}
        }	
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrecta')
		return false
    }
    
	/**
	 * @httpMethod GET
	 * @param hashSolicitudAccesoHex Obligatorio. Hash en formato hexadecimal asociado
	 *        a la solicitud de acceso.
	 * @return La solicitud de acceso asociada al hash.
	 */
    def encontrar () {
        if (params.hashSolicitudAccesoHex) {
            HexBinaryAdapter hexConverter = new HexBinaryAdapter();
            String hashSolicitudAccesoBase64 = new String(
				hexConverter.unmarshal(params.hashSolicitudAccesoHex))
            log.debug "hashSolicitudAccesoBase64: ${hashSolicitudAccesoBase64}"
            SolicitudAcceso solicitudAcceso = SolicitudAcceso.findWhere(hashSolicitudAccesoBase64:
                hashSolicitudAccesoBase64)
            if (solicitudAcceso) {
                response.status = Respuesta.SC_OK
                response.contentLength = solicitudAcceso.contenido.length
                response.setContentType("text/plain")
                response.outputStream <<  solicitudAcceso.contenido
                response.outputStream.flush()
                return false  
            }
            response.status = Respuesta.SC_NOT_FOUND
            render message(code: 'error.solicitudAccesoNotFound', 
                args:[params.hashSolicitudAccesoHex])
            return false
        }
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrectaHTML', args:[
			"${grailsApplication.config.grails.serverURL}/${params.controller}"])
        return false
    }
	
	/**
	 * @httpMethod GET
	 * @param eventoId Obligatorio. El identificador de la votación en la base de datos.
	 * @param nif Obligatorio. El nif del solicitante.
	 * @return La solicitud de acceso asociada al nif y el evento.
	 */
	def encontrarPorNif () {
		if(params.nif && params.long('eventoId')) {
			EventoVotacion evento
			EventoVotacion.withTransaction {
				evento =  EventoVotacion.get(params.eventoId)
			}
			if(!evento) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'eventNotFound', args:[params.eventoId])
				return
			}
			Usuario usuario
			Usuario.withTransaction {
				usuario =  Usuario.findByNif(params.nif)
			}
			if(!usuario) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'usuario.nifNoEncontrado', args:[params.nif])
				return
			}
			SolicitudAcceso solicitudAcceso
			SolicitudAcceso.withTransaction {
				solicitudAcceso =  SolicitudAcceso.findWhere(
					usuario: usuario, eventoVotacion:evento)
			}
			if(!solicitudAcceso) {
				response.status = Respuesta.SC_NOT_FOUND
				render message(code: 'error.nifSinSolicitudAcceso', args:[params.eventoId, params.nif])
				return
			}
			response.status = Respuesta.SC_OK
			response.contentLength = solicitudAcceso.mensajeSMIME?.contenido.length
			response.setContentType("text/plain")
			response.outputStream <<  solicitudAcceso.mensajeSMIME?.contenido
			response.outputStream.flush()
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

	
}