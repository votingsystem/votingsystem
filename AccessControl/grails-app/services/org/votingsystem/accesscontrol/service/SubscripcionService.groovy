package org.votingsystem.accesscontrol.service

import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS;
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
	
	ResponseVS checkUser(UserVS userVS, Locale locale) {
		log.debug "checkUser - userVS.nif  '${userVS.getNif()}'"
		String msg
		if(!userVS.getNif()) {
			msg = messageSource.getMessage('susbcripcion.errorDatosUsuario', null, locale)
			log.error("- checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:msg, type:TypeVS.USER_ERROR)
		}
		X509Certificate userCert = userVS.getCertificate()
		if (!userCert) {
			log.debug("Missing certificate!!!")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR, message:"Missing certificate!!!")
		}
		String validatedNIF = org.votingsystem.util.StringUtils.validarNIF(userVS.getNif())
		if(!validatedNIF) {
			msg = messageSource.getMessage('susbcripcion.errorNifUsuario', 
				[userVS.getNif()].toArray(), locale)
			log.error("- checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, 
				message:msg, type:TypeVS.USER_ERROR)
		}
		Usuario usuarioDB = Usuario.findByNif(validatedNIF.toUpperCase())
		Certificado certificado = null;
		if (!usuarioDB) {
            usuarioDB = new Usuario(userVS)
            usuarioDB.nif = nifValidado
            usuarioDB.type = Usuario.Type.USER
			usuarioDB.save();
			log.debug "- checkUser - user -> ${usuarioDB.nif} - id '${usuarioDB.id}'"
			certificado = new Certificado(usuario:usuarioDB,
				contenido:usuarioDB.getCertificate()?.getEncoded(),
				numeroSerie:usuarioDB.getCertificate()?.getSerialNumber()?.longValue(),
				estado:Certificado.Estado.OK, type:Certificado.Type.USUARIO,
				certificadoAutoridad:usuarioDB.getCertificateCA(),
				validoDesde:usuarioDB.getCertificate()?.getNotBefore(),
				validoHasta:usuarioDB.getCertificate()?.getNotAfter())
			certificado.save();
			log.debug "- checkUser - user cert -> id '${certificado.id}'"
		} else {
			certificado = Certificado.findWhere(
				usuario:usuarioDB, estado:Certificado.Estado.OK)
			if (!certificado?.numeroSerie == userCert.getSerialNumber()?.longValue()) {
				certificado.estado = Certificado.Estado.ANULADO
				certificado.save()
				log.debug "- checkUser - CANCELLED user cert id '${certificado.id}'"
				certificado = new Certificado(usuario:usuarioDB,
					contenido:userCert?.getEncoded(), estado:Certificado.Estado.OK,
					numeroSerie:userCert?.getSerialNumber()?.longValue(),
					certificadoAutoridad:userVS.getCertificateCA(),
					validoDesde:userVS.getCertificate()?.getNotBefore(),
					validoHasta:userVS.getCertificate()?.getNotAfter())
				certificado.save();
				log.debug "- checkUser - UPDATED user cert -> id '${certificado.id}'"
			}
			usuarioDB.setCertificateCA(userVS.getCertificateCA())
			usuarioDB.setCertificate(userVS.getCertificate())
			usuarioDB.setTimeStampToken(userVS.getTimeStampToken())
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, userVS:usuarioDB, data:certificado)
	}
	
	ResponseVS comprobarDispositivo(String nif, String telefono, String email, 
			String deviceId, Locale locale) {
		log.debug "comprobarDispositivo - nif:${nif} - telefono:${telefono} - email:${email} - deviceId:${deviceId}"
		if(!nif || !deviceId) {
			log.debug "Sin datos"
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
				messageSource.getMessage('error.requestWithoutData', null, locale))
		}
		String nifValidado = org.votingsystem.util.StringUtils.validarNIF(nif)
		if(!nifValidado) {
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:
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

			if (!TypeVS.CONTROL_CENTER.equals(actorConIP.serverType)) {
				msg = message(code: 'susbcripcion.actorNoCentroControl',
					args:[actorConIP.serverURL])
				log.error("checkControlCenter - ERROR - ${msg}")
				return new ResponseVS(message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
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
			return new ResponseVS(message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
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
				(TypeVS.CONTROL_CENTER_ASSOCIATION != TypeVS.valueOf(messageJSON.operation))) {
				msg = messageSource.getMessage('documentWithErrorsMsg',null, locale)
				log.error("asociarCentroControl - ERROR DATA - ${msg} -> ${smimeMessageReq.getSignedContent()}")
				return new ResponseVS(message:msg, type:TypeVS.CONTROL_CENTER_ASSOCIATION_ERROR, 
					statusCode:ResponseVS.SC_ERROR_REQUEST)
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
						statusCode:ResponseVS.SC_ERROR_REQUEST,
						type:TypeVS.CONTROL_CENTER_ASSOCIATION_ERROR)
				} else {
					ResponseVS respuesta = checkControlCenter(serverURL, locale)
					if (ResponseVS.SC_OK != respuesta.statusCode) {
						log.error("asociarCentroControl- ERROR CHECKING CONTROL CENTER - ${respuesta.message}")
						return new ResponseVS(message:respuesta.message,
							statusCode:ResponseVS.SC_ERROR_REQUEST,
							type:TypeVS.CONTROL_CENTER_ASSOCIATION_ERROR)
					} else {
						msg =  messageSource.getMessage('susbcripcion.centroControlAsociado',
							[respuesta.centroControl.nombre].toArray(), locale)
						log.debug(msg)
						return new ResponseVS(message: msg,
							centroControl:respuesta.centroControl,
							statusCode:ResponseVS.SC_OK,
							type:TypeVS.CONTROL_CENTER_ASSOCIATION)
					}
				}
			}
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex)
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
				message: messageSource.getMessage('error.errorConexionActor',
					[serverURL, ex.getMessage()].toArray(), locale),
				type:TypeVS.CONTROL_CENTER_ASSOCIATION_ERROR)
		}
	}
}