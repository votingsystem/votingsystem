package org.votingsystem.accesscontrol.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.accesscontrol.service.CsrService
import org.votingsystem.model.*
import org.votingsystem.util.ExceptionVS

import javax.xml.bind.annotation.adapters.HexBinaryAdapter

/**
 * @infoController Solicitudes de acceso
 * @descController Servicios relacionados con las solicitudes de acceso recibidas en una votación.
 * 
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class AccessRequestVSController {
    
    def accessRequestVSService
	def signatureVSService
	def csrService

	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/accessRequestVS/$id]
	 * @param [id] Obligatorio. El identificador de la solicitud de acceso en la base de datos.
	 * @return <a href="https://github.com/votingsystem/votingsystem/wiki/Solicitud-de-acceso">
	 * 			La solicitud de acceso</a> solicitada.
	 */
    def index () {
        if (params.long('id')) {
			def accessRequestVS
			AccessRequestVS.withTransaction {accessRequestVS = AccessRequestVS.get(params.id)}
            if (accessRequestVS) {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, messageBytes: accessRequestVS.
                        messageSMIME.content, contentType: ContentTypeVS.TEXT_STREAM)]
            } else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'voteCancellationAccessRequestNotFoundError'))]
        } else return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'requestWithErrors'))]
    }

	/**
	 * Service that validates vote requests (https://github.com/votingsystem/votingsystem/wiki/Solicitud-de-acceso)
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/accessRequestVS]
	 * @requestContentType [application/x-pkcs7-signature] User signed access request.
	 * @param [csr] Required. CSR to get the anonymous certificate which signs the vote.
	 * @return CSR signed.
	 */
    def processFileMap () {
		MessageSMIME messageSMIME = params[ContextVS.ACCESS_REQUEST_FILE_NAME]
		if(!messageSMIME) return [responseVS: ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
		ResponseVS responseVS = accessRequestVSService.saveRequest(messageSMIME)
		if (ResponseVS.SC_OK == responseVS.statusCode) {
            AccessRequestVS accessRequestVS = responseVS.data
            EventVSElection eventVS = responseVS.eventVS
			byte[] csrRequest = params[ContextVS.CSR_FILE_NAME]
			UserVS representative = (accessRequestVS.userVS.type == UserVS.Type.REPRESENTATIVE)?accessRequestVS.userVS:null
            try {
                CsrService.CsrResponse csrResponse = csrService.signCertVoteVS(csrRequest, responseVS.eventVS, representative)
                return [responseVS: new ResponseVS(messageSMIME:messageSMIME, type: TypeVS.ACCESS_REQUEST,
                        message: "EventVS_${eventVS.id}", messageBytes:csrResponse.issuedCert, contentType: ContentTypeVS.TEXT_STREAM)]
            } catch(Exception ex) {
                if (accessRequestVS) {
                    log.debug("cancelling accessRequestVS '${accessRequestVS.id}'")
                    AccessRequestVS.withTransaction {
                        accessRequestVS.metaInf = responseVS.message
                        accessRequestVS.state = AccessRequestVS.State.CANCELLED
                        accessRequestVS.save()
                    }
                }
                throw ex
            }
		} else return [responseVS:responseVS.setMessageSMIME(messageSMIME)]
    }
    
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/accessRequestVS/hashHex/$hashHex]
	 * @param [hashHex] Obligatorio. Hash en formato hexadecimal asociado
	 *        a la solicitud de acceso.
	 * @return La solicitud de acceso asociada al hash.
	 */
    def hashHex () {
        if (params.hashHex) {
            HexBinaryAdapter hexConverter = new HexBinaryAdapter();
            String hashAccessRequestBase64 = new String(hexConverter.unmarshal(params.hashHex))
            log.debug "hashAccessRequestBase64: ${hashAccessRequestBase64}"
            AccessRequestVS accessRequestVS = AccessRequestVS.findWhere(hashAccessRequestBase64:
                hashAccessRequestBase64)
            if (accessRequestVS) return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK,
                contentType:ContentTypeVS.TEXT_STREAM, messageBytes:accessRequestVS.content)]
            else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'accessRequestNotFound',args:[params.hashHex]))]
        }
        return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))]
    }
	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/accessRequestVS/eventVS/$eventId/nif/$nif]
	 * @param [eventId] Obligatorio. El identificador de la votación en la base de datos.
	 * @param [nif] Obligatorio. El nif del solicitante.
	 * @return La solicitud de acceso asociada al nif y el eventVS.
	 */
	def findByNif () {
		if(params.nif && params.long('eventId')) {
			EventVSElection eventVS
			EventVSElection.withTransaction {eventVS =  EventVSElection.get(params.eventId)}
			if(!eventVS) {
                return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND, message(code: 'eventVSNotFound',
                        args:[params.eventId]))]
			} else {
                UserVS userVS
                UserVS.withTransaction { userVS =  UserVS.findByNif(params.nif)}
                if(!userVS) {
                    return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                            message(code: 'userVSNotFoundByNIF', args:[params.nif]))]
                } else {
                    AccessRequestVS accessRequestVS
                    AccessRequestVS.withTransaction {
                        accessRequestVS =  AccessRequestVS.findWhere(userVS: userVS, eventVSElection:eventVS)
                    }
                    if(!accessRequestVS) {
                        return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                                message(code: 'nifWithoutAccessRequest', args:[params.eventId, params.nif]))]
                    } else {
                        return [responseVS:new ResponseVS(ResponseVS.SC_OK, contentType: ContentTypeVS.TEXT_STREAM,
                                messageBytes:  accessRequestVS.messageSMIME.content)]
                    }
                }
            }
		} else {
            return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                    contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrorsHTML',
                    args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))]
        }
	}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}