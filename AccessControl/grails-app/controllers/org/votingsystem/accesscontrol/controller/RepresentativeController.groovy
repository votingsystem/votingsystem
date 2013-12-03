package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.ImageVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubSystemVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils

class RepresentativeController {

	def representativeService
	def signatureVSService
	def grailsApplication
	
	private static final int MAX_FILE_SIZE_KB = 512;
	private static final int MAX_FILE_SIZE = 512 * 1024;

	
	/**
	 * @httpMethod [GET]
	 * @return La página principal de la aplicación web de representantes.
	 */
	def main() {
		render(view:"main" , model:[selectedSubsystem:SubSystemVS.REPRESENTATIVES.toString()])
	}
	
	/**
	 * @httpMethod [GET]
	 * @return Página a partir de la que se pueden crear representantes.
	 */
	def newRepresentative() {
		render(view:"newRepresentative" , model:[selectedSubsystem:SubSystemVS.REPRESENTATIVES.toString()])
	}
	
	/**
	 * @httpMethod [GET]
	 *
	 * @serviceURL [/representative/edit/$nif] 
	 * @param [nif] NIF del representante que se desea consultar.
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con datos del representante
	 */
	def editRepresentative() {
		String nif = NifUtils.validate(params.nif)
		if(!nif) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'nifWithErrors', args:[params.nif])
			return false
		}
		UserVS representative
		UserVS.withTransaction {
			representative =  UserVS.findWhere(type:UserVS.Type.REPRESENTATIVE,
				nif:nif)
		}
		if(!representative) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'representativeNifErrorMsg', args:[nif])
			return false
		} else {
			String name = "${representative.name} ${representative.firstName}"
			def resultMap = [id: representative.id, name:representative.name,
				firstName:representative.firstName, info:representative.info,
				nif:representative.nif, fullName:"${representative.name} ${representative.firstName}"]
			if(request.contentType?.contains(ContentTypeVS.JSON)) {
				render resultMap as JSON
				return false
			} else {
				render(view:"editRepresentative" , model:[representative:resultMap,
				selectedSubsystem:SubSystemVS.REPRESENTATIVES.toString()])
				return
			}
		}
	}
	
	/**
	 * @httpMethod [GET]
	 * @return Página de la sección de administración PARA representantes.
	 */
	def representativeAdmin() {
		render(view:"representativeAdmin" , model:[selectedSubsystem:SubSystemVS.REPRESENTATIVES.toString()])
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
		representativeMap.representatives = []
		if (params.long('id')) {
			UserVS representative
			UserVS.withTransaction {
				representative = UserVS.findWhere(id:params.long('id'), type:UserVS.Type.REPRESENTATIVE)
			}
			if (representative) {
				representativeMap = representativeService.getRepresentativeDetailedMap(representative)
				if(request.contentType?.contains(ContentTypeVS.JSON)) {
					render representativeMap as JSON
					return false
				} else {
					render(view:"index", model: [selectedSubsystem:SubSystemVS.REPRESENTATIVES.toString(),
						representative:representativeMap])
					return false
				}
			} else {
				String msg = message(code:'representativeIdErrorMsg', args:[params.id])
				log.debug msg
				render msg
			}
			return false
		} else {
			UserVS.withTransaction {
				representativeList = UserVS.findAllByType(UserVS.Type.REPRESENTATIVE, params)
			}
			representativeMap.offset = params.long('offset')
		}

		representativeMap.representativesTotalNumber = UserVS.countByType(UserVS.Type.REPRESENTATIVE)
		representativeMap.numberRepresentatives = representativeList.size()
		representativeList.each {representative ->
				representativeMap.representatives.add(representativeService.getRepresentativeMap(representative))
		}
		render representativeMap as JSON
	}
	
	/**
	 *
	 * Servicio que sirve para get información básica de un representante
	 * a partir de su NIF
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/representative/nif/$nif]
	 * @param [nif] NIF del representante que se desea consultar.
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con datos del representante
	 */
	def getByNif() {
		String nif = NifUtils.validate(params.nif)
		if(!nif) {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'nifWithErrors', args:[params.nif])
			return false
		}
		UserVS index
		UserVS.withTransaction {
			representative =  UserVS.findWhere(type:UserVS.Type.REPRESENTATIVE,
				nif:nif)
		}
		if(!index) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'representativeNifErrorMsg', args:[nif])
			return false
		} else {
			String name = "${index.name} ${index.firstName}"
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
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. PDFDocumentVS firmado
	 *                     en formato SMIME con los datos de la baja.
	 * @responseContentType [application/x-pkcs7-signature] Obligatorio. Recibo firmado por el sistema.
	 * 
	 */
	def revoke() {
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'requestWithoutFile')
			log.debug msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		ResponseVS responseVS = representativeService.
			processRevoke(messageSMIME, request.getLocale())
		params.responseVS = responseVS
		if (ResponseVS.SC_OK == responseVS.statusCode){
			response.contentType = org.votingsystem.model.ContentTypeVS.SIGNED
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
	 * 					   PDFDocumentVS firmado en formato SMIME con los datos de la solicitud
	 */
	def accreditations() {
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'requestWithoutFile')
			log.debug msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		ResponseVS responseVS = representativeService.processAccreditationsRequest(
			messageSMIME, request.getLocale())
		params.responseVS = responseVS
		if (ResponseVS.SC_OK == responseVS.statusCode){
			render responseVS.message
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
	 *                     PDFDocumentVS en formato SMIME con los datos del representante consultado.
	 * @return 
	 */
	def history() {
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'requestWithoutFile')
			log.debug msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		ResponseVS responseVS = representativeService.processVotingHistoryRequest(
			messageSMIME, request.getLocale())
		params.responseVS = responseVS
		if (ResponseVS.SC_OK == responseVS.statusCode){
			render responseVS.message
		}
	}
	
	/**
	 * 
	 * Servicio que guarda las selecciones de representantes echas por los usuarios
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/representative/userSelection]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. PDFDocumentVS firmado
	 *                     por el userVS que está eligiendo el representante.
	 * @responseContentType [application/x-pkcs7-signature] Recibo firmado por el sistema.
	 * @return Recibo que consiste en el PDFDocumentVS enviado por el userVS con la signatureVS añadida del servidor.
	 */
	def userSelection() {
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'requestWithoutFile')
			log.debug msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		ResponseVS responseVS = representativeService.saveUserRepresentative(
			messageSMIME, request.getLocale())
		
		params.responseVS = responseVS
		if (ResponseVS.SC_OK == responseVS.statusCode){
			response.contentType = org.votingsystem.model.ContentTypeVS.SIGNED
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
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		params.messageSMIMEReq = messageSMIMEReq
		if(imageBytes.length > MAX_FILE_SIZE) {
			response.status =  ResponseVS.SC_ERROR_REQUEST
			String msg = message(code: 'imageSizeExceededMsg', 
				args:[imageBytes.length/1024, MAX_FILE_SIZE_KB])
			log.error "processFileMap - ERROR - msg: ${msg}"
			params.responseVS = new ResponseVS(message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    type:TypeVS.REPRESENTATIVE_DATA_ERROR)
		} else {
			ResponseVS responseVS = representativeService.saveRepresentativeData(
				messageSMIMEReq, imageBytes, request.getLocale())
			params.responseVS = responseVS
			if(ResponseVS.SC_OK == responseVS.statusCode) {
				render responseVS.message
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
		ImageVS image;
		String msg
		if(params.long('id')) {
			ImageVS.withTransaction{
				image = ImageVS.get(params.long('id'))
			}
			if(!image) msg = message(code:'imageNotFound', args:[params.id])
		} else if(params.long('representativeId')) {
			UserVS index
			UserVS.withTransaction {
				representative = UserVS.get(params.long('representativeId'))
			}
			if (UserVS.Type.REPRESENTATIVE == index?.type) {
				image = ImageVS.findWhere(userVS:index,
					type:ImageVS.Type.REPRESENTATIVE)
				if(!image) msg = message(code:'representativeWithoutImageErrorMsg', args:[params.representativeId])
			} else {
				msg = message(code:'representativeIdErrorMsg', args[params.representativeId])
			}
		}
		if (image) {
			response.status = ResponseVS.SC_OK
			//response.setContentType(ContentTypeVS.TEXT)
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
	 * @param [id] Obligatorio. El id del eventVS en la base de datos del Control de Acceso
	 * 							en que se publicó
	 * @return El archivo zip con todos los datos necesarios para establecer el valor 
	 * del voteVS de los representates en el momento en que finaliza una votación.
	 */
	def accreditationsBackupForEvent() {
		log.debug("getAccreditationsBackupForEvent - event: ${params.id}")
		EventVSElection event = null
		EventVSElection.withTransaction {
			event = EventVSElection.get(params.long('id'))
		}
		String msg = null
		if(!event) {
			msg = message(code: 'eventVSNotFound')
			log.error "accreditationsBackupForEvent - ERROR - msg: ${msg}"
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		if(event.isActive(DateUtils.getTodayDate())) {
			msg = message(code: 'eventDateNotFinished')
			log.error "accreditationsBackupForEvent - ERROR - msg: ${msg}"
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}

		String downloadFileName = message(code:'repAccreditationsBackupForEventFileName',
			args:[event.id])
		ResponseVS responseVS = representativeService.getAccreditationsBackupForEvent(
				event, request.getLocale()) 
		
		if(ResponseVS.SC_OK == responseVS.statusCode) {
			File baseDirZipped = responseVS.file
			byte[] fileBytes = baseDirZipped.getBytes()
			response.setHeader("Content-Disposition", "inline; filename='${downloadFileName}'");
			response.setContentType("application/zip")
			response.contentLength = fileBytes.length
			response.outputStream <<  fileBytes
			response.outputStream.flush()
		} else params.status = responseVS
		
		
	}

}
