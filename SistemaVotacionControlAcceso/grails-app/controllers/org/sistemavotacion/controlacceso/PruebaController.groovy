package org.sistemavotacion.controlacceso

import org.bouncycastle.util.encoders.Base64;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.sistemavotacion.controlacceso.modelo.*
import org.sistemavotacion.red.*
import org.sistemavotacion.seguridad.CertUtil;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.utils.*
import org.sistemavotacion.exception.*
import org.apache.http.HttpRequest;
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.entity.mime.content.ByteArrayBody
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import org.apache.http.entity.mime.MultipartEntity

class PruebaController {
	
	static applicationTagLib = new org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib()
	def solicitudAccesoService
	def recolectorFirmaService
	def csrService
	def keyStoreService
	def firmaService
	
	def contexto = { }
	
	
	def pruebaPEM = {
		Respuesta respuesta = firmaService.inicializarAutoridadesCertificadoras()
		respuesta.codigoEstado
		log.debug("Codigo estado inicialización: ${respuesta.codigoEstado}")
		render "${respuesta.mensaje}"
		return false
	}
	
	def pruebaLong = {
		log.debug(" -- pruebaLong: ${params.id}")
		if(params.long('id')) {
			Long longi = params.long('id')
			
			render "Recibido parametro ${longi.getClass().getName()}: ${longi}"
			return false
		} 
		render "Solicitud sin parametro"
		return false
	}
	
	
	def pruebaAlmacen = {
		if (params.long('id')) {
            EventoVotacion eventoVotacion
            if (!params.evento) eventoVotacion = EventoVotacion.get(params.id)
			def mensaje = "almacenClaves Id: ${eventoVotacion.almacenClaves.id}"
			render mensaje
			log.debug(mensaje) 
			return false
		}
	}	
	
	def createSignedMail = {
		log.debug("createSignedMailcreateSignedMail")
		recolectorFirmaService.serviceMethod()
	}

	
	
	def excepcion = {
		throw new Exception ("Lanzado mensaje en excepcion")
	}
    
    def config = { 
        log.debug "baseRutaCopiaRespaldo: ${grailsApplication.config.SistemaVotacion.baseRutaCopiaRespaldo}"
		render "From message: " + message(code: 'mime.asunto.SolicitudAcceso') + "\n"
		render " --- From applicationTagLib: " + applicationTagLib.message(code: 'mime.asunto.SolicitudAcceso') + "\n"
		render " --- From SolicitudAccesoService: " + solicitudAccesoService.getMessageSolicitudAcceso() + "\n"
        return false
    }
    
    
    def sendException = { 
        log.debug "sendException"
        Respuesta respuesta = new Respuesta(codigoEstado:500, tipo:Tipo.ERROR)
        //throw new SistemaVotacionException(respuesta, new Exception("SistemaVotacionException"))
    }
    
    def index = {
        def msg
        String algoritmoHash = grailsApplication.config.SistemaVotacion.algoritmoHash;
        if (!params.origenHash) {
            msg = "Debe proporcionar el origen del hash"
            log.debug msg
            render msg
            return
        } 
        MessageDigest sha = MessageDigest.getInstance(algoritmoHash);
        byte[] resultDigest =  sha.digest( params.origenHash.getBytes() );
        msg = new String(Base64.encode(resultDigest));
        log.debug "algoritmoHash:'${algoritmoHash}' - origen:'${params.origenHash}' - hash:'${msg}'"
        render msg
        return 
    }

	def testAsync = {
        log.debug "Arranco controlador"
		def aCtx = startAsync()
		aCtx.setTimeout(5000);
        //aCtx.complete()
		
		render "Todo ok"
    }
	
	def sesion() {
		log.debug("request.getRemoteAddr(): ${request.getRemoteAddr()}")
		log.debug("request.getHeader('X-Forwarded-For'): ${request.getHeader('X-Forwarded-For')}")
		log.debug("request.getHeader('X-Forwarded-For'): ${request.getHeader('Client-IP')}")
		render request.getRemoteAddr()
		return
	}
    
}