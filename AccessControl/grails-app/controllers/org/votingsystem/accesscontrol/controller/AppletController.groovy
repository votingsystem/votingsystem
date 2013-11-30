package org.votingsystem.accesscontrol.controller
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
	 * @return Página HTML que sirve para cargar el Applet principal de signatureVS.
	 */
	def client () { }
	
	/**
	* @httpMethod [GET]
	* @return Página HTML que sirve para cargar el Applet principal de la herramienta de validación
	* 		   de archivos firmados y de copias de seguridad.
	*/
   def validationTool () { }
	
	
}