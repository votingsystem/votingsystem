package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*;
import org.sistemavotacion.seguridad.*
import org.sistemavotacion.smime.*;
import org.sistemavotacion.utils.*;
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
	
	Respuesta checkUser(Usuario usuario, Locale locale) {
		log.debug "- checkUser - usuario '${usuario.nif}'"
		String msg
		if(!usuario?.nif) {
			msg = messageSource.getMessage(
				'susbcripcion.errorDatosUsuario', null, locale)
			log.error("- checkUser - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:msg, tipo:Tipo.USER_ERROR)
		}
		String nifValidado = org.sistemavotacion.util.StringUtils.validarNIF(usuario.nif)
		if(!nifValidado) {
			msg = messageSource.getMessage('susbcripcion.errorNifUsuario', 
				[usuario.nif].toArray(), locale)
			log.error("- checkUser - ${msg}")
			return new Respuesta(codigoEstado:Respuesta.SC_ERROR_PETICION, 
				mensaje:msg, tipo:Tipo.USER_ERROR)
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
		return new Respuesta(codigoEstado:Respuesta.SC_OK, usuario:usuarioDB)
	}

	ControlAcceso checkAccessControl(String serverURL) {
		log.debug "---checkAccessControl - serverURL:${serverURL}"
		serverURL = StringUtils.checkURL(serverURL)
		ControlAcceso controlAcceso = ControlAcceso.findWhere(serverURL:serverURL)
		if (!controlAcceso) {
			String urlInfoControlAcceso = "${serverURL}/infoServidor"
			Respuesta respuesta = httpService.getInfo(urlInfoControlAcceso, null)
			if (Respuesta.SC_OK == respuesta.codigoEstado) {
				try {
					controlAcceso = ControlAcceso.parse(respuesta.mensaje)
					controlAcceso.save()
				} catch(Exception ex) {
					log.error(ex.getMessage(), ex)
				}
			} else return null
		} 
		return controlAcceso
	}
	
}