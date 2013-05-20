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
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
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
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION,
				mensaje:msg, tipo:Tipo.EVENTO_VOTACION_ERROR, evento:evento)
		}
    }
    
	public Respuesta generarCopiaRespaldo (EventoVotacion evento, Locale locale) {
		log.debug("generarCopiaRespaldo - eventoId: ${evento.id}")
		Respuesta respuesta;
		if (evento) {
			def votosContabilizados = Voto.findAllByEventoVotacionAndEstado(evento, Voto.Estado.OK)
			def solicitudesAcceso =  SolicitudAcceso.findAllByEventoVotacionAndEstado(evento, SolicitudAcceso.Estado.OK)
			def fecha = DateUtils.getShortStringFromDate(DateUtils.getTodayDate())
			String zipNamePrefix = messageSource.getMessage('votingBackupFileName', null, locale);
			def basedir = "${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}" + 
				"/${fecha}/${zipNamePrefix}_${evento.id}"
			new File(basedir).mkdirs()
			int i = 0
			def metaInformacionMap = [numeroVotos:votosContabilizados.size(),
				solicitudesAcceso:solicitudesAcceso.size(),
				URL:"${grailsApplication.config.grails.serverURL}/evento/${evento.id}",
				tipoEvento:Tipo.EVENTO_VOTACION.toString(), asunto:evento.asunto]
			String metaInformacionJSON = metaInformacionMap as JSON
			File metaInformacionFile = new File("${basedir}/meta.inf")
			metaInformacionFile.write(metaInformacionJSON)
			String votoFileName = messageSource.getMessage('votoFileName', null, locale)
			String solicitudAccesoFileName = messageSource.getMessage('solicitudAccesoFileName', null, locale)
			votosContabilizados.each { voto ->
				MensajeSMIME mensajeSMIME = voto.mensajeSMIME
				ByteArrayInputStream bais = new ByteArrayInputStream(mensajeSMIME.contenido);
				MimeMessage msg = new MimeMessage(null, bais);
				File smimeFile = new File("${basedir}/${votoFileName}_${i}")
				FileOutputStream fos = new FileOutputStream(smimeFile);
				msg.writeTo(fos);
				fos.close();
				i++;
			}
			solicitudesAcceso.each { solicitud ->
				MensajeSMIME mensajeSMIME = solicitud.mensajeSMIME
				ByteArrayInputStream bais = new ByteArrayInputStream(mensajeSMIME.contenido);
				MimeMessage msg = new MimeMessage(null, bais);
				File smimeFile = new File("${basedir}/${solicitudAccesoFileName}_${i}")
				FileOutputStream fos = new FileOutputStream(smimeFile);
				msg.writeTo(fos);
				fos.close();
				i++;
			}
			def ant = new AntBuilder()
			ant.zip(destfile: "${basedir}.zip", basedir: basedir)
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK, cantidad:votosContabilizados?.size(), file:new File("${basedir}.zip"))
		} else respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:messageSource.getMessage(
			'eventNotFound', [evento.id].toArray(), locale))
		return respuesta
	}
	
}

