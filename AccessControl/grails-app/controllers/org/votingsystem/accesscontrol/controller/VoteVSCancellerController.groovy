package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*

import javax.xml.bind.annotation.adapters.HexBinaryAdapter

/**
 * @infoController Anulación de votos
 * @descController Servicios relacionados con la anulación de votos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class VoteVSCancellerController {
	
	def voteVSService

	/**
	 * Servicio de consulta de votos anulados.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/voteVSCanceller/$hashHex]
	 * @param [hashHex] El hash en Hexadecimal del certificado del voto anulado.
	 * @return La anulación de voto firmada por el usuario.
	 */
	def index () {
		if(params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertVSBase64 = new String(hexConverter.unmarshal(params.hashHex))
			log.debug "hashCertVSBase64: '${hashCertVSBase64}'"
			VoteVSCanceller voteCanceller = null
			VoteVSCanceller.withTransaction{
				voteCanceller = VoteVSCanceller.findWhere(hashCertVSBase64:hashCertVSBase64)
			}
			if(!voteCanceller) return [responseVS : new ResponseVS(ResponseVS.SC_OK,
                    message(code: 'voteNotFound', args:[params.id]))]
			else {
				Map voteVSCancellerMap = voteVSService.getVoteVSCancellerMap(voteCanceller)
				render voteVSCancellerMap as JSON
			}
		} else return [responseVS : new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                contentType: ContentTypeVS.HTML, message: message(code: 'requestWithErrors',  args:[]))]
	}
	
	/**
	 * Servicio que anula votos.
	 * 
     * @httpMethod [POST]
	 * @serviceURL [/voteVSCanceller]
	 * @requestContentType [application/pkcs7-signature] documento correspondiente al
	 *              <a href="https://github.com/votingsystem/votingsystem/wiki/Anulador-de-voto">anulador de voto</a>
	 * 				firmado y cifrado	 
     * @requestContentType [application/pkcs7-signature]
	 * @return Recibo que consiste en el archivo firmado recibido con la signatureVS añadida del servidor. La respuesta viaja cifrada.
	 */
    def post () {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
		return [responseVS:voteVSService.processCancel(messageSMIME)]
    }

	/**
	 * Servicio que devuelve la información de la anulación de un voto a partir del
	 * identifiacador del voto en la base de datos
	 * @httpMethod [GET]
	 * @serviceURL [/voteVSCanceller/voteVS/${id}]
	 * @param [id] Obligatorio. Identificador del voto en la base de datos
	 * @responseContentType [application/json]
	 * @return documento JSON con la información del voto solicitado.
	 */
	def get() {
		VoteVS voteVS
		Map  cancellerMap
		VoteVS.withTransaction { voteVS = VoteVS.get(params.long('id')) }
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
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
