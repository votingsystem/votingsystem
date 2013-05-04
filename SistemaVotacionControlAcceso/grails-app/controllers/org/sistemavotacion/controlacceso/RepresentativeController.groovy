package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.util.StringUtils;
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
	
	/**
	 *
	 * Servicio que sirve para comprobar el representante de un usuario
	 *
	 * @param nif NIF del usuario que se desea consultar.
	 * @httpMethod GET
	 * @return Documento JSON con información básica del representante asociado 
	 *         al usuario cuyo nif se pada como parámetro nif
	 */
	def getByUserNif() {
		String nif = StringUtils.validarNIF(params.nif)
		if(!nif) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.errorNif', args:[params.nif])
			return false
		}
		Usuario usuario = Usuario.findByNif(nif)
		if(!usuario) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'usuario.nifNoEncontrado', args:[nif])
			return false
		}
		String msg = null
		if(Usuario.Type.REPRESENTATIVE == usuario.type) { 
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'userIsRepresentativeMsg', args:[nif])
			return false
		}
		if(!usuario.representative) {
			response.status = Respuesta.SC_NOT_FOUND
			if(Usuario.Type.USER_WITH_CANCELLED_REPRESENTATIVE == usuario.type) {
				msg = message(code: 'userRepresentativeUnsubscribedMsg', args:[nif])
			} else msg = message(code: 'nifWithoutRepresentative', args:[nif])
			render msg
			return false
		} else {
			Usuario representative = usuario.representative
			String name = "${representative.nombre} ${representative.primerApellido}"
			def resultMap = [representativeId: representative.id, representativeName:name,
				representativeNIF:representative.nif]
			render resultMap as JSON
			return false
		}
	}
	
	/**
	 *
	 * Servicio que sirve para obtener información básica de un representante 
	 * a partir de su NIF
	 *
	 * @param nif NIF del representante que se desea consultar.
	 * @httpMethod GET
	 * @return Documento JSON con datos del representante
	 */
	def getByNif() {
		String nif = StringUtils.validarNIF(params.nif)
		if(!nif) {
			response.status = Respuesta.SC_ERROR_PETICION
			render message(code: 'error.errorNif', args:[params.nif])
			return false
		}
		Usuario representative
		Usuario.withTransaction {
			representative =  Usuario.findWhere(type:Usuario.Type.REPRESENTATIVE,
				nif:nif)
		}
		if(!representative) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'representativeNifErrorMsg', args:[nif])
			return false
		} else {
			String name = "${representative.nombre} ${representative.primerApellido}"
			def resultMap = [representativeId: representative.id, representativeName:name,
				representativeNIF:representative.nif]
			render resultMap as JSON
			return false
		}
	}
	
	/**
	 *
	 * Servicio que da de baja un representante
	 *
	 * @param archivoFirmado Archivo firmado en formato SMIME en cuyo contenido se
	 *        encuentra información de la baja
	 * @httpMethod POST
	 * @return
	 */
	def guardarUnsubscribeRequestAdjuntandoValidacion() {
		Respuesta respuesta = representativeService.processUnsubscribeRequest(
			params.smimeMessageReq, request.getLocale())
		log.debug "guardarUnsubscribeRequest - statusCode: ${respuesta.codigoEstado} - mensaje: ${respuesta.mensaje}"
		if (Respuesta.SC_OK == respuesta.codigoEstado){
			byte[] mensajeValidadoBytes = firmaService.generarMultifirmaBytes(
					params.smimeMessageReq,	message(
						code:'unsubscribeRepresentativeValidationSubject'))
			MensajeSMIME mensajeSMIMEValidado = new MensajeSMIME(
					tipo:Tipo.REPRESENTATIVE_UNSUBSCRIBE_VALIDATION,
					smimePadre: respuesta.mensajeSMIME,
					usuario:respuesta.usuario, valido:true,
					contenido:mensajeValidadoBytes)
			MensajeSMIME.withTransaction {
				mensajeSMIMEValidado.save();
			}
			respuesta.mensajeSMIMEValidado = mensajeSMIMEValidado
		}
		flash.respuesta = respuesta
		response.status = respuesta.codigoEstado
		render respuesta.mensaje
		return false
	}
	
	/**
	 *
	 * Servicio que guarda las selecciones de representantes echas por los usuarios
	 *
	 * @param archivoFirmado Archivo firmado en formato SMIME en cuyo contenido se
	 *        encuentra los datos del representante en el que delega el firmante
	 * @httpMethod POST
	 * @return 
	 */
	def guardarAccreditationsRequest() {
		Respuesta respuesta = representativeService.processAccreditationsRequest(
			params.smimeMessageReq, request.getLocale())
		log.debug "guardarAccreditationsRequest - statusCode: ${respuesta.codigoEstado} - mensaje: ${respuesta.mensaje}"
		response.status = respuesta.codigoEstado
		render respuesta.mensaje
		return false
	}
	
	/**
	 *
	 * Servicio que guarda las consultas de historiales de votaciones de los
	 * representantes
	 *
	 * @param archivoFirmado Archivo firmado en formato SMIME con los datos
	 * 		  del representante consultado
	 * @httpMethod POST
	 * @return 
	 */
	def guardarVotingHistoryRequest() {
		Respuesta respuesta = representativeService.precessVotingHistoryRequest(
			params.smimeMessageReq, request.getLocale())
		if (Respuesta.SC_OK != respuesta.codigoEstado) {
			log.debug "Problemas procesando solicitud de copia de seguridad - ${respuesta.mensaje}"
		}
		response.status = respuesta.codigoEstado
		render respuesta.mensaje
		return false
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
			MensajeSMIME.withTransaction {
				mensajeSMIMEValidado.save();
			}
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
			params.smimeMessageReq, params.long('id'), imageBytes, request.getLocale())
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
