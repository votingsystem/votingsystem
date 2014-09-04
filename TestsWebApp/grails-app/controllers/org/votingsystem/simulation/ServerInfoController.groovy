package org.votingsystem.simulation

import grails.converters.JSON
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.ApplicationContextHolder


/**
 * @infoController Información de la aplicación
 * @descController Servicios que ofrecen datos sobre la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class ServerInfoController {

	/**
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 * @return documento JSON con datos de la aplicación
	 */
	def index() {
        HashMap serverInfo = new HashMap()
        serverInfo.controlCenters = []
        serverInfo.name = grailsApplication.config.VotingSystem.serverName
        serverInfo.serverURL = "${grailsApplication.config.grails.serverURL}"
        serverInfo.state = ActorVS.State.OK.toString()
        serverInfo.serverType = ActorVS.Type.CONTROL_CENTER.toString();
        File certChain = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath).getFile();
        serverInfo.certChainPEM = certChain?.text
        serverInfo.certChainURL = "${createLink(controller: 'serverInfo', action:'certChain', absolute:true)}"
        response.setHeader('Access-Control-Allow-Origin', "*")
        if (params.callback) render "${params.callback}(${serverInfo as JSON})"
        else render serverInfo as JSON
    }

    /**
     * @httpMethod [GET]
     * @return La cadena de certificación en formato PEM del servidor
     */
    def certChain () {
        File certChainPEMFile = grailsApplication.mainContext.getResource(
                grailsApplication.config.VotingSystem.certChainPath).getFile();
        byte[] fileBytes = certChainPEMFile.getBytes()
        response.status = ResponseVS.SC_OK
        response.contentLength = fileBytes.length
        response.outputStream <<  fileBytes
        response.outputStream.flush()
        return false
    }

    def appData() {}

	def testing() {}

}
