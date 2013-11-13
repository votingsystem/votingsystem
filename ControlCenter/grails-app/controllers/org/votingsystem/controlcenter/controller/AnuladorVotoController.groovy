package org.votingsystem.controlcenter.controller

import java.security.cert.X509Certificate;
import java.util.Collection;
import org.votingsystem.model.ContextVS;
import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS;
import javax.mail.internet.MimeMessage

import org.votingsystem.controlcenter.model.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.signature.util.CertUtil

/**
 * @infoController Anulación de Votos
 * @descController Servicios que permiten anular los votos de una votación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class AnuladorVotoController {
	
	def votoService

	/**
	 * @httpMethod [POST]
	 * @serviceURL [/anuladorVoto?url=${urlEvento}]
	 * @param [urlEvento] Obligatorio. URL en el control de acceso del evento al que está asociada la anulación
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. Documento firmado en formato 
	 * 			SMIME con el <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">El anulador de voto</a>
	 * @return Recibo firmado con el certificado del servidor
	 */
	def index() { 
		MessageSMIME messageSMIMEReq = params.messageSMIMEReq
		if(!messageSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		ResponseVS respuesta = votoService.processCancel(
			messageSMIMEReq, request.getLocale())
		EventoVotacion evento = respuesta.eventVS
		byte[] certChainBytes
		EventoVotacion.withTransaction {
			certChainBytes = evento.cadenaCertificacionControlAcceso
		}		
		Collection<X509Certificate> certColl = CertUtil.fromPEMToX509CertCollection(certChainBytes)
		X509Certificate receiverCert = certColl.iterator().next()
		params.receiverCert = receiverCert
		if(ResponseVS.SC_OK == respuesta.statusCode || 
			ResponseVS.SC_CANCELLATION_REPEATED == respuesta.statusCode) {
			response.setContentType(ContentTypeVS.SIGNED_AND_ENCRYPTED)
		}
		params.respuesta =  respuesta
	}
	

	/**
	 * Servicio que devuelve la información de la anulación de un voto a partir del 
	 * identifiacador del voto en la base de datos
	 * @httpMethod [GET]
	 * @serviceURL [/anuladorVoto/voto/${id}]
	 * @param [id] Obligatorio. Identificador del voto en la base de datos
	 * @responseContentType [application/json]
	 * @return Documento JSON con la información del voto solicitado.
	 */
	def get() {
		Voto voto
		Map  anuladorvotoMap
		Voto.withTransaction {
			voto = Voto.get(params.long('id'))
		}
		if(!voto) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
			return false
		}
		AnuladorVoto anulador
		AnuladorVoto.withTransaction {
			anulador = AnuladorVoto.findWhere(voto:voto)
		}
		if(!anulador) {
			response.status = ResponseVS.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
			return false
		}
		anuladorvotoMap = votoService.getAnuladorVotoMap(anulador)
		render anuladorvotoMap as JSON
		return false
	}
}
