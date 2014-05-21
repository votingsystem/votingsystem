package org.votingsystem.vicket.controller

import org.votingsystem.model.GroupVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.NifUtils

/**
 * @infoController SubscriptionVS
 * @descController Servicios relacionados con las subscripciones a grupos de Vickets
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class SubscriptionVSController {

    def groupVSService


    def test() {
        String nif = "7553172H"
        nif = NifUtils.validate(nif).toUpperCase();
        def systemAdmins
        if(!systemAdmins) {
            systemAdmins = new ArrayList<String>();
            "${grailsApplication.config.VotingSystem.adminsDNI}".split(",")?.each {
                systemAdmins.add(NifUtils.validate(it.trim()).toUpperCase())
            }
        }
        log.debug(" ====== systemAdmins: ${systemAdmins}")
        boolean result = systemAdmins.contains(nif)
        render "isUserAdmin : ${result}"
        return false
    }
	
}