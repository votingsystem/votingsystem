package org.votingsystem.controlcenter.controller

import com.sun.syndication.feed.synd.SyndContentImpl
import com.sun.syndication.feed.synd.SyndEntryImpl
import com.sun.syndication.feed.synd.SyndFeed
import com.sun.syndication.feed.synd.SyndFeedImpl
import com.sun.syndication.io.SyndFeedOutput
import grails.converters.JSON
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.HttpHelper
import org.votingsystem.util.StringUtils

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
		MessageSMIME messageSMIME = request.messageSMIMEReq
		if(!messageSMIME) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
		}
		SMIMEMessageWrapper smimeMessageReq = messageSMIME.getSmimeMessage()
        def messageJSON = JSON.parse(smimeMessageReq.getSignedContent())
		if (messageJSON.serverURL) {
            String serverURL = StringUtils.checkURL(messageJSON.serverURL)
			AccessControlVS accessControl = AccessControlVS.findWhere(serverURL:serverURL)
			if (accessControl) {
                return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code: 'controlCenterAlreadyAssociatedMsg', args:[accessControl.serverURL]))]
			} else {
				String accessControlURL = "${serverURL}/serverInfo"
				ResponseVS responseVS = HttpHelper.getInstance().getData(accessControlURL, ContentTypeVS.JSON)
				if (ResponseVS.SC_OK == responseVS.statusCode) {
                    AccessControlVS actorVS = ActorVS.parse(responseVS.message)
                    actorVS.save()
                    responseVS.setMessage( message(code: 'controlCenterAssociatedMsg', args:[accessControlURL]))
				}
                return [responseVS : responseVS]
            }
        } else return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code: 'requestWithErrors'))]
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
			render(text: getElectionsNews(params.feedType), contentType:"text/xml", encoding:"UTF-8")
		} else {
			if(params.feedType && "rss".equals(params.feedType)) {
				render(text: getElectionsNews("rss_1.0"), contentType:"text/xml", encoding:"UTF-8")
			} else if(params.feedType && "atom".equals(params.feedType)) {
				render(text: getElectionsNews("atom_1.0"), contentType:"text/xml", encoding:"UTF-8")
			} else {
				render(text: getElectionsNews("atom_1.0"), contentType:"text/xml", encoding:"UTF-8")
			}
            return false;
		}
	}
	
	private String getElectionsNews(feedType) {
		def elections
		EventVS.withTransaction {
			elections = EventVS.findAllWhere(state:EventVS.State.ACTIVE, [max: 30, sort: "dateCreated", order: "desc"])
		}
		def news = []
		elections.each { votacion ->
			String content = votacion.content
			String author = message(code: 'publishedBySystem')
			if(votacion.userVS) { author = "${votacion.userVS.firstName} ${votacion.userVS.lastName}" }
			String urlVotacion = "${createLink(controller: 'eventVSElection')}/" + votacion.id
			String accion = message(code: 'electionFeedAction')
			 //if(content?.length() > 500) content = content.substring(0, 500)
			String feedContent = "<p>${content}</p>" + "<a href='${urlVotacion}'>${accion}</a>"
			def desc = new SyndContentImpl(type: "text/html", value: feedContent);
			def newFeed = new SyndEntryImpl(title: votacion.subject, author:author,
					link: urlVotacion, publishedDate: votacion.dateCreated, description: desc);
			news.add(newFeed);
		}
		String feedsTitle = "${message(code: 'electionFeedsTitle')}" +
		" -${grailsApplication.config.VotingSystem.serverName}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType, title: feedsTitle,
				link: "${grailsApplication.config.grails.serverURL}/app/home#VOTACIONES",
				description: message(code: 'subscriptionVS.descripcionSubscripcionVotaciones'), entries: news);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}

    def feeds () {

    }

}