package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.*
import org.votingsystem.util.NifUtils

class RepresentativeController {

	def representativeService
    def representativeDelegationService
	def signatureVSService
	def grailsApplication
    def csrService

	
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
	 * @return documento JSON con datos del representante
	 */
	def edit() {
        if(params.nif) {
            String nif = NifUtils.validate(params.nif)
            if(!nif) {
                return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code: 'NIFWithErrorsMsg', args:[params.nif]))]
            } else {
                UserVS representative
                UserVS.withTransaction { representative =  UserVS.findWhere(type:UserVS.Type.REPRESENTATIVE, nif:nif) }
                if(!representative) {
                    return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                            message(code: 'representativeNifErrorMsg', args:[nif]))]
                } else {
                    String name = "${representative.name} ${representative.firstName}"
                    def resultMap = [id: representative.id, name:representative.name,
                                     firstName:representative.firstName, info: representative.description,
                                     nif:representative.nif, fullName:"${representative.name} ${representative.firstName}"]
                    render resultMap as JSON
                }
            }
        } else render(view:"editRepresentative" , model:[selectedSubsystem:SubSystemVS.REPRESENTATIVES.toString()])
	}

    def remove() {
        render(view:"removeRepresentative", model:[selectedSubsystem:SubSystemVS.REPRESENTATIVES.toString()])
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
		def representativeList = null
        def representativeMap = null
        if (params.long('id')) {
			UserVS representative
			UserVS.withTransaction {
				representative = UserVS.findWhere(id:params.long('id'), type:UserVS.Type.REPRESENTATIVE)
			}
			if (representative) {
				representativeMap = representativeService.getRepresentativeDetailedMap(representative)
				if(request.contentType?.contains(ContentTypeVS.JSON.getName())) {
					render representativeMap as JSON
				} else {
					render(view:"index", model: [selectedSubsystem:SubSystemVS.REPRESENTATIVES.toString(),
						representative:representativeMap])
				}
			} else return [responseVS : new ResponseVS(ResponseVS.SC_ERROR,
                        message(code:'representativeIdErrorMsg', args:[params.id]))]
		} else {
            representativeMap = new HashMap()
            representativeMap.representatives = []
			UserVS.withTransaction {
                representativeList = UserVS.createCriteria().list(max: params.max, offset: params.offset,
                        sort:params.sort, order:params.order) {
                    eq("type", UserVS.Type.REPRESENTATIVE)
                }
			}
			representativeMap.offset = params.long('offset')
            representativeMap.numTotalRepresentatives = representativeList.totalCount
            representativeMap.numRepresentatives = representativeList.totalCount
            representativeList.each {representative ->
                representativeMap.representatives.add(representativeService.getRepresentativeMap(representative))
            }
            render representativeMap as JSON
		}
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
	 * @return documento JSON con datos del representante
	 */
	def getByNif() {
		String nif = NifUtils.validate(params.nif)
		if(!nif) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: 'NIFWithErrorsMsg', args:[params.nif]))]
		}
		UserVS representative
		UserVS.withTransaction { representative =  UserVS.findWhere(type:UserVS.Type.REPRESENTATIVE, nif:nif) }
		if(!representative) {
            return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'representativeNifErrorMsg', args:[nif]))]
		} else {
			def resultMap = [representativeId: representative.id,
                    representativeName:"${representative.name} ${representative.firstName}",
				    representativeNIF:representative.nif]
			render resultMap as JSON
		}
	}
	
	/**
	 *
	 * Servicio que da de baja un representante
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/representative/revoke]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. documento firmado
	 *                     en formato SMIME con los datos de la baja.
	 * @responseContentType [application/x-pkcs7-signature] Obligatorio. Recibo firmado por el sistema.
	 * 
	 */
	def revoke() {
		MessageSMIME messageSMIME = request.messageSMIMEReq
		if(!messageSMIME) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
		}
		ResponseVS responseVS = representativeService.processRevoke(messageSMIME, request.getLocale())
		if (ResponseVS.SC_OK == responseVS.statusCode){
            responseVS.setContentType(ContentTypeVS.SIGNED)
		}
        return [responseVS : responseVS]
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
	 * 					   documento firmado en formato SMIME con los datos de la solicitud
	 */
	def accreditations() {
		MessageSMIME messageSMIME = request.messageSMIMEReq
		if(!messageSMIME) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
		} else  return [responseVS : representativeService.processAccreditationsRequest(
			messageSMIME, request.getLocale())]
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
	 *                     documento en formato SMIME con los datos del representante consultado.
	 * @return 
	 */
	def history() {
		MessageSMIME messageSMIME = request.messageSMIMEReq
		if(!messageSMIME) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
		} else return [responseVS : representativeService.processVotingHistoryRequest(messageSMIME,request.getLocale())]
	}
	
	/**
	 * 
	 * Servicio que guarda las selecciones de representantes echas por los usuarios
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/representative/delegation]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. documento firmado
	 *                     por el usuario que está eligiendo el representante.
	 * @responseContentType [application/x-pkcs7-signature] Recibo firmado por el sistema.
	 * @return Recibo que consiste en el documento enviado por el usuario con la firma añadida del servidor.
	 */
	def delegation() {
		MessageSMIME messageSMIME = request.messageSMIMEReq
		if(!messageSMIME) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
		}
		ResponseVS responseVS = representativeDelegationService.saveDelegation(messageSMIME, request.getLocale())
		if (ResponseVS.SC_OK == responseVS.statusCode){
            responseVS.setContentType(ContentTypeVS.SIGNED)
		}
        return [responseVS : responseVS]
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
		byte[] imageBytes = params[ContextVS.IMAGE_FILE_NAME]
		MessageSMIME messageSMIMEReq = params[ContextVS.REPRESENTATIVE_DATA_FILE_NAME]
		if(!messageSMIMEReq || !imageBytes) {
			String msg
			if(!imageBytes) msg = message(code: 'imageMissingErrorMsg')
			else msg = message(code: 'representativeDataMissingErrorMsg')
            return [responseVS :new ResponseVS(ResponseVS.SC_ERROR_REQUEST, msg)]
		}
		request.messageSMIMEReq = messageSMIMEReq
		if(imageBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
			response.status =  ResponseVS.SC_ERROR_REQUEST
			String msg = message(code: 'imageSizeExceededMsg', args:[imageBytes.length/1024,
                    ContextVS.IMAGE_MAX_FILE_SIZE_KB])
			log.error "processFileMap - ERROR - msg: ${msg}"
            return [responseVS : new ResponseVS(message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    type:TypeVS.REPRESENTATIVE_DATA_ERROR)]
		} else return [responseVS : representativeService.saveRepresentativeData(
                    messageSMIMEReq, imageBytes, request.getLocale())]
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
        Map dataMap = null
		if(params.long('id')) {
			ImageVS.withTransaction{ image = ImageVS.get(params.long('id')) }
			if(!image) msg = message(code:'imageNotFound', args:[params.id])
		} else if(params.long('representativeId')) {
			UserVS representative
			UserVS.withTransaction { representative = UserVS.get(params.long('representativeId')) }
			if (UserVS.Type.REPRESENTATIVE == representative?.type) {
                ImageVS.withTransaction{ image = ImageVS.findWhere(userVS:representative,
                        type:ImageVS.Type.REPRESENTATIVE) }
                dataMap = [fileName:"imageRepresentative_${representative.id}"]
				if(!image) msg = message(code:'representativeWithoutImageErrorMsg', args:[params.representativeId])
			} else  msg = message(code:'representativeIdErrorMsg', args[params.representativeId])
		}
		if (image) return [responseVS : new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.IMAGE,
                    messageBytes: image.fileBytes, data:dataMap)]
		else return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND, msg)]
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
	 * del voto de los representates en el momento en que finaliza una votación.
	 */
	def accreditationsBackupForEvent() {
		log.debug("getAccreditationsBackupForEvent - event: ${params.id}")
		EventVSElection event = null
		EventVSElection.withTransaction { event = EventVSElection.get(params.long('id')) }
		String msg = null
		if(!event) return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'eventVSNotFound'))]
		else {
            if(event.isActive(Calendar.getInstance().getTime())) {
                return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'eventDateNotFinished'))]
            } else {
                return [responseVS : representativeService.getAccreditationsBackupForEvent(
                        event, request.getLocale())]
            }
        }
	}


    /**
     *
     * Servicio que guarda las selecciones anónimas de representantes echas por los usuarios
     *
     * @httpMethod [POST]
     * @serviceURL [/representative/anonymousDelegation]
     * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. documento firmado por
     *                      el usuario con un certificado anónimo en el que figuran los datos del representante.
     * @responseContentType [application/x-pkcs7-signature] Recibo firmado por el sistema.
     * @return Recibo que consiste en el documento enviado por el usuario con la firma añadida del servidor.
     */
    def anonymousDelegation() {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS: new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
        }
        ResponseVS responseVS = representativeDelegationService.saveAnonymousDelegation(
                messageSMIMEReq, request.getLocale())
        if (ResponseVS.SC_OK == responseVS.statusCode){
            responseVS.setContentType(ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED)
        }
        return [responseVS : responseVS, receiverCert:messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate]
    }

    /**
     * Servicio que valida las delegaciones anónimas de representantes.
     *
     * @httpMethod [POST]
     * @serviceURL [/representative/anonymousDelegationRequest]
     * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] La solicitud de certificado de delegación.
     * @param [csr] Obligatorio. La solicitud de certificado de delegación anónima.
     * @return La solicitud de certificado de delegación anónima firmada.
     */
    def processAnonymousDelegationRequestFileMap() {
        MessageSMIME messageSMIMEReq = params[ContextVS.REPRESENTATIVE_DATA_FILE_NAME]
        request.messageSMIMEReq = messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = representativeDelegationService.validateAnonymousRequest(
                messageSMIMEReq, request.getLocale())
        if (ResponseVS.SC_OK == responseVS.statusCode) {
            byte[] csrRequest = params[ContextVS.CSR_FILE_NAME]
            ResponseVS csrValidationResponse = csrService.signAnonymousDelegationCert(csrRequest, request.getLocale())
            if (ResponseVS.SC_OK == csrValidationResponse.statusCode) {
                csrValidationResponse.setContentType(ContentTypeVS.MULTIPART_ENCRYPTED)
                return [responseVS:csrValidationResponse, receiverPublicKey:csrValidationResponse.data.requestPublicKey]
            } else return [responseVS:csrValidationResponse]
        } else return [responseVS:responseVS]
    }

    /**
     * Servicio que cancela delegaciones anónimas de representantes.
     *
     * @httpMethod [POST]
     * @serviceURL [/representative/cancelAnonymousDelegation]
     * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. documento firmado
     *                     por el usuario en el que figuran los datos de la cancelación.
     * @responseContentType [application/x-pkcs7-signature] Recibo firmado por el sistema.
     * @return Recibo que consiste en el documento enviado por el usuario con la firma añadida del servidor.
     */
    def cancelAnonymousDelegation() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
        }
        ResponseVS responseVS = representativeDelegationService.cancelAnonymousDelegation(messageSMIME, request.getLocale())
        if (ResponseVS.SC_OK == responseVS.statusCode){
            responseVS.setContentType(ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED)
        }
        return [responseVS : responseVS]
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }

}