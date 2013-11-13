package org.votingsystem.accesscontrol.service

import java.io.File;
import java.text.DecimalFormat
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.*;
import org.votingsystem.util.*;
import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.signature.smime.*;
import org.votingsystem.model.ContextVS
import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS;
import org.codehaus.groovy.grails.web.json.*

import javax.mail.Header;
import javax.mail.internet.MimeMessage;

import java.security.cert.X509Certificate;
import java.util.Locale;

import org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class EventoVotacionService {	

    def etiquetaService
    def subscripcionService
    def opcionDeEventoService
    def firmaService
    def eventoService
    def grailsApplication
	def almacenClavesService
	def httpService
	def messageSource
	def encryptionService
	def representativeService
	def filesService
	def timeStampService
	def sessionFactory

    ResponseVS saveEvent(MessageSMIME messageSMIMEReq, Locale locale) {
		EventoVotacion event = null
		Usuario firmante = messageSMIMEReq.getUsuario()
		log.debug("saveEvent - firmante: ${firmante?.nif}")
		String msg = null
		ResponseVS respuesta = null
		try {		
			String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
			def messageJSON = JSON.parse(documentStr)
			if (!messageJSON.centroControl || !messageJSON.centroControl.serverURL) {
				msg = messageSource.getMessage(
						'error.requestWithoutControlCenter', null, locale)
				log.error "saveEvent - DATA ERROR - ${msg} - messageJSON: ${messageJSON}" 
				return new ResponseVS(type:TypeVS.VOTING_EVENT_ERROR,
						message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
			}
			event = new EventoVotacion(asunto:messageJSON.asunto,
				contenido:messageJSON.contenido, usuario:firmante,
					fechaInicio: new Date().parse(
						"yyyy/MM/dd HH:mm:ss", messageJSON.fechaInicio),
					fechaFin: new Date().parse(
						"yyyy/MM/dd HH:mm:ss", messageJSON.fechaFin))
			respuesta = subscripcionService.checkControlCenter(
				messageJSON.centroControl.serverURL, locale)
			if(ResponseVS.SC_OK != respuesta.statusCode) {
				log.error "saveEvent - CHECKING CONTROL CENTER ERROR - ${respuesta.message}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:respuesta.message, type:TypeVS.VOTING_EVENT_ERROR)
			}  
			event.centroControl = respuesta.centroControl
			event.cadenaCertificacionCentroControl = respuesta.centroControl.cadenaCertificacion
			X509Certificate controlCenterCert = event.centroControl.certificadoX509
			respuesta = eventoService.setEventDatesState(event,locale)
			if(ResponseVS.SC_OK != respuesta.statusCode) {
				log.error "saveEvent - EVENT DATES ERROR - ${respuesta.message}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:respuesta.message, type:TypeVS.VOTING_EVENT_ERROR)
			} 
			if(messageJSON.cardinalidad) event.cardinalidadOpciones =
					Evento.Cardinalidad.valueOf(messageJSON.cardinalidad)
			else event.cardinalidadOpciones = Evento.Cardinalidad.UNA
			messageJSON.controlAcceso = [serverURL:grailsApplication.config.grails.serverURL,
				nombre:grailsApplication.config.VotingSystem.serverName] as JSONObject
			if (messageJSON.etiquetas) {
				Set<Etiqueta> etiquetaSet = etiquetaService.guardarEtiquetas(messageJSON.etiquetas)
				if(etiquetaSet) event.setEtiquetaSet(etiquetaSet)
			}
			EventoVotacion.withTransaction {
				event.save()
			}
			if (messageJSON.opciones) {
				Set<OpcionDeEvento> opciones = opcionDeEventoService.guardarOpciones(event, messageJSON.opciones)
				JSONArray arrayOpciones = new JSONArray()
				opciones.each { opcion ->
						arrayOpciones.add([id:opcion.id, contenido:opcion.contenido] as JSONObject  )
				}
				messageJSON.opciones = arrayOpciones
			}
			log.debug(" ------ Saved voting event '${event.id}'")
			messageJSON.id = event.id
			messageJSON.URL = "${grailsApplication.config.grails.serverURL}/eventoVotacion/${event.id}"
			messageJSON.fechaCreacion = DateUtils.getStringFromDate(event.dateCreated)
			messageJSON.type = TypeVS.VOTING_EVENT
			respuesta = almacenClavesService.generar(event)
			if(ResponseVS.SC_OK != respuesta.statusCode) {
				log.error "saveEvent - ERROR GENERATING EVENT KEYSTRORE- ${respuesta.message}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:respuesta.message, type:TypeVS.VOTING_EVENT_ERROR, event:event)
			} 
			messageJSON.certCAVotacion = new String(
				CertUtil.fromX509CertToPEM (respuesta.certificado))
			File cadenaCertificacion = grailsApplication.mainContext.getResource(
				grailsApplication.config.VotingSystem.certChainPath).getFile();
			messageJSON.cadenaCertificacion = new String(cadenaCertificacion.getBytes())
			
			X509Certificate certUsuX509 = firmante.getCertificate()
			messageJSON.usuario = new String(CertUtil.fromX509CertToPEM (certUsuX509))			
			
			/*Set<X509Certificate> trustedCAs = firmaService.getTrustedCerts()
			JSONArray trustedCAPEMArray= new JSONArray() ;
			for(X509Certificate trustedCA: trustedCAs) {
				trustedCAPEMArray.add(new String(CertUtil.fromX509CertToPEM (trustedCA)))
			}
			messageJSON.trustedCAs = trustedCAPEMArray*/

			String controCenterEventsURL = "${event.centroControl.serverURL}/eventoVotacion"

			Header header = new Header ("serverURL", "${grailsApplication.config.grails.serverURL}");
			String fromUser = grailsApplication.config.VotingSystem.serverName
			String toUser = event.centroControl.getNombre()
			String subject = messageSource.getMessage('mime.asunto.EventoVotacionValidado', null, locale)			
			byte[] smimeMessageRespBytes = firmaService.getSignedMimeMessage(
				fromUser, toUser, messageJSON.toString(), subject, header)
	
			ResponseVS encryptResponse = encryptionService.encryptSMIMEMessage(
					smimeMessageRespBytes, controlCenterCert, locale)
			if(ResponseVS.SC_OK != encryptResponse.statusCode) {
				event.estado = Evento.Estado.ACTORES_PENDIENTES_NOTIFICACION
				Evento.withTransaction {
					event.save()
				}
				log.error "saveEvent - ERROR ENCRYPTING MSG - ${encryptResponse.message}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:encryptResponse.message, , event:event, 
					type:TypeVS.VOTING_EVENT_ERROR)
			}
			ResponseVS respuestaNotificacion = httpService.sendMessage(
				encryptResponse.messageBytes, ContentTypeVS.SIGNED_AND_ENCRYPTED, controCenterEventsURL)
			if(ResponseVS.SC_OK != respuestaNotificacion.statusCode) {
				event.estado = Evento.Estado.ACTORES_PENDIENTES_NOTIFICACION
				Evento.withTransaction {
					event.save()
				}
				msg = messageSource.getMessage('controCenterCommunicationErrorMsg', 
					[respuestaNotificacion.message].toArray(), locale)	
				log.error "saveEvent - ERROR NOTIFYING CONTROL CENTER - ${msg}"
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
					message:msg, type:TypeVS.VOTING_EVENT_ERROR, eventVS:event)
			}
			MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT,
				smimePadre:messageSMIMEReq, event:event, valido:true,
				contenido:smimeMessageRespBytes)
			MessageSMIME.withTransaction {
				messageSMIMEResp.save()
			}
			return new ResponseVS(statusCode:ResponseVS.SC_OK, eventVS:event,
					type:TypeVS.VOTING_EVENT, data:messageSMIMEResp)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage('publishVotingErrorMessage', null, locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
				message:msg, type:TypeVS.VOTING_EVENT_ERROR, eventVS:event)
		}
    }
    
	public synchronized ResponseVS generarCopiaRespaldo (EventoVotacion event, Locale locale) {
		log.debug("generarCopiaRespaldo - eventId: ${event.id}")
		ResponseVS respuesta;
		String msg = null
		try {
			if (event.isOpen(DateUtils.getTodayDate())) {  
				msg = messageSource.getMessage('eventDateNotFinished', null, locale)
				String currentDateStr = DateUtils.getStringFromDate(
					new Date(System.currentTimeMillis()))
				log.error("generarCopiaRespaldo - DATE ERROR  ${msg} - " + 
					"fecha actual '${currentDateStr}' fecha final event '${event.fechaFin}'")
				return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
					message:msg, type:TypeVS.BACKUP_ERROR)
			}
			
			Map<String, File> mapFiles = filesService.getBackupFiles(event,
				TypeVS.VOTING_EVENT, locale)
			File zipResult   = mapFiles.zipResult
			File metaInfFile = mapFiles.metaInfFile
			File filesDir    = mapFiles.filesDir
			
			String serviceURLPart = messageSource.getMessage(
				'votingBackupPartPath', [event.id].toArray(), locale)
			String datePathPart = DateUtils.getShortStringFromDate(event.getDateFinish())
			String backupURL = "/backup/${datePathPart}/${serviceURLPart}.zip"
			String webappBackupPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}${backupURL}"
			
			if(zipResult.exists()) {
				log.debug("generarCopiaRespaldo - backup file already exists")
				return new ResponseVS(statusCode:ResponseVS.SC_OK, message:backupURL)
			}
			respuesta = representativeService.getAccreditationsBackupForEvent(event, locale)
			Map representativeDataMap = respuesta.data
			if(ResponseVS.SC_OK != respuesta.statusCode) {
				log.error("generarCopiaRespaldo - REPRESENTATIVE DATA GEN ERROR  ${respuesta.message}")
				return respuesta
			}			
			
			respuesta = firmaService.getEventTrustedCerts(event, locale)
			if(ResponseVS.SC_OK != respuesta.statusCode) {
				respuesta.type = TypeVS.BACKUP_ERROR
				return respuesta
			}
			
			Set<X509Certificate> systemTrustedCerts = firmaService.getTrustedCerts()
			byte[] systemTrustedCertsPEMBytes = CertUtil.fromX509CertCollectionToPEM(systemTrustedCerts)
			File systemTrustedCertsFile = new File("${filesDir.absolutePath}/systemTrustedCerts.pem")
			systemTrustedCertsFile.setBytes(systemTrustedCertsPEMBytes)
			
			Set<X509Certificate> eventTrustedCerts = (Set<X509Certificate>) respuesta.data
			byte[] eventTrustedCertsPEMBytes = CertUtil.fromX509CertCollectionToPEM(eventTrustedCerts)
			File eventTrustedCertsFile = new File("${filesDir.absolutePath}/eventTrustedCerts.pem")
			eventTrustedCertsFile.setBytes(eventTrustedCertsPEMBytes)

			byte[] timeStampCertPEMBytes = timeStampService.getSigningCert()
			File timeStampCertFile = new File("${filesDir.absolutePath}/timeStampCert.pem")
			timeStampCertFile.setBytes(timeStampCertPEMBytes)
				
			int numTotalVotes = Voto.countByEstadoAndEventoVotacion(
				Voto.Estado.OK, event)
			int numTotalAccessRequests = SolicitudAcceso.countByEstadoAndEventoVotacion(
				SolicitudAcceso.Estado.OK, event)
			def backupMetaInfMap = [numVotes:numTotalVotes,
				numAccessRequest:numTotalAccessRequests]
			Map eventMetaInfMap = eventoService.getMetaInfMap(event)
			eventMetaInfMap.put(TypeVS.BACKUP.toString(), backupMetaInfMap);
			eventMetaInfMap.put(TypeVS.REPRESENTATIVE_DATA.toString(), representativeDataMap);
			
			metaInfFile.write((eventMetaInfMap as JSON).toString())
			
			String voteFileName = messageSource.getMessage('voteFileName', null, locale)
			String representativeVoteFileName = messageSource.getMessage(
				'representativeVoteFileName', null, locale)
			String solicitudAccesoFileName = messageSource.getMessage(
				'solicitudAccesoFileName', null, locale)
			
			DecimalFormat formatted = new DecimalFormat("00000000");
			int votesBatch = 0;
			String votesBaseDir="${filesDir.absolutePath}/votes/batch_${formatted.format(++votesBatch)}"
			new File(votesBaseDir).mkdirs()

			int accessRequestBatch = 0;
			String accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
			new File(accessRequestBaseDir).mkdirs()
			def votes = null
			long begin = System.currentTimeMillis()
			Voto.withTransaction {
				def criteria = Voto.createCriteria()
				votes = criteria.scroll {
					eq("estado", Voto.Estado.OK)
					eq("eventoVotacion", event)
				}
				while (votes.next()) {
					Voto voto = (Voto) votes.get(0);
					Usuario representative = voto?.certificado?.usuario
					String voteFilePath = null
					if(representative) {//representative vote, not anonymous
						voteFilePath = "${votesBaseDir}/${representativeVoteFileName}_${representative.nif}.p7m"
					} else {
						//user vote, is anonymous
						voteFilePath = "${votesBaseDir}${voteFileName}_${formatted.format(voto.id)}.p7m"
					}
					MessageSMIME messageSMIME = voto.messageSMIME
					File smimeFile = new File(voteFilePath)
					smimeFile.setBytes(messageSMIME.contenido)
					if((votes.getRowNumber() % 100) == 0) {
						String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
							System.currentTimeMillis() - begin)
						log.debug("processed ${votes.getRowNumber()} votes of ${numTotalVotes} - ${elapsedTimeStr}");
						sessionFactory.currentSession.flush()
						sessionFactory.currentSession.clear()
					}
					if(((votes.getRowNumber() + 1) % 2000) == 0) {
						votesBaseDir="${filesDir.absolutePath}/votes/batch_${formatted.format(++votesBatch)}"
						new File(votesBaseDir).mkdirs()
					}	
				}
			}
			
			def accessRequests = null
			begin = System.currentTimeMillis()
			SolicitudAcceso.withTransaction {
				def criteria = SolicitudAcceso.createCriteria()
				accessRequests = criteria.scroll {
					eq("estado", SolicitudAcceso.Estado.OK)
					eq("eventoVotacion", event)
				}
				while (accessRequests.next()) {
					SolicitudAcceso accessRequest = (SolicitudAcceso) accessRequests.get(0);
					MessageSMIME messageSMIME = accessRequest.messageSMIME
					File smimeFile = new File("${accessRequestBaseDir}/${solicitudAccesoFileName}_${accessRequest.usuario.nif}.p7m")
					smimeFile.setBytes(messageSMIME.contenido)
					if((accessRequests.getRowNumber() % 100) == 0) {
						String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
							System.currentTimeMillis() - begin)
						log.debug(" - accessRequest ${accessRequests.getRowNumber()} of ${numTotalAccessRequests} - ${elapsedTimeStr}");
						sessionFactory.currentSession.flush()
						sessionFactory.currentSession.clear()
					} 
					if(((accessRequests.getRowNumber() + 1) % 2000) == 0) {
						accessRequestBaseDir="${filesDir.absolutePath}/accessRequest/batch_${formatted.format(++accessRequestBatch)}"
						new File(accessRequestBaseDir).mkdirs()
					}
				}
				
			}
			
			def ant = new AntBuilder()
			ant.zip(destfile: zipResult, basedir: "${filesDir}") {
				fileset(dir:"${filesDir}/..", includes: "meta.inf")
			}
			ant.copy(file: zipResult, tofile: webappBackupPath)
			
			if (!event.isAttached()) {
				event.attach()
			}
			event.metaInf = eventMetaInfMap as JSON
			event.save()
			
			log.debug("zip backup of event ${event.id} on file ${zipResult.absolutePath}")
			return new ResponseVS(statusCode:ResponseVS.SC_OK, message:backupURL,
				data:eventMetaInfMap)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg =  messageSource.getMessage('error.backupGenericErrorMsg', 
				[event?.id].toArray(), locale)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, 
				message:msg, type:TypeVS.BACKUP_ERROR)
		}

	}
        
    public Map getStatisticsMap (EventoVotacion event, Locale locale) {
        log.debug("getStatisticsMap - eventId: ${event?.id}")
        if(!event) return null
        def statisticsMap = new HashMap()
        statisticsMap.opciones = []
        statisticsMap.id = event.id
        statisticsMap.asunto = event.asunto
        statisticsMap.numeroSolicitudesDeAcceso = SolicitudAcceso.countByEventoVotacion(event)
        statisticsMap.numeroSolicitudesDeAccesoOK = SolicitudAcceso.countByEventoVotacionAndEstado(
                        event, SolicitudAcceso.Estado.OK)
        statisticsMap.numeroSolicitudesDeAccesoANULADAS =   SolicitudAcceso.countByEventoVotacionAndEstado(
                        event, SolicitudAcceso.Estado.ANULADO)
        statisticsMap.numeroVotos = Voto.countByEventoVotacion(event)
        statisticsMap.numeroVotosOK = Voto.countByEventoVotacionAndEstado(
                        event, Voto.Estado.OK)
        statisticsMap.numeroVotosANULADOS = Voto.countByEventoVotacionAndEstado(
                event, Voto.Estado.ANULADO)								
        event.opciones.each { opcion ->
            def numeroVotos = Voto.countByOpcionDeEventoAndEstado(
                    opcion, Voto.Estado.OK)
            def opcionMap = [id:opcion.id, contenido:opcion.contenido,
                    numeroVotos:numeroVotos]
            statisticsMap.opciones.add(opcionMap)
        }
        return statisticsMap
    }
	
}