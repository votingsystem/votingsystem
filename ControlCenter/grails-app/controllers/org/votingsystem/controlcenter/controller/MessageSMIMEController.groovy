package org.votingsystem.controlcenter.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils

/**
 * @infoController Messages firmados
 * @descController Servicios relacionados con los messages firmados manejados por la
 *                 aplicación.
 * 
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class MessageSMIMEController {
	
	/**
	 * @httpMethod [GET]
	 * @serviceURL [/messageSMIME/$id] 
	 * @param [id]	Obligatorio. Identificador del message en la base de datos
	 * @return El message solicitado.
	 */
    def index () {
        def messageSMIME
        MessageSMIME.withTransaction{ messageSMIME = MessageSMIME.get(params.long('id')) }
        if (messageSMIME) {
            if(ContentTypeVS.TEXT != request.contentTypeVS) {
                request.messageSMIME = messageSMIME
                forward(action:"contentViewer")
            } else {
                return [responseVS : new ResponseVS(statusCode:ResponseVS.SC_OK, contentType:ContentTypeVS.TEXT_STREAM,
                        messageBytes:messageSMIME.content)]
            }
        } else return [responseVS:new ResponseVS(ResponseVS.SC_NOT_FOUND,
                message(code: 'messageSMIMENotFound', args:[params.id]))]
        return false
    }

    def contentViewer() {
        String viewer = "message-smime"
        String smimeMessageStr
        String timeStampDate
        def signedContentJSON
        if(request.messageSMIME) {
            smimeMessageStr = Base64.getEncoder().encodeToString(request.messageSMIME.content)
            SMIMEMessage smimeMessage = request.messageSMIME.getSMIME()
            if(smimeMessage.getTimeStampToken() != null) {
                timeStampDate = DateUtils.getDateStr(smimeMessage.getTimeStampToken().getTimeStampInfo().getGenTime());
            }
            signedContentJSON = JSON.parse(request.messageSMIME.getSMIME()?.getSignedContent())
            if(signedContentJSON.operation) {
                TypeVS operationType = TypeVS.valueOf(signedContentJSON.operation)
                switch(operationType) {
                    case TypeVS.SEND_SMIME_VOTE:
                        viewer = "message-smime-votevs"
                        break;
                    case TypeVS.CANCEL_VOTE:
                        viewer = "message-smime-votevs-canceller"
                        break;
                }
                params.operation = signedContentJSON.operation
            }
        }
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
        return [responseVS:ResponseVS.EXCEPTION(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }

}