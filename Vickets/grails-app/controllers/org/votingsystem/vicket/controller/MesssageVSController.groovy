package org.votingsystem.vicket.controller

import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.vicket.model.MessageVS

/**
 * @infoController Aplicación
 * @descController Controlador que proporciona servicios de mensajes cifrados entre usuarios
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class MesssageVSController {

	def grailsApplication;
    def messageVSService

    def index() {
        MessageVS messageVS = request.messageVS
        if(!messageVS) {
            return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        }
        ResponseVS responseVS = messageVSService.send(messageVS, request.getLocale())
        /*if(ResponseVS.SC_OK == responseVS.getStatusCode()) {
            GroupVS newGroupVS = responseVS.data
            String URL = "${createLink(controller: 'groupVS', absolute:true)}/${newGroupVS.id}"
            responseVS.data = [statusCode:ResponseVS.SC_OK, message:message(code:'newVicketGroupOKMsg',
                    args:[newGroupVS.name]), URL:URL]
            responseVS.setContentType(ContentTypeVS.JSON)
        }*/
        return [responseVS:responseVS, receiverCert:messageSMIMEReq?.getUserVS()?.getCertificate()]
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