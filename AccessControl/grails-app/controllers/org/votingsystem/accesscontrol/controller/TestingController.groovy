package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.ResponseVS


/**
 * @infoController TestingController
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class TestingController {

    def backupService

    def index() {
        EventVS eventVS = EventVS.get(38)
        return [responseVS:backupService.requestBackup(eventVS, request.locale)]
    }

}