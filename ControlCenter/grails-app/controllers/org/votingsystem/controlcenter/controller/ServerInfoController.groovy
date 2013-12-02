package org.votingsystem.controlcenter.controller

import grails.converters.JSON
import org.votingsystem.model.AccessControlVS
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

	/**
	 * @httpMethod [GET]
	 * @serviceURL [/serverInfo]
	 * @responseContentType [application/json]
	 * @return Documento JSON con datos de la aplicación
	 */
	def index() { 
        HashMap serverInfo = new HashMap()
        serverInfo.name = grailsApplication.config.VotingSystem.serverName
		serverInfo.serverType = ActorVS.Type.CONTROL_CENTER.toString()
        serverInfo.serverURL = "${grailsApplication.config.grails.serverURL}"
        serverInfo.urlBlog = grailsApplication.config.VotingSystem.blogURL
        serverInfo.state = ActorVS.State.RUNNING.toString()
		serverInfo.environmentMode = ApplicationContextHolder.getEnvironment().toString()
		serverInfo.certChainURL = "${createLink(controller: 'certificateVS', action:'certChain', absolute:true)}"
		serverInfo.certChainPEM = signatureVSService.getServerCertChain().text
		def controlesAcceso = AccessControlVS.getAll()
		serverInfo.controlesAcceso = []
		controlesAcceso?.each {accessControl ->
			def controlesAccesoMap = [name:accessControl.name, serverURL:accessControl.serverURL,
				state:accessControl.state.toString()]
			serverInfo.controlesAcceso.add(controlesAccesoMap)
		}

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


}
