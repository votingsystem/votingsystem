package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.apache.log4j.Logger
import org.apache.log4j.RollingFileAppender
import org.votingsystem.model.ContentTypeVS

/**
 * @infoController Reports
 * @descController Informes de la aplicación en formato JSON
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class ReportsController {

    Logger reportslog = Logger.getLogger("reportsLog");

    def index() {
        log.info("prueba mensaje")
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


}