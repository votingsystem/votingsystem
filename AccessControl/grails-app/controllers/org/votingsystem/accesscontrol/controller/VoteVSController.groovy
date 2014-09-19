package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
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
		MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
             return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = voteVSService.validateVote(messageSMIMEReq, request.getLocale())
        if(ResponseVS.SC_OK == responseVS.getStatusCode()) return responseVS.data
		else return [responseVS:responseVS]
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
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }

}
