package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.utils.*;
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import java.security.cert.X509Certificate;
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.*
import org.sistemavotacion.smime.*
import java.util.Locale;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class SubscripcionService {	
		
	static transactional = true
    def grailsApplication
	def messageSource
	def httpService
	
	Respuesta guardarUsuario(Usuario usuario, Locale locale) {
		log.debug "guardarUsuario - usuario: ${usuario.nif} - CertificadoCA: ${usuario.getCertificadoCA()?.id}"
		if(!usuario.nif) {
			String mensajeError = messageSource.getMessage('susbcripcion.errorDatosUsuario', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:mensajeError)
		}
		X509Certificate certificadoUsu = usuario.getCertificate()
		String nifValidado = org.sistemavotacion.util.StringUtils.validarNIF(usuario.nif)
		if(!nifValidado) {
			String mensajeError = messageSource.getMessage(
				'susbcripcion.errorNifUsuario', [usuario.nif].toArray(), locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:mensajeError)
		}
		usuario.nif = nifValidado
		def usuarioDB = Usuario.findWhere(nif:nifValidado.toUpperCase())
		Certificado certificado = null;
		if (!usuarioDB) {
			usuarioDB = usuario.save();
			if (usuario.getCertificate()) {
				certificado = new Certificado(usuario:usuario,
					contenido:usuario.getCertificate()?.getEncoded(),
					numeroSerie:usuario.getCertificate()?.getSerialNumber()?.longValue(),
					estado:Certificado.Estado.OK, tipo:Certificado.Tipo.USUARIO,
					certificadoAutoridad:usuario.getCertificadoCA(),
					validoDesde:usuario.getCertificate()?.getNotBefore(),
					validoHasta:usuario.getCertificate()?.getNotAfter())
				certificado.save();
			}
		} else {
			certificado = Certificado.findWhere(
				usuario:usuarioDB, estado:Certificado.Estado.OK)
			if (!certificado?.numeroSerie == certificadoUsu.getSerialNumber()?.longValue()) {
				certificado.estado = Certificado.Estado.ANULADO
				certificado.save()
				certificado = new Certificado(usuario:usuarioDB,
					contenido:certificadoUsu?.getEncoded(), estado:Certificado.Estado.OK,
					numeroSerie:certificadoUsu?.getSerialNumber()?.longValue(),
					certificadoAutoridad:usuario.getCertificadoCA(),
					validoDesde:usuario.getCertificate()?.getNotBefore(),
					validoHasta:usuario.getCertificate()?.getNotAfter())
				certificado.save();
			}
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK, usuario:usuarioDB, certificadoDB:certificado)
	}
        
	Respuesta comprobarUsuario(SMIMEMessageWrapper smimeMessage, Locale locale) {
		def nif = smimeMessage.getFirmante()?.nif
		log.debug "comprobarUsuario - ${nif}"
		if(!nif) {
			String mensajeError = messageSource.getMessage('susbcripcion.errorDatosUsuario', null, locale)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:mensajeError)
		}
		def usuario = Usuario.findWhere(nif:nif.toUpperCase())
		if (usuario) return new Respuesta(codigoEstado:Respuesta.SC_OK,usuario:usuario)
		return guardarUsuario(smimeMessage.getFirmante(), locale)
	}
	
	Respuesta comprobarDispositivo(String nif, String telefono, String email, 
			String deviceId, Locale locale) {
		log.debug "comprobarDispositivo - nif:${nif} - telefono:${telefono} - email:${email} - deviceId:${deviceId}"
		if(!nif || !deviceId) {
			log.debug "Sin datos"
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:
				messageSource.getMessage('error.requestWithoutData', null, locale))
		}
		String nifValidado = org.sistemavotacion.util.StringUtils.validarNIF(nif)
		if(!nifValidado) {
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, mensaje:
				messageSource.getMessage('error.errorNif', [nif].toArray(), locale))
		}
		Usuario usuario = Usuario.findWhere(nif:nifValidado)
		if (!usuario) {
			usuario = new Usuario(nif:nifValidado, email:email, telefono:telefono).save()
		}
		Dispositivo dispositivo = Dispositivo.findWhere(deviceId:deviceId)
		if (!dispositivo || (dispositivo.usuario.id != usuario.id)) dispositivo = new Dispositivo(usuario:usuario, telefono:telefono, email:email, 
			deviceId:deviceId).save()
		return new Respuesta(codigoEstado:Respuesta.SC_OK, usuario:usuario, dispositivo:dispositivo)
	}
    
	CentroControl comprobarCentroControl(String serverURL) {
        log.debug "comprobarCentroControl - serverURL:${serverURL}"
        serverURL = StringUtils.checkURL(serverURL)
        CentroControl centroControl = CentroControl.findWhere(serverURL:serverURL)
        if (!centroControl) {
            String urlInfoCentroControl = "${serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLInfoServidor}"
			Respuesta respuesta = httpService.obtenerInfoActorConIP(urlInfoCentroControl, new CentroControl())
			if (Respuesta.SC_OK == respuesta.codigoEstado) {
				centroControl = respuesta.actorConIP
				centroControl.save()
			} else return null
        } else return centroControl
	}
	
	public ActorConIP obtenerInfoActorConIP (String urlInfo, ActorConIP actorConIP) {
		log.debug "obtenerInfoActorConIP - urlInfo: ${urlInfo}"
		def infoActorHTTPBuilder = new HTTPBuilder(urlInfo);
		infoActorHTTPBuilder.request(Method.GET) { req ->
			response.'200' = { resp, reader ->
				log.debug "***** OK: ${resp.statusLine}"
				actorConIP.nombre = reader.nombre
				actorConIP.serverURL = reader.serverURL
				actorConIP.estado = ActorConIP.Estado.valueOf(reader.estado)
				actorConIP.tipoServidor = Tipo.valueOf(reader.tipoServidor)
			}
			response.failure = { resp ->
				log.error "***** ERROR: ${resp.statusLine}"
				return null
			}
		}
		return actorConIP
	}

	public Respuesta asociarCentroControl(SMIMEMessageWrapper smimeMessage, Locale locale) {
		log.debug("asociarCentroControl - mensaje: ${smimeMessage.getSignedContent()}")
		def mensajeJSON = JSON.parse(smimeMessage.getSignedContent())
		Respuesta respuesta
		Tipo tipoMensaje
		if (mensajeJSON.serverURL) {
			String serverURL = StringUtils.checkURL(mensajeJSON.serverURL)
			CentroControl actorConIP = CentroControl.findWhere(serverURL:serverURL)
			if (actorConIP) {
				tipoMensaje = Tipo.SOLICITUD_ASOCIACION_CON_ACTOR_REPETIDO
				respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
					mensaje: messageSource.getMessage('susbcripcion.centroControlYaAsociado', [actorConIP.nombre].toArray(), locale))
			} else {
				tipoMensaje = Tipo.SOLICITUD_ASOCIACION
				def urlInfoCentroControl = "${serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLInfoServidor}"
				try {
					respuesta = httpService.obtenerInfoActorConIP(urlInfoCentroControl, new CentroControl())
					if (Respuesta.SC_OK == respuesta.codigoEstado) {
						actorConIP = respuesta.actorConIP
						if (Tipo.CENTRO_CONTROL.equals(actorConIP.tipoServidor)) {
							actorConIP.save()
							respuesta = new Respuesta(codigoEstado:Respuesta.SC_OK,
								mensaje: messageSource.getMessage('susbcripcion.centroControlAsociado', [actorConIP.nombre].toArray(), locale))
						} else {
							tipoMensaje = Tipo.SOLICITUD_ASOCIACION_CON_ERRORES
							respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
								mensaje:message(code: 'susbcripcion.actorNoCentroControl',
									args:[actorConIP.serverURL]))
						}
					} else {
						tipoMensaje = Tipo.ERROR_CONEXION_CON_ACTOR
						respuesta.mensaje = message(code: 'error.errorConexionActor',
							args:["Centro de Control", serverURL, respuesta.mensaje])
					}
				} catch (ConnectException ex) {
					log.error(ex.getMessage(), ex)
					return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, tipo: Tipo.ERROR_CONEXION_CON_ACTOR,
						mensaje: messageSource.getMessage('error.errorConexionActor',
							["Centro de Control", serverURL, ex.getMessage()].toArray(), locale))
				}
			}
		} else {
			tipoMensaje = Tipo.SOLICITUD_ASOCIACION_CON_ERRORES
			respuesta = new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje:messageSource.getMessage('error.PeticionIncorrectaHTML', 
					["${grailsApplication.config.grails.serverURL}/${params.controller}"].toArray(), locale))
		}
		Respuesta respuestaUsuario = comprobarUsuario(smimeMessage, locale)
		Usuario usuario = respuestaUsuario.usuario
		MensajeSMIME mensajeSMIME = new MensajeSMIME(tipo:tipoMensaje,
				usuario:usuario, valido:smimeMessage.isValidSignature(),
				contenido:smimeMessage.getBytes())
		MensajeSMIME.withTransaction {
			mensajeSMIME.save();
		}
		return respuesta;
	}
}