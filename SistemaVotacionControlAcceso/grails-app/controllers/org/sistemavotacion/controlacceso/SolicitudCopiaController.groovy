package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.util.FileUtils;
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import grails.converters.JSON


/**
 * @infoController Solicitud de copias de seguridad
 * @descController Servicios que gestiona solicitudes de copias de seguridad.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class SolicitudCopiaController {
	
	def pdfService

	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/solicitudCopia'.
	 */
	def index() { 
		redirect action: "restDoc"
	}
	
	/**
	 * Servicio que proporciona la copia de seguridad a partir de la URL que se envía
	 * al solicitante en el mail de confirmación que recibe al enviar la solicitud.
	 * 
	 * @httpMethod GET
	 * @param id Obligatorio. El identificador de la solicitud de copia de seguridad la base de datos.
	 * @return Archivo zip con la copia de seguridad.
	 */
	def obtener() {
		if (params.long('id')) {
			SolicitudCopia solicitud
			SolicitudCopia.withTransaction {
				solicitud = SolicitudCopia.get(params.id)
			}
			if(!solicitud) {
				response.status = Respuesta.SC_ERROR_PETICION
				render message(code: 'solicitudCopia.noEncontrada', args:[params.id]) 
				return false
			}
			if(!solicitud.filePath) {
				response.status = Respuesta.SC_ERROR_PETICION
				render message(code: 'backupDownloadedMsg', args:[params.id]) 
				return false
			}
			File copiaRespaldo = new File(solicitud.filePath)
			if (copiaRespaldo != null) {
				def bytesCopiaRespaldo = FileUtils.getBytesFromFile(copiaRespaldo)
				response.contentLength = bytesCopiaRespaldo.length
				response.setHeader("Content-disposition", "filename=${copiaRespaldo.getName()}")
				response.setHeader("NombreArchivo", "${copiaRespaldo.getName()}")
				response.setContentType("application/octet-stream")
				response.outputStream << bytesCopiaRespaldo
				response.outputStream.flush()
				solicitud.filePath = null
				solicitud.save()
				copiaRespaldo.delete()
				return false
			} else {
				log.error (message(code: 'error.SinCopiaRespaldo'))
				response.status = Respuesta.SC_ERROR_EJECUCION
				render message(code: 'error.SinCopiaRespaldo')
				return false
			}
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', 
			args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	/**
	 * Servicio que recibe solicitudes de copias de seguridad
	 *
	 * @httpMethod POST
	 * @param signedPDF Archivo PDF con los datos de la copia de seguridad.
	 * @return Si todo va bien devuelve un código de estado HTTP 200. Y el solicitante recibirá un
	 *         email con información para poder obtener la copia de seguridad.
	 */
	def validarSolicitud() {
		try {
			String nombreArchivo = ((MultipartHttpServletRequest) request)?.getFileNames()?.next();
			log.debug "Recibido archivo: ${nombreArchivo}"
			MultipartFile multipartFile = ((MultipartHttpServletRequest) request)?.getFile(nombreArchivo);
			if (multipartFile?.getBytes() != null || params.archivoFirmado) {
				Respuesta respuesta = pdfService.validarSolicitudCopia(
					multipartFile.getBytes(), request.getLocale())
				if (Respuesta.SC_OK != respuesta.codigoEstado) {
					log.debug "Problemas procesando solicitud de copia de seguridad - ${respuesta.mensaje}"
				}
				response.status = respuesta.codigoEstado
				render respuesta.mensaje
				return false
			}
		} catch (Exception ex) {
			log.error (ex.getMessage(), ex)
			response.status = Respuesta.SC_ERROR_PETICION
			render(ex.getMessage())
			return false
		}
	}
	
	/**
	 * Servicio que proporciona copias de las solicitudes de copias de seguridad recibidas.
	 *
	 * @httpMethod GET
	 * @param id Obligatorio. El identificador de la solicitud de copia de seguridad la base de datos.
	 * @return El PDF en el que se solicita la copia de seguridad.
	 */
	def obtenerSolicitud() {
		if (params.long('id')) {
			SolicitudCopia solicitud
			byte[] solicitudBytes
			SolicitudCopia.withTransaction {
				solicitud = SolicitudCopia.get(params.id)
				if(solicitud) {
					if(solicitud.documento) {//has PDF
						//response.setHeader("Content-disposition", "attachment; filename=manifiesto.pdf")
						response.contentType = "application/pdf"
						solicitudBytes = solicitud.documento?.pdf
					} else {//has SMIME
						response.contentType = "text/plain"
						solicitudBytes = solicitud.mensajeSMIME?.contenido
					}
				} 
			}
			if(!solicitud) {
				response.status = Respuesta.SC_ERROR_PETICION
				render message(code: 'solicitudCopia.noEncontrada', args:[params.id]) 
				return false
			}

			response.setHeader("Content-Length", "${solicitudBytes.length}")
			response.outputStream << solicitudBytes // Performing a binary stream copy
			response.outputStream.flush()
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', 
			args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}

}