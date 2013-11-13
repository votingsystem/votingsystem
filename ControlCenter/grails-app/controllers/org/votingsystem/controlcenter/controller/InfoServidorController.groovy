package org.votingsystem.controlcenter.controller

import grails.converters.JSON

import org.votingsystem.controlcenter.model.*

import grails.util.Environment

import org.votingsystem.groovy.util.*
import org.votingsystem.model.TypeVS;

/**
 * @infoController Información de la aplicación
 * @descController Servicios que ofrecen datos sobre la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class InfoServidorController {	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/infoServidor]
	 * @responseContentType [application/json]
	 * @return Documento JSON con datos de la aplicación
	 */
	def index() { 
        HashMap infoServidor = new HashMap()
        infoServidor.nombre = grailsApplication.config.VotingSystem.serverName
		infoServidor.serverType = TypeVS.CONTROL_CENTER.toString()
        infoServidor.serverURL = "${grailsApplication.config.grails.serverURL}"
        infoServidor.urlBlog = grailsApplication.config.VotingSystem.blogURL
        infoServidor.estado = ActorConIP.Estado.ACTIVO.toString()
		infoServidor.environmentMode = VotingSystemApplicationContex.instance.getEnvironment().toString()
		infoServidor.cadenaCertificacionURL = "${createLink(controller: 'certificado', action:'cadenaCertificacion', absolute:true)}"
		File cadenaCertificacion = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.certChainPath).getFile();
		infoServidor.cadenaCertificacionPEM = cadenaCertificacion?.text
		def controlesAcceso = ControlAcceso.getAll()
		infoServidor.controlesAcceso = []
		controlesAcceso?.each {controlAcceso ->
			def controlesAccesoMap = [nombre:controlAcceso.nombre, serverURL:controlAcceso.serverURL,
				estado:controlAcceso.estado.toString()]
			infoServidor.controlesAcceso.add(controlesAccesoMap)
		}

		response.setHeader('Access-Control-Allow-Origin', "*")
		
		if (params.callback) render "${params.callback}(${infoServidor as JSON})"
        else render infoServidor as JSON
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
	def informacion () { }
	
	def testing() {}

}
