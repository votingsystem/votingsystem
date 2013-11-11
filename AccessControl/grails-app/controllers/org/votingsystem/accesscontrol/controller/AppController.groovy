package org.votingsystem.accesscontrol.controller

import java.net.URLEncoder;
import java.security.KeyStore

import org.votingsystem.accesscontrol.model.Certificado

import grails.converters.JSON

import org.codehaus.groovy.tools.groovydoc.GroovyDocTool
import org.codehaus.groovy.tools.groovydoc.OutputTool
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyMethodDoc
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyRootDoc
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.votingsystem.model.ResponseVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.groovy.util.*
import org.votingsystem.accesscontrol.model.*

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class AppController {

	def grailsApplication;
	
	/**
	 * @httpMethod [GET]
	 * @return La página principal de la aplicación web de votación con parámetros de utilidad
	 * 		   para una sesión con cliente Android.
	 */
	def clienteAndroid() {
		log.debug("*** Si llega aqui mostrar message app market browserToken: ${params.browserToken}" )
		if(params.boolean('androidClientLoaded'))
			render(view:"index")
		/*String uri = "${grailsApplication.config.grails.serverURL}/eventoVotacion/mainPage?androidClientLoaded=false"
		if(params.browserToken) uri = "${uri}#${params.browserToken}"
		if(params.eventoId) uri = "${uri}&eventoId=${params.eventoId}"
		if(params.serverURL) uri = "${uri}&serverURL=${params.serverURL}"
		if(params.msg) {
			String msg = URLEncoder.encode("${params.msg}", "UTF-8")
			uri = "${uri}&msg=${msg}"
			log.debug("msg: ${msg}")
		}*/
		String uri = "${grailsApplication.config.grails.serverURL}/eventoVotacion/mainPage?androidClientLoaded=false"
		if(params.browserToken) uri = "${uri}#${params.browserToken}"
		if(params.eventoId) uri = "${uri}&eventoId=${params.eventoId}"
		if(params.serverURL) uri = "${uri}&serverURL=${params.serverURL}"
		if(params.msg) {
			String msg = URLEncoder.encode("${params.msg}", "UTF-8")
			uri = "${uri}&msg=${msg}"
			log.debug("msg: ${msg}")
		}
		redirect(uri:uri)
		return
	}
	
}