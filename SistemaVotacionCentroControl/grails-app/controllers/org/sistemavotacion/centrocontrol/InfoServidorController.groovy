package org.sistemavotacion.centrocontrol

import grails.converters.JSON
import org.sistemavotacion.centrocontrol.modelo.*
import grails.util.Environment


/**
 * @infoController Información de la aplicación
 * @descController Servicios que ofrecen datos sobre la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 * */
class InfoServidorController {	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/infoServidor'
	 */
	def index() { 
		redirect action: "restDoc"
	}
	
	/**
	 * @httpMethod GET
	 * @return La lista de servicios de la aplicación
	 */
	def listaServicios () { }
	
	/**
	 * @httpMethod GET
	 * @return Datos de las versiones de algunos componentes de la aplicación  
	 */
	def datosAplicacion () { }
	
	/**
	 * @httpMethod GET
	 * @return Información general de la aplicación
	 */
	def informacion () { }
	
	/**
	 * @httpMethod GET
	 * @return Documento JSON con datos de la aplicación
	 */
    def obtener () {
        HashMap infoServidor = new HashMap()
        infoServidor.nombre = grailsApplication.config.SistemaVotacion.serverName
		infoServidor.tipoServidor = Tipo.CENTRO_CONTROL.toString()
        infoServidor.serverURL = grailsApplication.config.grails.serverURL
        infoServidor.urlBlog = grailsApplication.config.SistemaVotacion.urlBlog
        infoServidor.estado = ActorConIP.Estado.ACTIVO.toString()
		infoServidor.environmentMode = Environment.current.toString()
		def sufijoURLCadenaCertificacion = grailsApplication.config.SistemaVotacion.sufijoURLCadenaCertificacion
		infoServidor.cadenaCertificacionURL = "${grailsApplication.config.grails.serverURL}${sufijoURLCadenaCertificacion}"
		File cadenaCertificacion = grailsApplication.mainContext.getResource(
			grailsApplication.config.SistemaVotacion.rutaCadenaCertificacion).getFile();
		infoServidor.cadenaCertificacionPEM = cadenaCertificacion?.text
		def controlesAcceso = ControlAcceso.getAll()
		infoServidor.controlesAcceso = []
		controlesAcceso?.collect {controlAcceso ->
			def controlesAccesoMap = [nombre:controlAcceso.nombre, serverURL:controlAcceso.serverURL,
				estado:controlAcceso.estado.toString()]
			infoServidor.controlesAcceso.add(controlesAccesoMap)
		}
		if (params.callback) render "${params.callback}(${infoServidor as JSON})"
        else render infoServidor as JSON
    }
}
