package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.util.CertUtils

/**
 * @infoController Información de la aplicación
 * @descController Servicios que ofrecen datos sobre la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class ServerInfoController {

	def timeStampService
    def signatureVSService
    
	/**
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 * @return documento JSON con datos de la aplicación
	 */
	def index() {
        HashMap serverInfo = new HashMap()
        File certChainFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath).getFile();
        serverInfo.certChainPEM = certChainFile.text
        serverInfo.certChainURL = "${createLink(controller: 'serverInfo', action:'certChain', absolute:true)}"
        serverInfo.name = grailsApplication.config.VotingSystem.serverName
        serverInfo.serverType = ActorVS.Type.VICKETS.toString()
        serverInfo.serverURL = "${grailsApplication.config.grails.serverURL}"
        serverInfo.webSocketURL = "${grailsApplication.config.webSocketURL}"
		serverInfo.state = ActorVS.State.OK.toString()
        serverInfo.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        byte [] signingCertPEMBytes = timeStampService.getSigningCertPEMBytes()
        if(signingCertPEMBytes) serverInfo.timeStampCertPEM = new String(signingCertPEMBytes)
        serverInfo.environmentMode = grails.util.Environment.current.toString()
		response.setHeader('Access-Control-Allow-Origin', "*")
		if (params.callback) render "${params.callback}(${serverInfo as JSON})"
        else render serverInfo as JSON
	}

    /**
     * @httpMethod [GET]
     * @return La cadena de certificación en formato PEM del servidor
     */
    def certChain () {
        byte[] serverCertPEMBytes = CertUtils.getPEMEncoded (signatureVSService.getServerCert())
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.TEXT,
                messageBytes:serverCertPEMBytes)]
    }

    /**
     * @httpMethod [GET]
     * @return La lista de servicios de la aplicación
     */
    def serviceList () { }

    /**
     * @httpMethod [GET]
     * @return Datos de las versiones de algunos componentes de la aplicación
     */
    def appData () { }

    /**
     * @httpMethod [GET]
     * @return Información general de la aplicación
     */
    def info () { }


    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
