package org.sistemavotacion.controlacceso

import java.io.FileOutputStream;
import com.itextpdf.text.pdf.AcroFields
import com.itextpdf.text.pdf.PRAcroForm
import com.itextpdf.text.pdf.PdfReader
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
	def timeStampService
	
	
	public Respuesta saveManifestSignature(Documento pdfDocument, Evento event, Locale locale) {
		log.debug "saveManifestSignature - pdfDocument.id: ${pdfDocument.id} - event: ${event.id}";
		try {
			Usuario usuario = pdfDocument.usuario
			Documento documento = Documento.findWhere(evento:event, usuario:usuario,
				estado:Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
			String mensajeValidacionDocumento
			if(documento) {
				mensajeValidacionDocumento = messageSource.getMessage(
						'pdfSignatureManifestRepeated',	[usuario.nif, event.asunto, DateUtils.
						getStringFromDate(documento.dateCreated)].toArray(), locale)
				log.debug ("saveManifestSignature - ${mensajeValidacionDocumento}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:mensajeValidacionDocumento)
			} else {
				Respuesta timeStampVerification = timeStampService.validateToken(
					pdfDocument.timeStampToken, event, locale)	
				if(Respuesta.SC_OK != timeStampVerification.codigoEstado) {
					log.error("saveManifestSignature - ERROR TIMESTAMP VOTE VALIDATION -> '${timeStampVerification.mensaje}'")
					return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
						mensaje:timeStampVerification.mensaje,
						tipo:Tipo.FIRMA_EVENTO_CON_ERRORES, evento:event)
				}
			
				Documento.withTransaction {
					pdfDocument.evento = event;
					pdfDocument.estado = Documento.Estado.FIRMA_MANIFIESTO_VALIDADA
					pdfDocument.save(flush:true)
				}
				mensajeValidacionDocumento = messageSource.getMessage(
					'pdfSignatureManifestOK',[event.asunto, pdfDocument.usuario.nif].toArray(), locale)
				log.debug ("saveManifestSignature - ${mensajeValidacionDocumento}")
				return new Respuesta(codigoEstado:Respuesta.SC_OK,
					mensaje:mensajeValidacionDocumento)
			}
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:ex.getMessage())
		
		}

	}

	
}