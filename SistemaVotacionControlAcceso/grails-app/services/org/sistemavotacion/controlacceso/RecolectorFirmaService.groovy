package org.sistemavotacion.controlacceso

import java.io.FileOutputStream;
import grails.converters.JSON

import org.sistemavotacion.controlacceso.modelo.Documento;
import org.sistemavotacion.controlacceso.modelo.Evento;
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
	
	
	public Respuesta saveManifestSignature(Documento pdfDocument, Evento event, Locale locale) {
		log.debug "saveManifestSignature - pdfDocument.id: ${pdfDocument.id} - event: ${event.id}";
		Documento documento = Documento.findWhere(evento:event, usuario:pdfDocument.usuario,
			estado:Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
		String mensajeValidacionDocumento
		if(documento) {
			mensajeValidacionDocumento = messageSource.getMessage(
					'pdfSignatureManifestRepeated',	[usuario.nif, evento.asunto, DateUtils.
					getStringFromDate(documento.dateCreated)].toArray(), locale)
			log.debug ("saveManifestSignature - ${mensajeValidacionDocumento}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:mensajeValidacionDocumento)
		} else {
			pdfDocument.evento = event;
			pdfDocument.estado = Documento.Estado.FIRMA_MANIFIESTO_VALIDADA
			pdfDocument.save()
			mensajeValidacionDocumento = messageSource.getMessage(
				'pdfSignatureManifestOK',[event.asunto, pdfDocument.usuario.nif].toArray(), locale)
			log.debug ("saveManifestSignature - ${mensajeValidacionDocumento}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:mensajeValidacionDocumento)
		}
	}

	
}