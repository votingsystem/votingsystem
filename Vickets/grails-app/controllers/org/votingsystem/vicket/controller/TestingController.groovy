package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.iban4j.CountryCode
import org.iban4j.Iban
import org.votingsystem.model.ResponseVS
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.util.LoggerVS
import org.votingsystem.vicket.util.WebViewWrapper
import org.votingsystem.vicket.websocket.SessionVSHelper

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

    def index() {

    }


    def webViewLoadTest() {
        WebViewWrapper webViewTest = WebViewWrapper.getInstance()
        webViewTest.loadWebView("http://vickets:8086/Vickets/polymerTest/webView?mode=simplePage");
        render "webViewLoadTest - OK"
        return false
    }

    def broadcast() {
        SessionVSHelper.getInstance().broadcast(new JSONObject([status:200, message:"Hello", coreSignal:"transactionvs-new"]))
        render "OK"
        return false
    }

    def tagAcccount() {
        UserVSAccount groupAccount
        UserVSAccount.withTransaction {
            groupAccount = UserVSAccount.findWhere(IBAN:"ES1978788989450000000003")
        }
        render groupAccount as JSON
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
                    Calendar.getInstance().getTime(), "Subject - ${randomInt}", true)
        }
        Long finish = System.currentTimeMillis()
        Long duration = finish - init;
        String durationStr = DateUtils.getElapsedTimeHoursMinutesFromMilliseconds(duration);
        render " --- Done numTransactions : ${numTransactions} - duration in millis: ${duration} - duration: ${durationStr}"
    }

    def checkVicket() {
        auditingService.checkVicketRequest(Calendar.getInstance().getTime())
        render "OK"
    }

    def backup() {
        auditingService.backupUserTransactionHistory(Calendar.getInstance().getTime())
        render "OK"
    }


    def webViewJSTest() {
        String jsCommand = "serverMessage('message to server webkit')"
        WebViewWrapper webViewTest = WebViewWrapper.getInstance().executeScript(jsCommand);
        render "webViewJSTest - OK"
        return false
    }

    def accounts() { }

    def testSocket() { }

    def socketvs() { }

    def webView() {}

    def polymer() {}
}