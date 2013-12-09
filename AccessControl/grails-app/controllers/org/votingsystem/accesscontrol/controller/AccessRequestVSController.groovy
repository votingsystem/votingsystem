package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.EventVSElection
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.AccessRequestVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
/**
 * @infoController Solicitudes de acceso
 * @descController Servicios relacionados con las solicitudes de acceso recibidas en una votación.
 * 
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class AccessRequestVSController {
    
    def accessRequestVSService
	def signatureVSService
	def csrService

	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/accessRequestVS/$id]
	 * @param [id] Obligatorio. El identificador de la solicitud de acceso en la base de datos.
	 * @return <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Solicitud-de-acceso">
	 * 			La solicitud de acceso</a> solicitada.
	 */
    def index () {
        if (params.long('id')) {
			def accessRequestVS
			AccessRequestVS.withTransaction {accessRequestVS = AccessRequestVS.get(params.id)}
            if (accessRequestVS) {
                params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK, messageBytes: accessRequestVS.
                        messageSMIME.content, contentType: ContentTypeVS.TEXT_STREAM)
            } else params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'voteCancellationAccessRequestNotFoundError'))
        } else params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'requestWithErrors'))
    }

	/**
	 * Servicio que valida las <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Solicitud-de-acceso">
	 * solicitudes de acceso</a> recibidas en una votación.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/accessRequestVS]
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] La solicitud de acceso.
	 * @param [csr] Obligatorio. La solicitud de certificado de voto.
	 * @return La solicitud de certificado de voto firmada.
	 */
    def processFileMap () {
		MessageSMIME messageSMIMEReq = params[grailsApplication.config.SistemaVotacion.accessRequestFileName]
		if(!messageSMIMEReq) {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))
			return
		}
		params.messageSMIMEReq = messageSMIMEReq
		AccessRequestVS accessRequestVS;
		ResponseVS responseVS = accessRequestVSService.saveRequest(messageSMIMEReq, request.getLocale())
		EventVSElection evento = responseVS.eventVS
		if (ResponseVS.SC_OK == responseVS.statusCode) {
			accessRequestVS = responseVS.data
			byte[] csrRequest = params[grailsApplication.config.SistemaVotacion.csrRequestFileName]
			UserVS representative = null
			if(accessRequestVS.userVS.type == UserVS.Type.REPRESENTATIVE) {
				representative = accessRequestVS.userVS
			}
			//log.debug("======== csrRequest: ${new String(csrRequest)}")
			ResponseVS csrValidationResponseVS = csrService.signCertVoteVS(csrRequest, evento,
                    representative, request.getLocale())
			if (ResponseVS.SC_OK == csrValidationResponseVS.statusCode) {
				responseVS.type = TypeVS.ACCESS_REQUEST;
                responseVS.messageBytes = csrValidationResponseVS.data.issuedCert
                params.responseVS = responseVS
				params.receiverPublicKey = csrValidationResponseVS.data.requestPublicKey
                params.responseVS.setContentType(ContentTypeVS.MULTIPART_ENCRYPTED.getName())
				return false
			} else {
				csrValidationResponseVS.type = TypeVS.ACCESS_REQUEST_ERROR;
				params.responseVS = csrValidationResponseVS
				if (accessRequestVS) accessRequestVSService.rechazarSolicitud(accessRequestVS, responseVS.message)
			}
		} else params.responseVS = responseVS
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
            String hashAccessRequestBase64 = new String(
				hexConverter.unmarshal(params.hashHex))
            log.debug "hashAccessRequestBase64: ${hashAccessRequestBase64}"
            AccessRequestVS accessRequestVS = AccessRequestVS.findWhere(hashAccessRequestBase64:
                hashAccessRequestBase64)
            if (accessRequestVS)  params.responseVS = new ResponseVS(statusCode:ResponseVS.SC_OK,
                contentType:ContentTypeVS.TEXT_STREAM, messageBytes:accessRequestVS.content)
            else params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'accessRequestNotFound',args:[params.hashHex]))
        }
        params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'requestWithErrorsHTML',
                args:["${grailsApplication.config.grails.serverURL}/${params.controller}"]))
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
			EventVSElection evento
			EventVSElection.withTransaction {evento =  EventVSElection.get(params.eventId)}
			if(!evento) {
                params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND, message(code: 'eventVSNotFound',
                        args:[params.eventId]))
			} else {
                UserVS userVS
                UserVS.withTransaction { userVS =  UserVS.findByNif(params.nif)}
                if(!userVS) {
                    params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                            message(code: 'userVSNotFoundByNIF', args:[params.nif]))
                } else {
                    AccessRequestVS accessRequestVS
                    AccessRequestVS.withTransaction {
                        accessRequestVS =  AccessRequestVS.findWhere(userVS: userVS, eventVSElection:evento)
                    }
                    if(!accessRequestVS) {
                        params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                                message(code: 'nifWithoutAccessRequest', args:[params.eventId, params.nif]))
                    } else {
                        params.responseVS = new ResponseVS(ResponseVS.SC_OK, contentType: ContentTypeVS.TEXT_STREAM,
                                messageBytes:  accessRequestVS.messageSMIME.content)
                    }
                }
            }
		} else {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'requestWithErrorsHTML',
                    args:["${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"]))
        }
	}

	
}