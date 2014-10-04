package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.VicketTagVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.vicket.model.VicketBatch

class VicketController {

    def vicketService
    def csrService
    def userVSService
    def transactionVSService

    def request() {}

    def cancel() {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = vicketService.cancelVicket(messageSMIMEReq, request.getLocale())
        return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate]
    }


    def cancelBatch () {
        JSONObject jsonRequest = JSON.parse(new String(params.requestBytes))
        JSONArray vicketCancellationArray = jsonRequest.getJSONArray("vicketCancellationList")
        vicketCancellationArray.each {
            log.debug("vicketCancellationArray -it: ${it}")
        }
    }

    /**
     * Servicio que valida las solicitudes de vickets de los usuarios.
     *
     * @httpMethod [POST]
     * @serviceURL [/model/request]
     * @requestContentType [application/x-pkcs7-signature] La solicitud de certificado de delegación.
     * @param [csr] Obligatorio. La solicitud de certificado de delegación anónima.
     * @return La solicitud de certificado de delegación anónima firmada.
     */
    def processRequestFileMap() {
        MessageSMIME messageSMIMEReq = params[ContextVS.VICKET_REQUEST_DATA_FILE_NAME]
        request.messageSMIMEReq = messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }

        VicketBatch vicketBatch = new VicketBatch(params[ContextVS.CSR_FILE_NAME], messageSMIMEReq,
                grailsApplication.config.grails.serverURL)
        if(!vicketBatch.tag) {
            VicketTagVS.withTransaction {
                VicketTagVS vicketTagVS = VicketTagVS.findWhere(name:vicketBatch.getTagVS())
                vicketBatch.setTag(vicketTagVS)
            }
        }
        ResponseVS responseVS = vicketService.processVicketRequest(vicketBatch)
        return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate]
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        Throwable rootCause = StackTraceUtils.extractRootCause(exception)
        log.error "Exception occurred. ${rootCause.getMessage()}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action_${rootCause.getClass().getSimpleName()}"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: rootCause.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:rootCause.getMessage())]
    }
}
