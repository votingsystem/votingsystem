package org.votingsystem.vicket.controller

import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS

/**
 * @infoController SubscriptionVS
 * @descController Servicios relacionados con las subscripciones a grupos de Vickets
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
class SubscriptionVSController {

    def groupVSService


    def test() { }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.VICKET_ERROR, reason:exception.getMessage())]
    }
	
}