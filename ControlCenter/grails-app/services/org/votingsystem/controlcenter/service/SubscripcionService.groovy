package org.votingsystem.controlcenter.service

import org.votingsystem.controlcenter.model.*;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.*
import org.votingsystem.signature.smime.*;
import org.votingsystem.groovy.util.*;

import java.security.cert.X509Certificate;

import grails.converters.JSON;

import java.util.Locale;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* */
class SubscripcionService {	
		
	static transactional = true
	
	def messageSource
    def grailsApplication  
	def httpService
	
	ResponseVS checkUser(Usuario usuario, Locale locale) {
		log.debug "- checkUser - usuario '${usuario.nif}'"
		String msg
		if(!usuario?.nif) {
			msg = messageSource.getMessage(
				'susbcripcion.errorDatosUsuario', null, locale)
			log.error("- checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, 
				message:msg, tipo:Tipo.USER_ERROR)
		}
		String nifValidado = org.votingsystem.util.StringUtils.validarNIF(usuario.nif)
		if(!nifValidado) {
			msg = messageSource.getMessage('susbcripcion.errorNifUsuario', 
				[usuario.nif].toArray(), locale)
			log.error("- checkUser - ${msg}")
			return new ResponseVS(statusCode:ResponseVS.SC_ERROR_PETICION, 
				message:msg, tipo:Tipo.USER_ERROR)
		}
		usuario.nif = nifValidado
		X509Certificate certificadoUsu = usuario.getCertificate()
		def usuarioDB = Usuario.findWhere(nif:usuario.getNif().toUpperCase())
		if (!usuarioDB) {
			usuarioDB = usuario.save();
			log.debug "- checkUser - NEW USER -> ${usuario.nif} - id '${usuarioDB.id}'"
			if (usuario.getCertificate()) {
				Certificado certificado = new Certificado(usuario:usuario,
					contenido:usuario.getCertificate()?.getEncoded(),
					numeroSerie:usuario.getCertificate()?.getSerialNumber()?.longValue(),
					estado:Certificado.Estado.OK, tipo:Certificado.Tipo.USUARIO,
					validoDesde:usuario.getCertificate()?.getNotBefore(),
					validoHasta:usuario.getCertificate()?.getNotAfter())
				certificado.save();
				log.debug "- checkUser - NEW USER CERT -> id '${certificado.id}'"
			}
		} else {
			Certificado certificado = Certificado.findWhere(
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
		}
		return new ResponseVS(statusCode:ResponseVS.SC_OK, usuario:usuarioDB)
	}

	ControlAcceso checkAccessControl(String serverURL) {
		log.debug "---checkAccessControl - serverURL:${serverURL}"
		serverURL = StringUtils.checkURL(serverURL)
		ControlAcceso controlAcceso = ControlAcceso.findWhere(serverURL:serverURL)
		if (!controlAcceso) {
			String urlInfoControlAcceso = "${serverURL}/infoServidor"
			ResponseVS respuesta = httpService.getInfo(urlInfoControlAcceso, null)
			if (ResponseVS.SC_OK == respuesta.statusCode) {
				try {
					controlAcceso = ControlAcceso.parse(respuesta.message)
					controlAcceso.save()
				} catch(Exception ex) {
					log.error(ex.getMessage(), ex)
				}
			} else return null
		} 
		return controlAcceso
	}
	
}