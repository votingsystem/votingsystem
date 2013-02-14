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
	
	
	def subscripcionService
	def firmaService
	def grailsApplication
	def messageSource

     Respuesta guardar (SMIMEMessageWrapper smimeMessage, Locale locale) {
        log.debug("guardarFirmaEvento - mensaje: ${smimeMessage.getSignedContent()}")
        Firma firma
        def codigoEstado
        def tipoRespuesta
        def mensaje
		MensajeSMIME mensajeSMIMEValidado
        def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
        Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
		if(200 != respuestaUsuario.codigoEstado) return respuestaUsuario
		Usuario usuario = respuestaUsuario.usuario
        EventoReclamacion eventoReclamacion = EventoReclamacion.get(mensajeJSON.id)
        MensajeSMIME mensajeSMIME = new MensajeSMIME(valido:smimeMessage.isValidSignature(),
            contenido:smimeMessage.getBytes(), usuario:usuario, evento:eventoReclamacion)
        if (!eventoReclamacion) {
            codigoEstado = 400
			mensaje = messageSource.getMessage('error.EventoNoEncontrado', 
                    [mensajeJSON.id].toArray() , locale)
            tipoRespuesta = Tipo.EVENTO_NO_ENCONTRADO
            mensajeSMIME.setTipo(Tipo.FIRMA_ERROR_EVENTO_NO_ENCONTRADO)
        } else {
            firma = Firma.findWhere(evento:eventoReclamacion, usuario:usuario)
            if (!firma || Evento.Cardinalidad.MULTIPLES.equals(
                    eventoReclamacion.cardinalidadRepresentaciones)) {
                log.debug("Firma correcta - usuario: ${usuario.nif}")
                firma = new Firma(usuario:usuario, evento:eventoReclamacion, 
					tipo:Tipo.FIRMA_EVENTO_RECLAMACION)
                mensajeSMIME.setTipo(Tipo.FIRMA_EVENTO_RECLAMACION)
                codigoEstado = 200
            } else {
                log.debug("Firma repetida")
                codigoEstado = 400
                tipoRespuesta = Tipo.FIRMA_EVENTO_RECLAMACION_REPETIDA
                mensaje = messageSource.getMessage('eventoReclamacion.firmaRepetida', 
                    [usuario.nif, eventoReclamacion.asunto].toArray() , locale)
                mensajeSMIME.setTipo(Tipo.FIRMA_EVENTO_RECLAMACION_REPETIDA)
            }
        }
		mensajeSMIME.save();
        if(firma && codigoEstado == 200) {
            firma.mensajeSMIME = mensajeSMIME
            firma.save();
            mensajeJSON.campos?.collect { campoItem ->
                CampoDeEvento campo = CampoDeEvento.findWhere(id:campoItem.id?.longValue())
                if (campo) {
                    new ValorCampoDeEvento(valor:campoItem.valor, firma:firma, campoDeEvento:campo).save()
                }
            }
			String asuntoMultiFirmaMimeMessage = messageSource.getMessage(
				'mime.asunto.FirmaReclamacionValidada', null, locale)
			MimeMessage multiFirmaMimeMessage =firmaService.generarMultifirma (
				smimeMessage, asuntoMultiFirmaMimeMessage)
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			multiFirmaMimeMessage.writeTo(baos);
			mensajeSMIMEValidado = new MensajeSMIME(tipo:Tipo.FIRMA_EVENTO_RECLAMACION_VALIDADA,
				smimePadre:mensajeSMIME, evento:eventoReclamacion,
				usuario:usuario, valido:true, contenido:baos.toByteArray())
			mensajeSMIMEValidado.save();
        }
        return new Respuesta(codigoEstado:codigoEstado, tipo:tipoRespuesta, mensaje:mensaje,
            fecha:DateUtils.getTodayDate(), mensajeSMIME:mensajeSMIME, evento:eventoReclamacion,
            usuario:usuario, smimeMessage:smimeMessage, mensajeSMIMEValidado:mensajeSMIMEValidado)
    }
	 
}
