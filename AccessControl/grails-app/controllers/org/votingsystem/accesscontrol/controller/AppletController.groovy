package org.votingsystem.accesscontrol.controller

import org.votingsystem.accesscontrol.model.*
import org.springframework.web.servlet.support.RequestContextUtils as RCU


/**
 * @infoController Applet
 * @descController Servicios relacionados con los applets de la aplicación.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
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