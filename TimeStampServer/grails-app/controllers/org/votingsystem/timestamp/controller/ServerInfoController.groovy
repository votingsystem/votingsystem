package org.votingsystem.timestamp.controller

import grails.converters.JSON
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS

/**
 * @infoController Información de la aplicación
 * @descController Servicios que ofrecen datos sobre la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class ServerInfoController {

	def timeStampService
    
	/**
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 * @return documento JSON con datos de la aplicación
	 */
	def index() {
        HashMap serverInfo = new HashMap()
        serverInfo.certChainPEM = new String(timeStampService.getSigningCertChainPEMBytes())
        serverInfo.certChainURL = "${createLink(controller: 'serverInfo', action:'certChain', absolute:true)}"
        serverInfo.name = grailsApplication.config.VotingSystem.serverName
        serverInfo.serverType = ActorVS.Type.TIMESTAMP_SERVER.toString()
        serverInfo.serverURL = "${grailsApplication.config.grails.serverURL}"
		serverInfo.state = ActorVS.State.OK.toString()
        serverInfo.environmentMode =  grails.util.Environment.current.toString()
		response.setHeader('Access-Control-Allow-Origin', "*")
		if (params.callback) render "${params.callback}(${serverInfo as JSON})"
        else render serverInfo as JSON
	}

    /**
     * @httpMethod [GET]
     * @return La cadena de certificación en formato PEM del servidor
     */
    def certChain () {
        byte[] serverCertPEMBytes = timeStampService.getSigningCertChainPEMBytes()
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.TEXT,
                messageBytes:serverCertPEMBytes)]
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }

}