package org.sistemavotacion.controlacceso

import org.sistemavotacion.controlacceso.modelo.*
import org.springframework.web.servlet.support.RequestContextUtils as RCU


/**
 * @infoController Applet
 * @descController Servicios relacionados con los applets de la aplicación.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 */
class AppletController {
	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/applet'.
	 */
	def index () { }
	
	/**
	 * @httpMethod GET
	 * @return Página HTML que sirve para cargar el Applet principal de firma.
	 */
	def cliente () { }
	
	/**
	 * @httpMethod GET
	 * @return Archivo JNLP con los datos del Applet principal de firma.
	 */
	def jnlpCliente () {
		render (view:"jnlpCliente", contentType: "application/x-java-jnlp-file")
	}
	
	/**
	 * @httpMethod GET
	 * @return Archivo JNLP con los datos del Applet de la herramienta de validación de archivos firmados
	 *         y copias de seguridad.
	 */
	def jnlpHerramienta () {
		render (view:"jnlpHerramienta", contentType: "application/x-java-jnlp-file")
	}
	
}