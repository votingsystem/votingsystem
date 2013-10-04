package org.sistemavotacion.controlacceso

import java.io.FileOutputStream;
import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.JSONObject;
import org.sistemavotacion.controlacceso.modelo.EventoReclamacion;
import org.sistemavotacion.controlacceso.modelo.Respuesta;
import org.sistemavotacion.smime.*;
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.util.*;
import javax.mail.internet.MimeMessage;
import org.sistemavotacion.controlacceso.modelo.*;
import java.util.Locale;
import java.util.Map;

class ReclamacionService {
	
	def firmaService
	def grailsApplication
	def messageSource
	def timeStampService

     Respuesta guardar (MensajeSMIME mensajeSMIMEReq, Locale locale) {
        log.debug("guardar -")
        def msg
		SMIMEMessageWrapper smimeMessage = mensajeSMIMEReq.getSmimeMessage()
        def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
		Usuario usuario = mensajeSMIMEReq.getUsuario()
		Respuesta respuesta = checkClaimJSONData(mensajeJSON, locale)
		if(Respuesta.SC_OK != respuesta.codigoEstado) return respuesta
		//log.debug("mensajeJSON: ${smimeMessage.getSignedContent()}")
        EventoReclamacion eventoReclamacion = EventoReclamacion.get(mensajeJSON.id)
        if (!eventoReclamacion) {
			msg = messageSource.getMessage('eventNotFound', 
                    [mensajeJSON.id].toArray() , locale)
			log.debug("guardar - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg,
				tipo:Tipo.FIRMA_EVENTO_RECLAMACION_ERROR)
        } else {
			Respuesta timeStampVerification = timeStampService.validateToken(
				usuario.getTimeStampToken(), eventoReclamacion, locale)
			if(Respuesta.SC_OK != timeStampVerification.codigoEstado) {
				log.error("saveManifestSignature - ERROR TIMESTAMP VALIDATION -> '${timeStampVerification.mensaje}'")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:timeStampVerification.mensaje,
					tipo:Tipo.FIRMA_EVENTO_CON_ERRORES, evento:eventoReclamacion)
			}
		
            Firma firma = Firma.findWhere(evento:eventoReclamacion, usuario:usuario)
            if (!firma || Evento.Cardinalidad.MULTIPLES.equals(
                    eventoReclamacion.cardinalidadRepresentaciones)) {
                log.debug("guardar - claim signature OK - signer: ${usuario.nif}")
                firma = new Firma(usuario:usuario, evento:eventoReclamacion, 
					tipo:Tipo.FIRMA_EVENTO_RECLAMACION, mensajeSMIME:mensajeSMIMEReq)
				firma.save();
				mensajeJSON.campos?.each { campoItem ->
					CampoDeEvento campo = CampoDeEvento.findWhere(id:campoItem.id?.longValue())
					if (campo) {
						new ValorCampoDeEvento(valor:campoItem.valor, firma:firma, campoDeEvento:campo).save()
					}
				}
				
				String fromUser = grailsApplication.config.SistemaVotacion.serverName
				String toUser = usuario.getNif()
				String subject = messageSource.getMessage(
					'mime.asunto.FirmaReclamacionValidada', null, locale)

				SMIMEMessageWrapper smimeMessageResp = firmaService.
					getMultiSignedMimeMessage (fromUser, toUser, smimeMessage, subject)
				MensajeSMIME mensajeSMIMEResp = new MensajeSMIME(tipo:Tipo.RECIBO,
					smimePadre:mensajeSMIMEReq, evento:eventoReclamacion, 
					valido:true, contenido:smimeMessageResp.getBytes())
				MensajeSMIME.withTransaction {
					mensajeSMIMEResp.save()
				}
				return new Respuesta(codigoEstado:Respuesta.SC_OK,
					mensajeSMIME:mensajeSMIMEResp, evento:eventoReclamacion,
					smimeMessage:smimeMessage, tipo:Tipo.FIRMA_EVENTO_RECLAMACION)
            } else {
				msg = messageSource.getMessage('eventoReclamacion.firmaRepetida',
					[usuario.nif, eventoReclamacion.asunto].toArray() , locale)
                log.error("guardar - ${msg} - signer: ${usuario.nif}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					evento:eventoReclamacion, mensaje:msg,
					tipo:Tipo.FIRMA_EVENTO_RECLAMACION_ERROR)
            }
        }
    }
	 
	 private Respuesta checkClaimJSONData(JSONObject claimDataJSON, Locale locale) {
		 int status = Respuesta.SC_ERROR_PETICION
		 org.bouncycastle.tsp.TimeStampToken tms;
		 String msg
		 try {
			 Tipo operationType = Tipo.valueOf(claimDataJSON.operation)
			 if (claimDataJSON.id && claimDataJSON.URL &&
				 (Tipo.FIRMA_RECLAMACION_SMIME == operationType)) {
				 status = Respuesta.SC_OK
			 } else msg = messageSource.getMessage('claimSignatureWithErrorsMsg', null, locale)
		 } catch(Exception ex) {
			 log.error(ex.getMessage(), ex)
			 msg = messageSource.getMessage('claimSignatureWithErrorsMsg', null, locale)
		 }
		 if(Respuesta.SC_OK != status) log.error(
			 "checkClaimJSONData - msg: ${msg} - data:${claimDataJSON.toString()}")
		 return new Respuesta(codigoEstado:status, mensaje:msg, 
			 tipo:Tipo.FIRMA_RECLAMACION_SMIME)
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
