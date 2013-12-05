package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.VoteVS
import org.votingsystem.model.ResponseVS

import java.security.cert.X509Certificate
/**
 * @infoController Servicio de Votos
 * @descController Servicio que procesa los votos recibidos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class VoteVSController {

	def voteVSService
	def grailsApplication
        
    def index() {}
	
	/**
	 * Servicio que recoge los votos enviados por los Centrol de Control
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/voteVS]
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio. El archivo voteVS firmado por el
	 *        <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/CertificateVS-de-voteVS">certificateVS de VoteVS.</a>
	 *        y el certificateVS del Centro de Control.
	 * @responseContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] 
	 * @return  <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Recibo-de-VoteVS">El recibo del voteVS.</a>
	 */
    def save() { 
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'requestWithoutFile')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg 
			return false
		}
	    ResponseVS responseVS = voteVSService.validateVote(messageSMIMEReq, request.getLocale())
		if (ResponseVS.SC_OK == responseVS.statusCode) {
			params.receiverCert = responseVS.data.certificate
			responseVS.data = responseVS.data.messageSMIME
			response.setContentType(ContentTypeVS.SIGNED)
		}
		params.responseVS = responseVS
	}
	
	/**
	 * Servicio que devuelve la información de un voteVS a partir del identificador
	 * del mismo en la base de datos
	 * @httpMethod [GET]
	 * @serviceURL [/voteVS/${id}]
	 * @param [id] Obligatorio. Identificador del voteVS en la base de datos
	 * @responseContentType [application/json]
	 * @return documento JSON con la información del voteVS solicitado.
	 */
	def get() {
		VoteVS voteVS
		Map  voteVSMap
		VoteVS.withTransaction {
			voteVS = VoteVS.get(params.long('id'))
			if(voteVS) voteVSMap = voteVSService.getVotoMap(voteVS)
		}
		if(!voteVS) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
			return false
		}

		render voteVSMap as JSON
		return false
	}
	
}
