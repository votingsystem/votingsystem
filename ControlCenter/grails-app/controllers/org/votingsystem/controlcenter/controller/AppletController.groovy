package org.votingsystem.controlcenter.controller

import org.votingsystem.controlcenter.model.*
import org.springframework.web.servlet.support.RequestContextUtils as RCU



class AppletController {
	
	/**
	 * @httpMethod [GET]
	 * @return Página HTML que sirve para cargar el Applet principal de firma.
	 */
	def client () { }
	
	/**
	 * @httpMethod [GET]
	 * @return Página HTML que sirve para cargar el Applet principal de la herramienta de validación
	 * 		   de archivos firmados y de copias de seguridad.
	 */
	def validationTool () { }
		
}