package org.votingsystem.accesscontrol.service

import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.util.*
import org.votingsystem.groovy.util.*;

import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import java.security.cert.X509Certificate;

import grails.converters.JSON

import org.codehaus.groovy.grails.web.json.*
import org.votingsystem.signature.smime.*

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
	
	ResponseVS checkUser(Usuario usuario, Locale locale) {
		log.debug "checkUser - user  '${usuario.nif}'"
		String msg
		if(!usuario.nif) {
			msg = messageSource.getMessage('susbcripcion.errorDatosUsuario', null, locale)
			log.error("- checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, 
				message:msg, type:TypeVS.USER_ERROR)
		}
		X509Certificate certificadoUsu = usuario.getCertificate()
		String nifValidado = org.votingsystem.util.StringUtils.validarNIF(usuario.nif)
		if(!nifValidado) {
			msg = messageSource.getMessage('susbcripcion.errorNifUsuario', 
				[usuario.nif].toArray(), locale)
			log.error("- checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, 
				message:msg, type:TypeVS.USER_ERROR)
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
					estado:Certificado.Estado.OK, type:Certificado.Type.USUARIO,
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
		return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:usuarioDB, certificateVS:certificado)
	}
	
	ResponseVS comprobarDispositivo(String nif, String telefono, String email, 
			String deviceId, Locale locale) {
		log.debug "comprobarDispositivo - nif:${nif} - telefono:${telefono} - email:${email} - deviceId:${deviceId}"
		if(!nif || !deviceId) {
			log.debug "Sin datos"
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, message:
				messageSource.getMessage('error.requestWithoutData', null, locale))
		}
		String nifValidado = org.votingsystem.util.StringUtils.validarNIF(nif)
		if(!nifValidado) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, message:
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
		return new ResponseVS(statusCode:ResponseVS.SC_OK, usuario:usuario, dispositivo:dispositivo)
	}
    
	ResponseVS checkControlCenter(String serverURL, Locale locale) {
        log.debug "checkControlCenter - serverURL:${serverURL}"
		String msg = null
        serverURL = StringUtils.checkURL(serverURL)
		String urlInfoCentroControl = "${serverURL}/infoServidor"
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
		ResponseVS respuesta = httpService.getInfo(urlInfoCentroControl, null)
		if (ResponseVS.SC_OK == respuesta.statusCode) {
			ActorConIP actorConIP = ActorConIP.parse(respuesta.message)

			if (!TypeVS.CENTRO_CONTROL.equals(actorConIP.serverType)) {
				msg = message(code: 'susbcripcion.actorNoCentroControl',
					args:[actorConIP.serverURL])
				log.error("checkControlCenter - ERROR - ${msg}")
				return new ResponseVS(message:msg, statusCode:ResponseVS.SC_ERROR_PETICION)
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
					estado:Certificado.Estado.OK, type:Certificado.Type.ACTOR_CON_IP,
					validoDesde:x509controlCenterCert?.getNotBefore(),
					validoHasta:x509controlCenterCert?.getNotAfter())
				controlCenterCert.save();
				log.debug("checkControlCenter - added Certificado  ${controlCenterCert.id}")
			}
			centroControlDB.certificadoX509 = x509controlCenterCert
			centroControlDB.cadenaCertificacion = certChainBytes
			return new ResponseVS(statusCode:ResponseVS.SC_OK,
				message:msg,  centroControl:centroControlDB)
		} else {
			msg = messageSource.getMessage('http.errorConectandoCentroControl', null, locale)
			log.error("checkControlCenter - ERROR CONECTANDO CONTROL CENTER - ${msg}")
			return new ResponseVS(message:msg, statusCode:ResponseVS.SC_ERROR_PETICION)
		}
	}


	public ResponseVS asociarCentroControl(MessageSMIME messageSMIMEReq, Locale locale) {
		log.debug("asociarCentroControl")
		SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
		Usuario firmante = messageSMIMEReq.getUsuario()
		String msg = null;
		String serverURL = null;
		try {
			def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
			if (!messageJSON.serverURL || !messageJSON.operation ||
				(TypeVS.ASOCIAR_CENTRO_CONTROL != TypeVS.valueOf(messageJSON.operation))) {
				msg = messageSource.getMessage('documentWithErrorsMsg',null, locale)
				log.error("asociarCentroControl - ERROR DATA - ${msg} -> ${smimeMessageReq.getSignedContent()}")
				return new ResponseVS(message:msg, type:TypeVS.ASOCIAR_CENTRO_CONTROL_ERROR, 
					statusCode:ResponseVS.SC_ERROR_PETICION)
			} else {
				serverURL = StringUtils.checkURL(messageJSON.serverURL)
				while (serverURL.endsWith("/")){
					serverURL = serverURL.substring(0, serverURL.length()-1)
				}
				CentroControl centroControl = CentroControl.findWhere(serverURL:serverURL)
				if (centroControl) {
					msg = messageSource.getMessage('susbcripcion.centroControlYaAsociado',
						[centroControl.nombre].toArray(), locale)
					log.error("asociarCentroControl- CONTROL CENTER ALREADY ASSOCIATED - ${msg}")
					return new ResponseVS(message:msg,
						statusCode:ResponseVS.SC_ERROR_PETICION,
						type:TypeVS.ASOCIAR_CENTRO_CONTROL_ERROR)
				} else {
					ResponseVS respuesta = checkControlCenter(serverURL, locale)
					if (ResponseVS.SC_OK != respuesta.statusCode) {
						log.error("asociarCentroControl- ERROR CHECKING CONTROL CENTER - ${respuesta.message}")
						return new ResponseVS(message:respuesta.message,
							statusCode:ResponseVS.SC_ERROR_PETICION,
							type:TypeVS.ASOCIAR_CENTRO_CONTROL_ERROR)
					} else {
						msg =  messageSource.getMessage('susbcripcion.centroControlAsociado',
							[respuesta.centroControl.nombre].toArray(), locale)
						log.debug(msg)
						return new ResponseVS(message: msg,
							centroControl:respuesta.centroControl,
							statusCode:ResponseVS.SC_OK,
							type:TypeVS.ASOCIAR_CENTRO_CONTROL)
					}
				}
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION,
				message: messageSource.getMessage('error.errorConexionActor',
					[serverURL, ex.getMessage()].toArray(), locale),
				type:TypeVS.ASOCIAR_CENTRO_CONTROL_ERROR)
		}
	}
}