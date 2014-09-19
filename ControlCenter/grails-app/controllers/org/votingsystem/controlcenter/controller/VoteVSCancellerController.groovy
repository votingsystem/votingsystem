package org.votingsystem.controlcenter.controller

import grails.converters.JSON
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.VoteVS
import org.votingsystem.model.VoteVSCanceller

/**
 * @infoController Anulación de Votos
 * @descController Servicios que permiten anular los votesVS de una votación
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class VoteVSCancellerController {
	
	def voteVSService

	/**
	 * @httpMethod [POST]
	 * @serviceURL [/voteVSCanceller?url=${urlEventVS}]
	 * @param [urlEventVS] Obligatorio. URL en el control de acceso del evento al que está asociada la anulación
	 * @requestContentType [application/x-pkcs7-signature] Obligatorio. Documento firmado en formato
	 * 			SMIME con el <a href="https://github.com/votingsystem/votingsystem/wiki/Anulador-de-voto">El anulador de voto</a>
	 * @return Recibo firmado con el certificado del servidor
	 */
	def index() { 
		MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = voteVSService.processCancel( messageSMIMEReq, request.getLocale())
        if(ResponseVS.SC_OK == responseVS.statusCode) return responseVS.data
        else return [responseVS:responseVS]
    }
	

	/**
	 * Servicio que devuelve la información de la anulación de un voto a partir del
	 * identifiacador del voto en la base de datos
	 * @httpMethod [GET]
	 * @serviceURL [/voteVSCanceller/voteVS/${id}]
	 * @param [id] Obligatorio. Identificador del voto en la base de datos
	 * @responseContentType [application/json]
	 * @return Documento JSON con la información del voto solicitado.
	 */
	def get() {
		VoteVS voteVS
		Map  cancellerMap
		VoteVS.withTransaction {voteVS = VoteVS.get(params.long('id'))}
        if(!voteVS) return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'voteNotFound', args:[params.id]))]
        else {
            VoteVSCanceller canceller
            VoteVSCanceller.withTransaction { canceller = VoteVSCanceller.findWhere(voteVS:voteVS) }
            if(!canceller) return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'voteNotFound', args:[params.id]))]
            else {
                cancellerMap = voteVSService.getVoteVSCancellerMap(canceller)
                render cancellerMap as JSON
            }
        }
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