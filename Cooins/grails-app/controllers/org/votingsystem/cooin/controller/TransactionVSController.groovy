package org.votingsystem.cooin.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.springframework.dao.DataAccessException
import org.votingsystem.cooin.model.CooinTransactionBatch
import org.votingsystem.cooin.model.TransactionVS
import org.votingsystem.groovy.util.RequestUtils
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils

class TransactionVSController {

    def userVSService
    def transactionVSService
    def signatureVSService
    def cooinService

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
        Map sortParamsMap = RequestUtils.getSortParamsMap(params)
        Map.Entry sortParam
        if(!sortParamsMap.isEmpty()) sortParam = sortParamsMap?.entrySet()?.iterator()?.next()
        List<TransactionVS> transactionList = null
        TransactionVS.withTransaction {
            if(params.searchText || params.dateFrom || params.dateTo || params.transactionvsType) {
                TransactionVS.Type transactionType = null
                BigDecimal amount = null
                Date dateFrom = null
                Date dateTo = null
                try {
                    if(params.transactionvsType) transactionType = TransactionVS.Type.valueOf(params.transactionvsType)
                    else transactionType = TransactionVS.Type.valueOf(params.searchText.toUpperCase())} catch(Exception ex) {}
                try {amount = new BigDecimal(params.searchText)} catch(Exception ex) {}
                if(params.dateFrom) try {dateFrom = DateUtils.getURLDate(params.dateFrom)} catch(Exception ex) {}
                if(params.dateTo) try {dateTo = DateUtils.getURLDate(params.dateTo)} catch(Exception ex) {}

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
            } else {
                transactionList = TransactionVS.createCriteria().list(max: params.max, offset: params.offset,
                        sort:sortParam?.key, order:sortParam?.value){
                    isNull('transactionParent')
                };
            }
        }
        List<Map> resultList = []
        transactionList.each {transactionItem ->
            resultList.add(transactionVSService.getTransactionMap(transactionItem))
        }
        def resultMap = [transactionRecords:resultList, offset:params.offset, max: params.max,
                         totalCount:transactionList.totalCount ]
        if(request.contentType?.contains("json")) render resultMap as JSON
        else render(view:'index', model: [transactionsMap: (resultMap as JSON)])
        return false
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
     * Service that process an anonymous transaction.
     *
     * @httpMethod [POST]
     * @serviceURL [/transactionVS/cooin]
     * @requestContentType [JSON] https://github.com/votingsystem/votingsystem/wiki/Lote-de-Cooins
     * @responseContentType [application/pkcs7-mime]. batch receipt.
     * @return
     */
    def cooin() {
        if(!request.JSON) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = cooinService.processCooinTransaction(new CooinTransactionBatch(request.JSON.toString()))
        return [responseVS:responseVS]
    }

    /**
     * Servicio que recibe los asignaciones de los usuarios en documentos SMIME
     *
     * @httpMethod [POST]
     * @serviceURL [/transactionVS]
     * @requestContentType [application/pkcs7-signature] Obligatorio.
     *                     documento SMIME firmado con un model emitido por el sistema.
     * @responseContentType [application/pkcs7-signature]. Recibo firmado por el sistema.
     * @return  Recibo que consiste en el documento recibido con la firma añadida del servidor.
     */
    def post() {
        MessageSMIME messageSMIME = request.messageSMIMEReq
        if(!messageSMIME) return [responseVS:ResponseVS.ERROR_REQUEST(message(code:'requestWithoutFile'))]
        return [responseVS:transactionVSService.processTransactionVS(messageSMIME)]
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

    def daoExceptionHandler(final DataAccessException exception) {
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}
