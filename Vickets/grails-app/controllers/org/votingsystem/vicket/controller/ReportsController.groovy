package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.apache.log4j.Logger
import org.apache.log4j.RollingFileAppender

/**
 * @infoController Reports
 * @descController Informes de la aplicación en formato JSON
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class ReportsController {

    private static Logger reportslog = Logger.getLogger("reportsLog");
    private static Logger transactionslog = Logger.getLogger("transactionsLog");

    def index() {
        if(request.contentType?.contains("json")) {
            RollingFileAppender appender = reportslog.getAppender("VicketServerReports")
            File reportsFile = new File(appender.file)
            def messageJSON = JSON.parse("{\"records\":[" + reportsFile.text + "]}")
            messageJSON.numTotalRecords = messageJSON.records.length()
            render messageJSON as JSON
            return false
        } else {
            render(view:'reports')
        }
    }

    def transactionvs() {
        if(request.contentType?.contains("json")) {
            RollingFileAppender appender = transactionslog.getAppender("VicketTransactionsReports")
            File reportsFile = new File(appender.file)
            //testfile.eachLine{ line ->}
            def messageJSON = JSON.parse("{\"${message(code: 'transactionRecordsLbl')}\":[" + reportsFile.text + "]}")
            int numTotalTransactions = 0
            if(params.transactionvsType) {
                messageJSON["${message(code: 'transactionRecordsLbl')}"] = messageJSON["${message(code: 'transactionRecordsLbl')}"].findAll() { item ->
                    if(item.type.equals(params.transactionvsType)) {
                        numTotalTransactions++
                        return item
                    }
                }
            }
            messageJSON.numTotalTransactions = numTotalTransactions
            messageJSON.queryRecordCount = numTotalTransactions
            render messageJSON as JSON
            return false
        } else {
            render(view:'transactionvs')
        }
    }


}