package org.votingsystem.cooin.controller

import grails.converters.JSON
import net.sf.json.JSONObject
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.model.DeviceVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.cooin.model.MessageVS
import org.votingsystem.cooin.websocket.SessionVSHelper

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
        render SessionVSHelper.getInstance().getConnectedUsersDataMap() as JSON
        return false
    }

    def sendMessage() {
        List<DeviceVS> userDeviceList
        DeviceVS.withTransaction {
            if(params.deviceId) {
                DeviceVS deviceVS = DeviceVS.get(params.long("deviceId"))
                userDeviceList = Arrays.asList(deviceVS)
            } else if(params.userId) {
                UserVS userVS = UserVS.get(params.long("userId"))
                if(userVS) {
                    userDeviceList  = DeviceVS.findAllWhere(userVS:userVS)
                }
            }

        }
        if(!userDeviceList || userDeviceList.isEmpty()) {
            Map result = [status:ResponseVS.SC_NOT_FOUND, message:"No device connected with specified paramas" , params:params]
            render result as JSON
        } else {
            SessionVSHelper.getInstance().sendMessage(userDeviceList, new JSONObject(
                    [status:ResponseVS.SC_OK, message:params.message, operation:TypeVS.MESSAGEVS_SIGN.toString()]).toString())
        }
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