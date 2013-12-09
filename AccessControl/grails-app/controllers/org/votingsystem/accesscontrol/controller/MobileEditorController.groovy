package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.ActorVS
import org.votingsystem.model.ControlCenterVS
/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class MobileEditorController {

	def grailsApplication;

	def eventVSClaim() { }
        
    def eventVSElection() {
		def controlCenters = ControlCenterVS.findAllWhere(state: ActorVS.State.RUNNING)
		def controlCenterList = []
		controlCenters.each {controlCenter ->
			def controlCenterMap = [id:controlCenter.id, name:controlCenter.name,
				state:controlCenter.state?.toString(),
				serverURL:controlCenter.serverURL, dateCreated:controlCenter.dateCreated]
			controlCenterList.add(controlCenterMap)
		}
		render(view:"eventVSElection" , model:[controlCenters: controlCenterList])
	}
        
    def eventVSManifest() { }
		
}
