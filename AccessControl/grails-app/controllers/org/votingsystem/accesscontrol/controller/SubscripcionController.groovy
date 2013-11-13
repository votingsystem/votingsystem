package org.votingsystem.accesscontrol.controller

import com.sun.syndication.feed.module.content.ContentModule
import com.sun.syndication.feed.module.content.ContentModuleImpl
import com.sun.syndication.feed.synd.*;
import com.sun.syndication.io.SyndFeedOutput;

import org.votingsystem.accesscontrol.model.*;
import org.votingsystem.model.ResponseVS;

import grails.converters.JSON


/**
 * @infoController Subscripciones
 * @descController Servicios relacionados con los feeds generados por la aplicación y
 * 				   con la asociación de Centros de Control.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * 
 * Basado en 
 * http://blogs.bytecode.com.au/glen/2006/12/22/generating-rss-feeds-with-grails-and-rome.html
 */
class SubscripcionController {
	
    
    def subscripcionService
	def usuarioService
	
	def supportedFormats = [ "rss_0.90", "rss_0.91", "rss_0.92", "rss_0.93", 
		"rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3"]
	
	/**
	 * Servicio que da de alta Centros de Control.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/subscripcion]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. 
	 *					Archivo con los datos del Centro de Control que se desea dar de alta.
	 */
	def index() { 
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'evento.peticionSinArchivo')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		ResponseVS respuesta = subscripcionService.asociarCentroControl(
			messageSMIME, request.getLocale())
		params.respuesta = respuesta
		if(ResponseVS.SC_OK == respuesta.statusCode) {
			CentroControl controlCenter = respuesta.centroControl
			Map controlCenterMap = usuarioService.getControlCenterMap(controlCenter)
			render controlCenterMap as JSON
		}
	}

	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/subscripcion/reclamaciones/$feedType]
	 * @param	[feedType] Opcional. Formatos de feed soportados rss_0.90, rss_0.91, rss_0.92, 
	 * 			rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3. Por defecto se sirve atom 1.0.
	 * @requestContentType [text/xml]
	 * @return Información en el formato solicitado sobre las reclamaciones publicadas.
	 */
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
	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/subscripcion/manifiestos/$feedType]
	 * @param	[feedType] Opcional. Formatos de feed soportados rss_0.90, rss_0.91, rss_0.92,
	 * 			rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3. Por defecto se sirve atom 1.0.
	 * @requestContentType [text/xml]
	 * @return Información en el formato solicitado sobre los manifiestos publicados.
	 */
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
	
	private String obtenerEntradasReclamaciones(feedType) {
		def reclamaciones = EventoReclamacion.findAllWhere(estado:Evento.Estado.ACTIVO,
			[max: 30, sort: "dateCreated", order: "desc"])
		def entradas = []
		reclamaciones.each { reclamacion ->
			String contenido = reclamacion.contenido
			String author = message(code: 'subscripcion.documentoSinAutor')
			if(reclamacion.usuario) {
				author = "${reclamacion.usuario.nombre} ${reclamacion.usuario.primerApellido}"
			}
			String urlReclamacion =  "${createLink(controller: 'eventoReclamacion')}/" + reclamacion.id
			String accion = message(code: 'signClaim')
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
		" -${grailsApplication.config.VotingSystem.serverName}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType,
				title: tituloSubscripcionReclamaciones,
				link: "${createLink(controller: 'eventoReclamacion', action: 'mainPage')}",
				description: message(code: 'subscripcion.descripcionSubscripcionReclamaciones'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}
	
	private String obtenerEntradasVotaciones(feedType) {
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
			String urlVotacion =  "${createLink(controller: 'eventoVotacion')}/" + votacion.id
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
		" -${grailsApplication.config.VotingSystem.serverName}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType,
				title: tituloSubscripcionVotaciones,
				link: "${createLink(controller: 'eventoVotacion', action: 'mainPage')}",
				description: message(code: 'subscripcion.descripcionSubscripcionVotaciones'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}

	private String obtenerEntradasManifiestos(feedType) {
		def manfiestos
		EventoFirma.withTransaction {
			manfiestos = EventoFirma.findAllWhere(estado:Evento.Estado.ACTIVO,
				[max: 30, sort: "dateCreated", order: "desc"])
		}
		def entradas = []
		manfiestos.each { manifiesto ->
			String contenido = manifiesto.contenido
			String author = message(code: 'subscripcion.documentoSinAutor')
			if(manifiesto.usuario) {
				author = "${manifiesto.usuario.nombre} ${manifiesto.usuario.primerApellido}"
			}
			String urlManifiesto = "${createLink(controller: 'eventoFirma')}/" + manifiesto.id
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
		" -${grailsApplication.config.VotingSystem.serverName}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType, 
				title: tituloSubscripcionManifiestos,
				link: "${createLink(controller: 'eventoFirma', action: 'mainPage')}",
				description: message(code: 'subscripcion.descripcionSubscripcionManifiestos'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}

}
