package org.votingsystem.accesscontrol.controller

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
        if(params.operation) {
            try {
                TypeVS operationType = TypeVS.valueOf(params.operation.toUpperCase())
                operationType = TypeVS.valueOf(params.operation.toUpperCase())
                switch(operationType) {
                    case TypeVS.SEND_SMIME_VOTE:
                        viewerType = 'voteVSViewer'
                        break;
                }
            } catch(Exception ex) { log.error(ex.getMessage(), ex)}
        }
        render(view:viewerType, model:[operation:params.operation])
    }

}