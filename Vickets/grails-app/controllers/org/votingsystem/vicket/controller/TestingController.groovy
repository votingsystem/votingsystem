package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.GroupVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.UserVSAccount
import org.votingsystem.model.VicketSource
import org.votingsystem.vicket.util.LoggerVS
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.util.IbanVSUtil
import org.votingsystem.vicket.util.MetaInfMsg
import org.votingsystem.vicket.websocket.SessionVSHelper

import java.lang.reflect.Constructor
import java.util.concurrent.ConcurrentHashMap

import java.text.Normalizer;
import java.text.Normalizer.Form;

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
    def webSocketService

    def index() { }

    def testSocket() {}

    def socketvs() {}

    def broadcast() {
        SessionVSHelper.getInstance().broadcast(new JSONObject([status:200, message:"Hello"]))
        render "OK"
        return false
    }

    def dockspawn() { }

    def validator() { }

    def tagAcccount() {
        UserVSAccount groupAccount
        UserVSAccount.withTransaction {
            groupAccount = UserVSAccount.findWhere(IBAN:"ES1978788989450000000003")
        }
        render groupAccount as JSON
    }

    def IBAN() {
        render IbanVSUtil.getInstance().getIBAN(1111111111)
        return false
    }

    def index1() {
        Long init = System.currentTimeMillis()
        Random randomGenerator = new Random();

        TransactionVS.Type[] transactionTypes = TransactionVS.Type.values()


        int numVotes = 1000
        for (int idx = 1; idx <= numVotes; ++idx){
            int randomInt = randomGenerator.nextInt(100);
            int transactionvsItemId = new Random().nextInt(transactionTypes.length);
            TransactionVS.Type transactionType = transactionTypes[transactionvsItemId]
            LoggerVS.logTransactionVS(Long.valueOf(idx), ResponseVS.SC_OK, transactionType.toString(), "fromUser${randomInt}",
                    "toUser${randomInt}", Currency.getInstance("EUR").getCurrencyCode(), new BigDecimal(randomInt),
                    Calendar.getInstance().getTime(), "Subject - ${randomInt}", true)
        }
        Long finish = System.currentTimeMillis()
        Long duration = finish - init;
        String durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        render " --- Done numVotes : ${numVotes} - duration in millis: ${duration} - duration: ${durationStr}"
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