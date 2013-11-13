package org.votingsystem.accesscontrol.service

import java.text.DecimalFormat
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.*;
import org.votingsystem.signature.smime.*;
import org.votingsystem.util.*;
import org.votingsystem.accesscontrol.model.*;

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
	def sessionFactory
	def timeStampService

	public ResponseVS saveManifest(Documento pdfDocument, Evento event, Locale locale) {
		Documento documento = Documento.findWhere(evento:event, estado:Documento.Estado.MANIFIESTO_VALIDADO)
		String messageValidacionDocumento
		if(documento) {
			messageValidacionDocumento = messageSource.getMessage('pdfManifestRepeated',
				[evento.asunto, documento.usuario?.nif].toArray(), locale)
			log.debug ("saveManifest - ${messageValidacionDocumento}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:messageValidacionDocumento)
		} else {
			pdfDocument.estado = Documento.Estado.MANIFIESTO_VALIDADO
			pdfDocument.evento = event
			pdfDocument.save()
			event.estado = Evento.Estado.ACTIVO
			event.usuario = pdfDocument.usuario
			event.save()
			messageValidacionDocumento = messageSource.getMessage('pdfManifestOK',
				[event.asunto, pdfDocument.usuario?.nif].toArray(), locale)
			log.debug ("saveManifest - ${messageValidacionDocumento}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK,
				message:messageValidacionDocumento)
		}

	}

	public synchronized ResponseVS generarCopiaRespaldo(EventoFirma event, Locale locale) {
		log.debug("generarCopiaRespaldo - eventoId: ${event.id}")
		ResponseVS respuesta;
		if(!event) {
			return respuesta = new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:messageSource.getMessage(
				'eventNotFound', [event.id].toArray(), locale))
		}

		int numSignatures = Documento.countByEventoAndEstado(event,
			Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
		Map<String, File> mapFiles = filesService.getBackupFiles(
			event, TypeVS.SIGN_EVENT, locale)
		File metaInfFile = mapFiles.metaInfFile
		File filesDir = mapFiles.filesDir
		File zipResult   = mapFiles.zipResult
		
		String serviceURLPart = messageSource.getMessage(
			'manifestsBackupPartPath', [event.id].toArray(), locale)
		String datePathPart = DateUtils.getShortStringFromDate(event.getDateFinish())
		String backupURL = "/backup/${datePathPart}/${serviceURLPart}.zip"
		String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
		
		if(zipResult.exists()) {
			log.debug("generarCopiaRespaldo - backup file already exists")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:backupURL)
		}
		
		Set<X509Certificate> systemTrustedCerts = firmaService.getTrustedCerts()
		byte[] systemTrustedCertsPEMBytes = CertUtil.fromX509CertCollectionToPEM(systemTrustedCerts)
		File systemTrustedCertsFile = new File("${filesDir.absolutePath}/systemTrustedCerts.pem")
		systemTrustedCertsFile.setBytes(systemTrustedCertsPEMBytes)
		
		byte[] timeStampCertPEMBytes = timeStampService.getSigningCert()
		File timeStampCertFile = new File("${filesDir.absolutePath}/timeStampCert.pem")
		timeStampCertFile.setBytes(timeStampCertPEMBytes)
		
		def backupMetaInfMap = [numSignatures:numSignatures]
		Map eventMetaInfMap = eventoService.getMetaInfMap(event)
		eventMetaInfMap.put(TypeVS.BACKUP.toString(), backupMetaInfMap);
		event.metaInf = eventMetaInfMap as JSON
		Evento.withTransaction {
			event.save()
		}

		metaInfFile.write((eventMetaInfMap as JSON).toString())
		
		DecimalFormat formatted = new DecimalFormat("00000000");
		int signaturesBatch = 0;
		String baseDir="${filesDir.absolutePath}/batch_${formatted.format(++signaturesBatch)}"
		new File(baseDir).mkdirs()
		
		String fileNamePrefix = messageSource.getMessage(
			'manifestSignatureLbl', null, locale);
		long begin = System.currentTimeMillis()
		Documento.withTransaction {
			def criteria = Documento.createCriteria()
			def firmasRecibidas = criteria.scroll {
				eq("evento", event)
				eq("estado", Documento.Estado.FIRMA_MANIFIESTO_VALIDADA)
			}
			while (firmasRecibidas.next()) {
				Documento firma = (Documento) firmasRecibidas.get(0);
				File pdfFile = new File("${baseDir}/${fileNamePrefix}_${String.format('%08d', firmasRecibidas.getRowNumber())}.pdf")
				pdfFile.setBytes(firma.pdf)
				if((firmasRecibidas.getRowNumber() % 100) == 0) {
					String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
						System.currentTimeMillis() - begin)
					log.debug(" - accessRequest ${firmasRecibidas.getRowNumber()} of ${numSignatures} - ${elapsedTimeStr}");
					sessionFactory.currentSession.flush()
					sessionFactory.currentSession.clear()
				}
				if(((firmasRecibidas.getRowNumber() + 1) % 2000) == 0) {
					baseDir="${filesDir.absolutePath}/batch_${formatted.format(++signaturesBatch)}"
					new File(baseDir).mkdirs()
				}
			}
		}
		
		def ant = new AntBuilder()
		ant.zip(destfile: zipResult, basedir: filesDir) {
			fileset(dir:"${filesDir.absolutePath}/..", includes: "meta.inf")
		}
		ant.copy(file: zipResult, tofile: webappBackupPath)
		
		return new ResponseVS(statusCode:ResponseVS.SC_OK,
			 message:backupURL, type:TypeVS.SIGN_EVENT) 
	}

}