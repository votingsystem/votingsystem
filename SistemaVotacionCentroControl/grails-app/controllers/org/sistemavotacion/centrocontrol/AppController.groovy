package org.sistemavotacion.centrocontrol

import grails.converters.JSON;
import org.sistemavotacion.centrocontrol.modelo.Respuesta

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal 
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
 * */
class AppController {

	def grailsApplication
	def restDocumentationService
	def hibernateProperties
	
	/**
	 * @httpMethod GET
	 * @return Información sobre los servicios que tienen como url base '/app'
	 */
	def index() {
	}
	
	/**
	 * @httpMethod GET
	 * @return La página principal de la aplicación web de votación.
	 */
	def home() {
	}

	/**
	 * @httpMethod GET
	 * @return Genera la documentación sobre los servicios REST
	 */
	/**/def generateRestDoc() {
		Respuesta respuesta = restDocumentationService.generateDocs()
		response.status = respuesta.codigoEstado
		String msg = respuesta.mensaje
		if(!msg) {
			if(Respuesta.SC_OK == respuesta.codigoEstado) msg = "OK"
			else msg = "ERROR"
		}
		render msg
		return false
	}
	
	/**
	 * @httpMethod GET
	 * @return Parámetros de configuración de Hibernate 
	 */
	/*def hibernate = {
		render hibernateProperties as JSON
		return false
	}*/
	
	/**
	 * @httpMethod GET
	 * @return La página principal de la aplicación web de votación con parámetros de utilidad
	 * 		   para una sesión con cliente Android.
	 */
	def clienteAndroid() {
		log.debug("*** Si llega aqui mostrar mensaje app market browserToken: ${params.browserToken}" )
		if(params.boolean('androidClientLoaded'))
			render(view:"index")
		String uri = "${grailsApplication.config.grails.serverURL}/app/home?androidClientLoaded=false"
		if(params.browserToken) uri = "${uri}#${params.browserToken}"
		if(params.eventoId) uri = "${uri}&eventoId=${params.eventoId}"
		if(params.serverURL) uri = "${uri}&serverURL=${params.serverURL}"
		if(params.msg) {
			String msg = URLEncoder.encode(params.msg, "UTF-8")
			uri = "${uri}&msg=${msg}"
			log.debug("msg: ${msg}")
		} 
		redirect(uri:uri)
		return
	}
	
}