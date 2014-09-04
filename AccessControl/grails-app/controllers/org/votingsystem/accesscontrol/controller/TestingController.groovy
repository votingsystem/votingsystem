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

    def index() {
        def certVS
        CertificateVS.withTransaction {
            certVS = CertificateVS.createCriteria().list (offset: 0) {
                eq("tipo", Tipo.REPRESENTATIVE_DATA)
                le("dateCreated", selectedDate)
            }
        }
    }

}