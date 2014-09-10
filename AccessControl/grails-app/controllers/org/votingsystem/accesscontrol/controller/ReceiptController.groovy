package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.TypeVS

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class ReceiptController {


    def contentViewer() {
        String viewerType = 'contentViewer'
        String smimeMessage
        def signedContentJSON
        if(params.messageSMIME) {
            smimeMessage = new String(params.messageSMIME.content, "UTF-8")
            signedContentJSON = JSON.parse(params.messageSMIME.getSmimeMessage()?.getSignedContent())
            params.operation = signedContentJSON.operation
        }
        if(params.operation) {
            try {
                TypeVS operationType = TypeVS.valueOf(params.operation.toUpperCase())
                operationType = TypeVS.valueOf(params.operation.toUpperCase())
                switch(operationType) {
                    case TypeVS.SEND_SMIME_VOTE:
                        viewerType = 'voteVSViewer'
                        break;
                    case TypeVS.CANCEL_VOTE:
                        viewerType = 'voteVSCancellerViewer'
                        break;
                }
            } catch(Exception ex) { log.error(ex.getMessage(), ex)}
        }
        render(view:viewerType, model:[operation:params.operation, smimeMessage:smimeMessage,
                                       signedContentMap:signedContentJSON])
    }

}