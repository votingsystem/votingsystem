package org.votingsystem.accesscontrol.controller

import grails.converters.JSON

import org.votingsystem.accesscontrol.model.*

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
    
    def firmaService
	def timeStampService
    
	/**
	 * @httpMethod [GET]
	 * @responseContentType [application/json]
	 * @return Documento JSON con datos de la aplicación
	 */
	def index() { 
        HashMap infoServidor = new HashMap()
        infoServidor.centrosDeControl = []
        infoServidor.nombre = grailsApplication.config.VotingSystem.serverName
        infoServidor.serverType = TypeVS.CONTROL_ACCESO.toString()
        infoServidor.serverURL = "${grailsApplication.config.grails.serverURL}"
        infoServidor.urlBlog = grailsApplication.config.VotingSystem.blogURL
		infoServidor.estado = ActorConIP.Estado.ACTIVO.toString()
		infoServidor.environmentMode = VotingSystemApplicationContex.instance.getEnvironment().toString()
        def centrosDeControl = CentroControl.findAllWhere(estado: ActorConIP.Estado.ACTIVO)
        centrosDeControl?.each {centroControl ->
            def centroControlMap = [id:centroControl.id, nombre:centroControl.nombre,
                estado:centroControl.estado?.toString(),
                serverURL:centroControl.serverURL, fechaCreacion:centroControl.dateCreated]
            infoServidor.centrosDeControl.add(centroControlMap)
        }
		infoServidor.cadenaCertificacionURL = "${createLink(controller: 'certificado', action:'cadenaCertificacion', absolute:true)}"
		File cadenaCertificacion = grailsApplication.mainContext.getResource(
			grailsApplication.config.VotingSystem.certChainPath).getFile();
		infoServidor.cadenaCertificacionPEM = cadenaCertificacion?.text
		infoServidor.timeStampCertPEM = new String(timeStampService.getSigningCert())

		
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
	
	
	/**
	 * <br/><u>SERVICIO DE PRUEBAS - DATOS FICTICIOS</u>. El esquema actual de certificación en plataformas
	 * Android pasa por que el usuario tenga que identificarse en un centro autorizado
	 * para poder instalar en su dispositivo el certificado de identificación.
	 *
	 * @httpMethod [GET]
	 * @return Direcciones a las que tendrían que ir los usuarios para poder obtener un certificado
	 * 		   de identificación.
	 */
	def centrosCertificacion () {  }
	
	def testing() {}
}
