package org.sistemavotacion.controlacceso

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.smime.*;
import org.sistemavotacion.util.*;
import org.sistemavotacion.controlacceso.modelo.*;

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.*
import java.io.File;
import java.io.FileOutputStream;
import javax.mail.internet.MimeMessage;
import java.util.Locale;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class EventoFirmaService {	
		
    static transactional = true

    def etiquetaService
    def firmaService
    def eventoService
    def grailsApplication
	def messageSource

	public Respuesta saveManifest(Documento pdfDocument, Evento event, Locale locale) {
		Documento documento = Documento.findWhere(evento:event, estado:Documento.Estado.MANIFIESTO_VALIDADO)
		String mensajeValidacionDocumento
		if(documento) {
			mensajeValidacionDocumento = messageSource.getMessage('pdfManifestRepeated',
				[evento.asunto, documento.usuario?.nif].toArray(), locale)
			log.debug ("saveManifest - ${mensajeValidacionDocumento}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:mensajeValidacionDocumento)
		} else {
			pdfDocument.estado = Documento.Estado.MANIFIESTO_VALIDADO
			pdfDocument.evento = event
			pdfDocument.save()
			event.estado = Evento.Estado.ACTIVO
			event.usuario = pdfDocument.usuario
			event.save()
			mensajeValidacionDocumento = messageSource.getMessage('pdfManifestOK',
				[event.asunto, pdfDocument.usuario?.nif].toArray(), locale)
			log.debug ("saveManifest - ${mensajeValidacionDocumento}")
			return new Respuesta(codigoEstado:Respuesta.SC_OK,
				mensaje:mensajeValidacionDocumento)
		}

	}

	public Respuesta generarCopiaRespaldo(EventoFirma evento, Locale locale) {
		log.debug("generarCopiaRespaldo - eventoId: ${evento.id}")
		Respuesta respuesta;
		if (evento) {
			def firmasRecibidas = Documento.findAllWhere(evento:evento,
				estado:Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
			def fecha = DateUtils.getDirStringFromDate(DateUtils.getTodayDate())
			String zipNamePrefix = messageSource.getMessage('manifestBackupFileName', null, locale);
			def basedir = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}/" + 
					"${fecha}/${zipNamePrefix}_${evento.id}"
			new File(basedir).mkdirs()
			int i = 0
			def metaInformacionMap = [numeroFirmas:firmasRecibidas.size(),
				URL:"${grailsApplication.config.grails.serverURL}/evento/${evento.id}",
				tipoEvento:Tipo.EVENTO_FIRMA.toString(), asunto:evento.asunto]
			String metaInformacionJSON = metaInformacionMap as JSON
			File metaInformacionFile = new File("${basedir}/meta.inf")
			metaInformacionFile.write(metaInformacionJSON)
			String fileNamePrefix = messageSource.getMessage('manifestSignatureLbl', null, locale);
			firmasRecibidas.each { firma ->
				File pdfFile = new File("${basedir}/${fileNamePrefix}_${i}.pdf")
				FileOutputStream fos = new FileOutputStream(pdfFile);
				fos.write(firma.pdf);
				fos.close();
				i++;
			}
			def ant = new AntBuilder()
			ant.zip(destfile: "${basedir}.zip", basedir: basedir)
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK, 
				cantidad:firmasRecibidas.size(), file:new File("${basedir}.zip"))
		} else respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:messageSource.getMessage(
				'eventNotFound', [evento.id].toArray(), locale))
		return respuesta
	}

}