package org.votingsystem.accesscontrol.service

import org.votingsystem.model.ActorVS
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.NifUtils

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class UserVSService {
	
	static transactional = true
	
	List<String> systemAdmins
	def grailsApplication

	public Map getUserVS(Date fromDate){
		def usersVS
		UserVS.withTransaction {
			def criteria = UserVS.createCriteria()
			usersVS = criteria.list {
				gt("dateCreated", fromDate)
			}
		}
		int numUsu = UserVS.countByDateCreatedGreaterThan(fromDate)

		Map datosRespuesta = [totalNumUsu:numUsu]
		return datosRespuesta
	}
	
	public Map getControlCenterMap(ControlCenterVS controlCenter) {
		CertificateVS certificate = CertificateVS.findWhere(actorVS:controlCenter, state:CertificateVS.State.OK)
		String certChainPEM
		if(certificate?.certChainPEM) {
			certChainPEM = new String(certificate.certChainPEM)
		}
		return [certChainPEM:certChainPEM, name:controlCenter.name,
			serverURL:controlCenter.serverURL, id:controlCenter.id, 
			serverType:ActorVS.Type.CONTROL_CENTER.toString()]
	}

    boolean isUserAdmin(String nif) {
        nif = NifUtils.validate(nif);
        boolean result = grailsApplication.config.VotingSystem.adminsDNI.contains(nif)
        if(result) log.debug("isUserAdmin - nif: ${nif}")
        return result
    }

}

