package org.votingsystem.vicket.controller

/**
 * @infoController Aplicación
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class ReceiptController {


    def contentViewer() {
        render(view:'contentViewer', model:[operation:params.operation])
    }

}