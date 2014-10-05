package org.votingsystem.vicket.controller

import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.vicket.model.MessageVS

/**
 * @infoController Aplicación
 * @descController Controlador que proporciona servicios de mensajes cifrados entre usuarios
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class MessageVSController {

	def grailsApplication;
    def messageVSService

    def index() {
        MessageVS messageVS = request.messageVS
        if(!messageVS) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = messageVSService.send(messageVS, request.getLocale())
        return [responseVS:responseVS]
    }

    def inbox() {
        //render(view:'inbox', model: [messageVSList:"${messageVSList as JSON}"])
    }

    /**
     * If any method in this controller invokes code that will throw a Exception then this method is invoked.
     */
    def exceptionHandler(final Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
        String metaInf = "EXCEPTION_${params.controller}Controller_${params.action}Action"
        return [responseVS:new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message: exception.getMessage(),
                metaInf:metaInf, type:TypeVS.ERROR, reason:exception.getMessage())]
    }
}