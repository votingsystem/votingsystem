package org.sistemavotacion.controlacceso

import grails.converters.JSON
import org.sistemavotacion.controlacceso.modelo.*
import grails.util.Environment

/**
 * @infoController Información de la aplicación
 * @descController Servicios que ofrecen datos sobre la aplicación
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 * */
class InfoServidorController {
    
    def firmaService
    
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/infoServidor'
	 */
	def index () { }
	
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
	 * @return Datos en formato JSON de la aplicación
	 */
    def obtener () {
        HashMap infoServidor = new HashMap()
        infoServidor.centrosDeControl = []
        infoServidor.nombre = grailsApplication.config.SistemaVotacion.serverName
        infoServidor.tipoServidor = Tipo.CONTROL_ACCESO.toString()
        infoServidor.serverURL = grailsApplication.config.grails.serverURL
        infoServidor.urlBlog = grailsApplication.config.SistemaVotacion.urlBlog
		infoServidor.estado = ActorConIP.Estado.ACTIVO.toString()
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
	
	/**
	 * <br/><u>SERVICIO DE PRUEBAS - DATOS FICTICIOS</u>. El esquema actual de certificación en plataformas
	 * Android pasa por que el usuario tenga que identificarse en un centro autorizado
	 * para poder instalar en su dispositivo el certificado de identificación.
	 *
	 * @httpMethod GET
	 * @return Direcciones a las que tendrían que ir los usuarios para poder obtener un certificado
	 * 		   de identificación.
	 */
	def centrosCertificacion () {  }
}
