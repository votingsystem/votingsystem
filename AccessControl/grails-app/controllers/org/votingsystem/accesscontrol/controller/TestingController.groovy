package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.EventVSElection
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
    def eventVSElectionService
    def representativeService

    def index() {
        EventVSElection eventVS = null;
        EventVSElection.withTransaction {eventVS = EventVSElection.get(8L)}
        representativeService.getAccreditationsBackupForEvent(eventVS)
        render "OK"
        return false
    }

    def polymer() {  }

}