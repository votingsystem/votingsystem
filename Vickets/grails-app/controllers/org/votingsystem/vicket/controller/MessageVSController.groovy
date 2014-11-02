package org.votingsystem.vicket.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.vicket.model.MessageVS
import org.votingsystem.vicket.websocket.SessionVSHelper

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
        if(!messageVS) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'requestWithoutFile'))]
        return [responseVS:messageVSService.send(messageVS)]
    }

    def inbox() {
        //render(view:'inbox', model: [messageVSList:"${messageVSList as JSON}"])
    }

    def connected() {
        List<Long> connectedUsers = SessionVSHelper.getInstance().connectedUsers
        render connectedUsers as JSON
    }

    def sendMessageByPhone() {
        SessionVSHelper.getInstance().sendMessage(params.long("userId"), params.message)
        render "OK"
        return false
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}