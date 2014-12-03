package org.votingsystem.cooin.controller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.runtime.StackTraceUtils
import org.iban4j.CountryCode
import org.iban4j.Iban
import org.votingsystem.groovy.util.TransactionVSUtils
import org.votingsystem.model.GroupVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubscriptionVS
import org.votingsystem.util.DateUtils
import org.votingsystem.cooin.model.TransactionVS
import org.votingsystem.cooin.util.LoggerVS
import org.votingsystem.cooin.util.WebViewWrapper
import org.votingsystem.cooin.websocket.SessionVSHelper

import java.time.Duration

/**
 * @infoController TestingController
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class TestingController {

    def userVSService
    def grailsApplication
    def transactionVSService
    def auditingService
    def filesService
    def webSocketService
    def balanceService
    def systemService
    def groupVSService
    def transactionVS_UserVSService
    def bankVSService

    def index() {
        render "OK"
        return false
    }

    def balance() {
        Map balanceTo = [EUR:[HIDROGENO:[total:new BigDecimal(880.5), timeLimited:new BigDecimal(700.5)],
                        NITROGENO:[total:new BigDecimal(100), timeLimited:new BigDecimal(50.5)]],
                        DOLLAR:[WILDTAG:[total:new BigDecimal(1454), timeLimited:new BigDecimal(400.5)]]]
        Map balanceFrom = [EUR:[HIDROGENO:new BigDecimal(1080.5), OXIGENO:new BigDecimal(350)], DOLLAR:[WILDTAG:new BigDecimal(6000)],
                        YEN:[WILDTAG1:new BigDecimal(8000)]]

        Map result = TransactionVSUtils.balancesCash(balanceTo, balanceFrom)
        Map allResults = [balanceTo:balanceTo, balanceFrom:balanceFrom, result:result]
        render allResults as JSON
    }

    def newWeek() {
        Calendar nextWeek = Calendar.getInstance()
        nextWeek.set(Calendar.WEEK_OF_YEAR, (nextWeek.get(Calendar.WEEK_OF_YEAR) + 1))
        balanceService.initWeekPeriod(nextWeek)
        /*List transactionList
        TransactionVS.withTransaction {
            //transactionList = TransactionVS.findAllWhere(type:[TransactionVS.Type.COOIN_INIT_PERIOD,
            //       TransactionVS.Type.COOIN_INIT_PERIOD_TIME_LIMITED])
            transactionList = TransactionVS.createCriteria().list(offset: 0) {
                inList("type", [TransactionVS.Type.COOIN_INIT_PERIOD,
                                TransactionVS.Type.COOIN_INIT_PERIOD_TIME_LIMITED] )
            }

            for(TransactionVS transaction : transactionList) {
                transaction.delete()
            }
        }*/
        render "OK"
        return false
    }

    def broadcast() {
        SessionVSHelper.getInstance().broadcast(new JSONObject([status:200, message:"Hello", coreSignal:"transactionvs-new"]))
        render "OK"
        return false
    }

    def IBAN() {
        String accountNumberStr = String.format("%010d", 12345L);
        Iban iban = new Iban.Builder().countryCode(CountryCode.ES).bankCode("7777").branchCode( "7777")
                .accountNumber(accountNumberStr).nationalCheckDigit("45").build();
        render iban.toString();
        return false
    }

    def logTransactions() {
        Long init = System.currentTimeMillis()
        Random randomGenerator = new Random();
        TransactionVS.Type[] transactionTypes = TransactionVS.Type.values()
        int numTransactions = 1000
        for (int idx = 1; idx <= numTransactions; ++idx){
            int randomInt = randomGenerator.nextInt(100);
            int transactionvsItemId = new Random().nextInt(transactionTypes.length);
            TransactionVS.Type transactionType = transactionTypes[transactionvsItemId]
            LoggerVS.logTransactionVS(Long.valueOf(idx), ResponseVS.SC_OK, transactionType.toString(), "fromUser${randomInt}",
                    "toUser${randomInt}", Currency.getInstance("EUR").getCurrencyCode(), new BigDecimal(randomInt),
                    Calendar.getInstance().getTime(), null, "Subject - ${randomInt}", true)
        }
        Long finish = System.currentTimeMillis()
        Long duration = finish - init;
        String durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        render " --- Done numTransactions : ${numTransactions} - duration in millis: ${duration} - duration: ${durationStr}"
    }

    def checkCooin() {
        auditingService.checkCooinRequest(Calendar.getInstance().getTime())
        render "OK"
    }


    def webViewLoadTest() {
        WebViewWrapper webViewTest = WebViewWrapper.getInstance()
        webViewTest.loadWebView("http://cooins:8086/Cooins/polymerTest/webView?mode=simplePage");
        render "webViewLoadTest - OK"
        return false
    }

    def webViewJSTest() {
        String jsCommand = "serverMessage('message to server webkit')"
        WebViewWrapper webViewTest = WebViewWrapper.getInstance().executeScript(jsCommand);
        render "webViewJSTest - OK"
        return false
    }

    def highcharts() { }

    def accounts() { }

    def testSocket() { }

    def socketvs() { }

    def webView() {}

    def polymer() {}

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}