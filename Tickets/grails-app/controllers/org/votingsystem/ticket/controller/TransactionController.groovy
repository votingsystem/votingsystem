package org.votingsystem.ticket.controller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.bouncycastle.util.encoders.Base64
import org.votingsystem.model.BatchRequest
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.ticket.TicketVS
import org.votingsystem.model.ticket.TicketVSBatchRequest
import org.votingsystem.signature.smime.SMIMEMessageWrapper

import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class TransactionController {

    def userVSService
    def transactionVSService
    def signatureVSService
    def ticketService

    /**
     * Servicio que recibe una transacción compuesta por un lote de Tickets
     *
     * @httpMethod [POST]
     * @serviceURL [/transaction/ticketBatch]
     * @requestContentType Documento JSON con la extructura https://github.com/jgzornoza/SistemaVotacion/wiki/Lote-de-Tickets
     * @responseContentType [application/pkcs7-mime]. Documento JSON cifrado en el que figuran los recibos de los ticket recibidos.
     * @return
     */
    def ticketBatch() {
        if(!params.requestBytes) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        TicketVSBatchRequest batchRequest;
        TicketVSBatchRequest.withTransaction {
            batchRequest = new TicketVSBatchRequest(state:BatchRequest.State.OK, content:params.requestBytes,
                    type: TypeVS.TICKET_REQUEST).save()
        }
        def requestJSON = JSON.parse(new String(params.requestBytes, "UTF-8"))
        byte[] decodedPK = Base64.decode(requestJSON.publicKey);
        PublicKey receiverPublic =  KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decodedPK));
        //log.debug("receiverPublic.toString(): " + receiverPublic.toString());
        JSONArray ticketsArray = requestJSON.tickets
        ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK)
        byte[] bytesResponse
        List<ResponseVS> responseList = new ArrayList<ResponseVS>()
        for(int i = 0; i < ticketsArray.size(); i++) {
            SMIMEMessageWrapper smimeMessageReq = new SMIMEMessageWrapper(new ByteArrayInputStream(
                    Base64.decode(ticketsArray.getString(i).getBytes())))
            ResponseVS signatureResponse = signatureVSService.processSMIMERequest(smimeMessageReq, ContentTypeVS.TICKET,
                    request.getLocale())
            if(ResponseVS.SC_OK == signatureResponse.getStatusCode()) {
                responseList.add(signatureResponse);
            } else {
                responseVS = signatureResponse
                break;
            }
        }
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
            String msg = message(code: "ticketBatchErrorMsg") + "--- ${responseVS.getMessage()}"
            cancelTicketBatchRequest(responseList, batchRequest, TypeVS.TICKET_SIGNATURE_ERROR, msg)
            return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    type: TypeVS.TICKET_SIGNATURE_ERROR,
                    contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: msg.getBytes())]
        } else {
            List<ResponseVS> depositResponseList = new ArrayList<ResponseVS>()
            for(ResponseVS response : responseList) {
                ResponseVS depositResponse = ticketService.processTicketDeposit(
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
                    cancelTicketBatchRequest(responseList, batchRequest, TypeVS.TICKET_REQUEST_WITH_ITEMS_REPEATED,
                            responseVS.data.message)
                    cancelTicketBatchDeposit(depositResponseList, batchRequest,TypeVS.TICKET_REQUEST_WITH_ITEMS_REPEATED,
                            responseVS.data.message)
                    return [receiverPublicKey:receiverPublic, responseVS:responseVS];
                } else {
                    String msg = message(code: "ticketBatchErrorMsg") + " ${responseVS.getMessage()}"
                    cancelTicketBatchRequest(responseList, batchRequest, TypeVS.TICKET_BATCH_ERROR, msg)
                    cancelTicketBatchDeposit(depositResponseList, batchRequest,TypeVS.TICKET_BATCH_ERROR, msg)
                    return [receiverPublicKey:receiverPublic,  responseVS:new ResponseVS(
                            statusCode:responseVS.getStatusCode(), type:TypeVS.TICKET_BATCH_ERROR,
                            contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: msg.getBytes())]
                }
            } else {
                List<String> ticketReceiptList = new ArrayList<String>()
                for(ResponseVS response: depositResponseList) {
                    //Map dataMap = [ticketReceipt:messageSMIMEResp, ticket:ticket]
                    ticketReceiptList.add(new String(Base64.encode(((MessageSMIME)response.getData().ticketReceipt).content)))
                }
                Map responseMap = [tickets:ticketReceiptList]
                byte[] responseBytes = "${responseMap as JSON}".getBytes()
                return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK,
                        contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: responseBytes)]
            }
        }
    }

    private void cancelTicketBatchDeposit(List<ResponseVS> responseList, TicketVSBatchRequest batchRequest, TypeVS typeVS,
                                          String reason) {
        log.error("cancelTicketBatchDeposit - batchRequest: '${batchRequest.id}' - reason: ${reason} - type: ${typeVS}")
        for(ResponseVS responseVS: responseList) {
            if(responseVS.data instanceof Map) {
                ((MessageSMIME)responseVS.data.ticketReceipt).type = typeVS
                ((MessageSMIME)responseVS.data.ticketReceipt).reason = reason
                ((MessageSMIME)responseVS.data.ticketReceipt).save()
                ((TicketVS)responseVS.data.ticket).setState(TicketVS.State.OK)
                ((TicketVS)responseVS.data.ticket).save()
            } else log.error("cancelTicketBatch unknown data type ${responseVS.data.getClass().getName()}")
        }
        batchRequest.setType(typeVS)
        batchRequest.setState(BatchRequest.State.ERROR)
        batchRequest.setReason(reason)
        batchRequest.save()
    }

    private void cancelTicketBatchRequest(List<ResponseVS> responseList, TicketVSBatchRequest batchRequest, TypeVS typeVS,
               String reason) {
        log.error("cancelTicketBatch - batchRequest: '${batchRequest.id}' - reason: ${reason} - type: ${typeVS}")
        for(ResponseVS responseVS: responseList) {
            if(responseVS.data instanceof MessageSMIME) {
                ((MessageSMIME)responseVS.data).batchRequest = batchRequest
                ((MessageSMIME)responseVS.data).type = typeVS
                ((MessageSMIME)responseVS.data).reason = reason
                ((MessageSMIME)responseVS.data).save()
            } else log.error("cancelTicketBatch unknown data type ${responseVS.data.getClass().getName()}")
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
     * @serviceURL [/transaction/deposit]
     * @requestContentType [application/x-pkcs7-signature,application/x-pkcs7-mime] Obligatorio.
     *                     documento SMIME firmado con un ticket emitido por el sistema.
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
        if(ContentTypeVS.TICKET == contentTypeVS) {
            responseVS = ticketService.processTicketDeposit(messageSMIMEReq, null, request.locale)
        } else responseVS = transactionVSService.processDeposit(messageSMIMEReq, request.locale)
        return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate]
    }

}
