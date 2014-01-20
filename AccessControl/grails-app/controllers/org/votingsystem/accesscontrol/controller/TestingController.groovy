package org.votingsystem.accesscontrol.controller

import grails.converters.JSON
import org.votingsystem.model.AccessRequestVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS


/**
 * @infoController TestingController
 * @descController Servicios de acceso a la aplicación web principal
 *
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 * */
class TestingController {


    def index() {


        MessageSMIME.withTransaction {
            List userDelegations = MessageSMIME.findAllWhere(type:TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST)
            for(MessageSMIME delegation:userDelegations) {
                delegation.type = TypeVS.ANONYMOUS_REPRESENTATIVE_REQUEST_USED
                delegation.save()
            }
        }


        render "OK";
        return false;

    }

}