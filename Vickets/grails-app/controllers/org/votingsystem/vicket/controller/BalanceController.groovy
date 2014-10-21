package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.groovy.util.RequestUtils
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS

class BalanceController {

    def balanceService
    def filesService
    def userVSAccountService

    def index() { }

    def db() {
        UserVS userVS
        UserVS.withTransaction { userVS = UserVS.findWhere(id:params.long('userId')) }
        if(!userVS) {
            response.status = ResponseVS.SC_NOT_FOUND
            render(text: message(code:'userVSNotFoundById', args:[params.userId]), encoding: "UTF-8")
        } else render userVSAccountService.getAccountsBalanceMap(userVS) as JSON
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
            if(request.contentType?.contains("json")) render resultMap as JSON
            else render(view:"userVS", model:[balanceMap:resultMap])
        }
    }

    /*
     * /balance/week/$year/$month/$day
     */
    def week() {
        Calendar calendar = RequestUtils.getCalendar(params)
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(calendar)
        Map<String, File> filesMap = filesService.getWeekReportFiles(timePeriod, null)
        File reportsFile = filesMap.reportsFile
        if(!reportsFile.exists()) {
            response.status = ResponseVS.SC_NOT_FOUND
            render message(code: 'reportsForWeekNotFoundMsg', args:[DateUtils.getDateStr(timePeriod.getDateFrom(), "dd MMM yyyy")])
        } else {
            def messageJSON = JSON.parse(reportsFile.text)
            if(request.contentType?.contains("json")) render messageJSON as JSON
            else render(view:'week', model: [balancesJSON:messageJSON as JSON])
        }
    }

    /*
    * /balance/weekdb/$year/$month/$day
    */
    def weekdb() {
        Calendar calendar = RequestUtils.getCalendar(params)
        return [responseVS:balanceService.calculatePeriod(DateUtils.getWeekPeriod(calendar))]
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
