package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.apache.log4j.Logger
import org.votingsystem.model.AccessRequestVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.DateUtils
import java.util.Calendar
import static java.util.Calendar.*


/**
 * @infoController TestingController
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class TestingController {


    def index() {
        List<String> admins = grailsApplication.config.VotingSystem.adminsDNI
        render admins as JSON
    }


}