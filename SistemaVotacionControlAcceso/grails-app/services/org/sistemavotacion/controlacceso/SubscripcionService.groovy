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
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class SubscripcionService {	
		
	static transactional = true
    def grailsApplication
	def messageSource
	def httpService
	
	Respuesta checkUser(Usuario usuario, Locale locale) {
		log.debug "checkUser - user  '${usuario.nif}'"
		String msg
		if(!usuario.nif) {
			msg = messageSource.getMessage('susbcripcion.errorDatosUsuario', null, locale)
			log.error("- checkUser - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:msg, tipo:Tipo.USER_ERROR)
		}
		X509Certificate certificadoUsu = usuario.getCertificate()
		String nifValidado = org.sistemavotacion.util.StringUtils.validarNIF(usuario.nif)
		if(!nifValidado) {
			msg = messageSource.getMessage('susbcripcion.errorNifUsuario', 
				[usuario.nif].toArray(), locale)
			log.error("- checkUser - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:msg, tipo:Tipo.USER_ERROR)
		}
		usuario.nif = nifValidado
		usuario.type = Usuario.Type.USER
		Usuario usuarioDB = Usuario.findWhere(nif:nifValidado.toUpperCase())
		Certificado certificado = null;
		if (!usuarioDB) {
			usuarioDB = usuario.save();
			log.debug "- checkUser - user -> ${usuario.nif} - id '${usuarioDB.id}'"
			if (usuario.getCertificate()) {
				certificado = new Certificado(usuario:usuarioDB,
					contenido:usuario.getCertificate()?.getEncoded(),
					numeroSerie:usuario.getCertificate()?.getSerialNumber()?.longValue(),
					estado:Certificado.Estado.OK, tipo:Certificado.Tipo.USUARIO,
					certificadoAutoridad:usuario.getCertificadoCA(),
					validoDesde:usuario.getCertificate()?.getNotBefore(),
					validoHasta:usuario.getCertificate()?.getNotAfter())
				certificado.save();
				log.debug "- checkUser - user cert -> id '${certificado.id}'"
			}
		} else {
			certificado = Certificado.findWhere(
				usuario:usuarioDB, estado:Certificado.Estado.OK)
			if (!certificado?.numeroSerie == certificadoUsu.getSerialNumber()?.longValue()) {
				certificado.estado = Certificado.Estado.ANULADO
				certificado.save()
				log.debug "- checkUser - CANCELLED user cert id '${certificado.id}'"
				certificado = new Certificado(usuario:usuarioDB,
					contenido:certificadoUsu?.getEncoded(), estado:Certificado.Estado.OK,
					numeroSerie:certificadoUsu?.getSerialNumber()?.longValue(),
					certificadoAutoridad:usuario.getCertificadoCA(),
					validoDesde:usuario.getCertificate()?.getNotBefore(),
					validoHasta:usuario.getCertificate()?.getNotAfter())
				certificado.save();
				log.debug "- checkUser - UPDATED user cert -> id '${certificado.id}'"
			}
			usuarioDB.setCertificadoCA(usuario.getCertificadoCA())
			usuarioDB.setCertificate(usuario.getCertificate())
			usuarioDB.setTimeStampToken(usuario.getTimeStampToken())
		}
		return new Respuesta(codigoEstado:Respuesta.SC_OK, usuario:usuarioDB, certificadoDB:certificado)
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
			usuario = new Usuario(nif:nifValidado, email:email, telefono:telefono,
				type:Usuario.Type.USER).save()
		}
		Dispositivo dispositivo = Dispositivo.findWhere(deviceId:deviceId)
		if (!dispositivo || (dispositivo.usuario.id != usuario.id)) dispositivo = new Dispositivo(usuario:usuario, telefono:telefono, email:email, 
			deviceId:deviceId).save()
		return new Respuesta(codigoEstado:Respuesta.SC_OK, usuario:usuario, dispositivo:dispositivo)
	}
    
	Respuesta checkControlCenter(String serverURL, Locale locale) {
        log.debug "checkControlCenter - serverURL:${serverURL}"
		String msg = null
        serverURL = StringUtils.checkURL(serverURL)
		String urlInfoCentroControl = "${serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLInfoServidor}"
		CentroControl centroControl = null
		CentroControl centroControlDB = CentroControl.findWhere(serverURL:serverURL)
		Certificado controlCenterCert = null
		X509Certificate x509controlCenterCertDB = null
		byte[] certChainBytes = null
		if(centroControlDB) {
			controlCenterCert = Certificado.findWhere(actorConIP:centroControlDB,
				estado:Certificado.Estado.OK)
			if(controlCenterCert) {
				ByteArrayInputStream bais = new ByteArrayInputStream(controlCenterCert.contenido)
				x509controlCenterCertDB = CertUtil.loadCertificateFromStream (bais) 
			}
		}
		Respuesta respuesta = httpService.getInfo(urlInfoCentroControl, null)
		if (Respuesta.SC_OK == respuesta.codigoEstado) {
			ActorConIP actorConIP = ActorConIP.parse(respuesta.mensaje)

			if (!Tipo.CENTRO_CONTROL.equals(actorConIP.tipoServidor)) {
				msg = message(code: 'susbcripcion.actorNoCentroControl',
					args:[actorConIP.serverURL])
				log.error("checkControlCenter - ERROR - ${msg}")
				return new Respuesta(mensaje:msg, codigoEstado:Respuesta.SC_ERROR_PETICION)
			}
			centroControl = new CentroControl(nombre:actorConIP.nombre,
						serverURL:actorConIP.serverURL, 
						cadenaCertificacion:actorConIP.cadenaCertificacion,
						estado:actorConIP.estado)
			if(!centroControlDB) {
				centroControlDB = centroControl.save()
				log.debug("checkControlCenter - SAVED ACTOR_CON_IP  ${centroControlDB.id}")
			}
			certChainBytes = centroControl.cadenaCertificacion
			X509Certificate x509controlCenterCert = CertUtil.
				fromPEMToX509CertCollection(certChainBytes).iterator().next()
			if(controlCenterCert) { 
				if(x509controlCenterCert.getSerialNumber().longValue() !=
					x509controlCenterCertDB.getSerialNumber().longValue()) {
					controlCenterCert.estado = Certificado.Estado.ANULADO
					x509controlCenterCertDB = null
					controlCenterCert.save()
					log.debug("checkControlCenter - CANCELLING Certificado  ${controlCenterCert.id}")
				}
			} 
			if(!x509controlCenterCertDB) {		
				controlCenterCert = new Certificado(
					actorConIP:centroControlDB,
					contenido:x509controlCenterCert?.getEncoded(),
					numeroSerie:x509controlCenterCert?.getSerialNumber()?.longValue(),
					estado:Certificado.Estado.OK, tipo:Certificado.Tipo.ACTOR_CON_IP,
					validoDesde:x509controlCenterCert?.getNotBefore(),
					validoHasta:x509controlCenterCert?.getNotAfter())
				controlCenterCert.save();
				log.debug("checkControlCenter - added Certificado  ${controlCenterCert.id}")
			}
			centroControlDB.certificadoX509 = x509controlCenterCert
			centroControlDB.cadenaCertificacion = certChainBytes
			return new Respuesta(codigoEstado:Respuesta.SC_OK,
				mensaje:msg,  centroControl:centroControlDB)
		} else {
			msg = messageSource.getMessage('http.errorConectandoCentroControl', null, locale)
			log.error("checkControlCenter - ERROR CONECTANDO CONTROL CENTER - ${msg}")
			return new Respuesta(mensaje:msg, codigoEstado:Respuesta.SC_ERROR_PETICION)
		}
	}


	public Respuesta asociarCentroControl(MensajeSMIME mensajeSMIMEReq, Locale locale) {
		log.debug("asociarCentroControl")
		SMIMEMessageWrapper smimeMessageReq = mensajeSMIMEReq.getSmimeMessage()
		Usuario firmante = mensajeSMIMEReq.getUsuario()
		String msg = null;
		String serverURL = null;
		try {
			def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
			if (!mensajeJSON.serverURL || !mensajeJSON.operation ||
				(Tipo.ASOCIAR_CENTRO_CONTROL != Tipo.valueOf(mensajeJSON.operation))) {
				msg = messageSource.getMessage('documentWithErrorsMsg',null, locale)
				log.error("asociarCentroControl - ERROR DATA - ${msg} -> ${smimeMessageReq.getSignedContent()}")
				return new Respuesta(mensaje:msg, tipo:Tipo.ASOCIAR_CENTRO_CONTROL_ERROR, 
					codigoEstado:Respuesta.SC_ERROR_PETICION)
			} else {
				serverURL = StringUtils.checkURL(mensajeJSON.serverURL)
				while (serverURL.endsWith("/")){
					serverURL = serverURL.substring(0, serverURL.length()-1)
				}
				CentroControl centroControl = CentroControl.findWhere(serverURL:serverURL)
				if (centroControl) {
					msg = messageSource.getMessage('susbcripcion.centroControlYaAsociado',
						[centroControl.nombre].toArray(), locale)
					log.error("asociarCentroControl- CONTROL CENTER ALREADY ASSOCIATED - ${msg}")
					return new Respuesta(mensaje:msg,
						codigoEstado:Respuesta.SC_ERROR_PETICION,
						tipo:Tipo.ASOCIAR_CENTRO_CONTROL_ERROR)
				} else {
					Respuesta respuesta = checkControlCenter(serverURL, locale)
					if (Respuesta.SC_OK != respuesta.codigoEstado) {
						log.error("asociarCentroControl- ERROR CHECKING CONTROL CENTER - ${respuesta.mensaje}")
						return new Respuesta(mensaje:respuesta.mensaje,
							codigoEstado:Respuesta.SC_ERROR_PETICION,
							tipo:Tipo.ASOCIAR_CENTRO_CONTROL_ERROR)
					} else {
						msg =  messageSource.getMessage('susbcripcion.centroControlAsociado',
							[respuesta.centroControl.nombre].toArray(), locale)
						log.debug(msg)
						return new Respuesta(mensaje: msg,
							centroControl:respuesta.centroControl,
							codigoEstado:Respuesta.SC_OK,
							tipo:Tipo.ASOCIAR_CENTRO_CONTROL)
					}
				}
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION,
				mensaje: messageSource.getMessage('error.errorConexionActor',
					[serverURL, ex.getMessage()].toArray(), locale),
				tipo:Tipo.ASOCIAR_CENTRO_CONTROL_ERROR)
		}
	}
}