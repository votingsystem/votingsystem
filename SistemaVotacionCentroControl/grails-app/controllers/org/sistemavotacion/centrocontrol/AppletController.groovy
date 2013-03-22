package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*
import org.springframework.web.servlet.support.RequestContextUtils as RCU



class AppletController {
	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/applet'.
	 */
	def index() { 
		redirect action: "restDoc"
	}
	
	/**
	 * @httpMethod GET
	 * @return Página HTML que sirve para cargar el Applet principal de firma.
	 */
	def cliente () { }
	
	/**
	 * @httpMethod GET
	 * @return Página HTML que sirve para cargar el Applet principal de la herramienta de validación
	 * 		   de archivos firmados y de copias de seguridad.
	 */
	def herramientaValidacion () { }
	
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