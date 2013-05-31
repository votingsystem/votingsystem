package org.sistemavotacion.centrocontrol

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.util.Set;
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.codehaus.groovy.grails.web.json.JSONArray;
import org.codehaus.groovy.grails.web.json.JSONObject;
import org.sistemavotacion.centrocontrol.modelo.*
import org.springframework.context.*
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import org.sistemavotacion.seguridad.cms.*
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.seguridad.*;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import grails.converters.JSON
import org.sistemavotacion.util.*
import org.sistemavotacion.smime.*;
import org.springframework.beans.factory.DisposableBean;
import java.util.Locale;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* */
class EventoVotacionService {
	
    static transactional = false
	
	static scope = "session"
	
	def messageSource
	def subscripcionService
    def grailsApplication  
	def httpService
	def etiquetaService
	def firmaService
	
	List<String> administradoresSistema
	
	Respuesta saveEvent(MensajeSMIME mensajeSMIMEReq, Locale locale) {		
        log.debug("- saveEvent")
		Respuesta respuesta
		SMIMEMessageWrapper smimeMessageReq = mensajeSMIMEReq.getSmimeMessage()
		String msg
		try {
			ControlAcceso controlAcceso = subscripcionService.checkAccessControl(
				smimeMessageReq.getHeader("serverURL")[0])
			if(!controlAcceso) {
				msg = message(code:'accessControlNotFound', args:[serverURL])
				log.debug("- saveEvent - ${msg}")
				return new Respuesta(tipo:Tipo.EVENTO_VOTACION_ERROR,
					mensaje:msg, codigoEstado:Respuesta.SC_ERROR_PETICION)
			}
			def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
			if(!mensajeJSON.certCAVotacion || !mensajeJSON.usuario ||
				!mensajeJSON.id || !mensajeJSON.opciones || !mensajeJSON.URL ||
				!mensajeJSON.centroControl) {
				msg = messageSource.getMessage('documentParamsErrorMsg', null, locale)
				log.error("saveEvent - ERROR - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:msg, tipo:Tipo.EVENTO_VOTACION_ERROR)	
			}
			String serverURL = grailsApplication.config.grails.serverURL
			if (!serverURL.equals(mensajeJSON.centroControl?.serverURL)) {
				msg = messageSource.getMessage('localServerURLErrorMsg', 
					[mensajeJSON.centroControl?.serverURL, serverURL].toArray(), locale)
				log.error("saveEvent - ERROR - ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:msg, tipo:Tipo.EVENTO_VOTACION_ERROR)	
			}
			X509Certificate certCAVotacion = CertUtil.fromPEMToX509Cert(mensajeJSON.certCAVotacion?.bytes)
			byte[] cadenaCertificacion = mensajeJSON.cadenaCertificacion?.getBytes()
			X509Certificate userCert = CertUtil.fromPEMToX509Cert(mensajeJSON.usuario?.bytes)
			Usuario user = Usuario.getUsuario(userCert);
			//Publish request comes with Access Control cert
			respuesta = subscripcionService.checkUser(user, locale)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				log.error("saveEvent - USER CHECK ERROR - ${respuesta.mensaje}")
				return  new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
					mensaje:respuesta.mensaje, tipo:Tipo.EVENTO_VOTACION_ERROR)	
			} 
			user = respuesta.usuario
			def evento = new EventoVotacion(eventoVotacionId:mensajeJSON.id,
				asunto:mensajeJSON.asunto, cadenaCertificacionControlAcceso:cadenaCertificacion,
				contenido:mensajeJSON.contenido, url:mensajeJSON.URL, controlAcceso:controlAcceso,
				usuario:user, fechaInicio:DateUtils.getDateFromString(mensajeJSON.fechaInicio),
				fechaFin:DateUtils.getDateFromString(mensajeJSON.fechaFin))
			respuesta = setEventDatesState(evento, locale)
			if(Respuesta.SC_OK != respuesta.codigoEstado) {
				return  new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje:respuesta.mensaje, tipo:Tipo.EVENTO_VOTACION_ERROR)
			}
			EventoVotacion.withTransaction { evento.save() }
			Certificado certificadoCAVotacion = new Certificado(esRaiz:true,
				actorConIP:controlAcceso, estado:Certificado.Estado.OK,
				tipo:Certificado.Tipo.RAIZ_VOTOS, eventoVotacion:evento,
				contenido:certCAVotacion.getEncoded(),
				numeroSerie:certCAVotacion.getSerialNumber().longValue(),
				validoDesde:certCAVotacion?.getNotBefore(),
				validoHasta:certCAVotacion?.getNotAfter())
			Certificado.withTransaction {certificadoCAVotacion.save()}
			salvarOpciones(evento, mensajeJSON)
			if (mensajeJSON.etiquetas) {
				Set<Etiqueta> etiquetas = etiquetaService.guardarEtiquetas(mensajeJSON.etiquetas)
				evento.setEtiquetaSet(etiquetas)
			}
			evento.save()
			log.debug("saveEvent - SAVED event - '${evento.id}'")
			return new Respuesta(codigoEstado:Respuesta.SC_OK, 
				evento:evento, tipo:Tipo.EVENTO_VOTACION)
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje: messageSource.getMessage('saveDocumentoErrorMsg', null, locale),
				tipo:Tipo.EVENTO_VOTACION_ERROR)
		}
	}
	
    Set<OpcionDeEvento> salvarOpciones(EventoVotacion evento, JSONObject json) {
        log.debug("salvarOpciones - ")
        def opcionesSet = json.opciones.collect { opcionItem ->
                def opcion = new OpcionDeEvento(eventoVotacion:evento,
                        contenido:opcionItem.contenido, opcionDeEventoId:opcionItem.id)
                return opcion.save();
        }
        return opcionesSet
    }

	Respuesta setEventDatesState (EventoVotacion evento, Locale locale) {
		if(evento.fechaInicio.after(evento.fechaFin)) {
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:messageSource.getMessage('dateRangeErrorMsg',
					[evento.fechaInicio, evento.fechaFin].toArray(), locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(evento.fechaFin)) evento.setEstado(
			EventoVotacion.Estado.FINALIZADO)
		if (fecha.after(evento.fechaInicio) && fecha.before(evento.fechaFin))
			evento.setEstado(EventoVotacion.Estado.ACTIVO)
		if (fecha.before(evento.fechaInicio)) evento.setEstado(
			EventoVotacion.Estado.PENDIENTE_COMIENZO)
		log.debug("setEventdatesState - estado ${evento.estado.toString()}")
		return new Respuesta(codigoEstado:Respuesta.SC_OK)
	}
	
	
	Respuesta comprobarFechasEvento (EventoVotacion evento, Locale locale) {
		log.debug("comprobarFechasEvento")
		if(evento.estado && evento.estado == EventoVotacion.Estado.CANCELADO) {
			return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento)
		}
		if(evento.fechaInicio.after(evento.fechaFin)) {
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:messageSource.getMessage(
                'error.fechaInicioAfterFechaFinalMsg', null, locale) )
		}
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(evento.fechaFin) && 
			evento.estado != EventoVotacion.Estado.FINALIZADO) {
			EventoVotacion.withTransaction {
				evento.estado = EventoVotacion.Estado.FINALIZADO
				evento.save()
			}
		} else if(evento.fechaInicio.after(fecha) && 
			evento.estado != EventoVotacion.Estado.PENDIENTE_COMIENZO) {
			EventoVotacion.withTransaction {
				evento.estado = EventoVotacion.Estado.PENDIENTE_COMIENZO
				evento.save()
			}
		} else if(evento.fechaInicio.before(fecha) &&
			evento.fechaFin.after(fecha) && 
			evento.estado != EventoVotacion.Estado.ACTIVO) {
			EventoVotacion.withTransaction {
				evento.estado = EventoVotacion.Estado.ACTIVO
				evento.save()
			}
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK, evento:evento)
	}
	
	//{"operation":"CANCELAR_EVENTO","accessControlURL":"...","eventId":"..","estado":"CANCELADO","UUID":"..."}
	private Respuesta checkCancelEventJSONData(JSONObject cancelDataJSON, Locale locale) {
		int status = Respuesta.SC_ERROR_PETICION
		Tipo tipoRespuesta = Tipo.CANCELAR_EVENTO_ERROR
		String msg
		try {
			Tipo operationType = Tipo.valueOf(cancelDataJSON.operation)
			if (cancelDataJSON.accessControlURL && cancelDataJSON.eventId &&
				cancelDataJSON.estado && (Tipo.CANCELAR_EVENTO == operationType) &&
				((EventoVotacion.Estado.CANCELADO == EventoVotacion.Estado.valueOf(cancelDataJSON.estado)) ||
					(EventoVotacion.Estado.BORRADO_DE_SISTEMA == EventoVotacion.Estado.valueOf(cancelDataJSON.estado)))) {
				status = Respuesta.SC_OK
			} else {
				msg = messageSource.getMessage(
					'evento.datosCancelacionError', null, locale)
			}
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('evento.datosCancelacionError', null, locale)
		}
		if(Respuesta.SC_OK == status) tipoRespuesta = Tipo.CANCELAR_EVENTO
		else log.error("checkCancelEventJSONData - msg: ${msg} - data:${cancelDataJSON.toString()}")
		return new Respuesta(codigoEstado:status, mensaje:msg, tipo:tipoRespuesta)
	}
	
	public Respuesta cancelEvent(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		SMIMEMessageWrapper smimeMessageReq = mensajeSMIMEReq.getSmimeMessage()
		Usuario signer = mensajeSMIMEReq.usuario
		EventoVotacion evento
		String msg
		try {
			log.debug("cancelEvent - mensaje: ${smimeMessageReq.getSignedContent()}")
			def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
			Respuesta respuesta = checkCancelEventJSONData(mensajeJSON, locale)
			if(Respuesta.SC_OK !=  respuesta.codigoEstado) return respuesta
			byte[] certChainBytes
			EventoVotacion.withTransaction {
				evento = EventoVotacion.findWhere(
					eventoVotacionId:mensajeJSON.eventId,
					estado:EventoVotacion.Estado.ACTIVO)
				certChainBytes = evento?.cadenaCertificacionControlAcceso
			}
			if(!evento) {
				msg = messageSource.getMessage('evento.eventoNotFound',
					[mensajeJSON?.eventId].toArray(), locale)
				log.error("cancelEvent - msg: ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					tipo:Tipo.CANCELAR_EVENTO_ERROR, mensaje:msg)
			}
			Collection<X509Certificate> certColl = CertUtil.fromPEMToX509CertCollection(certChainBytes)
			X509Certificate accessControlCert = certColl.iterator().next()
			if(!firmaService.isSignerCertificate(mensajeSMIMEReq.getSigners(), accessControlCert)) {
				msg = messageSource.getMessage('eventCancelacionCertError', null, locale)
				log.error("cancelEvent - msg: ${msg}")
				return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					tipo:Tipo.CANCELAR_EVENTO_ERROR, mensaje:msg, evento:evento)
			}
			//local receipt
			String fromUser = grailsApplication.config.SistemaVotacion.serverName
			String toUser = evento.controlAcceso.serverURL
			String subject = messageSource.getMessage(
				'mime.asunto.cancelEventValidated', null, locale)
			
			SMIMEMessageWrapper smimeMessageResp = firmaService.
					getMultiSignedMimeMessage(fromUser, toUser, smimeMessageReq, subject)
			MensajeSMIME mensajeSMIMEResp = new MensajeSMIME(tipo:Tipo.RECIBO,
				smimePadre:mensajeSMIMEReq, evento:evento, valido:true,
				contenido:smimeMessageResp.getBytes())
			MensajeSMIME.withTransaction {
				mensajeSMIMEResp.save()
			}
			EventoVotacion.withTransaction { 
				evento.estado = EventoVotacion.Estado.valueOf(mensajeJSON.estado)
				evento.dateCanceled = new Date(System.currentTimeMillis());
				evento.save()
			}
			log.debug("cancelEvent - cancelled event with id: ${evento.id}")
			return new Respuesta(codigoEstado:Respuesta.SC_OK,mensaje:msg,
				tipo:Tipo.CANCELAR_EVENTO, mensajeSMIME:mensajeSMIMEResp,
				evento:evento)			
		} catch(Exception ex) {
			log.error(ex.getMessage(), ex)
			msg = messageSource.getMessage('evento.datosCancelacionError', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:msg, evento:evento, tipo:Tipo.CANCELAR_EVENTO_ERROR)
		}
	}

	public Map optenerEventoVotacionJSONMap(EventoVotacion eventoItem) {
		def eventoMap = [id: eventoItem.id,
			fechaCreacion: eventoItem.dateCreated,
			asunto:eventoItem.asunto, contenido:eventoItem.contenido,
			etiquetas:eventoItem.etiquetaSet?.collect {etiquetaItem ->
					return [id:etiquetaItem.id, contenido:etiquetaItem.nombre]},
			duracion:DateUtils.getElapsedTime(eventoItem.getFechaInicio(),
			eventoItem.getFechaFin()),
			URL:eventoItem.url,
			estado:eventoItem.estado.toString(),
			fechaInicio:eventoItem.getFechaInicio(),
			fechaFin:eventoItem.getFechaFin(),
			eventoVotacionId:eventoItem.eventoVotacionId,
			certificadoCA_DeEvento:"${grailsApplication.config.grails.serverURL}/certificado/eventCA?eventAccessControlURL=${eventoItem.url}",
			informacionVotosEnControlAccesoURL:"${eventoItem.controlAcceso.serverURL}/eventoVotacion/${eventoItem.eventoVotacionId}/informacionVotos",
			informacionVotosURL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/votes?eventAccessControlURL=${eventoItem.url}"]
			def controlAccesoMap = [serverURL:eventoItem.controlAcceso.serverURL,
					nombre:eventoItem.controlAcceso.nombre]
			eventoMap.controlAcceso = controlAccesoMap
			if(eventoItem.usuario) eventoMap.usuario = "${eventoItem.usuario?.nombre} ${eventoItem.usuario?.primerApellido}"
			else eventoMap.usuario = null
		eventoMap.opciones = eventoItem.opciones?.collect {opcion ->
				return [id:opcion.id, contenido:opcion.contenido,
				opcionDeEventoId:opcion.opcionDeEventoId]}
		return eventoMap
	}
	
	boolean isUserAdmin(String nif) {
		log.debug("isUserAdmin - nif: ${nif}")
		if(!administradoresSistema) {
			administradoresSistema = Arrays.asList(
			"${grailsApplication.config.SistemaVotacion.adminsDNI}".split(","))
		}
		return administradoresSistema.contains(nif)
	}
}