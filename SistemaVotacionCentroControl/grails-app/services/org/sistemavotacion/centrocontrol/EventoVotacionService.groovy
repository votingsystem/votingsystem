package org.sistemavotacion.centrocontrol

import java.io.ByteArrayInputStream;
import java.security.cert.X509Certificate;
import java.util.Set;
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
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
* */
class EventoVotacionService {
	
    static transactional = false
	
	static scope = "session"
	
	def messageSource
	def subscripcionService
    def grailsApplication  
	def httpService
	def etiquetaService
	
	List<String> administradoresSistema
	
	Respuesta salvarEvento(SMIMEMessageWrapper smimeMessage, 
		ControlAcceso controlAcceso, Locale locale) {		
		log.debug("salvarEvento - mensaje: ${smimeMessage.getSignedContent()}")
		def Tipo tipoMensaje
		Respuesta respuesta
		if (smimeMessage.isValidSignature) tipoMensaje = Tipo.EVENTO_VOTACION
		else tipoMensaje = Tipo.EVENTO_VOTACION_ERROR
		def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
		MensajeSMIME mensajeMime = new MensajeSMIME(tipo:tipoMensaje,
			valido:smimeMessage.isValidSignature, 
			contenido:smimeMessage.getBytes())
		MensajeSMIME.withTransaction {mensajeMime.save();}
		String urlEvento = "${controlAcceso.serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLEventoVotacion}${mensajeJSON.id}"
		X509Certificate certCAVotacion
		if(mensajeJSON.certCAVotacion)
			certCAVotacion = CertUtil.fromPEMToX509Cert(mensajeJSON.certCAVotacion?.bytes)
			
		byte[] cadenaCertificacion = mensajeJSON.cadenaCertificacion?.getBytes()

		Usuario usuario
		if(mensajeJSON.usuario) {
			X509Certificate cert = CertUtil.fromPEMToX509Cert(mensajeJSON.usuario?.bytes)
			usuario = Usuario.getUsuario(cert);
			Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(usuario, locale)
			if(Respuesta.SC_OK != respuestaUsuario.codigoEstado) return respuestaUsuario
		}
		def evento = new EventoVotacion(tipo:tipoMensaje, eventoVotacionId:mensajeJSON.id,
			asunto:mensajeJSON.asunto, cadenaCertificacionControlAcceso:cadenaCertificacion,
			contenido:mensajeJSON.contenido, mensajeMime:mensajeMime, 
			url:urlEvento, controlAcceso:controlAcceso, usuario:usuario,
			fechaInicio:DateUtils.getDateFromString(mensajeJSON.fechaInicio),
			fechaFin:DateUtils.getDateFromString(mensajeJSON.fechaFin))
		evento.estado = obtenerEstadoEvento(evento)
		EventoVotacion.withTransaction { evento.save() }
		Certificado certificadoCAVotacion = new Certificado(esRaiz:true,
			actorConIP:controlAcceso, estado:Certificado.Estado.OK,
			tipo:Certificado.Tipo.RAIZ_VOTOS, eventoVotacion:evento,
			contenido:certCAVotacion.getEncoded(), 
			numeroSerie:certCAVotacion.getSerialNumber().longValue(),
			validoDesde:certCAVotacion?.getNotBefore(),
			validoHasta:certCAVotacion?.getNotAfter())
		Certificado.withTransaction {certificadoCAVotacion.save()}
		if (mensajeJSON.centroControl) {
			String serverURL = grailsApplication.config.grails.serverURL
			if (!serverURL.equals(mensajeJSON.centroControl?.serverURL)) {
				log.error("Received: '${mensajeJSON.centroControl?.serverURL}' - expected: '${serverURL}'")
				throw new Exception ("Centro de control equivocado")
			}
		}
		if (mensajeJSON.opciones) salvarOpciones(evento, mensajeJSON)
		if (mensajeJSON.etiquetas) {
			Set<Etiqueta> etiquetas = etiquetaService.guardarEtiquetas(mensajeJSON.etiquetas)
			evento.setEtiquetaSet(etiquetas)
		}
		evento.save()
		return new Respuesta(codigoEstado:Respuesta.SC_OK, fecha:DateUtils.getTodayDate(),
				tipo:Tipo.EVENTO_INICIALIZADO,	mensajeSMIME:mensajeMime,
				evento:evento, smimeMessage:smimeMessage)
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

	EventoVotacion.Estado obtenerEstadoEvento (EventoVotacion evento) {
		EventoVotacion.Estado estado
		Date fecha = DateUtils.getTodayDate()
		if (fecha.after(evento.fechaFin)) estado = EventoVotacion.Estado.FINALIZADO
		if (fecha.after(evento.fechaInicio) && fecha.before(evento.fechaFin))
			estado = EventoVotacion.Estado.ACTIVO
		if (fecha.before(evento.fechaInicio))  estado = EventoVotacion.Estado.PENDIENTE_COMIENZO
		return estado
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
		return new Respuesta(codigoEstado:200, evento:evento)
	}
	
	public Respuesta cancelarEvento(SMIMEMessageWrapper smimeMessage, Locale locale) {
		log.debug("cancelarEvento - mensaje: ${smimeMessage.getSignedContent()}")
		def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
		Respuesta respuestaUsuario = subscripcionService.comprobarUsuario(smimeMessage, locale)
		Usuario usuario = respuestaUsuario.usuario
		MensajeSMIME mensajeSMIME
		EventoVotacion evento
		Respuesta respuesta
		if (mensajeJSON.eventoId &&
				mensajeJSON.estado && ((EventoVotacion.Estado.CANCELADO ==
					EventoVotacion.Estado.valueOf(mensajeJSON.estado)) ||
					(EventoVotacion.Estado.BORRADO_DE_SISTEMA ==
					EventoVotacion.Estado.valueOf(mensajeJSON.estado)))) {
				EventoVotacion.withTransaction {
					evento = EventoVotacion.findWhere(id:mensajeJSON.eventoId?.longValue(),
						estado:EventoVotacion.Estado.ACTIVO)
					if (evento) {
						log.debug(" ----- cancelarEvento - nif firmante: ${smimeMessage.firmante?.nif}")
						 if(evento.usuario?.nif.equals(smimeMessage.firmante?.nif) ||
							isUserAdmin(smimeMessage.firmante?.nif)){
								log.debug("Usuario con privilegios para cancelar evento")
								evento.estado = EventoVotacion.Estado.valueOf(mensajeJSON.estado)
								evento.dateCanceled = new Date(System.currentTimeMillis());
								evento.save()
								mensajeSMIME = new MensajeSMIME(usuario:usuario,
									tipo:Tipo.getTipoEnFuncionEstado(evento.estado),
									valido:smimeMessage.isValidSignature(),
									contenido:smimeMessage.getBytes(), evento:evento)
								mensajeSMIME.save();
								String mensajeRespuesta;
								switch(evento.estado) {
									case EventoVotacion.Estado.CANCELADO:
										 mensajeRespuesta = messageSource.getMessage('evento.cancelado',
											 [mensajeJSON?.eventoId].toArray(), locale)
										 break;
									 case EventoVotacion.Estado.BORRADO_DE_SISTEMA:
										 mensajeRespuesta = messageSource.getMessage('evento.borrado',
											 [mensajeJSON?.eventoId].toArray(), locale)
										 break;
									 
								}
								respuesta = new Respuesta(
									codigoEstado:Respuesta.SC_OK, mensaje: mensajeRespuesta)
						 } else {
							 respuesta = new Respuesta(codigoEstado:400, mensaje: messageSource.getMessage(
								 'csr.usuarioNoAutorizado', 
								 [smimeMessage.firmante?.nif].toArray(), locale))
						 }
					}
					mensajeSMIME = new MensajeSMIME(tipo:Tipo.EVENTO_CANCELADO_ERROR,
						usuario:usuario, valido:smimeMessage.isValidSignature(),
						contenido:smimeMessage.getBytes(), evento:evento)
					mensajeSMIME.save();
				}
		}
		if(!respuesta) {
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_EJECUCION, 
				mensaje:messageSource.getMessage('evento.datosCancelacionError', 
				[mensajeJSON?.eventoId,	mensajeJSON.estado].toArray(), locale))
		}
		return respuesta
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
			certificadoCA_DeEvento:"${grailsApplication.config.grails.serverURL}/certificado/certificadoCA_DeEvento?controlAccesoId=${eventoItem.controlAcceso?.id}&idEvento=${eventoItem.id}",
			informacionVotosEnControlAccesoURL:"${eventoItem.controlAcceso.serverURL}/evento/informacionVotos?id=${eventoItem.eventoVotacionId}",
			informacionVotosURL:"${grailsApplication.config.grails.serverURL}/eventoVotacion/obtenerVotos?eventoVotacionId=${eventoItem.eventoVotacionId}&controlAccesoServerURL=${eventoItem.controlAcceso.serverURL}"]
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