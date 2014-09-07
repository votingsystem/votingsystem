package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.CertificateVS

/**
 * @infoController TestingController
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 * */
class TestingController {

    def signatureVSService

    def index() {
        signatureVSService.loadCertAuthorities()
    }

}