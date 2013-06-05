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

    Respuesta saveEvent(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		EventoVotacion evento = null
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
				log.error "saveEvent - DATA ERROR - ${msg}" 
				return new Respuesta(tipo:Tipo.EVENTO_VOTACION_ERROR,
						mensaje:msg, codigoEstado:Respuesta.SC_ERROR_PETICION)
			}
			evento = new EventoVotacion(asunto:mensajeJSON.asunto,
				contenido:mensajeJSON.contenido, usuario:firmante,
					fechaInicio: new Date().parse(
						"yyyy-MM-dd HH:mm:ss", mensajeJSON.fechaInicio),
					fechaFin: new Date().parse(
						"yyyy-MM-dd HH:mm:ss", mensajeJSON.fechaFin))
			respuesta = subscripcionService.checkControlCenter(
				mensajeJSON.centroControl.serverURL)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				log.error "saveEvent - CHECKING CONTROL CENTER ERROR - ${respuesta.mensaje}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:respuesta.mensaje, tipo:Tipo.EVENTO_VOTACION_ERROR)
			}  
			evento.centroControl = respuesta.centroControl
			evento.cadenaCertificacionCentroControl = respuesta.centroControl.cadenaCertificacion
			X509Certificate controlCenterCert = evento.centroControl.certificadoX509
			respuesta = eventoService.setEventDatesState(evento,locale)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				log.error "saveEvent - EVENT DATES ERROR - ${respuesta.mensaje}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:respuesta.mensaje, tipo:Tipo.EVENTO_VOTACION_ERROR)
			} 
			if(mensajeJSON.cardinalidad) evento.cardinalidadOpciones =
					Evento.Cardinalidad.valueOf(mensajeJSON.cardinalidad)
			else evento.cardinalidadOpciones = Evento.Cardinalidad.UNA
			mensajeJSON.controlAcceso = [serverURL:grailsApplication.config.grails.serverURL,
				nombre:grailsApplication.config.SistemaVotacion.serverName] as JSONObject
			if (mensajeJSON.etiquetas) {
				Set<Etiqueta> etiquetaSet = etiquetaService.guardarEtiquetas(mensajeJSON.etiquetas)
				if(etiquetaSet) evento.setEtiquetaSet(etiquetaSet)
			}
			EventoVotacion.withTransaction {
				evento.save()
			}
			if (mensajeJSON.opciones) {
				Set<OpcionDeEvento> opciones = opcionDeEventoService.guardarOpciones(evento, mensajeJSON.opciones)
				JSONArray arrayOpciones = new JSONArray()
				opciones.each { opcion ->
						arrayOpciones.add([id:opcion.id, contenido:opcion.contenido] as JSONObject  )
				}
				mensajeJSON.opciones = arrayOpciones
			}
			log.debug(" ------ Saved voting event '${evento.id}'")
			mensajeJSON.id = evento.id
			mensajeJSON.URL = "${grailsApplication.config.grails.serverURL}/eventoVotacion/${evento.id}"
			mensajeJSON.fechaCreacion = DateUtils.getStringFromDate(evento.dateCreated)
			mensajeJSON.tipo = Tipo.EVENTO_VOTACION
			respuesta = almacenClavesService.generar(evento)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				log.error "saveEvent - ERROR GENERATING EVENT KEYSTRORE- ${respuesta.mensaje}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:respuesta.mensaje, tipo:Tipo.EVENTO_VOTACION_ERROR, evento:evento)
			} 
			mensajeJSON.certCAVotacion = new String(
				CertUtil.fromX509CertToPEM (respuesta.certificado))
			File cadenaCertificacion = grailsApplication.mainContext.getResource(
				grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion).getFile();
			mensajeJSON.cadenaCertificacion = new String(cadenaCertificacion.getBytes())
			
			X509Certificate certUsuX509 = firmante.getCertificate()
			mensajeJSON.usuario = new String(CertUtil.fromX509CertToPEM (certUsuX509))

			String controCenterEventsURL = "${evento.centroControl.serverURL}" +
				"${grailsApplication.config.SistemaVotacion.sufijoURLInicializacionEvento}"

			Header header = new Header ("serverURL", "${grailsApplication.config.grails.serverURL}");
			String fromUser = grailsApplication.config.SistemaVotacion.serverName
			String toUser = evento.centroControl.getNombre()
			String subject = messageSource.getMessage('mime.asunto.EventoVotacionValidado', null, locale)			
			byte[] smimeMessageRespBytes = firmaService.getSignedMimeMessage(
				fromUser, toUser, mensajeJSON.toString(), subject, header)
	
			Respuesta encryptResponse = encryptionService.encryptSMIMEMessage(
					smimeMessageRespBytes, controlCenterCert, locale)
			if(Respuesta.SC_OK != encryptResponse.codigoEstado) {
				evento.estado = Evento.Estado.ACTORES_PENDIENTES_NOTIFICACION
				Evento.withTransaction {
					evento.save()
				}
				log.error "saveEvent - ERROR ENCRYPTING MSG - ${encryptResponse.mensaje}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:encryptResponse.mensaje, , evento:evento, 
					tipo:Tipo.EVENTO_VOTACION_ERROR)
			}
			String contentType = "${grailsApplication.config.pkcs7SignedContentType};" +
				"${grailsApplication.config.pkcs7EncryptedContentType}"
			Respuesta respuestaNotificacion = httpService.sendMessage(
				encryptResponse.messageBytes, contentType, controCenterEventsURL)
			if(Respuesta.SC_OK != respuestaNotificacion.codigoEstado) {
				evento.estado = Evento.Estado.ACTORES_PENDIENTES_NOTIFICACION
				Evento.withTransaction {
					evento.save()
				}
				msg = messageSource.getMessage('controCenterCommunicationErrorMsg', 
					[respuestaNotificacion.mensaje].toArray(), locale)	
				log.error "saveEvent - ERROR NOTIFYING CONTROL CENTER - ${msg}"
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg, tipo:Tipo.EVENTO_VOTACION_ERROR, evento:evento)
			}
			MensajeSMIME mensajeSMIMEResp = new MensajeSMIME(tipo:Tipo.RECIBO,
				smimePadre:mensajeSMIMEReq, evento:evento, valido:true,
				contenido:smimeMessageRespBytes)
			MensajeSMIME.withTransaction {
				mensajeSMIMEResp.save()
			}
			return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento,
					tipo:Tipo.EVENTO_VOTACION, mensajeSMIME:mensajeSMIMEResp)
		} catch(Exception ex) {
			log.error (ex.getMessage(), ex)
			msg = messageSource.getMessage('publishVotingErrorMessage', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR,
				mensaje:msg, tipo:Tipo.EVENTO_VOTACION_ERROR, evento:evento)
		}
    }
    
	public synchronized Respuesta generarCopiaRespaldo (EventoVotacion evento, Locale locale) {
		log.debug("generarCopiaRespaldo - eventoId: ${evento.id}")
		Respuesta respuesta;
		String msg = null
		try {
			Date currentDate = new Date(System.currentTimeMillis())
			if (!evento?.fechaFin.before(currentDate)) { 
				msg = messageSource.getMessage('eventDateNotFinished', null, locale)
				log.error("generarCopiaRespaldo - DATE ERROR  ${msg} - " + 
					"fecha actual '{evento.currentDate}' fecha final evento '${evento.fechaFin}'")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:msg, tipo:Tipo.BACKUP_GEN_ERROR)
			}
			respuesta = representativeService.getAccreditationsBackupForEvent(evento, locale)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				log.error("generarCopiaRespaldo - REPRESENTATIVE DATA GEN ERROR  ${respuesta.mensaje}")
				return respuesta
			}
			def ant = new AntBuilder()
			def basedir = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}" +
				"/${fecha}/${zipNamePrefix}_${evento.id}"
			File baseDirZip = new File("${basedir}.zip")
			if(baseDirZip.exists()) {
				log.debug("generarCopiaRespaldo - backup file already exists")
				return new Respuesta(codigoEstado:Respuesta.SC_OK,file:baseDirZip)
			}
			new File(basedir).mkdirs()
			
			File representativeZipReport = new File("${basedir}/representativesReport.zip")
			ant.copy(file: respuesta.file, toFile: representativeZipReport)
			
			File representativesReportZip = respuesta.file
			List<Voto> votes = null
			//all votes from non representatives
			Voto.withTransaction {
				def criteria = Voto.createCriteria()
				votes = criteria.list {
					createAlias("certificado", "certificado")
					eq("evento", evento)
					eq("estado", Voto.Estado.OK)
					isNull("certificado.usuario")
				}
			}
			List<SolicitudAcceso> solicitudesAcceso = null
			solicitudesAcceso.withTransaction {
				def criteria = solicitudesAcceso.createCriteria()
				solicitudesAcceso = criteria.list {
					createAlias("usuario", "usuario")
					eq("evento", evento)
					eq("estado", SolicitudAcceso.Estado.OK)
					/*or {
						eq("usuario.type", Usuario.Type.USER)
						eq("usuario.type", Usuario.Type.USER_WITH_CANCELLED_REPRESENTATIVE)
						eq("usuario.type", Usuario.Type.EX_REPRESENTATIVE)
					}*/
					not {
						eq("usuario.type", Usuario.Type.REPRESENTATIVE)
					}
				}
			}
			def fecha = DateUtils.getShortStringFromDate(DateUtils.getTodayDate())
			String zipNamePrefix = messageSource.getMessage('votingBackupFileName', null, locale);

			
			def metaInformacionMap = [numVotes:votes.size(),
				numAccessRequest:solicitudesAcceso.size(),
				URL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/${evento.id}",
				type:Tipo.EVENTO_VOTACION.toString(), subject:evento.asunto]
			

			def eventMetaInfJSON = JSON.parse(evento.metaInf)
			
			String metaInformacionJSON = metaInformacionMap as JSON
			eventMetaInfJSON.userVotesReport = metaInformacionJSON
			
			evento.metaInf = eventMetaInfJSON.toString()
			Evento.withTransaction {
				event.save()
			}
			
			String usersDataBaseDir = "${basedir}/users"
			
			File metaInformacionFile = new File("${basedir}/meta.inf")
			metaInformacionFile.write(metaInformacionJSON)
			String votoFileName = messageSource.getMessage('votoFileName', null, locale)
			String solicitudAccesoFileName = messageSource.getMessage('solicitudAccesoFileName', null, locale)
			String votesBaseDir="${usersDataBaseDir}/votes"
			votes.each { voto ->
				MensajeSMIME mensajeSMIME = voto.mensajeSMIME
				File smimeFile = new File("${votesBaseDir}/${votoFileName}_${voto.id}")
				smimeFile.setBytes(mensajeSMIME.contenido)
			}
			String accessRequestBaseDir="${usersDataBaseDir}/accessRequest"
			solicitudesAcceso.each { solicitud ->
				MensajeSMIME mensajeSMIME = solicitud.mensajeSMIME
				File smimeFile = new File("${accessRequestBaseDir}/${solicitudAccesoFileName}_${solicitud.usuario.nif}")
				smimeFile.setBytes(mensajeSMIME.contenido)
			}

			ant.zip(destfile: baseDirZip, basedir: basedir)
			Map datos = [cantidad:votes?.size(), type:Tipo.EVENTO_VOTACION]
			return new Respuesta(codigoEstado:Respuesta.SC_OK, datos:datos, file:baseDirZip)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg =  messageSource.getMessage('error.backupGenericErrorMsg', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR, 
				mensaje:msg, tipo:Tipo.BACKUP_GEN_ERROR)
		}

	}
	
}