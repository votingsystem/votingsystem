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
		String depsPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}/applet/lib"
		log.debug "deps path: ${depsPath}"
		def appletJarDependencies = []
		File keyStore = new File(depsPath).eachFile() { file->
			if(file.path.endsWith(".jar"))
				appletJarDependencies.add(file.getName()) 
		}

		render (view:"jnlpCliente", contentType: "application/x-java-jnlp-file", 
			model: [appletJarDependencies: appletJarDependencies])
	}
	
	/**
	 * @httpMethod GET
	 * @return Archivo JNLP con los datos del Applet de la herramienta de validación de archivos firmados
	 *         y copias de seguridad.
	 */
	def jnlpHerramienta () {
		String depsPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}/applet/lib"
		log.debug "deps path: ${depsPath}"
		def appletJarDependencies = []
		File keyStore = new File(depsPath).eachFile() { file->
			if(file.path.endsWith(".jar"))
				appletJarDependencies.add(file.getName())
		}
		
		render (view:"jnlpHerramienta", contentType: "application/x-java-jnlp-file",
			model: [appletJarDependencies: appletJarDependencies])
	}
	
}