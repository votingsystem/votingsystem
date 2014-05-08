package org.votingsystem.vicket.controller

import org.votingsystem.model.GroupVS
import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.UserVS

/**
 * @infoController SubscriptionVS
 * @descController Servicios relacionados con las subscripciones a grupos de Vickets
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class SubscriptionVSController {
	
	/**
	 * @httpMethod [POST]
	 * @return
	 */
	def activate () { }

    def test() {
        def result
        Map resultMap = [:]

        UserVS uservs = UserVS.get(4L)
        GroupVS groupvs = GroupVS.get(21L)

        SubscriptionVS.withTransaction {
            new SubscriptionVS(userVS:uservs, groupVS:groupvs, state:SubscriptionVS.State.ACTIVE).save()

        }
        render "OK"
    }
	
}