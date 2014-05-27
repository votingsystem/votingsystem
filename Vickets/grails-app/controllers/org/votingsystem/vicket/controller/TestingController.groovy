package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.apache.avro.generic.GenericData
import org.apache.log4j.Logger
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.CurrencyVS
import org.votingsystem.model.FieldEventVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.vicket.LoggerVS
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

    def userVSService
    def grailsApplication
    def transactionVSService
    def auditingService
    def filesService


    //logTransactionVS(int status, String type, String fromUser, String toUser, String currency, BigDecimal amount, String msg, Date dateCreated, String subject)

    def index() {
        Long init = System.currentTimeMillis()
        Random randomGenerator = new Random();

        TransactionVS.Type[] transactionTypes = TransactionVS.Type.values()


        int numVotes = 1000
        for (int idx = 1; idx <= numVotes; ++idx){
            int randomInt = randomGenerator.nextInt(100);
            int transactionvsItemId = new Random().nextInt(transactionTypes.length);
            TransactionVS.Type transactionType = transactionTypes[transactionvsItemId]
            LoggerVS.logTransactionVS(Long.valueOf(idx), ResponseVS.SC_OK, transactionType.toString(), "fromUser${randomInt}",
                    "toUser${randomInt}", CurrencyVS.EURO.toString(), new BigDecimal(randomInt), "message ${randomInt}",
                    Calendar.getInstance().getTime(), "Subject - ${randomInt}")
        }
        Long finish = System.currentTimeMillis()
        Long duration = finish - init;
        String durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        render " --- Done numVotes : ${numVotes} - duration in millis: ${duration} - duration: ${durationStr}"
    }

    def monitor() {
        filesService.monitorFile(new File("/home/jgzornoza/github/SistemaVotacion/Vickets/VicketReports/VicketTransactionsReports.log"))
        render "OK"
    }

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

    def users() {
        Calendar calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, 0)
        def result = userVSService.getUserVS(calendar.getTime())
        render result as JSON
    }


}