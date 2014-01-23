package org.votingsystem.ticket.controller

import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ticket.TransactionVS
import org.votingsystem.util.ApplicationContextHolder

/**
 * @infoController TestingController
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class TestingController {

    def grailsApplication
    def transactionVSService

    def index() {
        UserVS userVS = UserVS.get(10)
        def userInputTransactions = TransactionVS.findAllWhere(toUserVS: userVS, type:TransactionVS.Type.USER_INPUT)
        ResponseVS responseVS = transactionVSService.getUserBalance(userVS)
        render responseVS.data
        return false
    }

}