package org.sistemavotacion.centrocontrol

import org.sistemavotacion.centrocontrol.modelo.*
import org.springframework.web.servlet.support.RequestContextUtils as RCU



class AppletController {
	
	/**
	 * @httpMethod [GET]
	 * @return Página HTML que sirve para cargar el Applet principal de firma.
	 */
	def cliente () { }
	
	/**
	 * @httpMethod [GET]
	 * @return Página HTML que sirve para cargar el Applet principal de la herramienta de validación
	 * 		   de archivos firmados y de copias de seguridad.
	 */
	def herramientaValidacion () { }
		
}