package org.votingsystem.controlcenter.service

import grails.transaction.Transactional
import org.votingsystem.model.ActorVS
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ControlCenterVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.NifUtils

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class UserVSService {

	def grailsApplication
	
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

}

