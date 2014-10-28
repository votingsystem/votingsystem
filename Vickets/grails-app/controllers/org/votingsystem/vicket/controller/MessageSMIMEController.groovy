package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.TransactionVS
import org.votingsystem.vicket.util.AsciiDocUtil

/**
 * @infoController Mensajes firmados
 * @descController Servicios relacionados con los messages firmados manejados por la
 *                 aplicación.
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class MessageSMIMEController {

    def userVSService

    /**
     * @httpMethod [GET]
     * @serviceURL [/messageSMIME/$id]
     * @param [id]	Obligatorio. Identificador del message en la base de datos
     * @return El message solicitado.
     */
    def index() {
        def messageSMIME = request.messageSMIME
        if(!messageSMIME) {
            MessageSMIME.withTransaction{ messageSMIME = MessageSMIME.get(params.long('id')) }
        }
        if (messageSMIME) {
            if(ContentTypeVS.TEXT != request.contentTypeVS) {
                request.messageSMIME = messageSMIME
                forward(action:"contentViewer")
                return false
            } else {
                return [responseVS : new ResponseVS(statusCode:ResponseVS.SC_OK, contentType:ContentTypeVS.TEXT_STREAM,
                        messageBytes:messageSMIME.content)]
            }
        } else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'messageSMIMENotFound', args:[params.id]))]
    }

    def transactionVS() {
        TransactionVS transactionVS = null
        TransactionVS.withTransaction {
            transactionVS = TransactionVS.get(params.long('id'))
        }
        if(transactionVS) {
            request.messageSMIME = transactionVS.messageSMIME
            forward(action:"index")
        } else return [responseVS : new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'transactionVSNotFound', args:[params.id]))]
    }

    def contentViewer() {
        String viewer = "message-smime"
        String smimeMessageStr
        String timeStampDate
        boolean isAsciiDoc = false
        def signedContentJSON
        if(request.messageSMIME) {
            smimeMessageStr = Base64.getEncoder().encodeToString(request.messageSMIME.content)
            SMIMEMessage smimeMessage = request.messageSMIME.getSMIME()
            if(smimeMessage.getTimeStampToken() != null) {
                timeStampDate = DateUtils.getDateStr(smimeMessage.getTimeStampToken().getTimeStampInfo().getGenTime());
            }
            if(smimeMessage.getContentTypeVS() == ContentTypeVS.ASCIIDOC) {
                signedContentJSON = JSON.parse(AsciiDocUtil.getMetaInfVS(
                        request.messageSMIME.getSMIME()?.getSignedContent()))
                signedContentJSON.asciiDoc = request.messageSMIME.getSMIME()?.getSignedContent()
                signedContentJSON.asciiDocHTML = AsciiDocUtil.getHTML(request.messageSMIME.getSMIME()?.getSignedContent())
            } else {
                signedContentJSON = JSON.parse(request.messageSMIME.getSMIME()?.getSignedContent())
            }
            TypeVS operation = TypeVS.valueOf(signedContentJSON.operation)
            if(TypeVS.VICKET_SEND != operation) {
                if(!signedContentJSON.fromUserVS) signedContentJSON.fromUserVS =
                        userVSService.getUserVSBasicDataMap(request.messageSMIME.userVS)
            }
            switch(operation) {
                case TypeVS.VICKET_GROUP_NEW:
                    viewer = "message-smime-groupvs-new"
                    break;
                case TypeVS.FROM_BANKVS:
                    viewer = "message-smime-transactionvs-from-bankvs"
                    break;
            }
            params.operation = signedContentJSON.operation
        }
        /*if(params.operation) {
            try {
                TypeVS operationType = TypeVS.valueOf(params.operation.toUpperCase())
                operationType = TypeVS.valueOf(params.operation.toUpperCase())
                switch(operationType) {
                    case TypeVS.FROM_BANKVS:
                        viewer = "message-smime"
                        break;
                    case TypeVS.FROM_GROUP_TO_ALL_MEMBERS:
                        viewer = "message-smime"
                        break;
                }
            } catch(Exception ex) { log.error(ex.getMessage(), ex)}
        }*/
        Map model = [operation:params.operation, smimeMessage:smimeMessageStr,
               viewer:viewer, signedContentMap:signedContentJSON, timeStampDate:timeStampDate]
        if(request.contentType?.contains("json")) {
            render model as JSON
        } else render(view:'contentViewer', model:model)
    }


    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}