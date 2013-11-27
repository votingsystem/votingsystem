package org.votingsystem.controlcenter.controller

import org.votingsystem.model.EventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.VoteVS
import org.votingsystem.model.VoteVSCanceller

import java.security.cert.X509Certificate

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.CertUtil

/**
 * @infoController Anulación de Votos
 * @descController Servicios que permiten anular los votesVS de una votación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class VoteVSCancellerController {
	
	def voteVSService

	/**
	 * @httpMethod [POST]
	 * @serviceURL [/voteVSCanceller?url=${urlEventVS}]
	 * @param [urlEventVS] Obligatorio. URL en el control de acceso del evento al que está asociada la anulación
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. Documento firmado en formato 
	 * 			SMIME con el <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voteVS">El anulador de voteVS</a>
	 * @return Recibo firmado con el certificateVS del servidor
	 */
	def index() { 
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'requestWithoutFile')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		ResponseVS responseVS = voteVSService.processCancel(
			messageSMIMEReq, request.getLocale())
		EventVS eventVS = responseVS.eventVS
		byte[] certChainBytes
        EventVSElection.withTransaction {
			certChainBytes = eventVS.certChainAccessControl
		}		
		Collection<X509Certificate> certColl = CertUtil.fromPEMToX509CertCollection(certChainBytes)
		X509Certificate receiverCert = certColl.iterator().next()
		params.receiverCert = receiverCert
		if(ResponseVS.SC_OK == responseVS.statusCode || ResponseVS.SC_CANCELLATION_REPEATED == responseVS.statusCode) {
			response.setContentType(ContentTypeVS.SIGNED_AND_ENCRYPTED)
		}
		params.responseVS =  responseVS
	}
	

	/**
	 * Servicio que devuelve la información de la anulación de un voteVS a partir del
	 * identifiacador del voteVS en la base de datos
	 * @httpMethod [GET]
	 * @serviceURL [/voteVSCanceller/voteVS/${id}]
	 * @param [id] Obligatorio. Identificador del voteVS en la base de datos
	 * @responseContentType [application/json]
	 * @return Documento JSON con la información del voteVS solicitado.
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
			return false
		}
		anuladorvoteVSMap = voteVSService.getAnuladorVotoMap(anulador)
		render anuladorvoteVSMap as JSON
		return false
	}
}
