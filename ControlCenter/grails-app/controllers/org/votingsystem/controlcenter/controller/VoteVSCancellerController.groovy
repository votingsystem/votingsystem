package org.votingsystem.controlcenter.controller

import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
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
	 * 			SMIME con el <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">El anulador de voto</a>
	 * @return Recibo firmado con el certificado del servidor
	 */
	def index() { 
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
        if(!messageSMIMEReq) {
            params.responseVS = new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))
            return
        }
        params.responseVS = voteVSService.processCancel( messageSMIMEReq, request.getLocale())
		byte[] certChainBytes
        EventVSElection.withTransaction { certChainBytes = params.responseVS.eventVS.certChainAccessControl }
		Collection<X509Certificate> certColl = CertUtil.fromPEMToX509CertCollection(certChainBytes)
		X509Certificate receiverCert = certColl.iterator().next()
		params.receiverCert = receiverCert
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
		VoteVS.withTransaction {
			voteVS = VoteVS.get(params.long('id'))
		}
        if(!voteVS) params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'voteNotFound', args:[params.id]))
        else {
            VoteVSCanceller canceller
            VoteVSCanceller.withTransaction { canceller = VoteVSCanceller.findWhere(voteVS:voteVS) }
            if(!canceller) params.responseVS = new ResponseVS(ResponseVS.SC_NOT_FOUND,
                    message(code: 'voteNotFound', args:[params.id]))
            else {
                cancellerMap = voteVSService.getAnuladorVotoMap(canceller)
                render cancellerMap as JSON
            }
        }
	}
}
