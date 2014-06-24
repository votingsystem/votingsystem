package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.apache.log4j.Logger
import org.apache.log4j.RollingFileAppender
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS

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
            def messageJSON = JSON.parse("{\"transactionRecords\":[" + reportsFile.text + "]}")
            if(params.transactionvsType) {
                messageJSON.transactionRecords = messageJSON["transactionRecords"].findAll() { item ->
                    if(item.type.equals(params.transactionvsType)) { return item }
                }
            }
            messageJSON.numTotalTransactions = messageJSON.transactionRecords.size()
            messageJSON.queryRecordCount = messageJSON.transactionRecords.size()
            render messageJSON as JSON
            return false
        } else {
            render(view:'transactionvs')
        }
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }
}