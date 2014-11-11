package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.apache.log4j.Logger
import org.apache.log4j.RollingFileAppender
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.*
import org.votingsystem.vicket.model.Vicket
import org.votingsystem.vicket.model.VicketRequestBatch
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

class VicketController {

    private static Logger vicketsIssuedlog = Logger.getLogger("vicketsIssuedLog");

    def vicketService
    def userVSService
    def transactionVSService


    def request() {
        render(view:'request')
    }

    def issuedLog() {
        if(request.contentType?.contains("json")) {
            RollingFileAppender appender = vicketsIssuedlog.getAppender("VicketsIssued")
            File reportsFile = new File(appender.file)
            //testfile.eachLine{ line ->}
            def messageJSON = JSON.parse("{" + reportsFile.text + "}")
            render messageJSON as JSON
            return false
        } else {
            render(view:'issued')
        }
    }

    def cancel() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.getErrorRequestResponse(message(code:'requestWithoutFile'))]
        return [responseVS:vicketService.cancelVicket(messageSMIME)]
    }


    def cancelBatch () {
        JSONObject jsonRequest = JSON.parse(new String(params.requestBytes))
        JSONArray vicketCancellationArray = jsonRequest.getJSONArray("vicketCancellationList")
        vicketCancellationArray.each {
            log.debug("vicketCancellationArray -it: ${it}")
        }
    }

    /**
     * Service that validates cash requests
     *
     * @httpMethod [POST]
     * @serviceURL [/vicket/request]
     * @requestContentType [application/x-pkcs7-signature] The request with the amount required signed by the user
     * @param [csr] Required. The anonymous certificate request for the cash.
     * @return The anonymous certificate request signed (with this you can make secured anonymous transactions).
     */
    def processRequestFileMap() {
        MessageSMIME messageSMIMEReq = params[ContextVS.VICKET_REQUEST_DATA_FILE_NAME]
        request.messageSMIMEReq = messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }

        VicketRequestBatch vicketBatch = new VicketRequestBatch(params[ContextVS.CSR_FILE_NAME], messageSMIMEReq,
                grailsApplication.config.grails.serverURL)
        if(!vicketBatch.tagVS) {
            TagVS.withTransaction {
                TagVS tagVS = TagVS.findWhere(name:vicketBatch.getTag())
                vicketBatch.setTagVS(tagVS)
            }
        }
        return [responseVS:vicketService.processVicketRequest(vicketBatch)]
    }

    def wallet() {}

    def status() {
        String hashCertVSBase64 = new String(new HexBinaryAdapter().unmarshal(params.hashCertVSHex))
        Vicket vicket
        Vicket.withTransaction {
            vicket = Vicket.findWhere(hashCertVS:hashCertVSBase64)
        }
        int statusCode = ResponseVS.SC_MESSAGE_FROM_VS
        String msg = null
        if(!vicket) msg = message(code:'vicketNotFoundErrorMsg')
        else {
            switch(vicket.state) {
                case Vicket.State.EXPENDED:
                    msg = message(code:'vicketExpendedShortErrorMsg')
                    break;
                case Vicket.State.CANCELLED:
                    msg = message(code:'vicketCancelledErrorMsg')
                    break;
                case Vicket.State.OK:
                    if(vicket.validTo.after(Calendar.getInstance().getTime())) {
                        statusCode = ResponseVS.SC_OK
                        msg = message(code:'vicketOKMsg')
                        break;
                    } else {
                        vicket.state = Vicket.State.LAPSED
                        Vicket.withTransaction { vicket.save()}
                    }
                case Vicket.State.LAPSED:
                    msg = message(code:'vicketLapsedShortErrorMsg')
                    break;
            }
        }
        response.status = statusCode
        render msg
        return false
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
