package org.sistemavotacion.centrocontrol

import grails.converters.JSON
import org.sistemavotacion.centrocontrol.modelo.*
import grails.util.Environment

class InfoServidorController {

	public enum Estado {SUSPENDIDO, ACTIVO, INACTIVO}
	
	def index = { }
	
	def listaServicios = { }
	
	def datosAplicacion = { }
	
	def informacion = { }
	
    def obtener = {
        HashMap infoServidor = new HashMap()
        infoServidor.nombre = grailsApplication.config.SistemaVotacion.serverName
		infoServidor.tipoServidor = Tipo.CENTRO_CONTROL.toString()
        infoServidor.serverURL = grailsApplication.config.grails.serverURL
        infoServidor.urlBlog = grailsApplication.config.SistemaVotacion.urlBlog
        infoServidor.estado = Estado.ACTIVO.toString()
		infoServidor.environmentMode = Environment.current.toString()
		def sufijoURLCadenaCertificacion = grailsApplication.config.SistemaVotacion.sufijoURLCadenaCertificacion
		infoServidor.cadenaCertificacionURL = "${grailsApplication.config.grails.serverURL}${sufijoURLCadenaCertificacion}"
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
