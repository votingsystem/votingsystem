package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.util.ApplicationContextHolder;
import org.votingsystem.model.ActorVS
/**
 * @infoController Información de la aplicación
 * @descController Servicios que ofrecen datos sobre la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class ServerInfoController {

    def signatureVSService
	def timeStampVSService
    
	/**
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 * @return PDFDocumentVS JSON con datos de la aplicación
	 */
	def index() { 
        HashMap serverInfo = new HashMap()
        serverInfo.controlCenters = []
        serverInfo.name = grailsApplication.config.VotingSystem.serverName
        serverInfo.serverType = ActorVS.Type.ACCESS_CONTROL.toString()
        serverInfo.serverURL = "${grailsApplication.config.grails.serverURL}"
        serverInfo.urlBlog = grailsApplication.config.VotingSystem.blogURL
		serverInfo.state = ActorVS.State.RUNNING.toString()
		serverInfo.environmentMode = ApplicationContextHolder.getEnvironment().toString()
        def controlCenters = ControlCenterVS.findAllWhere(state: ActorVS.State.RUNNING)
        controlCenters?.each {controlCenter ->
            def controlCenterMap = [id:controlCenter.id, name:controlCenter.name, serverURL:controlCenter.serverURL,
                state:controlCenter.state?.toString(), dateCreated:controlCenter.dateCreated]
            serverInfo.controlCenters.add(controlCenterMap)
        }
		serverInfo.certChainURL = "${createLink(controller: 'certificateVS', action:'certChain', absolute:true)}"
		File certChain = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.certChainPath).getFile();
		serverInfo.certChainPEM = certChain?.text
		serverInfo.timeStampCertPEM = new String(timeStampVSService.getSigningCert())
		
		response.setHeader('Access-Control-Allow-Origin', "*")
		
		if (params.callback) render "${params.callback}(${serverInfo as JSON})"
        else render serverInfo as JSON
	}
	
	/**
	 * @httpMethod [GET]
	 * @return La lista de servicios de la aplicación
	 */
	def listaServicios () { }
	
	/**
	 * @httpMethod [GET]
	 * @return Datos de las versiones de algunos componentes de la aplicación
	 */
	def datosAplicacion () { }
	
	/**
	 * @httpMethod [GET]
	 * @return Información general de la aplicación
	 */
	def info () { }
	
	
	/**
	 * <br/><u>SERVICIO DE PRUEBAS - DATOS FICTICIOS</u>. El esquema actual de certificación en plataformas
	 * Android pasa por que el userVS tenga que identificarse en un centro autorizado
	 * para poder instalar en su deviceVS el certificateVS de identificación.
	 *
	 * @httpMethod [GET]
	 * @return Direcciones a las que tendrían que ir los usuarios para poder get un certificateVS
	 * 		   de identificación.
	 */
	def certificationCenters () {  }
	
	def testing() {}
}
