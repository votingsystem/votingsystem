package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.VoteVS

/**
 * @infoController Servicio de Votos
 * @descController Servicio que procesa los votos recibidos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class VoteVSController {

	def voteVSService
	def grailsApplication
        
    def index() { }
	
	/**
	 * Servicio que recoge los votos enviados por los Centrol de Control
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/voteVS]
	 * @requestContentType [application/x-pkcs7-signature] Obligatorio. El archivo voto firmado por el
	 *        <a href="https://github.com/votingsystem/votingsystem/wiki/Certificado-de-voto">certificado de VoteVS.</a>
	 *        y el certificado del Centro de Control.
	 * @responseContentType [application/x-pkcs7-signature]
	 * @return  <a href="https://github.com/votingsystem/votingsystem/wiki/Recibo-de-VoteVS">El recibo del voto.</a>
	 */
    def save() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
		else return [responseVS:voteVSService.validateVote(messageSMIME)]
	}
	
	/**
	 * Servicio que devuelve la información de un voto a partir del identificador
	 * del mismo en la base de datos
	 * @httpMethod [GET]
	 * @serviceURL [/voteVS/${id}]
	 * @param [id] Obligatorio. Identificador del voto en la base de datos
	 * @responseContentType [application/json]
	 * @return documento JSON con la información del voto solicitado.
	 */
	def get() {
		VoteVS voteVS
		Map  voteVSMap
		VoteVS.withTransaction {
			voteVS = VoteVS.get(params.long('id'))
			if(voteVS) voteVSMap = voteVSService.getVoteVSMap(voteVS)
		}
		if(!voteVS) return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'voteNotFound', args:[params.id]))]
		else render voteVSMap as JSON
	}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
