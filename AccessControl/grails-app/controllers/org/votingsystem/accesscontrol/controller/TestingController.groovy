package org.votingsystem.accesscontrol.controller

import org.votingsystem.model.EventVSElection
import org.votingsystem.model.RepresentationDocumentVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.DateUtils

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
        //representativeService.getAccreditationsBackupForEvent(eventVS)
        eventVSElectionService.generateBackup(eventVS)
        render "OK"
        return false
    }

    def dates() {
        Date selectedDate = DateUtils.addDays(-5).getTime();
        int numDocs = 0
        RepresentationDocumentVS.withTransaction {
            List<RepresentationDocumentVS> docs = RepresentationDocumentVS.findAll()
            for(RepresentationDocumentVS doc: docs) {
                doc.setDateCreated(selectedDate)
                doc.save()
                numDocs++
            }
        }
        render "Num. RepresentationDocumentVS modified: $numDocs - selectedDate: ${selectedDate.toString()}"
        return false
    }


}