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
class EditorController {

	def grailsApplication;
	def messageSource

	def manifest() {
		render(view:"manifest" , model:[
			selectedSubsystem:Subsystem.MANIFESTS.toString()])
	}
        
    def vote() { 
		def controlCenters = CentroControl.findAllWhere(estado: ActorConIP.Estado.ACTIVO)
		def controlCenterList = []
		controlCenters.each {controlCenter ->
            def controlCenterMap = [id:controlCenter.id, nombre:controlCenter.nombre,
                estado:controlCenter.estado?.toString(),
                serverURL:controlCenter.serverURL, fechaCreacion:controlCenter.dateCreated]
            controlCenterList.add(controlCenterMap)
        }
		
		render(view:"vote" , model:[controlCenters: controlCenterList, 
			selectedSubsystem:Subsystem.VOTES.toString()])
	}
        
    def claim() { 
		render(view:"claim" , model:[
			selectedSubsystem:Subsystem.CLAIMS.toString()])
	}
		
}
