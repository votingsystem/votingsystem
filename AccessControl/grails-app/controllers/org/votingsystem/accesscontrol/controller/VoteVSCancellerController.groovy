package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.VoteVSCanceller
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.VoteVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
/**
 * @infoController Anulación de votos
 * @descController Servicios relacionados con la anulación de votos.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class VoteVSCancellerController {
	
	def voteVSService

	/**
	 * Servicio de consulta de votos anulados.
	 *
	 * @httpMethod [GET]
	 * @serviceURL [/voteVSCanceller/$hashHex]
	 * @param [hashHex] El hash en Hexadecimal del certificateVS del voteVS anulado.
	 * @return La anulación de voteVS firmada por el userVS.
	 */
	def index () {
		if(params.hashHex) {
			HexBinaryAdapter hexConverter = new HexBinaryAdapter();
			String hashCertVoteBase64 = new String(
				hexConverter.unmarshal(params.hashHex))
			log.debug "hashCertVoteBase64: '${hashCertVoteBase64}'"
			VoteVSCanceller anuladorVoto = null
			VoteVSCanceller.withTransaction{
				anuladorVoto = VoteVSCanceller.findWhere(
					hashCertVoteBase64:hashCertVoteBase64)
			}
			if(!anuladorVoto) {
				response.status = ResponseVS.SC_NOT_FOUND
				render message(code: 'voteNotFound', args:[params.id])
			} else {
				Map anuladorvoteVSMap = voteVSService.getAnuladorVotoMap(anuladorVoto)
				render anuladorvoteVSMap as JSON
			}
		} else {
			response.status = ResponseVS.SC_ERROR_REQUEST
			render message(code: 'requestWithErrorsHTML', args:[
				"${grailsApplication.config.grails.serverURL}/${params.controller}/restDoc"])
		}
		return false
	}
	
	/**
	 * Servicio que anula votos.
	 * 
     * @httpMethod [POST]
	 * @serviceURL [/voteVSCanceller]
	 * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] PDFDocumentVS correspondiente al
	 *              <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voteVS">anulador de voteVS</a>
	 * 				firmado y cifrado	 
     * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] 
	 * @return Recibo que consiste en el archivo firmado recibido con la signatureVS añadida del servidor. La respuesta viaja cifrada.
	 */
    def post () {
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'requestWithoutFile')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		params.receiverCert = messageSMIMEReq.getUserVS().getCertificate()
		ResponseVS responseVS = voteVSService.processCancel(messageSMIMEReq, request.getLocale())
		if (ResponseVS.SC_OK == responseVS.statusCode) {
			response.setContentType(ContentTypeVS.SIGNED_AND_ENCRYPTED)		
        }
		params.responseVS = responseVS
    }

	/**
	 * Servicio que devuelve la información de la anulación de un voteVS a partir del
	 * identifiacador del voteVS en la base de datos
	 * @httpMethod [GET]
	 * @serviceURL [/voteVSCanceller/voteVS/${id}]
	 * @param [id] Obligatorio. Identificador del voteVS en la base de datos
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con la información del voteVS solicitado.
	 */
	def get() {
		VoteVS voteVS
		Map  anuladorvoteVSMap
		VoteVS.withTransaction {
			voteVS = VoteVS.get(params.long('id'))
		}
		if(!voteVS) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
			return false
		}
		VoteVSCanceller anulador
		VoteVSCanceller.withTransaction {
			anulador = VoteVSCanceller.findWhere(voteVS:voteVS)
		}
		if(!anulador) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
		} else {
			anuladorvoteVSMap = voteVSService.getAnuladorVotoMap(anulador)
			render anuladorvoteVSMap as JSON
		}
		return false
	}
}
