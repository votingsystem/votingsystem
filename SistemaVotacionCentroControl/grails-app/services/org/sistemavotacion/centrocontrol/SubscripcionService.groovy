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
* Licencia: http://bit.ly/j9jZQH
* */
class SubscripcionService {	
		
	static transactional = true
	
	def messageSource
    def grailsApplication  
	def httpService
	
	Respuesta guardarUsuario(Usuario usuario, Locale locale) {
		log.debug "guardarUsuario - usuario: ${usuario.nif}"
		if(!usuario.nif) {
			String mensajeError = messageSource.getMessage(
				'susbcripcion.errorDatosUsuario', null, locale)
			return new Respuesta(codigoEstado:400, mensaje:mensajeError)
		}
		X509Certificate certificadoUsu = usuario.getCertificate()
		def usuarioDB = Usuario.findWhere(nif:usuario.getNif().toUpperCase())
		if (!usuarioDB) {
			usuarioDB = usuario.save();
			if (usuario.getCertificate()) {
				Certificado certificado = new Certificado(usuario:usuario,
					contenido:usuario.getCertificate()?.getEncoded(),
					numeroSerie:usuario.getCertificate()?.getSerialNumber()?.longValue(),
					estado:Certificado.Estado.OK, tipo:Certificado.Tipo.USUARIO,
					validoDesde:usuario.getCertificate()?.getNotBefore(),
					validoHasta:usuario.getCertificate()?.getNotAfter())
				certificado.save();
			}
		} else {
			def certificadoDB = Certificado.findWhere(
				usuario:usuarioDB, estado:Certificado.Estado.OK)
			if (!certificadoDB?.numeroSerie == certificadoUsu.getSerialNumber()?.longValue()) {
				certificadoDB.estado = Certificado.Estado.ANULADO
				certificadoDB.save()
				Certificado certificado = new Certificado(usuario:usuarioDB,
					contenido:certificadoUsu?.getEncoded(), estado:Certificado.Estado.OK,
					numeroSerie:certificadoUsu?.getSerialNumber()?.longValue(),
					certificadoAutoridad:usuario.getCertificadoCA(),
					validoDesde:usuario.getCertificate()?.getNotBefore(),
					validoHasta:usuario.getCertificate()?.getNotAfter())
				certificado.save();
			}
		}
		return new Respuesta(codigoEstado:200, usuario:usuarioDB)
	}
	
	
	Respuesta comprobarUsuario(Usuario checkedUsuario, Locale locale) {
		log.debug "comprobarUsuario - ${checkedUsuario?.nif}"
		if(!checkedUsuario?.nif) {
			String mensajeError = messageSource.getMessage(
				'susbcripcion.errorDatosUsuario', null, locale)
			return new Respuesta(codigoEstado:400, mensaje:mensajeError)
		}
		Usuario usuario = Usuario.findByNif(checkedUsuario?.nif?.toUpperCase())
		if (usuario) return new Respuesta(codigoEstado:200, usuario:usuario)
		return guardarUsuario(checkedUsuario, locale)
	}

	ControlAcceso comprobarControlAcceso(String serverURL) {
		log.debug "---comprobarControlAcceso - serverURL:${serverURL}"
		serverURL = StringUtils.checkURL(serverURL)
		ControlAcceso controlAcceso = ControlAcceso.findWhere(serverURL:serverURL)
		if (!controlAcceso) {
			String urlInfoControlAcceso = "${serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLInfoServidor}"
			Respuesta respuesta = httpService.obtenerInfoActorConIP(urlInfoControlAcceso, new ControlAcceso())
			if (200 == respuesta.codigoEstado) {
				controlAcceso = respuesta.actorConIP
				controlAcceso.save()
			} else return null
		} 
		return controlAcceso
	}
	
}