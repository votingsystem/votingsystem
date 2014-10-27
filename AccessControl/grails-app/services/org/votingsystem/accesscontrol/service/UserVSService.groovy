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

	def grailsApplication

	public Map getUserVS(Date fromDate){
        List<UserVS> userVSList = UserVS.createCriteria().list(offset:0) {
            gt("dateCreated", fromDate)
        }
		return [totalNumUsu:userVSList.totalCount]
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
        return grailsApplication.config.vs.adminsDNI.contains(nif)
    }

}

