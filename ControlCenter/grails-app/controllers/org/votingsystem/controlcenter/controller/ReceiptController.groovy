package org.votingsystem.controlcenter.controller

import grails.converters.JSON
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class ReceiptController {


    def contentViewer() {
        String viewer = "receipt-votingsystem"
        String smimeMessageStr
        String timeStampDate
        def signedContentJSON
        if(params.messageSMIME) {
            smimeMessageStr = new String(params.messageSMIME.content, "UTF-8")
            SMIMEMessageWrapper smimeMessage = params.messageSMIME.getSmimeMessage()
            if(smimeMessage.getTimeStampToken() != null) {
                timeStampDate = DateUtils.getLongDate_Es(smimeMessage.getTimeStampToken().getTimeStampInfo().getGenTime());
            }
            signedContentJSON = JSON.parse(params.messageSMIME.getSmimeMessage()?.getSignedContent())
            params.operation = signedContentJSON.operation
        }
        if(params.operation) {
            try {
                TypeVS operationType = TypeVS.valueOf(params.operation.toUpperCase())
                operationType = TypeVS.valueOf(params.operation.toUpperCase())
                switch(operationType) {
                    case TypeVS.SEND_SMIME_VOTE:
                        viewer = "receipt-votevs"
                        break;
                }
            } catch(Exception ex) { log.error(ex.getMessage(), ex)}
        }
        render(view:'receiptViewer', model:[operation:params.operation, smimeMessage:smimeMessageStr,
                                            viewer:viewer, signedContentMap:signedContentJSON, timeStampDate:timeStampDate])
    }

}