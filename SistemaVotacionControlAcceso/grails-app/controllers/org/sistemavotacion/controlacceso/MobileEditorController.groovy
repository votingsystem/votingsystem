package org.sistemavotacion.controlacceso

import java.net.URLEncoder;
import java.security.KeyStore
import org.sistemavotacion.controlacceso.modelo.Certificado
import org.sistemavotacion.controlacceso.modelo.Respuesta
import grails.converters.JSON

import org.codehaus.groovy.tools.groovydoc.GroovyDocTool
import org.codehaus.groovy.tools.groovydoc.OutputTool
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyMethodDoc
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyRootDoc
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.utils.*
import org.sistemavotacion.controlacceso.modelo.*

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class MobileEditorController {

	def grailsApplication;

	def manifest() { }
        
    def vote() { 
		def controlCenters = CentroControl.findAllWhere(estado: ActorConIP.Estado.ACTIVO)
		def controlCenterList = []
		controlCenters.each {controlCenter ->
			def controlCenterMap = [id:controlCenter.id, nombre:controlCenter.nombre,
				estado:controlCenter.estado?.toString(),
				serverURL:controlCenter.serverURL, fechaCreacion:controlCenter.dateCreated]
			controlCenterList.add(controlCenterMap)
		}
		
		render(view:"vote" , model:[controlCenters: controlCenterList])
	}
        
    def claim() { }
		
}
