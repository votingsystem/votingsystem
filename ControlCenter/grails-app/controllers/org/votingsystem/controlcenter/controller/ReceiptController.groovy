package org.votingsystem.controlcenter.controller

import org.votingsystem.model.TypeVS

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class ReceiptController {


    def index() {
        TypeVS operationType = TypeVS.valueOf(params.operation.toUpperCase())
        String viewerType = 'contentViewer'
        if(params.operation) {
            try {
                operationType = TypeVS.valueOf(params.operation.toUpperCase())
                switch(operationType) {

                }
            } catch(Exception ex) { log.error(ex.getMessage(), ex)}
        }
        render(view:viewerType, model:[operation:params.operation])
    }

}