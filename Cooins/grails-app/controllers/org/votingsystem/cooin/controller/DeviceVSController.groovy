package org.votingsystem.cooin.controller

import grails.converters.JSON
import org.codehaus.groovy.runtime.StackTraceUtils
import org.votingsystem.cooin.websocket.SessionVSHelper
import org.votingsystem.model.DeviceVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.NifUtils

import java.security.cert.X509Certificate

class DeviceVSController {

    def list() {
        if(!params.nif) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'nifMissingErrorMsg', args:[]))]
        String nif = NifUtils.validate(params.nif)
        List<DeviceVS> deviceVSList
        DeviceVS.withTransaction {
            deviceVSList = DeviceVS.createCriteria().list {
                userVS { eq('nif', nif) }
            }
        }
        List result = []
        for(DeviceVS deviceVS : deviceVSList) {
            String deviceName = deviceVS.deviceName ? deviceVS.deviceName : "${deviceVS.type.toString()} - $deviceVS.email"
            X509Certificate certX509 = CertUtils.loadCertificate (deviceVS.certificateVS.content)
            result.add([id:deviceVS.id, deviceName:deviceName, certPEM:new String(CertUtils.getPEMEncoded(certX509), "UTF-8")])
        }
        Map resultMap = [deviceList:result]
        render resultMap as JSON
    }

    def connected() {
        if(!params.nif) return [responseVS:new ResponseVS(ResponseVS.SC_ERROR_REQUEST, message(code:'nifMissingErrorMsg', args:[]))]
        String nif = NifUtils.validate(params.nif)
        List<DeviceVS> deviceVSList
        DeviceVS.withTransaction {
            deviceVSList = DeviceVS.createCriteria().list {
                userVS { eq('nif', nif) }
            }
        }
        List result = []
        for(DeviceVS deviceVS : deviceVSList) {
            if(SessionVSHelper.getInstance().get(deviceVS.id)) {
                String deviceName = deviceVS.deviceName ? deviceVS.deviceName : "${deviceVS.type.toString()} - $deviceVS.email"
                X509Certificate certX509 = CertUtils.loadCertificate (deviceVS.certificateVS.content)
                result.add([id:deviceVS.id, deviceName:deviceName, certPEM:new String(CertUtils.getPEMEncoded(certX509), "UTF-8")])
            }
        }
        Map resultMap = [deviceList:result]
        render resultMap as JSON
    }

    /**
     * Invoked if any method in this controller throws an Exception.
     */
    def exceptionHandler(final Exception exception) {
        return [responseVS:ResponseVS.getExceptionResponse(params.controller, params.action, exception,
                StackTraceUtils.extractRootCause(exception))]
    }
}
