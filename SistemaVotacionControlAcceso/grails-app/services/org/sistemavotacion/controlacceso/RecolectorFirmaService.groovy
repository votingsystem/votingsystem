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

class RecolectorFirmaService {
	
	def grailsApplication
	def messageSource
	def firmaService
	def subscripcionService
	
	synchronized Respuesta guardar (SMIMEMessageWrapper smimeMessage, Locale locale) {
		log.debug("guardarFirmaEvento - mensaje: ${smimeMessage.getSignedContent()}")
		def firma
		def codigoEstado
		def mensaje
		MensajeSMIME mensajeSMIMEValidado
		def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
		Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
		if(200 != respuestaUsuario.codigoEstado) return respuestaUsuario
		Usuario usuario = respuestaUsuario.usuario
		EventoFirma eventoFirma = EventoFirma.get(mensajeJSON.eventoId)
		def mensajeSMIME = new MensajeSMIME(valido:smimeMessage.isValidSignature(),
			contenido:smimeMessage.getBytes(), usuario:usuario, evento:eventoFirma)
		if (!eventoFirma) {
			codigoEstado = 400
			mensaje = messageSource.getMessage('error.EventoFirmaNoEncontrado',
					[mensajeJSON.eventoId].toArray(), locale) 
			mensajeSMIME.setTipo(Tipo.FIRMA_ERROR_EVENTO_NO_ENCONTRADO)
		} else {
			firma = Firma.findWhere(evento:eventoFirma, usuario:usuario)
			if (!firma) {
				log.debug("Firma correcta - eventoId: ${eventoFirma?.id}")
				firma = new Firma(usuario:usuario, evento:eventoFirma,
					tipo:Tipo.FIRMA_EVENTO_FIRMA)
				mensajeSMIME.setTipo(Tipo.FIRMA_EVENTO_FIRMA)
				codigoEstado = 200
			} else {
				log.debug("Firma repetida")
				codigoEstado = 400
				mensaje = messageSource.getMessage('eventoFirma.firmaRepetida',
					[usuario.nif, eventoFirma.asunto].toArray(), locale)
				mensajeSMIME.setTipo(Tipo.FIRMA_EVENTO_FIRMA_REPETIDA)
			}
		}
		mensajeSMIME.save();
		if(firma && codigoEstado == 200) {
			firma.mensajeSMIME = mensajeSMIME
			firma.save();
			String asuntoMultiFirmaMimeMessage = messageSource.getMessage('mime.asunto.FirmaValidada', null, locale)
			MimeMessage multiFirmaMimeMessage =firmaService.generarMultifirma (
				smimeMessage, asuntoMultiFirmaMimeMessage)
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			multiFirmaMimeMessage.writeTo(baos);
			mensajeSMIMEValidado = new MensajeSMIME(tipo:Tipo.FIRMA_VALIDADA,
				smimePadre:mensajeSMIME, evento:eventoFirma,
				usuario:usuario, valido:true, contenido:baos.toByteArray())
			mensajeSMIMEValidado.save();
		}
		return new Respuesta(codigoEstado:codigoEstado, mensaje:mensaje,
			fecha:DateUtils.getTodayDate(), mensajeSMIME:mensajeSMIME, evento:eventoFirma,
			usuario:usuario, smimeMessage:smimeMessage, mensajeSMIMEValidado:mensajeSMIMEValidado)
	}
	
	
	
}