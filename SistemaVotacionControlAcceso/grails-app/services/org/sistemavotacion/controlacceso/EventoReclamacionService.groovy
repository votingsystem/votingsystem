package org.sistemavotacion.controlacceso

import java.security.cert.X509Certificate
import java.text.DecimalFormat
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
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
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class EventoReclamacionService {	
		
    static transactional = true

    def etiquetaService
    def firmaService
    def eventoService
    def grailsApplication
	def messageSource
	def filesService
	def timeStampService
	def sessionFactory

    Respuesta saveEvent(MensajeSMIME mensajeSMIMEReq, Locale locale) {		
		EventoReclamacion evento
		try {
			Usuario firmante = mensajeSMIMEReq.getUsuario()
			String documentStr = mensajeSMIMEReq.getSmimeMessage().getSignedContent()
			log.debug("saveEvent - firmante: ${firmante.nif} - documentStr: ${documentStr}")
			def mensajeJSON = JSON.parse(documentStr)
			Date fechaFin = new Date().parse("yyyy/MM/dd HH:mm:ss", mensajeJSON.fechaFin)
			if(fechaFin.before(DateUtils.getTodayDate())) {
				String msg = messageSource.getMessage(
					'publishDocumentDateErrorMsg', 
					[DateUtils.getStringFromDate(fechaFin)].toArray(), locale)
				log.error("DATE ERROR - msg: ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
					mensaje:msg, tipo:Tipo.EVENTO_RECLAMACION_ERROR, evento:evento)
			}
			evento = new EventoReclamacion(usuario:firmante,
					asunto:mensajeJSON.asunto, contenido:mensajeJSON.contenido,
					copiaSeguridadDisponible:mensajeJSON.copiaSeguridadDisponible,
					fechaFin:fechaFin)
			if(mensajeJSON.cardinalidad) evento.cardinalidadRepresentaciones =
				Evento.Cardinalidad.valueOf(mensajeJSON.cardinalidad)
			else evento.cardinalidadRepresentaciones = Evento.Cardinalidad.UNA
			if(mensajeJSON.fechaInicio) evento.fechaInicio = new Date().parse(
						"yyyy/MM/dd HH:mm:ss", mensajeJSON.fechaInicio)
			else evento.fechaInicio = DateUtils.getTodayDate();
			Respuesta respuesta = eventoService.setEventDatesState(evento, locale)
			if(Respuesta.SC_OK != respuesta.codigoEstado) return respuesta 
			evento = respuesta.evento.save()
			if (mensajeJSON.etiquetas) {
				Set<Etiqueta> etiquetaSet = etiquetaService.guardarEtiquetas(mensajeJSON.etiquetas)
				evento.setEtiquetaSet(etiquetaSet)
			}
			mensajeJSON.id = evento.id
			mensajeJSON.fechaCreacion = DateUtils.getStringFromDate(evento.dateCreated)
			mensajeJSON.tipo = Tipo.EVENTO_RECLAMACION
			def camposValidados = []
			JSONArray arrayCampos = new JSONArray()
			mensajeJSON.campos?.each { campoItem ->
				def campo = new CampoDeEvento(evento:evento, contenido:campoItem.contenido)
				campo.save();
				arrayCampos.add(new JSONObject([id:campo.id, contenido:campo.contenido]))
			}
			mensajeJSON.controlAcceso = [serverURL:grailsApplication.config.grails.serverURL,
				nombre:grailsApplication.config.SistemaVotacion.serverName]  as JSONObject
			mensajeJSON.campos = arrayCampos
			
			String fromUser = grailsApplication.config.SistemaVotacion.serverName
			String toUser = firmante.getNif()
			String subject = messageSource.getMessage(
					'mime.asunto.EventoReclamacionValidado', null, locale)
			
			byte[] smimeMessageRespBytes = firmaService.getSignedMimeMessage(
				fromUser, toUser,  mensajeJSON.toString(), subject, null)
			
			MensajeSMIME mensajeSMIMEResp = new MensajeSMIME(tipo:Tipo.RECIBO,
				smimePadre:mensajeSMIMEReq, evento:evento,
				valido:true, contenido:smimeMessageRespBytes)
			MensajeSMIME.withTransaction {
				mensajeSMIMEResp.save()
			}
			return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento,
				mensajeSMIME:mensajeSMIMEResp, tipo:Tipo.EVENTO_RECLAMACION)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:messageSource.getMessage('publishClaimErrorMessage', null, locale), 
				tipo:Tipo.EVENTO_RECLAMACION_ERROR, evento:evento)
		}
    }

    public synchronized Respuesta generarCopiaRespaldo (EventoReclamacion event, Locale locale) {
        log.debug("generarCopiaRespaldo - eventoId: ${event.id}")
		Respuesta respuesta;
        if (!event) {
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:
				messageSource.getMessage('event.peticionSinEvento', null, locale))
        }		
		Map<String, File> mapFiles = filesService.getBackupFiles(
			event, Tipo.EVENTO_RECLAMACION, locale)
		File metaInfFile = mapFiles.metaInfFile
		File filesDir = mapFiles.filesDir
		File zipResult   = mapFiles.zipResult
		
		String serviceURLPart = messageSource.getMessage(
			'claimsBackupPartPath', [event.id].toArray(), locale)
		String datePathPart = DateUtils.getShortStringFromDate(event.getDateFinish())
		String backupURL = "/backup/${datePathPart}/${serviceURLPart}.zip"
		String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
		
		if(zipResult.exists()) {
			log.debug("generarCopiaRespaldo - backup file already exists")
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:backupURL)
		}		
		
		int numSignatures = Firma.countByEventoAndTipo(event, 
			Tipo.FIRMA_EVENTO_RECLAMACION)	
		def backupMetaInfMap = [numSignatures:numSignatures]
		Map eventMetaInfMap =  eventoService.getMetaInfMap(event)
		eventMetaInfMap.put(Tipo.BACKUP.toString(), backupMetaInfMap);
		event.metaInf = eventMetaInfMap as JSON
		Evento.withTransaction {
			event.save()
		}
		metaInfFile.write((eventMetaInfMap as JSON).toString())
		
		String fileNamePrefix = messageSource.getMessage('claimLbl', null, locale);
		
		DecimalFormat formatted = new DecimalFormat("00000000");
		int claimsBatch = 0;
		String baseDir="${filesDir.absolutePath}/batch_${formatted.format(++claimsBatch)}"
		new File(baseDir).mkdirs()
		
		long begin = System.currentTimeMillis()
		Firma.withTransaction {
			def criteria = Firma.createCriteria()
			def firmasRecibidas = criteria.scroll {
				eq("evento", event)
				eq("tipo", Tipo.FIRMA_EVENTO_RECLAMACION)
			}
			
			
			
			while (firmasRecibidas.next()) {
				Firma firma = (Firma) firmasRecibidas.get(0);
				MensajeSMIME mensajeSMIME = firma.mensajeSMIME
				File smimeFile = new File("${baseDir}/${fileNamePrefix}_${formatted.format(firmasRecibidas.getRowNumber())}.p7m")
				smimeFile.setBytes(mensajeSMIME.contenido)
				if((firmasRecibidas.getRowNumber() % 100) == 0) {
					String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
						System.currentTimeMillis() - begin)
					log.debug(" - accessRequest ${firmasRecibidas.getRowNumber()} of ${numSignatures} - ${elapsedTimeStr}");
					sessionFactory.currentSession.flush()
					sessionFactory.currentSession.clear()
				}
				if(((firmasRecibidas.getRowNumber() + 1) % 2000) == 0) {
					baseDir="${filesDir.absolutePath}/batch_${formatted.format(++claimsBatch)}"
					new File(baseDir).mkdirs()
				}
			}
		}	

		Set<X509Certificate> systemTrustedCerts = firmaService.getTrustedCerts()
		byte[] systemTrustedCertsPEMBytes = CertUtil.fromX509CertCollectionToPEM(systemTrustedCerts)
		File systemTrustedCertsFile = new File("${filesDir.absolutePath}/systemTrustedCerts.pem")
		systemTrustedCertsFile.setBytes(systemTrustedCertsPEMBytes)
		
		byte[] timeStampCertPEMBytes = timeStampService.getSigningCert()
		File timeStampCertFile = new File("${filesDir.absolutePath}/timeStampCert.pem")
		timeStampCertFile.setBytes(timeStampCertPEMBytes)
		
		def ant = new AntBuilder()
		ant.zip(destfile: zipResult, basedir: filesDir) {
			fileset(dir:"${filesDir}/..", includes: "meta.inf")
		}
		ant.copy(file: zipResult, tofile: webappBackupPath)

		return new Respuesta(codigoEstado:Respuesta.SC_OK,
			tipo:Tipo.EVENTO_RECLAMACION, mensaje:backupURL)
    }

}