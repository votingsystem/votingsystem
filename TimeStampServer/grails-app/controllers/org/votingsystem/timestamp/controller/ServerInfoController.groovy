package org.votingsystem.timestamp.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
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
        serverInfo.name = grailsApplication.config.vs.serverName
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
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}