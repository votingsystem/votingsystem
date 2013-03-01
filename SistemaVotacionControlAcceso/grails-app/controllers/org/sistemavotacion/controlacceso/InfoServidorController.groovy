package org.sistemavotacion.controlacceso

import grails.converters.JSON
import org.sistemavotacion.controlacceso.modelo.*
import grails.util.Environment

class InfoServidorController {

    public enum Estado {SUSPENDIDO, ACTIVO, INACTIVO}
    
    def firmaService
    
	def index = { }
	
	def listaServicios = { }
	
	def informacion = { }
	
	def datosAplicacion = { }
	
	def centrosCertificacion = { 
		
	}
	
    def obtener = {
        HashMap infoServidor = new HashMap()
        infoServidor.centrosDeControl = []
        infoServidor.nombre = grailsApplication.config.SistemaVotacion.serverName
        infoServidor.tipoServidor = Tipo.CONTROL_ACCESO.toString()
        infoServidor.serverURL = grailsApplication.config.grails.serverURL
        infoServidor.urlBlog = grailsApplication.config.SistemaVotacion.urlBlog
		infoServidor.estado = Estado.ACTIVO.toString()
		infoServidor.environmentMode = Environment.current.toString()
        List<CentroControl> centrosDeControl = CentroControl.getAll()
        centrosDeControl?.collect {centroControl ->
            def centroControlMap = [id:centroControl.id, nombre:centroControl.nombre,
                estado:centroControl.estado?.toString(),
                serverURL:centroControl.serverURL, fechaCreacion:centroControl.dateCreated]
            infoServidor.centrosDeControl.add(centroControlMap)
        }
		def sufijoURLCadenaCertificacion = grailsApplication.config.SistemaVotacion.sufijoURLCadenaCertificacion
		infoServidor.cadenaCertificacionURL = "${grailsApplication.config.grails.serverURL}${sufijoURLCadenaCertificacion}"
		if (params.callback) render "${params.callback}(${infoServidor as JSON})"
        else render infoServidor as JSON
    }

}
