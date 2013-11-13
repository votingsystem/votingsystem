package org.votingsystem.accesscontrol.service

import java.io.FileOutputStream;

import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.votingsystem.accesscontrol.model.EventoReclamacion;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.*;
import org.votingsystem.signature.util.*;
import org.votingsystem.util.*;

import javax.mail.internet.MimeMessage;

import org.votingsystem.accesscontrol.model.*;

import java.util.Locale;
import java.util.Map;

class ReclamacionService {
	
	def firmaService
	def grailsApplication
	def messageSource
	def timeStampService

     ResponseVS guardar (MessageSMIME messageSMIMEReq, Locale locale) {
        log.debug("guardar -")
        def msg
		SMIMEMessageWrapper smimeMessage = messageSMIMEReq.getSmimeMessage()
        def messageJSON = JSON.parse(smimeMessage.getSignedContent())
		Usuario usuario = messageSMIMEReq.getUsuario()
		ResponseVS respuesta = checkClaimJSONData(messageJSON, locale)
		if(ResponseVS.SC_OK != respuesta.statusCode) return respuesta
		//log.debug("messageJSON: ${smimeMessage.getSignedContent()}")
        EventoReclamacion eventoReclamacion = EventoReclamacion.get(messageJSON.id)
        if (!eventoReclamacion) {
			msg = messageSource.getMessage('eventNotFound', 
                    [messageJSON.id].toArray() , locale)
			log.debug("guardar - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg,
				type:TypeVS.CLAIM_EVENT_SIGNATURE_ERROR)
        } else {
			ResponseVS timeStampVerification = timeStampService.validateToken(
				usuario.getTimeStampToken(), eventoReclamacion, locale)
			if(ResponseVS.SC_OK != timeStampVerification.statusCode) {
				log.error("saveManifestSignature - ERROR TIMESTAMP VALIDATION -> '${timeStampVerification.message}'")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:timeStampVerification.message,
					type:TypeVS.EVENT_SIGN_WITH_ERRORS, eventVS:eventoReclamacion)
			}
		
            Firma firma = Firma.findWhere(evento:eventoReclamacion, usuario:usuario)
            if (!firma || Evento.Cardinalidad.MULTIPLES.equals(
                    eventoReclamacion.cardinalidadRepresentaciones)) {
                log.debug("guardar - claim signature OK - signer: ${usuario.nif}")
                firma = new Firma(usuario:usuario, evento:eventoReclamacion, 
					type:TypeVS.CLAIM_EVENT_SIGN, messageSMIME:messageSMIMEReq)
				firma.save();
				messageJSON.campos?.each { campoItem ->
					CampoDeEvento campo = CampoDeEvento.findWhere(id:campoItem.id?.longValue())
					if (campo) {
						new ValorCampoDeEvento(valor:campoItem.valor, firma:firma, campoDeEvento:campo).save()
					}
				}
				
				String fromUser = grailsApplication.config.VotingSystem.serverName
				String toUser = usuario.getNif()
				String subject = messageSource.getMessage(
					'mime.asunto.FirmaReclamacionValidada', null, locale)

				SMIMEMessageWrapper smimeMessageResp = firmaService.
					getMultiSignedMimeMessage (fromUser, toUser, smimeMessage, subject)
				MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT,
					smimePadre:messageSMIMEReq, evento:eventoReclamacion, 
					valido:true, contenido:smimeMessageResp.getBytes())
				MessageSMIME.withTransaction {
					messageSMIMEResp.save()
				}
				messageSMIMEResp.smimeMessage = smimeMessageResp
				return new ResponseVS(statusCode:ResponseVS.SC_OK,
					data:messageSMIMEResp, eventVS:eventoReclamacion,
					smimeMessage:smimeMessage, type:TypeVS.CLAIM_EVENT_SIGN)
            } else {
				msg = messageSource.getMessage('eventoReclamacion.firmaRepetida',
					[usuario.nif, eventoReclamacion.asunto].toArray() , locale)
                log.error("guardar - ${msg} - signer: ${usuario.nif}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					eventVS:eventoReclamacion, message:msg,
					type:TypeVS.CLAIM_EVENT_SIGNATURE_ERROR)
            }
        }
    }
	 
	 private ResponseVS checkClaimJSONData(JSONObject claimDataJSON, Locale locale) {
		 int status = ResponseVS.SC_ERROR_REQUEST
		 org.bouncycastle.tsp.TimeStampToken tms;
		 String msg
		 try {
			 TypeVS operationType = TypeVS.valueOf(claimDataJSON.operation)
			 if (claimDataJSON.id && claimDataJSON.URL &&
				 (TypeVS.SMIME_CLAIM_SIGNATURE == operationType)) {
				 status = ResponseVS.SC_OK
			 } else msg = messageSource.getMessage('claimSignatureWithErrorsMsg', null, locale)
		 } catch(Exception ex) {
			 log.error(ex.getMessage(), ex)
			 msg = messageSource.getMessage('claimSignatureWithErrorsMsg', null, locale)
		 }
		 if(ResponseVS.SC_OK != status) log.error(
			 "checkClaimJSONData - msg: ${msg} - data:${claimDataJSON.toString()}")
		 return new ResponseVS(statusCode:status, message:msg, 
			 type:TypeVS.SMIME_CLAIM_SIGNATURE)
	 }
	 
	 public Map getStatisticsMap (EventoReclamacion event, Locale locale) {
		 log.debug("getStatisticsMap - eventId: ${event?.id}")
		 if(!event) return null
		 def statisticsMap = new HashMap()
		 statisticsMap.camposReclamacion = []
		 statisticsMap.id = event.id
		 statisticsMap.asunto = event.asunto
		 statisticsMap.numeroFirmas = Firma.countByEvento(event)
		 statisticsMap.estado =  event.estado.toString()
		 statisticsMap.fechaInicio = event.getFechaInicio()
		 statisticsMap.fechaFin = event.getFechaFin()
		 statisticsMap.solicitudPublicacionURL = "${grailsApplication?.config.grails.serverURL}" +
			 "/eventoReclamacion/${event.id}/firmado"
		 statisticsMap.solicitudPublicacionValidadaURL = "${grailsApplication?.config.grails.serverURL}" +
			 "/eventoReclamacion/${event.id}/validado"
		 statisticsMap.informacionFirmasReclamacionURL = "${grailsApplication?.config.grails.serverURL}" +
			 "/eventoReclamacion/${event.id}/informacionFirmas"
		 statisticsMap.URL = "${grailsApplication.config.grails.serverURL}/evento/${event.id}"
		 event.camposEvento.each { campo ->
			 statisticsMap.camposReclamacion.add(campo.contenido)
		 }
		 return statisticsMap
	 }
	 
}
