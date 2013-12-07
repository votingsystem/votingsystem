package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.ActorVS
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.SubSystemVS
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
		render(view:"manifest" , model:[selectedSubsystem:SubSystemVS.MANIFESTS.toString()])
	}
        
    def vote() { 
		def controlCenters = ControlCenterVS.findAllWhere(state: ActorVS.State.RUNNING)
		def controlCenterList = []
		controlCenters.each {controlCenter ->
            def controlCenterMap = [id:controlCenter.id, name:controlCenter.name, state:controlCenter.state?.toString(),
                serverURL:controlCenter.serverURL, dateCreated:controlCenter.dateCreated]
            controlCenterList.add(controlCenterMap)
        }
		render(view:"vote" , model:[controlCenters: controlCenterList, selectedSubsystem:SubSystemVS.VOTES.toString()])
	}
        
    def claim() { 
		render(view:"claim" , model:[selectedSubsystem:SubSystemVS.CLAIMS.toString()])
	}
		
}
