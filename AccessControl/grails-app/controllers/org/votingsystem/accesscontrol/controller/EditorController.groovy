package org.votingsystem.accesscontrol.controller

import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ActorVS
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubSystemVS
import org.votingsystem.model.TypeVS

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class EditorController {

	def grailsApplication;
	def messageSource

	def manifest() {
		render(view:"/eventVSManifest/manifest" , model:[selectedSubsystem:SubSystemVS.MANIFESTS.toString()])
	}
        
    def vote() { 
		def controlCenters = ControlCenterVS.findAllWhere(state: ActorVS.State.OK)
		def controlCenterList = []
		controlCenters.each {controlCenter ->
            def controlCenterMap = [id:controlCenter.id, name:controlCenter.name, state:controlCenter.state?.toString(),
                serverURL:controlCenter.serverURL, dateCreated:controlCenter.dateCreated]
            controlCenterList.add(controlCenterMap)
        }
		render(view:"/eventVSElection/editor" , model:[controlCenters: controlCenterList, selectedSubsystem:SubSystemVS.VOTES.toString()])
	}
        
    def claim() { 
		render(view:"/eventVSClaim/claim" , model:[selectedSubsystem:SubSystemVS.CLAIMS.toString()])
	}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
