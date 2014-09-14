package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.TypeVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.util.AsciiDocUtil

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
        boolean isAsciiDoc = false
        def signedContentJSON
        if(params.messageSMIME) {
            smimeMessageStr = new String(params.messageSMIME.content, "UTF-8")
            SMIMEMessageWrapper smimeMessage = params.messageSMIME.getSmimeMessage()
            if(smimeMessage.getTimeStampToken() != null) {
                timeStampDate = DateUtils.getLongDate_Es(smimeMessage.getTimeStampToken().getTimeStampInfo().getGenTime());
            }
            if(smimeMessage.getContentTypeVS() == ContentTypeVS.ASCIIDOC) {
                signedContentJSON = JSON.parse(AsciiDocUtil.getMetaInfVS(
                        params.messageSMIME.getSmimeMessage()?.getSignedContent()))
                signedContentJSON.asciiDoc = params.messageSMIME.getSmimeMessage()?.getSignedContent()
                signedContentJSON.asciiDocHTML = AsciiDocUtil.getHTML(params.messageSMIME.getSmimeMessage()?.getSignedContent())
            } else {
                signedContentJSON = JSON.parse(params.messageSMIME.getSmimeMessage()?.getSignedContent())
            }

            log.debug("========== ${signedContentJSON}")
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
                    case TypeVS.VICKET_DEPOSIT_FROM_VICKET_SOURCE:
                        viewer = "receipt-deposit-from-vicketsource"
                        break;
                }
            } catch(Exception ex) { log.error(ex.getMessage(), ex)}
        }
        render(view:'receiptViewer', model:[operation:params.operation, smimeMessage:smimeMessageStr,
                viewer:viewer, signedContentMap:signedContentJSON, timeStampDate:timeStampDate])
    }

}