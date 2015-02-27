package org.votingsystem.controlcenter.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
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
	 * @requestContentType [application/pkcs7-signature] Obligatorio. Documento firmado en formato
	 * 			SMIME con el <a href="https://github.com/votingsystem/votingsystem/wiki/Anulador-de-voto">El anulador de voto</a>
	 * @return Recibo firmado con el certificado del servidor
	 */
	def index() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.ERROR_REQUEST(message(code:'requestWithoutFile'))]
        else return [responseVS:voteVSService.processCancel(messageSMIME)]
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
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}