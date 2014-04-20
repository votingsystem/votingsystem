package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.apache.avro.generic.GenericData
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.vicket.TransactionVS
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

    def checkVicket() {
        auditingService.checkVicketRequest(Calendar.getInstance().getTime())
        render "OK"
    }

    def backup() {
        auditingService.backupUserTransactionHistory(Calendar.getInstance().getTime())
        render "OK"
    }

    def transactionTest() {
        Date selectedDate = null
        Calendar calendar = Calendar.getInstance()
        if(params.year && params.month && params.day) {
            calendar = Calendar.getInstance()
            calendar.set(Calendar.YEAR, params.int('year'))
            calendar.set(Calendar.MONTH, params.int('month') - 1) //Zero based
            calendar.set(Calendar.DAY_OF_MONTH, params.int('day'))
        } else calendar = DateUtils.getMonday(calendar)

        UserVS userVS = UserVS.get(2)
        render transactionVSService.getUserInfoMap(userVS, calendar) as JSON
        return false
    }

    def index() {

    }
}