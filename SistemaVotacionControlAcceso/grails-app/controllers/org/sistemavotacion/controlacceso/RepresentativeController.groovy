package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import grails.converters.JSON

class RepresentativeController {

	def representativeService
	def firmaService
	
	private static final int MAX_FILE_SIZE_KB = 512;
	private static final int MAX_FILE_SIZE = 512 * 1024;
	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/representative'.
	 */
	def index() {
		redirect action: "restDoc"
	}
	
	def check() {
		if(params.nif) {
			
		}
		
	}
	
	/**
	 * 
	 * Servicio que guarda las selecciones de representantes echas por los usuarios
	 *
	 * @param archivoFirmado Archivo firmado en formato SMIME en cuyo contenido se
	 *        encuentra la votación que se desea publicar en formato HTML.
	 * @httpMethod POST
	 * @return Recibo que consiste en el archivo firmado enviado por el usuario con la firma añadida del servidor.
	 */
	def guardarSelectAdjuntandoValidacion() {
		Respuesta respuesta = representativeService.saveUserRepresentative(
			params.smimeMessageReq, request.getLocale())
		if (Respuesta.SC_OK == respuesta.codigoEstado){
			String mensajeValidado = firmaService.obtenerCadenaFirmada(
				params.smimeMessageReq.getSignedContent(),
				message(code:'representativeSelectValidationSubject'), null)
			MensajeSMIME mensajeSMIMEValidado = new MensajeSMIME(
					tipo:Tipo.REPRESENTATIVE_SELECTION_VALIDATION,
					smimePadre: respuesta.mensajeSMIME,
					usuario:respuesta.usuario, valido:true,
					contenido:mensajeValidado.getBytes())
			mensajeSMIMEValidado.save();
			respuesta.mensajeSMIMEValidado = mensajeSMIMEValidado
		}
		flash.respuesta = respuesta
		return false
	}
	
	def detailed() {
		if (params.long('id')) {
			Usuario representative
			Usuario.withTransaction {
				representative = Usuario.get(params.long('id'))
			}
			if (Usuario.Type.REPRESENTATIVE == representative?.type) {
				response.setContentType("application/json")
				def representativeMap = representativeService.getRepresentativeDetailedJSONMap(representative)
				render representativeMap as JSON
				return false
			} else {
				response.status = Respuesta.SC_NOT_FOUND
				String msg = "User ${params.id} isn't representative"
				log.debug msg
				render msg
				return false
			}
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def get() {
        def representativeList = []
        def representativeMap = new HashMap()
        //eventosMap.eventos = new HashMap()
        representativeMap.representatives = []
        if (params.ids?.size() > 0) {
				Usuario.withTransaction {
					Usuario.getAll(params.ids).collect {usuario ->
						if (Usuario.Type.REPRESENTATIVE == usuario.type) {
							representativeList << usuario;
						} else log.debug "User ${usuario.id} insn't representative"
				}
            }
            if (representativeList.size() == 0) {
                    response.status = Respuesta.SC_NOT_FOUND
                    render message(code: 'representativeNotFound', args:[params.ids])
                    return
            }
        } else {
			params.sort = "representationsNumber"
			log.debug " -Params: " + params
			Usuario.withTransaction {
				representativeList = Usuario.findAllByType(Usuario.Type.REPRESENTATIVE, params)
			}
            representativeMap.offset = params.long('offset')
        }

	    representativeMap.representativesTotalNumber = Usuario.
				   countByType(Usuario.Type.REPRESENTATIVE)
        representativeMap.numberRepresentativesInRequest = representativeList.size()
        representativeList.collect {representative ->
				representativeMap.representatives.add(representativeService.getRepresentativeJSONMap(representative))
        }
        response.setContentType("application/json")
        render representativeMap as JSON
	}
	
	def prueba() {
		response.status =  Respuesta.SC_ERROR_PETICION
		String msg = message(code: 'imageMissingErrorMsg')
		log.debug "imageFileName: ${grailsApplication.config.SistemaVotacion.nombreSolicitudCSR}"
		log.debug "imageFileName: ${grailsApplication.config.SistemaVotacion.imageFileName}"
		log.debug "ERROR - msg: ${msg}"
		render msg
		return false
	}
	
	
    def guardar() { 
		MultipartFile imageMultipartFile = ((MultipartHttpServletRequest)
			request)?.getFile("${grailsApplication.config.SistemaVotacion.imageFileName}");
		byte[] imageBytes = imageMultipartFile?.getBytes()
		if(!imageBytes) {
			response.status =  Respuesta.SC_ERROR_PETICION
			String msg = message(code: 'imageMissingErrorMsg')
			log.debug "ERROR - msg: ${msg}"
			render msg
			return false
		}
		if(imageBytes && imageBytes.length > MAX_FILE_SIZE) {
			response.status =  Respuesta.SC_ERROR_PETICION
			String msg = message(code: 'imageSizeExceededMsg', 
				args:[imageBytes.length/1024, MAX_FILE_SIZE_KB])
			log.debug "ERROR - msg: ${msg}"
			render msg
			return false
		}
		Respuesta respuesta = representativeService.saveRepresentativeData(
			params.smimeMessageReq, imageBytes, request.getLocale())
		response.status =  respuesta.codigoEstado
		log.debug "mensaje:${respuesta.mensaje}"
		render respuesta.mensaje
		return false
		
	}
	
	def info() {
		if (params.long('id')) {
			Usuario representative 
			Usuario.withTransaction {
				representative = Usuario.get(params.long('id'))
			}
			if (Usuario.Type.REPRESENTATIVE == representative?.type) {
				render representative.info;
				return false
			} else {
				String msg = "User ${representative?.id} isn't representative"
				log.debug msg
				render msg
				return false
			}
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
	def image() {
		if (params.long('id')) {
			Image image;
			Image.withTransaction{
				image = Image.get(params.long('id'))
			}
			if (image) {
				response.status = Respuesta.SC_OK
				//response.setContentType("text/plain")
				response.contentLength = image.fileBytes.length
				response.outputStream <<  image.fileBytes
				response.outputStream.flush()
				return false
			}
			response.status = Respuesta.SC_NOT_FOUND
			render "No se he encontrado la imagen con id ${params.id}"
			return false
		}
		response.status = Respuesta.SC_ERROR_PETICION
		render message(code: 'error.PeticionIncorrectaHTML', args:["${grailsApplication.config.grails.serverURL}/${params.controller}"])
		return false
	}
	
}
