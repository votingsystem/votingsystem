package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*
import org.votingsystem.util.NifUtils

class RepresentativeController {

	def representativeService
    def representativeDelegationService
	def signatureVSService
	def grailsApplication
	
	/**
	 * @httpMethod [GET]
	 * @return Form to activate representatives
	 */
	def newRepresentative() {
		render(view:"newRepresentative")
	}
	
	/**
	 * Service tha provides form completed with representative data that matches the NIF requested
     * @httpMethod [GET]
	 * @serviceURL [/representative/edit/$nif] 
	 * @param [nif] Required. Representative NIF
	 * @return HTML form
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
        } else render(view:"editRepresentative")
	}

    /**
     * Service tha provides representation state of the NIF passed as param
     * @httpMethod [GET]
     * @serviceURL [/representative/state/$nif]
     * @param [nif] Required. The NIF to check
     * @return representation state in JSON format
     */
    def state() {
        if(params.nif) {
            Map resultMap = representativeDelegationService.checkRepresentationState(params.nif)
            render resultMap as JSON
        } else return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'missingParamErrorMsg', args:['nif']))]
    }

    /**
     * Service tha provides the representative data that matches the NIF requested
     * @httpMethod [GET]
     * @serviceURL [/representative/nif/$nif]
     * @param [nif] Required. Representante NIF
     * @responseContentType [application/json]
     * @return representative data in JSON format
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
	 * Service tha provides the list of representatives
	 * @httpMethod [GET]
	 * @serviceURL [/representative/$id?]
	 * @param [id] Optional. The representative identifier in the database.
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
				if(request.contentType?.contains('json')) {
					render representativeMap as JSON
				} else {
					render(view:"representative", model: [representativeMap:representativeMap])
				}
			} else return [responseVS : new ResponseVS(ResponseVS.SC_ERROR,
                        message(code:'representativeIdErrorMsg', args:[params.id]))]
		} else if(request.contentType?.contains("json")) {
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
                representativeMap.representatives.add(representativeService.getRepresentativeDetailedMap(representative))
            }
            render representativeMap as JSON
		} else render(view:"index")
	}

	
	/**
	 * Service thar revokes representatives
	 * @httpMethod [POST]
	 * @serviceURL [/representative/revoke]
	 * @requestContentType [application/pkcs7-signature] required. Signed documet
	 * @responseContentType [application/pkcs7-signature] request signed by system
	 */
	def revoke() {
		MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS : representativeService.processRevoke(messageSMIME)]
	}
	
	/**
     * Service that generates a zip file with the representative state.
	 * 
	 * @httpMethod [POST]
	 * @serviceURL [/representative/accreditations]
	 * @requestContentType [application/pkcs7-signature] Required. Signed document with the request data
     * @return When the zip is generated the system sends an email with download instructions
     */
	def accreditations() {
		MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS : representativeService.processAccreditationsRequest(messageSMIME)]
	}
	
	/**
     * Service that provides a zip file with the representative voting history
	 *                     
	 * @httpMethod [POST]
	 * @serviceURL [/representative/history]
	 * @requestContentType [application/pkcs7-signature] Required. Signed document with the request data
	 * @return When the zip is generated the system sends an email with download instructions
	 */
	def history() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS : representativeService.processVotingHistoryRequest(messageSMIME)]
	}
	
	/**
	 * Service that saves the representative selected by the user
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/representative/delegation]
	 * @requestContentType [application/pkcs7-signature] Required. Document signed with the selected representative data.
	 * @responseContentType [application/pkcs7-signature]
	 * @return The signed request with the additional system signature
	 */
	def delegation() {
		MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
		ResponseVS responseVS = representativeDelegationService.saveDelegation(messageSMIME)
		if (ResponseVS.SC_OK == responseVS.statusCode){
            responseVS.setContentType(ContentTypeVS.SIGNED)
		}
        return [responseVS : responseVS]
	}
	
	/**
	 * Service that registers representatives
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/representative]
	 * @param [image] Required. The image associated with the repsentative.
	 * @param [representativeData] Required. Document signed with the representative data.
	 */
    def processFileMap() { 
		byte[] imageBytes = params[ContextVS.IMAGE_FILE_NAME]
		MessageSMIME messageSMIMEReq = params[ContextVS.REPRESENTATIVE_DATA_FILE_NAME]
        request.messageSMIMEReq = messageSMIMEReq
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
		} else return [responseVS : representativeService.saveRepresentativeData(messageSMIMEReq, imageBytes)]
	}

	/**
	 * Service tha returns the image associated with one representative
	 * 
	 * @httpMethod [GET]
	 * @serviceURL [/representative/image/$id, /representative/$representativeId/image]
	 * @param [id] Optional. The image identifier in the database
	 * @param [representativeId] Optional. The representante identifier in the database
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
			} else  msg = message(code:'representativeIdErrorMsg', args:[params.representativeId])
		}
		if (image) return [responseVS : new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.IMAGE,
                    messageBytes: image.fileBytes, data:dataMap)]
		else return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND, msg)]
	}
	
	/**
	 * Service that generates a zip file with the representative state data when the election finish in order to make
     * the re-count
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/representative/accreditationsBackupForEvent/$id]
	 * @param [id] Required. The eventVS identifier in the Access Control database
	 * @return Zip file with all the files necessary to verify de representative state when the eventVS finish
	 */
	def accreditationsBackupForEvent() {
		EventVSElection event = null
		EventVSElection.withTransaction { event = EventVSElection.get(params.long('id')) }
		if(!event) return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'eventVSNotFound'))]
		else {
            if(event.isActive(Calendar.getInstance().getTime())) {
                return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'eventActiveErrorMsg'))]
            } else {
                return [responseVS : representativeService.getAccreditationsBackupForEvent(event)]
            }
        }
	}

    /**
     * Service that process the user anonymous representative selection
     *
     * @httpMethod [POST]
     * @serviceURL [/representative/anonymousDelegation]
     * @requestContentType [application/pkcs7-signature] Required. Document signed by an anonymous certificate with
     *                      the data of the representative selected.
     * @responseContentType [application/pkcs7-signature]
     * @return If it's all fine the request signed by the system.
     */
    def anonymousDelegation() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        //return [responseVS : responseVS, receiverCert:messageSMIMEReq?.getSMIME()?.getSigner()?.certificate]
        return [responseVS : representativeDelegationService.saveAnonymousDelegation(messageSMIME)]
    }

    /**
     * Service that provides the anonymous certificate required to select representative anonymously
     *
     * @httpMethod [POST]
     * @serviceURL [/representative/anonymousDelegationRequest]
     * @requestContentType [application/pkcs7-signature] The signed request
     * @param [csr] Required. the anonymous certificate CSR.
     * @return the anonymous certificate signed.
     */
    def processAnonymousDelegationRequestFileMap() {
        MessageSMIME messageSMIMEReq = params[ContextVS.REPRESENTATIVE_DATA_FILE_NAME]
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        } else request.messageSMIMEReq = messageSMIMEReq
        return [responseVS:representativeDelegationService.validateAnonymousRequest(
                messageSMIMEReq, params[ContextVS.CSR_FILE_NAME])]
    }

    /**
     * Service that cancels anonymous delegations
     *
     * @httpMethod [POST]
     * @serviceURL [/representative/cancelAnonymousDelegation]
     * @requestContentType [application/pkcs7-signature] Required. Document signed by the user with the data required
     *                      to cancel an anonymoous certificate
     * @responseContentType [application/pkcs7-signature]
     * @return If it's all fine the request signed by the system.
     */
    def cancelAnonymousDelegation() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
        }
        ResponseVS responseVS = representativeDelegationService.cancelAnonymousDelegation(messageSMIME)
        return [responseVS : responseVS]
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}