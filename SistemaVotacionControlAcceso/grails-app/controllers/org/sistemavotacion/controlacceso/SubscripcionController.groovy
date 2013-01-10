package org.sistemavotacion.controlacceso

import com.sun.syndication.feed.module.content.ContentModule
import com.sun.syndication.feed.module.content.ContentModuleImpl
import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.SyndFeedOutput;
import org.sistemavotacion.controlacceso.modelo.*;
import grails.converters.JSON


/**
 * @author jgzornoza
 * Licencia: http://bit.ly/j9jZQH
 * 
 * Basado en 
 * http://blogs.bytecode.com.au/glen/2006/12/22/generating-rss-feeds-with-grails-and-rome.html
 */
class SubscripcionController {
	
    
    def subscripcionService
	
	def supportedFormats = [ "rss_0.90", "rss_0.91", "rss_0.92", "rss_0.93", "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3"]
	
	def index() { }
       
    def  guardarAsociacionConCentroControl = { 
		Respuesta respuesta = subscripcionService.asociarCentroControl(
			params.smimeMessageReq, request.getLocale())
        response.status = respuesta.codigoEstado
		if (200 == respuesta.codigoEstado) render respuesta.getMap() as JSON
		else render respuesta.mensaje
        return false	
    }
	
	def reclamaciones() {
		if(params.feedType && supportedFormats.contains(params.feedType)) {
			render(text: obtenerEntradasReclamaciones(params.feedType),
				contentType:"text/xml", encoding:"UTF-8")
		} else {
			if(params.feedType && "rss".equals(params.feedType)) {
				render(text: obtenerEntradasReclamaciones("rss_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
				return false;
			} else if(params.feedType && "atom".equals(params.feedType)) {
				render(text: obtenerEntradasReclamaciones("atom_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
			} else {
				render(text: obtenerEntradasReclamaciones("atom_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
			}
		}
	}
	
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
	
    def manifiestos() {
		if(params.feedType && supportedFormats.contains(params.feedType)) {
			render(text: obtenerEntradasManifiestos(params.feedType), 
				contentType:"text/xml", encoding:"UTF-8")
		} else {
			if(params.feedType && "rss".equals(params.feedType)) {
				render(text: obtenerEntradasManifiestos("rss_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
				return false;
			} else if(params.feedType && "atom".equals(params.feedType)) {
				render(text: obtenerEntradasManifiestos("atom_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
			} else {
				render(text: obtenerEntradasManifiestos("atom_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
			}
		}
	}
	
	def obtenerEntradasReclamaciones(feedType) {
		def reclamaciones = EventoReclamacion.findAllWhere(estado:Evento.Estado.ACTIVO,
			[max: 30, sort: "dateCreated", order: "desc"])
		def entradas = []
		reclamaciones.each { reclamacion ->
			String contenido = reclamacion.contenido
			String author = message(code: 'subscripcion.documentoSinAutor')
			if(reclamacion.usuario) {
				author = "${reclamacion.usuario.nombre} ${reclamacion.usuario.primerApellido}"
			}
			String urlReclamacion = "${grailsApplication.config.grails.serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLFirmarReclamacion}" + reclamacion.id
			String accion = message(code: 'subscripcion.firmarReclamacion')
			 //if(contenido?.length() > 500) contenido = contenido.substring(0, 500)
			String contenidoFeed = "<p>${contenido}</p>" +
				"<a href='${urlReclamacion}'>${accion}</a>"
			def desc = new SyndContentImpl(type: "text/html", value: contenidoFeed);
			def entrada = new SyndEntryImpl(title: reclamacion.asunto, author:author,
					link: urlReclamacion,
					publishedDate: reclamacion.dateCreated, description: desc);
			entradas.add(entrada);
		}
		String tituloSubscripcionReclamaciones = "${message(code: 'subscripcion.tituloSubscripcionReclamaciones')}" +
		" -${grailsApplication.config.SistemaVotacion.NombreControlAcceso}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType,
				title: tituloSubscripcionReclamaciones,
				link: "${grailsApplication.config.grails.serverURL}/app/index#RECLAMACIONES",
				description: message(code: 'subscripcion.descripcionSubscripcionReclamaciones'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}
	
	def obtenerEntradasVotaciones(feedType) {
		def votaciones
		EventoVotacion.withTransaction {
			votaciones = EventoVotacion.findAllWhere(estado:Evento.Estado.ACTIVO,
				[max: 30, sort: "dateCreated", order: "desc"])
		}
		def entradas = []
		votaciones.each { votacion ->
			String contenido = votacion.contenido
			String author = message(code: 'subscripcion.documentoSinAutor')
			if(votacion.usuario) {
				author = "${votacion.usuario.nombre} ${votacion.usuario.primerApellido}"
			}
			String urlVotacion = "${grailsApplication.config.grails.serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLVotar}" + votacion.id
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
		" -${grailsApplication.config.SistemaVotacion.NombreControlAcceso}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType,
				title: tituloSubscripcionVotaciones,
				link: "${grailsApplication.config.grails.serverURL}/app/index#VOTACIONES",
				description: message(code: 'subscripcion.descripcionSubscripcionVotaciones'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}

	def obtenerEntradasManifiestos(feedType) {
		def manfiestos = EventoFirma.findAllWhere(estado:Evento.Estado.ACTIVO, 
			[max: 30, sort: "dateCreated", order: "desc"])
		def entradas = []
		manfiestos.each { manifiesto ->
			String contenido = manifiesto.contenido
			String author = message(code: 'subscripcion.documentoSinAutor')
			if(manifiesto.usuario) {
				author = "${manifiesto.usuario.nombre} ${manifiesto.usuario.primerApellido}"
			}
			String urlManifiesto = "${grailsApplication.config.grails.serverURL}${grailsApplication.config.SistemaVotacion.sufijoURLFirmarManifiesto}" + manifiesto.id
			String accion = message(code: 'subscripcion.firmarManifiesto')
 			//if(contenido?.length() > 500) contenido = contenido.substring(0, 500)
			String contenidoFeed = "<p>${contenido}</p>" + 
				"<a href='${urlManifiesto}'>${accion}</a>"
			def desc = new SyndContentImpl(type: "text/html", value: contenidoFeed);
			def entrada = new SyndEntryImpl(title: manifiesto.asunto, author:author,
					link: urlManifiesto,
					publishedDate: manifiesto.dateCreated, description: desc);
			entradas.add(entrada);
		}
		String tituloSubscripcionManifiestos = "${message(code: 'subscripcion.tituloSubscripcionManifiestos')}" +
		" -${grailsApplication.config.SistemaVotacion.NombreControlAcceso}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType, 
				title: tituloSubscripcionManifiestos,
				link: "${grailsApplication.config.grails.serverURL}/app/index#MANIFIESTOS",
				description: message(code: 'subscripcion.descripcionSubscripcionManifiestos'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}

}
