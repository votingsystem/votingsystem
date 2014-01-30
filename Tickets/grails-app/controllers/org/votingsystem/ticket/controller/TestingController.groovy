package org.votingsystem.ticket.controller

import grails.converters.JSON
import org.apache.avro.generic.GenericData
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ticket.TransactionVS
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

    def accounts() {}


    def index() {
        Date selectedDate = null
        Calendar calendar = Calendar.getInstance()
        if(params.year && params.month && params.day) {
            calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, params.int('year'))
            calendar.set(Calendar.MONTH, params.int('month') - 1) //Zero based
            calendar.set(Calendar.DAY_OF_MONTH, params.int('day'))
        } else calendar = DateUtils.getMonday(calendar)

        UserVS userVS = UserVS.get(4)


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

        Date selectedDate = null


        Calendar calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);



        render DateUtils.getMonday(calendar).getTime()
        return false
    }

    def subject() {
        Map resultMap = checkSubject("OU=DigitalCurrency,OU=AMOUNT:10,CN=ticketProviderURL:http://tickets:8083/Tickets, OU=CURRENCY:euro")

        render resultMap as JSON
        return false
    }


}