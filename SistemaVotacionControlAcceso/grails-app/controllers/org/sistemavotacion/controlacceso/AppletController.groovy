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
	def index() { 
		redirect action: "restDoc"
	}
	
	/**
	 * @httpMethod GET
	 * @return Página HTML que sirve para cargar el Applet principal de firma.
	 */
	def cliente () { 
		String depsPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}/applet/lib"
		log.debug "deps path: ${depsPath}"
		def appletJarDependencies = []
		new File(depsPath).eachFile() { file->
			if(file.path.endsWith(".jar"))
				appletJarDependencies.add(file.getName())
		}

		render (view:"cliente", model:[appletJarDependencies: appletJarDependencies])
	}
	
	/**
	* @httpMethod GET
	* @return Página HTML que sirve para cargar el Applet principal de la herramienta de validación
	* 		   de archivos firmados y de copias de seguridad.
	*/
   def herramientaValidacion () { 
	   String depsPath = "${grailsApplication.mainContext.getResource('.')?.getFile()}/applet/lib"
	   log.debug "deps path: ${depsPath}"
	   def appletJarDependencies = []
	   new File(depsPath).eachFile() { file->
		   if(file.path.endsWith(".jar"))
			   appletJarDependencies.add(file.getName())
	   }

	   render (view:"herramientaValidacion", model:[appletJarDependencies: appletJarDependencies])
   }
	
	
}