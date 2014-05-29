package org.votingsystem.accesscontrol.controller

import grails.converters.JSON

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