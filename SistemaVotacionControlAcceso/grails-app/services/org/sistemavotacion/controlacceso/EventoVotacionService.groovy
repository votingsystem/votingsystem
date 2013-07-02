package org.sistemavotacion.controlacceso

import java.io.File;
import java.util.Date;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.sistemavotacion.seguridad.*;
import org.sistemavotacion.util.*;
import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.smime.*;
import grails.converters.JSON
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

    Respuesta saveEvent(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		EventoVotacion event = null
		Usuario firmante = mensajeSMIMEReq.getUsuario()
		log.debug("saveEvent - firmante: ${firmante?.nif}")
		String msg = null
		Respuesta respuesta = null
		try {		
			String documentStr = mensajeSMIMEReq.getSmimeMessage()?.getSignedContent()
			def mensajeJSON = JSON.parse(documentStr)
			if (!mensajeJSON.centroControl || !mensajeJSON.centroControl.serverURL) {
				msg = messageSource.getMessage(
						'error.requestWithoutControlCenter', null, locale)
				log.error "saveEvent - DATA ERROR - ${msg} - mensajeJSON: ${mensajeJSON}" 
				return new Respuesta(tipo:Tipo.EVENTO_VOTACION_ERROR,
						mensaje:msg, codigoEstado:Respuesta.SC_ERROR_PETICION)
			}
			event = new EventoVotacion(asunto:mensajeJSON.asunto,
				contenido:mensajeJSON.contenido, usuario:firmante,
					fechaInicio: new Date().parse(
						"yyyy-MM-dd HH:mm:ss", mensajeJSON.fechaInicio),
					fechaFin: new Date().parse(
						"yyyy-MM-dd HH:mm:ss", mensajeJSON.fechaFin))
			respuesta = subscripcionService.checkControlCenter(
				mensajeJSON.centroControl.serverURL, locale)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				log.error "saveEvent - CHECKING CONTROL CENTER ERROR - ${respuesta.mensaje}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:respuesta.mensaje, tipo:Tipo.EVENTO_VOTACION_ERROR)
			}  
			event.centroControl = respuesta.centroControl
			event.cadenaCertificacionCentroControl = respuesta.centroControl.cadenaCertificacion
			X509Certificate controlCenterCert = event.centroControl.certificadoX509
			respuesta = eventoService.setEventDatesState(event,locale)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				log.error "saveEvent - EVENT DATES ERROR - ${respuesta.mensaje}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:respuesta.mensaje, tipo:Tipo.EVENTO_VOTACION_ERROR)
			} 
			if(mensajeJSON.cardinalidad) event.cardinalidadOpciones =
					Evento.Cardinalidad.valueOf(mensajeJSON.cardinalidad)
			else event.cardinalidadOpciones = Evento.Cardinalidad.UNA
			mensajeJSON.controlAcceso = [serverURL:grailsApplication.config.grails.serverURL,
				nombre:grailsApplication.config.SistemaVotacion.serverName] as JSONObject
			if (mensajeJSON.etiquetas) {
				Set<Etiqueta> etiquetaSet = etiquetaService.guardarEtiquetas(mensajeJSON.etiquetas)
				if(etiquetaSet) event.setEtiquetaSet(etiquetaSet)
			}
			EventoVotacion.withTransaction {
				event.save()
			}
			if (mensajeJSON.opciones) {
				Set<OpcionDeEvento> opciones = opcionDeEventoService.guardarOpciones(event, mensajeJSON.opciones)
				JSONArray arrayOpciones = new JSONArray()
				opciones.each { opcion ->
						arrayOpciones.add([id:opcion.id, contenido:opcion.contenido] as JSONObject  )
				}
				mensajeJSON.opciones = arrayOpciones
			}
			log.debug(" ------ Saved voting event '${event.id}'")
			mensajeJSON.id = event.id
			mensajeJSON.URL = "${grailsApplication.config.grails.serverURL}/eventoVotacion/${event.id}"
			mensajeJSON.fechaCreacion = DateUtils.getStringFromDate(event.dateCreated)
			mensajeJSON.tipo = Tipo.EVENTO_VOTACION
			respuesta = almacenClavesService.generar(event)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				log.error "saveEvent - ERROR GENERATING EVENT KEYSTRORE- ${respuesta.mensaje}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:respuesta.mensaje, tipo:Tipo.EVENTO_VOTACION_ERROR, event:event)
			} 
			mensajeJSON.certCAVotacion = new String(
				CertUtil.fromX509CertToPEM (respuesta.certificado))
			File cadenaCertificacion = grailsApplication.mainContext.getResource(
				grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion).getFile();
			mensajeJSON.cadenaCertificacion = new String(cadenaCertificacion.getBytes())
			
			X509Certificate certUsuX509 = firmante.getCertificate()
			mensajeJSON.usuario = new String(CertUtil.fromX509CertToPEM (certUsuX509))

			String controCenterEventsURL = "${event.centroControl.serverURL}" +
				"${grailsApplication.config.SistemaVotacion.sufijoURLInicializacionEvento}"

			Header header = new Header ("serverURL", "${grailsApplication.config.grails.serverURL}");
			String fromUser = grailsApplication.config.SistemaVotacion.serverName
			String toUser = event.centroControl.getNombre()
			String subject = messageSource.getMessage('mime.asunto.EventoVotacionValidado', null, locale)			
			byte[] smimeMessageRespBytes = firmaService.getSignedMimeMessage(
				fromUser, toUser, mensajeJSON.toString(), subject, header)
	
			Respuesta encryptResponse = encryptionService.encryptSMIMEMessage(
					smimeMessageRespBytes, controlCenterCert, locale)
			if(Respuesta.SC_OK != encryptResponse.codigoEstado) {
				event.estado = Evento.Estado.ACTORES_PENDIENTES_NOTIFICACION
				Evento.withTransaction {
					event.save()
				}
				log.error "saveEvent - ERROR ENCRYPTING MSG - ${encryptResponse.mensaje}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:encryptResponse.mensaje, , event:event, 
					tipo:Tipo.EVENTO_VOTACION_ERROR)
			}
			String contentType = "${grailsApplication.config.pkcs7SignedContentType};" +
				"${grailsApplication.config.pkcs7EncryptedContentType}"
			Respuesta respuestaNotificacion = httpService.sendMessage(
				encryptResponse.messageBytes, contentType, controCenterEventsURL)
			if(Respuesta.SC_OK != respuestaNotificacion.codigoEstado) {
				event.estado = Evento.Estado.ACTORES_PENDIENTES_NOTIFICACION
				Evento.withTransaction {
					event.save()
				}
				msg = messageSource.getMessage('controCenterCommunicationErrorMsg', 
					[respuestaNotificacion.mensaje].toArray(), locale)	
				log.error "saveEvent - ERROR NOTIFYING CONTROL CENTER - ${msg}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg, tipo:Tipo.EVENTO_VOTACION_ERROR, evento:event)
			}
			MensajeSMIME mensajeSMIMEResp = new MensajeSMIME(tipo:Tipo.RECIBO,
				smimePadre:mensajeSMIMEReq, event:event, valido:true,
				contenido:smimeMessageRespBytes)
			MensajeSMIME.withTransaction {
				mensajeSMIMEResp.save()
			}
			return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:event,
					tipo:Tipo.EVENTO_VOTACION, mensajeSMIME:mensajeSMIMEResp)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage('publishVotingErrorMessage', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:msg, tipo:Tipo.EVENTO_VOTACION_ERROR, evento:event)
		}
    }
    
	public synchronized Respuesta generarCopiaRespaldo (EventoVotacion event, Locale locale) {
		log.debug("generarCopiaRespaldo - eventId: ${event.id}")
		Respuesta respuesta;
		String msg = null
		try {
			if (event.isOpen(DateUtils.getTodayDate())) {  
				msg = messageSource.getMessage('eventDateNotFinished', null, locale)
				String currentDateStr = DateUtils.getStringFromDate(
					new Date(System.currentTimeMillis()))
				log.error("generarCopiaRespaldo - DATE ERROR  ${msg} - " + 
					"fecha actual '${currentDateStr}' fecha final event '${event.fechaFin}'")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:msg, tipo:Tipo.BACKUP_ERROR)
			}
			
			Map<String, File> mapFiles = filesService.getBackupFiles(event,
				Tipo.EVENTO_VOTACION, locale)
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
				return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:backupURL)
			}
			respuesta = representativeService.getAccreditationsBackupForEvent(event, locale)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				log.error("generarCopiaRespaldo - REPRESENTATIVE DATA GEN ERROR  ${respuesta.mensaje}")
				return respuesta
			}			
			
			respuesta = firmaService.getEventTrustedCerts(event, locale)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				respuesta.tipo = Tipo.BACKUP_ERROR
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
			def metaInfMap = eventoService.updateEventMetaInf(event, Tipo.BACKUP, backupMetaInfMap)
			log.debug(" metaInfFile ${metaInfFile.path}")
			metaInfFile.write((metaInfMap as JSON).toString())
			
			String voteFileName = messageSource.getMessage('voteFileName', null, locale)
			String representativeVoteFileName = messageSource.getMessage(
				'representativeVoteFileName', null, locale)
			String solicitudAccesoFileName = messageSource.getMessage(
				'solicitudAccesoFileName', null, locale)
			String votesBaseDir="${filesDir.absolutePath}/votes"
			new File(votesBaseDir).mkdirs()

			
			String accessRequestBaseDir="${filesDir.absolutePath}/accessRequest"
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
						String voteId = String.format('%08d', voto.id)
						voteFilePath = "${votesBaseDir}/${voteFileName}_${voteId}.p7m"
					}
					MensajeSMIME mensajeSMIME = voto.mensajeSMIME
					File smimeFile = new File(voteFilePath)
					smimeFile.setBytes(mensajeSMIME.contenido)
					if((votes.getRowNumber() % 100) == 0) {
						String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
							System.currentTimeMillis() - begin)
						log.debug("processed ${votes.getRowNumber()} votes of ${numTotalVotes} - ${elapsedTimeStr}");
						sessionFactory.currentSession.flush()
						sessionFactory.currentSession.clear()
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
					MensajeSMIME mensajeSMIME = accessRequest.mensajeSMIME
					File smimeFile = new File("${accessRequestBaseDir}/${solicitudAccesoFileName}_${accessRequest.usuario.nif}.p7m")
					smimeFile.setBytes(mensajeSMIME.contenido)
					if((accessRequests.getRowNumber() % 100) == 0) {
						String elapsedTimeStr = DateUtils.getElapsedTimeHoursMinutesMillisFromMilliseconds(
							System.currentTimeMillis() - begin)
						log.debug(" - accessRequest ${accessRequests.getRowNumber()} of ${numTotalAccessRequests} - ${elapsedTimeStr}");
						sessionFactory.currentSession.flush()
						sessionFactory.currentSession.clear()
					} 
				}
			}
			
			def ant = new AntBuilder()
			ant.zip(destfile: zipResult, basedir: "${filesDir}") {
				fileset(dir:"${filesDir}/..", includes: "meta.inf")
			}
			ant.copy(file: zipResult, tofile: webappBackupPath)
			
			log.debug("zip backup of event ${event.id} on file ${zipResult.absolutePath}")
			return new Respuesta(codigoEstado:Respuesta.SC_OK, mensaje:backupURL)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg =  messageSource.getMessage('error.backupGenericErrorMsg', 
				[event?.id].toArray(), locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR, 
				mensaje:msg, tipo:Tipo.BACKUP_ERROR)
		}

	}
	
}