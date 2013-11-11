package org.votingsystem.accesscontrol.controller

import org.votingsystem.accesscontrol.model.*
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.StringUtils;
import org.votingsystem.model.ContextVS
import grails.converters.JSON

import org.votingsystem.util.DateUtils;

class RepresentativeController {

	def representativeService
	def firmaService
	def grailsApplication
	
	private static final int MAX_FILE_SIZE_KB = 512;
	private static final int MAX_FILE_SIZE = 512 * 1024;

	
	/**
	 * @httpMethod [GET]
	 * @return La página principal de la aplicación web de representantes.
	 */
	def mainPage() {
		render(view:"mainPage" , model:[selectedSubsystem:Subsystem.REPRESENTATIVES.toString()])
	}
	
	/**
	 * @httpMethod [GET]
	 * @return Página a partir de la que se pueden crear representantes.
	 */
	def newRepresentative() {
		render(view:"newRepresentative" , model:[selectedSubsystem:Subsystem.REPRESENTATIVES.toString()])
	}
	
	/**
	 * @httpMethod [GET]
	 *
	 * @serviceURL [/representative/edit/$nif] 
	 * @param [nif] NIF del representante que se desea consultar.
	 * @responseContentType [application/json]
	 * @return Documento JSON con datos del representante
	 */
	def editRepresentative() {
		String nif = StringUtils.validarNIF(params.nif)
		if(!nif) {
			response.status = ResponseVS.SC_ERROR_PETICION
			render message(code: 'error.errorNif', args:[params.nif])
			return false
		}
		Usuario representative
		Usuario.withTransaction {
			representative =  Usuario.findWhere(type:Usuario.Type.REPRESENTATIVE,
				nif:nif)
		}
		if(!representative) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'representativeNifErrorMsg', args:[nif])
			return false
		} else {
			String name = "${representative.nombre} ${representative.primerApellido}"
			def resultMap = [id: representative.id, nombre:representative.nombre,
				primerApellido:representative.primerApellido, info:representative.info,
				nif:representative.nif, fullName:"${representative.nombre} ${representative.primerApellido}"]
			if(request.contentType?.contains("application/json")) {
				render resultMap as JSON
				return false
			} else {
				render(view:"editRepresentative" , model:[representative:resultMap,
				selectedSubsystem:Subsystem.REPRESENTATIVES.toString()])
				return
			}
		}
	}
	
	/**
	 * @httpMethod [GET]
	 * @return Página de la sección de administración PARA representantes.
	 */
	def representativeAdmin() {
		render(view:"representativeAdmin" , model:[selectedSubsystem:Subsystem.REPRESENTATIVES.toString()])
	}
	
	/**
	 * Servicio de consulta de representantes
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/representative/$id?]
	 * @param [id] Opcional. El identificador del representante en la base de datos.
	 * @responseContentType [application/json]
	 */
	def index() {
		def representativeList = []
		def representativeMap = new HashMap()
		//eventosMap.eventos = new HashMap()
		representativeMap.representatives = []
		if (params.long('id')) {
			Usuario representative
			Usuario.withTransaction {
				representative = Usuario.findWhere(id:params.long('id'),
					type:Usuario.Type.REPRESENTATIVE)
			}
			if (representative) {
				representativeMap = representativeService.
					getRepresentativeDetailedJSONMap(representative)
				if(request.contentType?.contains("application/json")) {
					render representativeMap as JSON
					return false
				} else {
					render(view:"index", model: [selectedSubsystem:Subsystem.REPRESENTATIVES.toString(),
						representative:representativeMap])
					return
				}
			} else {
				String msg = message(code:'representativeIdErrorMsg', args:[params.id])
				log.debug msg
				render msg
			}
			return false
		} else {
			log.debug " -Params: " + params
			Usuario.withTransaction {
				representativeList = Usuario.findAllByType(Usuario.Type.REPRESENTATIVE, params)
			}
			representativeMap.offset = params.long('offset')
		}

		representativeMap.representativesTotalNumber = Usuario.
				   countByType(Usuario.Type.REPRESENTATIVE)
		representativeMap.numberRepresentativesInRequest = representativeList.size()
		representativeList.each {representative ->
				representativeMap.representatives.add(representativeService.getRepresentativeJSONMap(representative))
		}
		render representativeMap as JSON
	}
	
	/**
	 *
	 * Servicio que sirve para obtener información básica de un representante 
	 * a partir de su NIF
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/representative/nif/$nif]
	 * @param [nif] NIF del representante que se desea consultar.
	 * @responseContentType [application/json]
	 * @return Documento JSON con datos del representante
	 */
	def getByNif() {
		String nif = StringUtils.validarNIF(params.nif)
		if(!nif) {
			response.status = ResponseVS.SC_ERROR_PETICION
			render message(code: 'error.errorNif', args:[params.nif])
			return false
		}
		Usuario index
		Usuario.withTransaction {
			representative =  Usuario.findWhere(type:Usuario.Type.REPRESENTATIVE,
				nif:nif)
		}
		if(!index) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'representativeNifErrorMsg', args:[nif])
			return false
		} else {
			String name = "${index.nombre} ${index.primerApellido}"
			def resultMap = [representativeId: index.id, representativeName:name,
				representativeNIF:index.nif]
			render resultMap as JSON
			return false
		}
	}
	
	/**
	 *
	 * Servicio que da de baja un representante
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/representative/revoke]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. Documento firmado 
	 *                     en formato SMIME con los datos de la baja.
	 * @responseContentType [application/x-pkcs7-signature] Obligatorio. Recibo firmado por el sistema.
	 * 
	 */
	def revoke() {
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.debug msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
		ResponseVS respuesta = representativeService.
			processRevoke(messageSMIME, request.getLocale())
		params.respuesta = respuesta
		if (ResponseVS.SC_OK == respuesta.statusCode){
			response.contentType = ContextVS.SIGNED_CONTENT_TYPE
		}
	}
	
	/**
	 *
	 * Servicio que tramita solicitudes de información sobre las acreditaciones que tiene
	 * un representante. 
	 * El solicitante debe proporcionar una dirección de email en la que recibirá 
	 * instrucciones de descarga de las acreditaciones en vigor del representante.
	 * 
	 * @httpMethod [POST]
	 * @serviceURL [/representative/accreditations]
	 * 
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. 
	 * 					   Documento firmado en formato SMIME con los datos de la solicitud
	 */
	def accreditations() {
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.debug msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
		ResponseVS respuesta = representativeService.processAccreditationsRequest(
			messageSMIME, request.getLocale())
		params.respuesta = respuesta
		if (ResponseVS.SC_OK == respuesta.statusCode){
			render respuesta.message
		}
	}
	
	/**
	 *
	 * Servicio que guarda las consultas de historiales de votaciones de los
	 * representantes
	 *                     
	 * @httpMethod [POST]
	 * @serviceURL [/representative/history]
	 * 
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. 
	 *                     Documento en formato SMIME con los datos del representante consultado.
	 * @return 
	 */
	def history() {
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.debug msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
		ResponseVS respuesta = representativeService.processVotingHistoryRequest(
			messageSMIME, request.getLocale())
		params.respuesta = respuesta
		if (ResponseVS.SC_OK == respuesta.statusCode){
			render respuesta.message
		}
	}
	
	/**
	 * 
	 * Servicio que guarda las selecciones de representantes echas por los usuarios
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/representative/userSelection]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. Documento firmado
	 *                     por el usuario que está eligiendo el representante.
	 * @responseContentType [application/x-pkcs7-signature] Recibo firmado por el sistema.
	 * @return Recibo que consiste en el documento enviado por el usuario con la firma añadida del servidor.
	 */
	def userSelection() {
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.debug msg
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
		ResponseVS respuesta = representativeService.saveUserRepresentative(
			messageSMIME, request.getLocale())
		
		params.respuesta = respuesta
		if (ResponseVS.SC_OK == respuesta.statusCode){
			response.contentType = ContextVS.SIGNED_CONTENT_TYPE
		}
	}
	
	/**
	 * Servicio que valida las solicitudes de representación
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/representative]
	 * @param [image] Obligatorio. La imagen asociada al representante.
	 * @param [representativeData] Obligatorio. Los datos del representante firmados en un archivo SMIME.
	 * @requestContentType [image/gif] Posible type de contenido asociado al parámetro 'image' 
	 * @requestContentType [image/jpeg] Posible type de contenido asociado al parámetro 'image' 
	 * @requestContentType [image/png] Posible type de contenido asociado al parámetro 'image' 
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] El type de contenido 
	 * 					   asociado a los datos del representante. 
	 */
    def processFileMap() { 
		byte[] imageBytes = params[grailsApplication.config.SistemaVotacion.imageFileName]
		MessageSMIME messageSMIMEReq = params[
			grailsApplication.config.SistemaVotacion.representativeDataFileName]
		if(!messageSMIMEReq || !imageBytes) {
			String msg
			if(!imageBytes) msg = message(code: 'imageMissingErrorMsg')
			else msg = message(code: 'representativeDataMissingErrorMsg')
			log.error "processFileMap - ERROR - msg: ${msg}"
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
		params.messageSMIMEReq = messageSMIMEReq
		if(imageBytes.length > MAX_FILE_SIZE) {
			response.status =  ResponseVS.SC_ERROR_PETICION
			String msg = message(code: 'imageSizeExceededMsg', 
				args:[imageBytes.length/1024, MAX_FILE_SIZE_KB])
			log.error "processFileMap - ERROR - msg: ${msg}"
			params.respuesta = new ResponseVS(message:msg,
				statusCode:ResponseVS.SC_ERROR_PETICION,
				type:TypeVS.REPRESENTATIVE_DATA_ERROR)
		} else {
			ResponseVS respuesta = representativeService.saveRepresentativeData(
				messageSMIMEReq, imageBytes, request.getLocale())
			params.respuesta = respuesta
			if(ResponseVS.SC_OK == respuesta.statusCode) {
				render respuesta.message
			}
		}
	}

	/**
	 * Servicio que devuelve la imagen asociada a un representante
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/representative/image/$id]
     * @serviceURL [/representative/$representativeId/image]
	 * @param [id] Obligatorio. El id de la imagen en la base de datos.
	 * @param [representativeId] Obligatorio. El id del representante en la base de datos.
	 */
	def image() {
		Image image;
		String msg
		if(params.long('id')) {
			Image.withTransaction{
				image = Image.get(params.long('id'))
			}
			if(!image) msg = message(code:'imageNotFound', args:[params.id])
		} else if(params.long('representativeId')) {
			Usuario index
			Usuario.withTransaction {
				representative = Usuario.get(params.long('representativeId'))
			}
			if (Usuario.Type.REPRESENTATIVE == index?.type) {
				image = Image.findWhere(usuario:index, 
					type:Image.Type.REPRESENTATIVE)
				if(!image) msg = message(code:'representativeWithoutImageErrorMsg', args:[params.representativeId])
			} else {
				msg = message(code:'representativeIdErrorMsg', args[params.representativeId])
			}
		}
		if (image) {
			response.status = ResponseVS.SC_OK
			//response.setContentType("text/plain")
			response.contentLength = image.fileBytes.length
			response.outputStream <<  image.fileBytes
			response.outputStream.flush()
			return false
		}
		response.status = ResponseVS.SC_NOT_FOUND
		log.debug msg
		render msg
		return false
	}
	
	/**
	 * Servicio que devuelve la copia de seguridad en formato zip con los datos de los 
	 * representantes en el momento en el que ha finalizado una votación.
	 * 
	 * Este archivo se utiliza para hacer los recuentos definitivos.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/representative/accreditationsBackupForEvent/$id]
	 * @param [id] Obligatorio. El id del evento en la base de datos del Control de Acceso
	 * 							en que se publicó
	 * @return El archivo zip con todos los datos necesarios para establecer el valor 
	 * del voto de los representates en el momento en que finaliza una votación.
	 */
	def accreditationsBackupForEvent() {
		log.debug("getAccreditationsBackupForEvent - event: ${params.id}")
		EventoVotacion event = null
		EventoVotacion.withTransaction {
			event = EventoVotacion.get(params.long('id'))
		}
		String msg = null
		if(!event) {
			msg = message(code: 'eventNotFound')
			log.error "accreditationsBackupForEvent - ERROR - msg: ${msg}"
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}
		if(event.isOpen(DateUtils.getTodayDate())) {
			msg = message(code: 'eventDateNotFinished')
			log.error "accreditationsBackupForEvent - ERROR - msg: ${msg}"
			response.status = ResponseVS.SC_ERROR_PETICION
			render msg
			return false
		}

		String downloadFileName = message(code:'repAccreditationsBackupForEventFileName',
			args:[event.id])
		ResponseVS respuesta = representativeService.getAccreditationsBackupForEvent(
				event, request.getLocale()) 
		
		if(ResponseVS.SC_OK == respuesta.statusCode) {
			File baseDirZipped = respuesta.file
			byte[] fileBytes = baseDirZipped.getBytes()
			response.setHeader("Content-Disposition", "inline; filename='${downloadFileName}'");
			response.setContentType("application/zip")
			response.contentLength = fileBytes.length
			response.outputStream <<  fileBytes
			response.outputStream.flush()
		} else params.status = respuesta
		
		
	}

}
