package org.votingsystem.ticket.controller

import grails.converters.JSON
import org.apache.avro.generic.GenericData
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ticket.TransactionVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.DateUtils

import java.text.SimpleDateFormat

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
    def auditingService

    def accounts() {}


    def index1() {
        auditingService.backupUserTransactionHistory(Calendar.getInstance().getTime())
        render "OK"
    }

    def index2() {
        Map pruebaMap = ["http://tickets:8083/Tickets/messageSMIME/index/2":12334,
                "http://tickets:8083/Tickets/messageSMIME/index/3":22222]
        render pruebaMap as JSON
        return false
    }


    def index() {
        Date selectedDate = null
        Calendar calendar = Calendar.getInstance()
        if(params.year && params.month && params.day) {
            calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, params.int('year'))
            calendar.set(Calendar.MONTH, params.int('month') - 1) //Zero based
            calendar.set(Calendar.DAY_OF_MONTH, params.int('day'))
        } else calendar = DateUtils.getMonday(calendar)

        UserVS userVS = UserVS.get(2)


        //Calendar calendar = Calendar.getInstance();
        //calendar.add(Calendar.DAY_OF_YEAR, -7);
        //calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        render transactionVSService.getUserInfoMap(userVS, calendar) as JSON
        //def userInputTransactions = TransactionVS.findAllWhere(toUserVS: userVS, type:TransactionVS.Type.USER_INPUT)
        //ResponseVS responseVS = transactionVSService.getUserBalance(userVS)
        //render responseVS.data
        return false
    }


    def date() {

        Calendar weekFromCalendar = Calendar.getInstance();
        weekFromCalendar = DateUtils.getMonday(weekFromCalendar)
        Calendar weekToCalendar = weekFromCalendar.clone();
        weekToCalendar.add(Calendar.DAY_OF_YEAR, 7)
        render "weekFrom: ${weekFromCalendar.getTime()} - weekTo: ${weekToCalendar.getTime()}"
        return false
    }

    def subject() {
        Map resultMap = checkSubject("OU=DigitalCurrency,OU=AMOUNT:10,CN=ticketProviderURL:http://tickets:8083/Tickets, OU=CURRENCY:euro")

        render resultMap as JSON
        return false
    }


}