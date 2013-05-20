package org.sistemavotacion.centrocontrol

import org.sistemavotacion.utils.StringUtils;
import groovyx.net.http.*
import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*
import org.sistemavotacion.centrocontrol.modelo.*;
import grails.converters.JSON
import org.sistemavotacion.smime.*
import com.sun.syndication.feed.module.content.ContentModule
import com.sun.syndication.feed.module.content.ContentModuleImpl
import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.SyndFeedOutput;
/**
 * @infoController Subscripciones
 * @descController Servicios relacionados con los feeds generados por la aplicación y
 * 				   con la asociación de Controles de Acceso.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 *
 * Mecanismo de feeds basado en:
 * http://blogs.bytecode.com.au/glen/2006/12/22/generating-rss-feeds-with-grails-and-rome.html
 */
class SubscripcionController {
	
	def httpService

	def supportedFormats = [ "rss_0.90", "rss_0.91", "rss_0.92", "rss_0.93", "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3"]
	
	/**
	 * Servicio que da de alta Controles de Acceso.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/subscripcion]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. 
	 *					Dcoumento con los datos del control de acceso que se desea dar de alta.
	 */
	def index() { 
		MensajeSMIME mensajeSMIME = flash.mensajeSMIMEReq
		if(!mensajeSMIME) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = Respuesta.SC_ERROR_PETICION
			render msg
			return false
		}
		SMIMEMessageWrapper smimeMessageReq = mensajeSMIME.getSmimeMessage()
        def mensajeJSON = JSON.parse(smimeMessageReq.getSignedContent())
        if (mensajeJSON.serverURL) {
            String serverURL = StringUtils.checkURL(mensajeJSON.serverURL)
			ControlAcceso controlAcceso = ControlAcceso.findWhere(serverURL:serverURL)
			String mensaje
			if (controlAcceso) {
				response.status = Respuesta.SC_ERROR_PETICION
				mensaje = message(code: 'controlCenterAlreadyAssociatedMsg', args:[controlAcceso.serverURL]) 
			} else {
				def urlInfoControlAcceso = "${serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLInfoServidor}"
				Respuesta respuesta = httpService.obtenerInfoActorConIP(urlInfoControlAcceso, new ControlAcceso())
				response.status = respuesta.codigoEstado
				if (Respuesta.SC_OK == respuesta.codigoEstado) {					
					mensaje = message(code: 'controlCenterAssociatedMsg', args:[urlInfoControlAcceso]) 
					ControlAcceso actorConIP = respuesta.actorConIP
					actorConIP.save()
				} else mensaje = respuesta.mensaje
			}
            log.debug mensaje
            render mensaje
            return false
        } 
        response.status = Respuesta.SC_ERROR_PETICION
        render message(code: 'error.PeticionIncorrecta')
        return false	
	}
	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/subscripcion/votaciones/$feedType]
	 * @param	[feedType] Opcional. Formatos de feed soportados rss_0.90, rss_0.91, rss_0.92,
	 * 			rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3. Por defecto se sirve atom 1.0.
	 * @requestContentType [text/xml]
	 * @return Información en el formato solicitado sobre las votaciones publicadas.
	 */
	def votaciones() {
		if(params.feedType && supportedFormats.contains(params.feedType)) {
			render(text: obtenerEntradasVotaciones(params.feedType),
				contentType:"text/xml", encoding:"UTF-8")
		} else {
			if(params.feedType && "rss".equals(params.feedType)) {
				render(text: obtenerEntradasVotaciones("rss_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
				return false;
			} else if(params.feedType && "atom".equals(params.feedType)) {
				render(text: obtenerEntradasVotaciones("atom_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
			} else {
				render(text: obtenerEntradasVotaciones("atom_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
			}
		}
	}
	
	private String obtenerEntradasVotaciones(feedType) {
		def votaciones
		EventoVotacion.withTransaction {
			votaciones = EventoVotacion.findAllWhere(estado:EventoVotacion.Estado.ACTIVO,
				[max: 30, sort: "dateCreated", order: "desc"])
		}
		def entradas = []
		votaciones.each { votacion ->
			String contenido = votacion.contenido
			String author = message(code: 'subscripcion.documentoSinAutor')
			if(votacion.usuario) {
				author = "${votacion.usuario.nombre} ${votacion.usuario.primerApellido}"
			}
			String urlVotacion = "${grailsApplication.config.grails.serverURL}" +
				"${grailsApplication.config.SistemaVotacion.sufijoURLVotar}" + votacion.id
			String accion = message(code: 'subscripcion.votar')
			 //if(contenido?.length() > 500) contenido = contenido.substring(0, 500)
			String contenidoFeed = "<p>${contenido}</p>" +
				"<a href='${urlVotacion}'>${accion}</a>"
			def desc = new SyndContentImpl(type: "text/html", value: contenidoFeed);
			def entrada = new SyndEntryImpl(title: votacion.asunto, author:author,
					link: urlVotacion,
					publishedDate: votacion.dateCreated, description: desc);
			entradas.add(entrada);
		}
		String tituloSubscripcionVotaciones = "${message(code: 'subscripcion.tituloSubscripcionVotaciones')}" +
		" -${grailsApplication.config.SistemaVotacion.serverName}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType,
				title: tituloSubscripcionVotaciones,
				link: "${grailsApplication.config.grails.serverURL}/app/home#VOTACIONES",
				description: message(code: 'subscripcion.descripcionSubscripcionVotaciones'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}

}