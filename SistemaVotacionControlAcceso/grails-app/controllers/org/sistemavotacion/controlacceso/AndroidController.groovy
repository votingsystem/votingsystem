package org.sistemavotacion.controlacceso

import java.net.URLEncoder;
import grails.converters.JSON

class AndroidController {

	//This is for the problem sending the raw file in Cloudfoundry
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
