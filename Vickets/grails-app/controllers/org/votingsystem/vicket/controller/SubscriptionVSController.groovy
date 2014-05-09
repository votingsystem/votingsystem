package org.votingsystem.vicket.controller

import org.votingsystem.model.GroupVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS

/**
 * @infoController SubscriptionVS
 * @descController Servicios relacionados con las subscripciones a grupos de Vickets
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class SubscriptionVSController {

    def groupVSService

	/**
	 * @httpMethod [POST]
	 * @return
	 */
	def update () {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = null
        try {
            responseVS = groupVSService.updateSubscription(messageSMIMEReq, request.getLocale())
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            String msg = message(code:'subscribeGroupVSErrorMessage')
            responseVS = new ResponseVS(statusCode:ResponseVS.SC_ERROR, message: msg, reason:msg, type:TypeVS.VICKET_ERROR)
        }
        return [responseVS:responseVS]
    }

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