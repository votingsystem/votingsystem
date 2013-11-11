package org.votingsystem.accesscontrol.service

import java.io.FileOutputStream;

import com.itextpdf.text.pdf.AcroFields
import com.itextpdf.text.pdf.PRAcroForm
import com.itextpdf.text.pdf.PdfReader

import grails.converters.JSON

import org.votingsystem.accesscontrol.model.Documento;
import org.votingsystem.accesscontrol.model.Evento;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.*;
import org.votingsystem.signature.util.*;
import org.votingsystem.util.*;
import org.votingsystem.model.TypeVS
import javax.mail.internet.MimeMessage;

import org.votingsystem.accesscontrol.model.*;

import java.util.Locale;

class RecolectorFirmaService {
	
	def grailsApplication
	def messageSource
	def firmaService
	def subscripcionService
	def timeStampService
	
	
	public ResponseVS saveManifestSignature(Documento pdfDocument, Evento event, Locale locale) {
		log.debug "saveManifestSignature - pdfDocument.id: ${pdfDocument.id} - event: ${event.id}";
		try {
			Usuario usuario = pdfDocument.usuario
			Documento documento = Documento.findWhere(evento:event, usuario:usuario,
				estado:Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
			String messageValidacionDocumento
			if(documento) {
				messageValidacionDocumento = messageSource.getMessage(
						'pdfSignatureManifestRepeated',	[usuario.nif, event.asunto, DateUtils.
						getStringFromDate(documento.dateCreated)].toArray(), locale)
				log.debug ("saveManifestSignature - ${messageValidacionDocumento}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
					message:messageValidacionDocumento)
			} else {
				ResponseVS timeStampVerification = timeStampService.validateToken(
					pdfDocument.timeStampToken, event, locale)	
				if(ResponseVS.SC_OK != timeStampVerification.statusCode) {
					log.error("saveManifestSignature - ERROR TIMESTAMP VOTE VALIDATION -> '${timeStampVerification.message}'")
					return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
						message:timeStampVerification.message,
						type:TypeVS.FIRMA_EVENTO_CON_ERRORES, eventVS:event)
				}
			
				Documento.withTransaction {
					pdfDocument.evento = event;
					pdfDocument.estado = Documento.Estado.FIRMA_MANIFIESTO_VALIDADA
					pdfDocument.save(flush:true)
				}
				messageValidacionDocumento = messageSource.getMessage(
					'pdfSignatureManifestOK',[event.asunto, pdfDocument.usuario.nif].toArray(), locale)
				log.debug ("saveManifestSignature - ${messageValidacionDocumento}")
				return new ResponseVS(statusCode:ResponseVS.SC_OK,
					message:messageValidacionDocumento)
			}
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:ex.getMessage())
		
		}

	}

	
}