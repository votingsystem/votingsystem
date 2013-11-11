package org.votingsystem.accesscontrol.service

import java.security.cert.X509Certificate
import java.text.DecimalFormat
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.*;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.*;
import org.votingsystem.accesscontrol.model.*;

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

    ResponseVS saveEvent(MessageSMIME messageSMIMEReq, Locale locale) {		
		EventoReclamacion evento
		try {
			Usuario firmante = messageSMIMEReq.getUsuario()
			String documentStr = messageSMIMEReq.getSmimeMessage().getSignedContent()
			log.debug("saveEvent - firmante: ${firmante.nif} - documentStr: ${documentStr}")
			def messageJSON = JSON.parse(documentStr)
			Date fechaFin = new Date().parse("yyyy/MM/dd HH:mm:ss", messageJSON.fechaFin)
			if(fechaFin.before(DateUtils.getTodayDate())) {
				String msg = messageSource.getMessage(
					'publishDocumentDateErrorMsg', 
					[DateUtils.getStringFromDate(fechaFin)].toArray(), locale)
				log.error("DATE ERROR - msg: ${msg}")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
					message:msg, type:TypeVS.EVENTO_RECLAMACION_ERROR, eventVS:evento)
			}
			evento = new EventoReclamacion(usuario:firmante,
					asunto:messageJSON.asunto, contenido:messageJSON.contenido,
					copiaSeguridadDisponible:messageJSON.copiaSeguridadDisponible,
					fechaFin:fechaFin)
			if(messageJSON.cardinalidad) evento.cardinalidadRepresentaciones =
				Evento.Cardinalidad.valueOf(messageJSON.cardinalidad)
			else evento.cardinalidadRepresentaciones = Evento.Cardinalidad.UNA
			if(messageJSON.fechaInicio) evento.fechaInicio = new Date().parse(
						"yyyy/MM/dd HH:mm:ss", messageJSON.fechaInicio)
			else evento.fechaInicio = DateUtils.getTodayDate();
			ResponseVS respuesta = eventoService.setEventDatesState(evento, locale)
			if(ResponseVS.SC_OK != respuesta.statusCode) return respuesta 
			evento = respuesta.eventVS.save()
			if (messageJSON.etiquetas) {
				Set<Etiqueta> etiquetaSet = etiquetaService.guardarEtiquetas(messageJSON.etiquetas)
				evento.setEtiquetaSet(etiquetaSet)
			}
			messageJSON.id = evento.id
			messageJSON.fechaCreacion = DateUtils.getStringFromDate(evento.dateCreated)
			messageJSON.type = TypeVS.EVENTO_RECLAMACION
			def camposValidados = []
			JSONArray arrayCampos = new JSONArray()
			messageJSON.campos?.each { campoItem ->
				def campo = new CampoDeEvento(evento:evento, contenido:campoItem.contenido)
				campo.save();
				arrayCampos.add(new JSONObject([id:campo.id, contenido:campo.contenido]))
			}
			messageJSON.controlAcceso = [serverURL:grailsApplication.config.grails.serverURL,
				nombre:grailsApplication.config.VotingSystem.serverName]  as JSONObject
			messageJSON.campos = arrayCampos
			
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = firmante.getNif()
			String subject = messageSource.getMessage(
					'mime.asunto.EventoReclamacionValidado', null, locale)
			
			byte[] smimeMessageRespBytes = firmaService.getSignedMimeMessage(
				fromUser, toUser,  messageJSON.toString(), subject, null)
			
			MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECIBO,
				smimePadre:messageSMIMEReq, evento:evento,
				valido:true, contenido:smimeMessageRespBytes)
			MessageSMIME.withTransaction {
				messageSMIMEResp.save()
			}
			return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:evento,
				messageSMIME:messageSMIMEResp, type:TypeVS.EVENTO_RECLAMACION)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:messageSource.getMessage('publishClaimErrorMessage', null, locale), 
				type:TypeVS.EVENTO_RECLAMACION_ERROR, eventVS:evento)
		}
    }

    public synchronized ResponseVS generarCopiaRespaldo (EventoReclamacion event, Locale locale) {
        log.debug("generarCopiaRespaldo - eventoId: ${event.id}")
		ResponseVS respuesta;
        if (!event) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, message:
				messageSource.getMessage('event.peticionSinEvento', null, locale))
        }		
		Map<String, File> mapFiles = filesService.getBackupFiles(
			event, TypeVS.EVENTO_RECLAMACION, locale)
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
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:backupURL)
		}		
		
		int numSignatures = Firma.countByEventoAndType(event, 
			TypeVS.FIRMA_EVENTO_RECLAMACION)	
		def backupMetaInfMap = [numSignatures:numSignatures]
		Map eventMetaInfMap =  eventoService.getMetaInfMap(event)
		eventMetaInfMap.put(TypeVS.BACKUP.toString(), backupMetaInfMap);
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
				eq("type", TypeVS.FIRMA_EVENTO_RECLAMACION)
			}
			
			
			
			while (firmasRecibidas.next()) {
				Firma firma = (Firma) firmasRecibidas.get(0);
				MessageSMIME messageSMIME = firma.messageSMIME
				File smimeFile = new File("${baseDir}/${fileNamePrefix}_${formatted.format(firmasRecibidas.getRowNumber())}.p7m")
				smimeFile.setBytes(messageSMIME.contenido)
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

		return new ResponseVS(statusCode:ResponseVS.SC_OK,
			type:TypeVS.EVENTO_RECLAMACION, message:backupURL)
    }

}