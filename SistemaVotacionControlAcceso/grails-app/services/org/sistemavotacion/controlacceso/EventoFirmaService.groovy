package org.sistemavotacion.controlacceso

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.smime.*;
import org.sistemavotacion.util.*;
import org.sistemavotacion.controlacceso.modelo.*;
import java.security.cert.X509Certificate;
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.*
import java.io.File;
import java.io.FileOutputStream;
import javax.mail.internet.MimeMessage;
import java.util.Locale;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class EventoFirmaService {	
		
    static transactional = true

    def etiquetaService
    def firmaService
    def eventoService
    def grailsApplication
	def messageSource
	def filesService

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

	public synchronized Respuesta generarCopiaRespaldo(EventoFirma event, Locale locale) {
		log.debug("generarCopiaRespaldo - eventoId: ${event.id}")
		Respuesta respuesta;
		if(!event) {
			return respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:messageSource.getMessage(
				'eventNotFound', [event.id].toArray(), locale))
		}
		def firmasRecibidas = Documento.findAllWhere(evento:event,
			estado:Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
		
		Map<String, File> mapFiles = filesService.getBackupFiles(
			event, Tipo.EVENTO_FIRMA, locale)
		File metaInfFile = mapFiles.metaInfFile
		File filesDir = mapFiles.filesDir
		File zipResult   = mapFiles.zipResult
		
		Set<X509Certificate> systemTrustedCerts = firmaService.getTrustedCerts()
		byte[] systemTrustedCertsPEMBytes = CertUtil.fromX509CertCollectionToPEM(systemTrustedCerts)
		File systemTrustedCertsFile = new File("${filesDir.absolutePath}/systemTrustedCerts.pem")
		systemTrustedCertsFile.setBytes(systemTrustedCertsPEMBytes)
		
		def backupMetaInfMap = [numSignatures:firmasRecibidas.size()]
		def eventMetaInfMap = eventoService.updateEventMetaInf(
			event, Tipo.BACKUP, backupMetaInfMap)
		metaInfFile.write((eventMetaInfMap as JSON).toString())
		
		String fileNamePrefix = messageSource.getMessage(
			'manifestSignatureLbl', null, locale);
		int i = 1
		firmasRecibidas.each { firma ->
			File pdfFile = new File("${filesDir.absolutePath}/${fileNamePrefix}_${String.format('%08d', i++)}.pdf")
			pdfFile.setBytes(firma.pdf)
		}
		
		def ant = new AntBuilder()
		ant.zip(destfile: zipResult, basedir: "${filesDir.absolutePath}") {
			fileset(dir:"${filesDir}/..", includes: "meta.inf")
		}
		
		return new Respuesta(codigoEstado:Respuesta.SC_OK,
			file:zipResult, tipo:Tipo.EVENTO_FIRMA) 
	}

}