package org.sistemavotacion.centrocontrol

import java.security.cert.X509Certificate;
import java.util.Collection;

import grails.converters.JSON
import javax.mail.internet.MimeMessage
import org.sistemavotacion.centrocontrol.modelo.*;
import org.sistemavotacion.smime.SMIMEMessageWrapper
import org.sistemavotacion.seguridad.CertUtil

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
	 * @serviceURL [/anuladorVoto]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. Documento firmado en formato 
	 * 			SMIME con el <a href="https://github.com/jgzornoza/SistemaVotacion/wiki/Anulador-de-voto">El anulador de voto</a>
	 * @return Recibo firmado con el certificado del servidor
	 */
	def index() { 
		MensajeSMIME mensajeSMIMEReq = params.mensajeSMIMEReq
		if(!mensajeSMIMEReq) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		Respuesta respuesta = votoService.processCancel(
			mensajeSMIMEReq, request.getLocale())
		EventoVotacion evento = respuesta.evento
		byte[] certChainBytes
		EventoVotacion.withTransaction {
			certChainBytes = evento.cadenaCertificacionControlAcceso
		}		
		Collection<X509Certificate> certColl = CertUtil.fromPEMToX509CertCollection(certChainBytes)
		X509Certificate receiverCert = certColl.iterator().next()
		params.receiverCert = receiverCert
		if(Respuesta.SC_OK == respuesta.codigoEstado || 
			Respuesta.SC_ANULACION_REPETIDA == respuesta.codigoEstado) {
			response.setContentType("${grailsApplication.config.pkcs7SignedContentType};" + 
				"${grailsApplication.config.pkcs7EncryptedContentType}")
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
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
			return false
		}
		AnuladorVoto anulador
		AnuladorVoto.withTransaction {
			anulador = AnuladorVoto.findWhere(voto:voto)
		}
		if(!anulador) {
			response.status = Respuesta.SC_NOT_FOUND
			render message(code: 'voteNotFound', args:[params.id])
			return false
		}
		anuladorvotoMap = votoService.getAnuladorVotoMap(anulador)
		render anuladorvotoMap as JSON
		return false
	}
}
