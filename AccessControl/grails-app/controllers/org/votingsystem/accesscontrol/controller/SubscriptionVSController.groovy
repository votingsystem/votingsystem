package org.votingsystem.accesscontrol.controller

import com.sun.syndication.feed.synd.SyndContentImpl
import com.sun.syndication.feed.synd.SyndEntryImpl
import com.sun.syndication.feed.synd.SyndFeed
import com.sun.syndication.feed.synd.SyndFeedImpl
import com.sun.syndication.io.SyndFeedOutput
import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSClaim
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.EventVSManifest
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubSystemVS
import org.votingsystem.util.StringUtils
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
class SubscriptionVSController {
	
    
    def subscriptionVSService
	def userVSService
	
	def supportedFormats = [ "rss_0.90", "rss_0.91", "rss_0.92", "rss_0.93", 
		"rss_0.94", "rss_1.0", "rss_2.0", "atom_0.3"]
	
	/**
	 * Servicio que da de alta Centros de Control.
	 *
	 * @httpMethod [POST]
	 * @serviceURL [/subscriptionVS]
	 * @requestContentType [application/x-pkcs7-signature, application/x-pkcs7-mime] Obligatorio. 
	 *					Archivo con los datos del Centro de Control que se desea dar de alta.
	 */
	def index() { 
		MessageSMIME messageSMIME = request.messageSMIMEReq
		if(!messageSMIME) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,message(code: "requestWithoutFile"))]
		}
		ResponseVS responseVS = subscriptionVSService.matchControlCenter(messageSMIME, request.getLocale())
		if(ResponseVS.SC_OK == responseVS.statusCode) {
            responseVS.data = userVSService.getControlCenterMap(responseVS.data.controlCenterVS)
            responseVS.setContentType(ContentTypeVS.JSON)
		}
        return [responseVS : responseVS]
	}

    /**
     * Servicio que comprueba Centros de Control. Recibe como parámetro la URL del servidor que se desea comprobar.
     *
     * @httpMethod [GET]
     * @serviceURL [/subscriptionVS/checkControlCenter]
     *
     * @param [serverURL] Obligatorio. La URL del Centro de Control que se desee comprobar
     * @responseContentType [application/json]
     * @return documento JSON con el mapa de datos del Centro de Control solicitado
     * @requestContentType [application/] Obligatorio.
     *					Archivo con los datos del Centro de Control que se desea dar de alta.
     */
    def checkControlCenter() {
        if(!params.serverURL) {
            return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                    message(code: 'missingParamErrorMsg', args:["serverURL"]))]
        } else {
            String serverURL = StringUtils.checkURL(params.serverURL)
            ControlCenterVS controlCenter = ControlCenterVS.findWhere(serverURL:serverURL)
            if(!controlCenter) {
                return [responseVS : new ResponseVS(ResponseVS.SC_ERROR_REQUEST,
                        message(code: 'serverNotFoundErrorMsg', args:[serverURL]))]
            } else {
                Map controlCenterMap = userVSService.getControlCenterMap(controlCenter)
                render controlCenterMap as JSON
            }
        }
    }

	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/subscriptionVS/reclamaciones/$feedType]
	 * @param	[feedType] Opcional. Formatos de feed soportados rss_0.90, rss_0.91, rss_0.92, 
	 * 			rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3. Por defecto se sirve atom 1.0.
	 * @requestContentType [text/xml]
	 * @return Información en el formato solicitado sobre las reclamaciones publicadas.
	 */
	def claims() {
		if(params.feedType && supportedFormats.contains(params.feedType)) {
			render(text: getClaimsFeeds(params.feedType), contentType:"text/xml", encoding:"UTF-8")
		} else {
			if(params.feedType && "rss".equals(params.feedType)) {
				render(text: getClaimsFeeds("rss_1.0"), contentType:"text/xml", encoding:"UTF-8")
			} else if(params.feedType && "atom".equals(params.feedType)) {
				render(text: getClaimsFeeds("atom_1.0"), contentType:"text/xml", encoding:"UTF-8")
			} else  render(text: getClaimsFeeds("atom_1.0"), contentType:"text/xml", encoding:"UTF-8")
            return false;
		}
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
			render(text: getElectionFeeds(params.feedType), contentType:"text/xml", encoding:"UTF-8")
		} else {
			if(params.feedType && "rss".equals(params.feedType)) {
				render(text: getElectionFeeds("rss_1.0"), contentType:"text/xml", encoding:"UTF-8")
			} else if(params.feedType && "atom".equals(params.feedType)) {
				render(text: getElectionFeeds("atom_1.0"), contentType:"text/xml", encoding:"UTF-8")
			} else  render(text: getElectionFeeds("atom_1.0"), contentType:"text/xml", encoding:"UTF-8")
            return false;
		}
	}
	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/subscriptionVS/manifiestos/$feedType]
	 * @param	[feedType] Opcional. Formatos de feed soportados rss_0.90, rss_0.91, rss_0.92,
	 * 			rss_0.93, rss_0.94, rss_1.0, rss_2.0, atom_0.3. Por defecto se sirve atom 1.0.
	 * @requestContentType [text/xml]
	 * @return Información en el formato solicitado sobre los manifiestos publicados.
	 */
    def manifests() {
		if(params.feedType && supportedFormats.contains(params.feedType)) {
			render(text: getManifestsNews(params.feedType), contentType:"text/xml", encoding:"UTF-8")
		} else {
			if(params.feedType && "rss".equals(params.feedType)) {
				render(text: getManifestsNews("rss_1.0"), contentType:"text/xml", encoding:"UTF-8")
			} else if(params.feedType && "atom".equals(params.feedType)) {
				render(text: getManifestsNews("atom_1.0"), contentType:"text/xml", encoding:"UTF-8")
			} else  render(text: getManifestsNews("atom_1.0"), contentType:"text/xml", encoding:"UTF-8")
            return false;
		}
	}
	
	private String getClaimsFeeds(feedType) {
		def claims = EventVSClaim.findAllWhere(state:EventVS.State.ACTIVE,
			[max: 30, sort: "dateCreated", order: "desc"])
		def entradas = []
		claims.each { claim ->
			String content = claim.content
			String author = message(code: 'publishedBySystem')
			if(claim.userVS) {
				author = "${claim.userVS.firstName} ${claim.userVS.lastName}"
			}
			String urlReclamacion =  "${createLink(controller: 'eventVSClaim')}/" + claim.id
			String accion = message(code: 'signClaim')
			 //if(content?.length() > 500) content = content.substring(0, 500)
			String feedContent = "<p>${content}</p>" +
				"<a href='${urlReclamacion}'>${accion}</a>"
			def desc = new SyndContentImpl(type: "text/html", value: feedContent);
			def entrada = new SyndEntryImpl(title: claim.subject, author:author,
					link: urlReclamacion,
					publishedDate: claim.dateCreated, description: desc);
			entradas.add(entrada);
		}
		String tituloSubscripcionReclamaciones = "${message(code: 'claimsSubscriptionTitle')}" +
		" -${grailsApplication.config.VotingSystem.serverName}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType,
				title: tituloSubscripcionReclamaciones,
				link: "${createLink(controller: 'eventVSClaim', action: 'main')}",
				description: message(code: 'claimsSubscriptionDescription'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}
	
	private String getElectionFeeds(feedType) {
		def elections
		EventVSElection.withTransaction {
			elections = EventVSElection.findAllWhere(state:EventVS.State.ACTIVE,
				[max: 30, sort: "dateCreated", order: "desc"])
		}
		def entradas = []
		elections.each { votacion ->
			String content = votacion.content
			String author = message(code: 'publishedBySystem')
			if(votacion.userVS) {
				author = "${votacion.userVS.firstName} ${votacion.userVS.lastName}"
			}
			String urlVotacion =  "${createLink(controller: 'eventVSElection')}/" + votacion.id
			String accion = message(code: 'vote')
			 //if(content?.length() > 500) content = content.substring(0, 500)
			String feedContent = "<p>${content}</p>" +
				"<a href='${urlVotacion}'>${accion}</a>"
			def desc = new SyndContentImpl(type: "text/html", value: feedContent);
			def entrada = new SyndEntryImpl(title: votacion.subject, author:author,
					link: urlVotacion,
					publishedDate: votacion.dateCreated, description: desc);
			entradas.add(entrada);
		}
		String tituloSubscripcionVotaciones = "${message(code: 'electionsSubscriptionTitle')}" +
		" -${grailsApplication.config.VotingSystem.serverName}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType,
				title: tituloSubscripcionVotaciones,
				link: "${createLink(controller: 'eventVSElection', action: 'main')}",
				description: message(code: 'electionsSubscriptionDescription'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}

	private String getManifestsNews(feedType) {
		def manfiestos
		EventVSManifest.withTransaction {
			manfiestos = EventVSManifest.findAllWhere(state:EventVS.State.ACTIVE,
				[max: 30, sort: "dateCreated", order: "desc"])
		}
		def entradas = []
		manfiestos.each { manifest ->
			String content = manifest.content
			String author = message(code: 'publishedBySystem')
			if(manifest.userVS) {
				author = "${manifest.userVS.firstName} ${manifest.userVS.lastName}"
			}
			String manifestURL = "${createLink(controller: 'eventVSManifest')}/" + manifest.id
			String accion = message(code: 'signManifest')
 			//if(content?.length() > 500) content = content.substring(0, 500)
			String feedContent = "<p>${content}</p>" +
				"<a href='${manifestURL}'>${accion}</a>"
			def desc = new SyndContentImpl(type: "text/html", value: feedContent);
			def entrada = new SyndEntryImpl(title: manifest.subject, author:author,
					link: manifestURL,
					publishedDate: manifest.dateCreated, description: desc);
			entradas.add(entrada);
		}
		String manifestSubscriptionTitle = "${message(code: 'manifestSubscriptionTitle')}" +
		" -${grailsApplication.config.VotingSystem.serverName}-"
		SyndFeed feed = new SyndFeedImpl(feedType: feedType, 
				title: manifestSubscriptionTitle,
				link: "${createLink(controller: 'eventVSManifest', action: 'main')}",
				description: message(code: 'manifestSubscriptionDescription'),
				entries: entradas);
		StringWriter writer = new StringWriter();
		SyndFeedOutput output = new SyndFeedOutput();
		output.output(feed,writer);
		writer.close();
		return writer.toString();
	}

    def feeds() {
        render(view:"feeds" , model:[selectedSubsystem:SubSystemVS.FEEDS.toString()])
    }
}
