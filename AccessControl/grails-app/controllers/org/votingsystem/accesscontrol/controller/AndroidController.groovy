package org.votingsystem.accesscontrol.controller

import java.net.URLEncoder;
import grails.converters.JSON

/**
 * @infoController Android app Controller
 * @descController Controlador que sirve la aplicación para clientes Android.
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class AndroidController {
	
	/**
	 * Este servicio surgió para resolver un problema que surgió en un servicio de hosting.
	 * No era posible acceder al archivo de la aplicación diréctamente.
	 * 
	 * @httpMethod [GET]
	 * @return La aplicación de votación para clientes Android.
	 */
    def app() { 
		response.setHeader("Content-disposition", "attachment; filename=SistemaVotacion.apk")
		response.contentType = "application/vnd.android.package-archive";
		String prefijo = "${grailsApplication.mainContext.getResource('.')?.getFile()}"
		String sufijo ="/android/SistemaVotacion.apk"
		File androidApp = new File("${prefijo}${sufijo}");
		response.setHeader("Content-Length", "${androidApp.length()}")
		response.status = 200
		response.outputStream << androidApp.getBytes() // Performing a binary stream copy
		response.outputStream.flush()
		return
	}
			
}
