package org.sistemavotacion.controlacceso

import java.io.FileOutputStream;
import grails.converters.JSON
import org.sistemavotacion.controlacceso.modelo.Respuesta;
import org.sistemavotacion.smime.*;
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.util.*;
import javax.mail.internet.MimeMessage;
import org.sistemavotacion.controlacceso.modelo.*;
import java.util.Locale;

class ReclamacionService {
	
	def firmaService
	def grailsApplication
	def messageSource

     Respuesta guardar (MensajeSMIME mensajeSMIMEReq, Locale locale) {
        log.debug("guardar -")
        def msg
		SMIMEMessageWrapper smimeMessage = mensajeSMIMEReq.getSmimeMessage()
        def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
		Usuario usuario = mensajeSMIMEReq.getUsuario()
        EventoReclamacion eventoReclamacion = EventoReclamacion.get(mensajeJSON.id)
        if (!eventoReclamacion) {
			msg = messageSource.getMessage('eventNotFound', 
                    [mensajeJSON.id].toArray() , locale)
			log.debug("guardar - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:msg,
				tipo:Tipo.FIRMA_EVENTO_RECLAMACION_ERROR)
        } else {
            Firma firma = Firma.findWhere(evento:eventoReclamacion, usuario:usuario)
            if (!firma || Evento.Cardinalidad.MULTIPLES.equals(
                    eventoReclamacion.cardinalidadRepresentaciones)) {
                log.debug("guardar - claim signature OK - signer: ${usuario.nif}")
                firma = new Firma(usuario:usuario, evento:eventoReclamacion, 
					tipo:Tipo.FIRMA_EVENTO_RECLAMACION, mensajeSMIME:mensajeSMIMEReq)
				firma.save();
				mensajeJSON.campos?.collect { campoItem ->
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
	 
}
