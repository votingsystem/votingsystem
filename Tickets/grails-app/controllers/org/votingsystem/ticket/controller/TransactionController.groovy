package org.votingsystem.ticket.controller

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONArray
import org.bouncycastle.util.encoders.Base64
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper

import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class TransactionController {

    def userVSService
    def transactionVSService
    def signatureVSService

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
            String msg = message(code: "ticketBatchErrorMsg") + " ${responseVS.getMessage()}"
            log.error(msg)
            return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK,
                    contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: msg.getBytes())]
        } else {
            List<ResponseVS> depositResponseList = new ArrayList<ResponseVS>()
            for(ResponseVS response : responseList) {
                ResponseVS depositResponse = transactionVSService.processTicketDeposit(response.data, request.locale)
                if(ResponseVS.SC_OK == depositResponse.getStatusCode()) {
                    depositResponseList.add(depositResponse);
                } else {
                    responseVS = depositResponse
                    break;
                }
            }
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                if(ResponseVS.SC_ERROR_REQUEST_REPEATED == responseVS.getStatusCode()) {
                    return [receiverPublicKey:receiverPublic, responseVS:responseVS];
                }
                String msg = message(code: "ticketBatchErrorMsg") + " ${responseVS.getMessage()}"
                //_ TODO _  cancel Tickets
                log.error(msg)
                return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK,
                        contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: msg.getBytes())]
            } else {
                List<String> ticketReceiptList = new ArrayList<String>()
                for(ResponseVS response: depositResponseList) {
                    ticketReceiptList.add(new String(((MessageSMIME)response.getData()).content, "UTF-8"))
                }
                Map responseMap = [tickets:ticketReceiptList]
                byte[] responseBytes = "${responseMap as JSON}".getBytes()
                return [receiverPublicKey:receiverPublic, responseVS:new ResponseVS(statusCode:ResponseVS.SC_OK,
                        contentType: ContentTypeVS.MULTIPART_ENCRYPTED, messageBytes: responseBytes)]
            }
        }
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
            responseVS = transactionVSService.processTicketDeposit(messageSMIMEReq, request.locale)
        } else responseVS = transactionVSService.processDeposit(messageSMIMEReq, request.locale)
        return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate]
    }

}
