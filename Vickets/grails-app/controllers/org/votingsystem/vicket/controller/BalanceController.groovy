package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils

class BalanceController {

    def balanceService

    def index() { }

    def userVS() {
        if(request.contentType?.contains("json")) {
            UserVS uservs
            UserVS.withTransaction { uservs = UserVS.findWhere(id:params.long('userId')) }
            if(!uservs) {
                response.status = ResponseVS.SC_NOT_FOUND
                render(text: message(code:'userVSNotFoundById', args:[params.userId]), encoding: "UTF-8")
            } else {
                DateUtils.TimePeriod timePeriod = org.votingsystem.util.DateUtils.getWeekPeriod(Calendar.getInstance().getTime())
                Map resultMap = balanceService.genBalance(uservs, timePeriod)
                render resultMap as JSON
            }
            return false
        }
    }

}
