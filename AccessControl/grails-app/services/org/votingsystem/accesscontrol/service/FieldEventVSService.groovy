package org.votingsystem.accesscontrol.service

import org.codehaus.groovy.grails.web.json.JSONArray
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.FieldEventVS;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class FieldEventVSService {
	
    static transactional = true

    Set<FieldEventVS> saveFieldsEventVS(EventVSElection eventVS, JSONArray fieldsEventVS) {
        log.debug("saveFieldsEventVS - eventVS: ${eventVS.id} - fieldsEventVS: ${fieldsEventVS}")
        Set<FieldEventVS> fieldsEventVSSet = fieldsEventVS.collect {
			eventVS.refresh()
            return new FieldEventVS(eventVS:eventVS, content:it.content).save();
        }
        return fieldsEventVSSet
    }
	
}

