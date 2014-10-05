package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.dao.DataAccessException
import org.votingsystem.groovy.util.RequestUtils
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.Vicket
import org.votingsystem.vicket.model.VicketTransactionBatch

class TransactionVSController {

    def userVSService
    def transactionVSService
    def signatureVSService
    def vicketService

    def get() {
        Map resultMap = [:]
        String receipt
        if (params.long('id')) {
            TransactionVS result
            TransactionVS.withTransaction {
                result = TransactionVS.get(params.long('id'))
            }
            if(result) {
                receipt = new String(result.messageSMIME.content, "UTF-8")
                resultMap = transactionVSService.getTransactionMap(result)
                resultMap.receipt = receipt
            }
        }
        render resultMap as JSON
    }

    def index() {
        if(request.contentType?.contains("json")) {
            Map sortParamsMap = RequestUtils.getSortParamsMap(params)
            Map.Entry sortParam
            if(!sortParamsMap.isEmpty()) sortParam = sortParamsMap?.entrySet()?.iterator()?.next()
            List<TransactionVS> transactionList = null
            int totalTransactions = 0;
            TransactionVS.withTransaction {
                if(params.searchText || params.searchFrom || params.searchTo || params.transactionvsType) {
                    TransactionVS.Type transactionType = null
                    BigDecimal amount = null
                    Date dateFrom = null
                    Date dateTo = null
                    try {
                        if(params.transactionvsType) transactionType = TransactionVS.Type.valueOf(params.transactionvsType)
                        else transactionType = TransactionVS.Type.valueOf(params.searchText.toUpperCase())} catch(Exception ex) {}
                    try {amount = new BigDecimal(params.searchText)} catch(Exception ex) {}
                    //searchFrom:2014/04/14 00:00:00, max:100, searchTo
                    if(params.searchFrom) try {dateFrom = DateUtils.getDateFromString(params.searchFrom)} catch(Exception ex) {}
                    if(params.searchTo) try {dateTo = DateUtils.getDateFromString(params.searchTo)} catch(Exception ex) {}

                    transactionList = TransactionVS.createCriteria().list(max: params.max, offset: params.offset,
                            sort:sortParam?.key, order:sortParam?.value) {
                        or {

                            if(transactionType) eq("type", transactionType)
                            if(amount) eq("amount", amount)
                            ilike('subject', "%${params.searchText}%")
                            ilike('currencyCode', "%${params.searchText}%")
                        }
                        and {
                            isNull('transactionParent')
                            if(dateFrom && dateTo) {between("dateCreated", dateFrom, dateTo)}
                            else if(dateFrom) {ge("dateCreated", dateFrom)}
                            else if(dateTo) {le("dateCreated", dateTo)}
                        }
                    }
                    totalTransactions = transactionList.totalCount
                } else {
                    transactionList = TransactionVS.createCriteria().list(max: params.max, offset: params.offset,
                            sort:sortParam?.key, order:sortParam?.value){
                        isNull('transactionParent')
                    };
                    totalTransactions = transactionList.totalCount
                }
            }
            def resultList = []
            transactionList.each {transactionItem ->
                resultList.add(transactionVSService.getTransactionMap(transactionItem))
            }
            def resultMap = [transactionRecords:resultList, queryRecordCount: totalTransactions,
                             numTotalTransactions:totalTransactions ]
            render resultMap as JSON
        }
    }

    /**
     * Servicio que devuelve las transacciones de un usuario
     *
     * @httpMethod [GET]
     * @serviceURL [/userVS/$id/transacionVS/$timePeriod] with $timePeriod one from TimePeriod.Lapse ->
     *              {YEAR, MONTH, WEEK, DAY, HOUR, MINUTE, SECOND}
     * @requestContentType [application/json]
     * @responseContentType [application/json]
     * @return
     */
    def userVS() {
        if(params.long('id')) {
            UserVS userVS = null
            UserVS.withTransaction { userVS = UserVS.get(params.long('id')) }
            if(!userVS) {
                return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                        message: message(code: 'itemNotFoundMsg', args:[params.long('id')]))]
            }
            DateUtils.TimePeriod.Lapse lapse =  DateUtils.TimePeriod.Lapse.valueOf(params.timePeriod.toUpperCase())
            DateUtils.TimePeriod timePeriod = DateUtils.getLapsePeriod(Calendar.getInstance(request.locale).getTime(), lapse)
            Map resultMap = transactionVSService.getDataWithBalancesMap(userVS, timePeriod)
            render resultMap as JSON
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }

    /**
     * Servicio que recibe una transacción compuesta por un lote de Vickets
     *
     * @httpMethod [POST]
     * @serviceURL [/transactionVS/vicket]
     * @requestContentType Documento JSON con la extructura https://github.com/votingsystem/votingsystem/wiki/Lote-de-Vickets
     * @responseContentType [application/pkcs7-mime]. Documento JSON cifrado en el que figuran los recibos de los model recibidos.
     * @return
     */
    def vicket() {
        if(!request.JSON) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = vicketService.processVicketTransaction(new VicketTransactionBatch(request.JSON))
        return [responseVS:responseVS]
    }

    /**
     * Servicio que recibe los asignaciones de los usuarios en documentos SMIME
     *
     * @httpMethod [POST]
     * @serviceURL [/transactionVS]
     * @requestContentType [application/x-pkcs7-signature] Obligatorio.
     *                     documento SMIME firmado con un model emitido por el sistema.
     * @responseContentType [application/x-pkcs7-signature]. Recibo firmado por el sistema.
     * @return  Recibo que consiste en el documento recibido con la firma añadida del servidor.
     */
    def post() {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ContentTypeVS contentTypeVS = ContentTypeVS.getByName(request?.contentType)
        ResponseVS responseVS = null
        if(ContentTypeVS.VICKET == contentTypeVS) {
            responseVS = vicketService.processTransactionVS(messageSMIMEReq, null, request.locale)
        } else responseVS = transactionVSService.processTransactionVS(messageSMIMEReq, request.locale)
        return [responseVS:responseVS]
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        Throwable rootCause = StackTraceUtils.extractRootCause(exception)
        log.error " Exception occurred. ${rootCause.getMessage()}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action_${rootCause.getClass().getSimpleName()}"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: rootCause.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:rootCause.getMessage())]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        Throwable rootCause = StackTraceUtils.extractRootCause(exception)
        log.error " Exception occurred. ${rootCause.getMessage()}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action_${exception.getClass().getSimpleName()}"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: message(code:'paramsErrorMsg'),
                metaInf:metaInf, type:TypeVS.ERROR, reason:rootCause.getMessage())]
    }

}
