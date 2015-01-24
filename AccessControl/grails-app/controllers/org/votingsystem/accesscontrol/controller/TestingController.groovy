package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.ResponseVS
import org.votingsystem.throwable.ExceptionVS

/**
 * @infoController TestingController
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class TestingController {

    def signatureVSService
    def grailsApplication

    def index() {
        render "OK"
        return false
    }

    def polymer() {  }

}