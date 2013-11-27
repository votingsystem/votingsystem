package org.votingsystem.controlcenter.controller

import com.sun.syndication.feed.synd.SyndContentImpl
import com.sun.syndication.feed.synd.SyndEntryImpl
import com.sun.syndication.feed.synd.SyndFeed
import com.sun.syndication.feed.synd.SyndFeedImpl
import org.votingsystem.util.StringUtils
import org.votingsystem.model.AccessControlVS
import org.votingsystem.model.ActorVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.ResponseVS
import grails.converters.JSON
import com.sun.syndication.io.SyndFeedOutput
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
/**
 * @infoController Subscripciones
 * @descController Servicios relacionados con los feeds generados por la aplicación y
 * 				   con la asociación de Controles de Acceso.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 *
 * Mecanismo de feeds basado en:
 * http://blogs.bytecode.com.au/glen/2006/12/22/generating-rss-feeds-with-grails-and-rome.html
 */
class SubscriptionVSController {
	
	def httpService

	def supportedFormats = [ "rss_0.90", "rss_0.91", "rss_0.92", "rss_0.93", "rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3"]
	
	/**
	 * Servicio que da de alta Controles de Acceso.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/subscriptionVS]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. 
	 *					Dcoumento con los datos del control de acceso que se desea dar de alta.
	 */
	def index() { 
		MessageSMIME messageSMIME = params.messageSMIMEReq
		if(!messageSMIME) {
			String msg = message(code:'requestWithoutFile')
			log.error msg
			response.status = ResponseVS.SC_ERROR_REQUEST
			render msg
			return false
		}
		SMIMEMessageWrapper smimeMessageReq = messageSMIME.getSmimeMessage()
        def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
		int status
		if (messageJSON.serverURL) {
            String serverURL = StringUtils.checkURL(messageJSON.serverURL)
			AccessControlVS accessControl = AccessControlVS.findWhere(serverURL:serverURL)
			String message
			if (accessControl) {
				response.status = ResponseVS.SC_ERROR_REQUEST
				message = message(code: 'controlCenterAlreadyAssociatedMsg', args:[accessControl.serverURL])
			} else {
				def urlInfoAccessControlVS = "${serverURL}/serverInfo"
				ResponseVS responseVS = httpService.getInfo(urlInfoAccessControl, null)
				status = responseVS.statusCode
				if (ResponseVS.SC_OK == responseVS.statusCode) {
					message = message(code: 'controlCenterAssociatedMsg', args:[urlInfoAccessControl])
					try {
						AccessControlVS actorVS = ActorVS.parse(responseVS.message)
						actorVS.save()
					} catch(Exception ex) {
						log.error(ex.getMessage(), ex)
						status = ResponseVS.SC_ERROR
						message = "ERROR"
					}
				} else message = responseVS.message
			}
			log.debug message
			response.status = status
            render message
            return false
        } 
        response.status = ResponseVS.SC_ERROR_REQUEST
        render message(code: 'requestWithErrors')
        return false	
	}
	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/subscriptionVS/votaciones/$feedType]
	 * @param	[feedType] Opcional. Formatos de feed soportados rss_0.90, rss_0.91, rss_0.92,
	 * 			rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3. Por defecto se sirve atom 1.0.
	 * @requestContentType [text/xml]
	 * @return Información en el formato solicitado sobre las votaciones publicadas.
	 */
	def elections() {
		if(params.feedType && supportedFormats.contains(params.feedType)) {
			render(text: getEntradasVotaciones(params.feedType),
				contentType:"text/xml", encoding:"UTF-8")
		} else {
			if(params.feedType && "rss".equals(params.feedType)) {
				render(text: getEntradasVotaciones("rss_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
				return false;
			} else if(params.feedType && "atom".equals(params.feedType)) {
				render(text: getEntradasVotaciones("atom_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
			} else {
				render(text: getEntradasVotaciones("atom_1.0"),
					contentType:"text/xml", encoding:"UTF-8")
			}
		}
	}
	
	private String getEntradasVotaciones(feedType) {
		def elections
		EventVS.withTransaction {
			elections = EventVS.findAllWhere(state:EventVS.State.ACTIVE,
				[max: 30, sort: "dateCreated", order: "desc"])
		}
		def entradas = []
		elections.each { votacion ->
			String content = votacion.content
			String author = message(code: 'publishedBySystem')
			if(votacion.userVS) {
				author = "${votacion.userVS.name} ${votacion.userVS.firstName}"
			}
			String urlVotacion = "${createLink(controller: 'eventVSElection')}/" + votacion.id
			String accion = message(code: 'subscriptionVS.votar')
			 //if(content?.length() > 500) content = content.substring(0, 500)
			String feedContent = "<p>${content}</p>" +
				"<a href='${urlVotacion}'>${accion}</a>"
			def desc = new SyndContentImpl(type: "text/html", value: feedContent);
			def entrada = new SyndEntryImpl(title: votacion.subject, author:author,
					link: urlVotacion,
					publishedDate: votacion.dateCreated, description: desc);
			entradas.add(entrada);
		}
		String tituloSubscripcionVotaciones = "${message(code: 'subscriptionVS.tituloSubscripcionVotaciones')}" +
		" -${grailsApplication.config.VotingSystem.serverName}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType,
				title: tituloSubscripcionVotaciones,
				link: "${grailsApplication.config.grails.serverURL}/app/home#VOTACIONES",
				description: message(code: 'subscriptionVS.descripcionSubscripcionVotaciones'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}

}