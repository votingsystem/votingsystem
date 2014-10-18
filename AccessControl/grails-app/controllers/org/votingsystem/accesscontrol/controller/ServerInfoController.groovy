package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.ExceptionVS

/**
 * @infoController Información de la aplicación
 * @descController Servicios que ofrecen datos sobre la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class ServerInfoController {

    def signatureVSService
	def timeStampService
    def systemService
    
	/**
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 * @return documento JSON con datos de la aplicación
	 */
	def index() {
        HashMap serverInfo = new HashMap()
        serverInfo.name = grailsApplication.config.VotingSystem.serverName
        serverInfo.serverType = ActorVS.Type.ACCESS_CONTROL.toString()
        serverInfo.serverURL = "${grailsApplication.config.grails.serverURL}"
        serverInfo.urlTimeStampServer="${grailsApplication.config.VotingSystem.urlTimeStampServer}"
        serverInfo.urlBlog = grailsApplication.config.VotingSystem.blogURL
		serverInfo.state = ActorVS.State.OK.toString()
        serverInfo.environmentMode =  grails.util.Environment.current.toString()
        def controlCenter = systemService.getControlCenter()
        if(controlCenter) serverInfo.controlCenter = [id:controlCenter.id, name:controlCenter.name, serverURL:controlCenter.serverURL,
                state:controlCenter.state?.toString(), dateCreated:controlCenter.dateCreated]
		serverInfo.certChainURL = "${createLink(controller: 'certificateVS', action:'certChain', absolute:true)}"
		File certChain = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.certChainPath).getFile();
		serverInfo.certChainPEM = certChain?.text
		serverInfo.timeStampCertPEM = new String(timeStampService.getSigningCertPEMBytes())
		
		response.setHeader('Access-Control-Allow-Origin', "*")
		
		if (params.callback) render "${params.callback}(${serverInfo as JSON})"
        else render serverInfo as JSON
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
	 * <br/><u>SERVICIO DE PRUEBAS - DATOS FICTICIOS</u>. El esquema actual de certificación en plataformas
	 * Android pasa por que el usuario tenga que identificarse en un centro autorizado
	 * para poder instalar en su deviceVS el certificado de identificación.
	 *
	 * @httpMethod [GET]
	 * @return Direcciones a las que tendrían que ir los usuarios para poder get un certificado
	 * 		   de identificación.
	 */
	def certificationCenters () {  }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}