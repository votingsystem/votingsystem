package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.votingsystem.groovy.util.RequestUtils
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.UserVSAccount

class BalanceController {

    def balanceService
    def filesService

    def index() { }

    def db() {
        UserVS uservs
        UserVS.withTransaction { uservs = UserVS.findWhere(id:params.long('userId')) }
        if(!uservs) {
            response.status = ResponseVS.SC_NOT_FOUND
            render(text: message(code:'userVSNotFoundById', args:[params.userId]), encoding: "UTF-8")
        } else {
            List<UserVSAccount> userVSAccounts
            userVSAccounts = UserVSAccount.findAllWhere(userVS:uservs, state:UserVSAccount.State.ACTIVE)
            Map result = [:]
            for(UserVSAccount account: userVSAccounts) {
                if(result[(account.IBAN)]) result[(account.IBAN)].add([(account.tag.name):account.balance.toString()])
                else result[(account.IBAN)] = [[(account.tag.name):account.balance.toString()]]
            }
            render result as JSON
        }
    }

    def userVS() {
        UserVS uservs
        UserVS.withTransaction { uservs = UserVS.findWhere(id:params.long('userId')) }
        if(!uservs) {
            response.status = ResponseVS.SC_NOT_FOUND
            render(text: message(code:'userVSNotFoundById', args:[params.userId]), encoding: "UTF-8")
        } else {
            DateUtils.TimePeriod timePeriod = org.votingsystem.util.DateUtils.getWeekPeriod(RequestUtils.getCalendar(params))
            Map resultMap = balanceService.genBalance(uservs, timePeriod)
            if(request.contentType?.contains("json")) {
                render resultMap as JSON
            } else {
                render(view:"userVS", model:[balanceMap:resultMap])
            }
        }
    }

    /*
     * /balance/weekReport/$requestFile/$year/$month/$day"
     */
    def weekReport() {
        Calendar calendar = RequestUtils.getCalendar(params)
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar)
        Map<String, File> filesMap = filesService.getWeekReportFiles(timePeriod, null)
        File reportsFile = filesMap.reportsFile
        if(reportsFile.exists()) {
            def messageJSON = JSON.parse(reportsFile.text)
            if(request.contentType?.contains("json")) {
                render messageJSON as JSON
            } else {
                render(view:'balancesWeek', model: [balancesMap:messageJSON])
            }
        } else {
            response.status = ResponseVS.SC_NOT_FOUND
            render message(code: 'reportsForWeekNotFoundMsg', args:[DateUtils.getDateStr(timePeriod.getDateFrom(), "dd MMM yyyy")])
            return false
        }
    }

}
