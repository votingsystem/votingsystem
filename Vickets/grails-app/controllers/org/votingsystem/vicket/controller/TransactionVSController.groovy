package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.bouncycastle.util.encoders.Base64
import org.codehaus.groovy.grails.web.json.JSONArray
import org.springframework.dao.DataAccessException
import org.votingsystem.groovy.util.RequestUtils
import org.votingsystem.model.*
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.model.Vicket
import org.votingsystem.vicket.model.VicketBatchRequest
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
import org.codehaus.groovy.runtime.StackTraceUtils
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

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
                resultMap = transactionVSService.getTransactionMap(result, request.locale)
                resultMap.receipt = receipt
            }
        }
        if(request.contentType?.contains("json")) {
            render resultMap as JSON
        } else {
            render(view:'transactionViewer', model: [transactionvsMap:resultMap])
        }
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
                resultList.add(transactionVSService.getTransactionMap(transactionItem, request.locale))
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
            Map resultMap = transactionVSService.getUserVSTransactionVSMap(userVS, timePeriod, params, request.locale)
            render resultMap as JSON
        } else return [responseVS:new ResponseVS(statusCode: ResponseVS.SC_ERROR_REQUEST,
                message: message(code: 'requestWithErrors'))]
    }

    /**
     * Servicio que recibe una transacción compuesta por un lote de Vickets
     *
     * @httpMethod [POST]
     * @serviceURL [/transactionVS/vicketBatch]
     * @requestContentType Documento JSON con la extructura https://github.com/votingsystem/votingsystem/wiki/Lote-de-Vickets
     * @responseContentType [application/pkcs7-mime]. Documento JSON cifrado en el que figuran los recibos de los model recibidos.
     * @return
     */
    def vicketBatch() {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        if(!params.requestBytes) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        VicketBatchRequest batchRequest;
        VicketBatchRequest.withTransaction {
            batchRequest = new VicketBatchRequest(state:BatchRequest.State.OK, content:params.requestBytes,
                    type: TypeVS.VICKET_REQUEST).save()
        }
        def requestJSON = JSON.parse(new String(params.requestBytes, ContextVS.UTF_8))
        byte[] decodedPK = Base64.decode(requestJSON.publicKey);
        PublicKey receiverPublic =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
        //log.debug("receiverPublic.toString(): " + receiverPublic.toString());
        JSONArray vicketsArray = requestJSON.vickets
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK)
        byte[] bytesResponse
        List<ResponseVS> responseList = new ArrayList<ResponseVS>()
        for(int i = 0; i < vicketsArray.size(); i++) {
            SMIMEMessageWrapper smimeMessageReq = new SMIMEMessageWrapper(new ByteArrayInputStream(
                    Base64.decode(vicketsArray.getString(i).getBytes())))
            ResponseVS signatureResponse = signatureVSService.processSMIMERequest(smimeMessageReq, ContentTypeVS.VICKET,
                    request.getLocale())
            if(ResponseVS.SC_OK == signatureResponse.getStatusCode()) {
                responseList.add(signatureResponse);
            } else {
                responseVS = signatureResponse
                break;
            }
        }
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            String msg = message(code: "vicketBatchErrorMsg") + "--- ${responseVS.getMessage()}"
            String metaInfMsg = MetaInfMsg.getErrorMsg(methodName, "vicketBatchError")
            cancelVicketBatchRequest(responseList, batchRequest, TypeVS.ERROR, msg, metaInfMsg)
            return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    type: TypeVS.ERROR, metaInf: metaInfMsg, contentType: ContentTypeVS.MULTIPART_ENCRYPTED,
                    messageBytes: msg.getBytes())]
        } else {
            List<ResponseVS> depositResponseList = new ArrayList<ResponseVS>()
            for(ResponseVS response : responseList) {
                ResponseVS depositResponse = vicketService.processVicketDeposit(
                        response.data, batchRequest, request.locale)
                if(ResponseVS.SC_OK == depositResponse.getStatusCode()) {
                    depositResponseList.add(depositResponse);
                } else {
                    responseVS = depositResponse
                    break;
                }
            }
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                    String metaInfMsg = MetaInfMsg.getErrorMsg(methodName, TypeVS.VICKET_REQUEST_WITH_ITEMS_REPEATED.toString())
                    cancelVicketBatchRequest(responseList, batchRequest, TypeVS.VICKET_REQUEST_WITH_ITEMS_REPEATED,
                            responseVS.data.message, metaInfMsg)
                    cancelVicketBatchDeposit(depositResponseList, batchRequest,TypeVS.VICKET_REQUEST_WITH_ITEMS_REPEATED,
                            responseVS.data.message, metaInfMsg)
                    return [receiverPublicKey:receiverPublic, responseVS:responseVS];
                } else {
                    String msg = message(code: "vicketBatchErrorMsg") + " ${responseVS.getMessage()}"
                    cancelVicketBatchRequest(responseList, batchRequest, TypeVS.VICKET_BATCH_ERROR, msg)
                    cancelVicketBatchDeposit(depositResponseList, batchRequest,TypeVS.VICKET_BATCH_ERROR, msg)
                    return [receiverPublicKey:receiverPublic,  responseVS:new ResponseVS(
                            statusCode:responseVS.getStatusCode(), type:TypeVS.VICKET_BATCH_ERROR,
                            contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: msg.getBytes())]
                }
            } else {
                List<String> vicketReceiptList = new ArrayList<String>()
                for(ResponseVS response: depositResponseList) {
                    //Map dataMap = [vicketReceipt:messageSMIMEResp, model:model]
                    vicketReceiptList.add(new String(Base64.encode(((MessageSMIME)response.getData().vicketReceipt).content)))
                }
                Map responseMap = [vickets:vicketReceiptList]
                byte[] responseBytes = "${responseMap as JSON}".getBytes()
                return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK,
                        contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: responseBytes)]
            }
        }
    }

    private void cancelVicketBatchDeposit(List<ResponseVS> responseList, VicketBatchRequest batchRequest, TypeVS typeVS,
              String reason, String metaInf) {
        log.error("cancelVicketBatchDeposit - batchRequest: '${batchRequest.id}' - reason: ${reason} - type: ${typeVS}")
        for(ResponseVS responseVS: responseList) {
            if(responseVS.data instanceof Map) {
                ((MessageSMIME)responseVS.data.vicketReceipt).type = typeVS
                ((MessageSMIME)responseVS.data.vicketReceipt).reason = reason
                ((MessageSMIME)responseVS.data.vicketReceipt).save()
                ((Vicket)responseVS.data.vicket).setState(Vicket.State.OK)
                ((Vicket)responseVS.data.vicket).save()
            } else log.error("cancelVicketBatch unknown data type ${responseVS.data.getClass().getName()}")
        }
        batchRequest.setType(typeVS)
        batchRequest.setState(BatchRequest.State.ERROR)
        batchRequest.setReason(reason)
        batchRequest.save()
    }

    private void cancelVicketBatchRequest(List<ResponseVS> responseList, VicketBatchRequest batchRequest, TypeVS typeVS,
               String reason, String metaInf) {
        log.error("cancelVicketBatch - batchRequest: '${batchRequest.id}' - reason: ${reason} - type: ${typeVS}")
        for(ResponseVS responseVS: responseList) {
            if(responseVS.data instanceof MessageSMIME) {
                ((MessageSMIME)responseVS.data).batchRequest = batchRequest
                ((MessageSMIME)responseVS.data).type = typeVS
                ((MessageSMIME)responseVS.data).reason = reason
                ((MessageSMIME)responseVS.data).save()
            } else log.error("cancelVicketBatch unknown data type ${responseVS.data.getClass().getName()}")
        }
        batchRequest.setType(typeVS)
        batchRequest.setState(BatchRequest.State.ERROR)
        batchRequest.setReason(reason)
        batchRequest.save()
    }

    /**
     * Servicio que recibe los asignaciones de los usuarios en documentos SMIME
     *
     * @httpMethod [POST]
     * @serviceURL [/transactionVS/deposit]
     * @requestContentType [application/x-pkcs7-signature] Obligatorio.
     *                     documento SMIME firmado con un model emitido por el sistema.
     * @responseContentType [application/x-pkcs7-signature]. Recibo firmado por el sistema.
     * @return  Recibo que consiste en el documento recibido con la firma añadida del servidor.
     */
    def deposit() {
        MessageSMIME messageSMIMEReq = request.messageSMIMEReq
        if(!messageSMIMEReq) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ContentTypeVS contentTypeVS = ContentTypeVS.getByName(request?.contentType)
        ResponseVS responseVS = null
        if(ContentTypeVS.VICKET == contentTypeVS) {
            responseVS = vicketService.processVicketDeposit(messageSMIMEReq, null, request.locale)
        } else responseVS = transactionVSService.processDeposit(messageSMIMEReq, request.locale)
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
