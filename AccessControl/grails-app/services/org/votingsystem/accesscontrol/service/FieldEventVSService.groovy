package org.votingsystem.accesscontrol.service

import org.codehaus.groovy.grails.web.json.JSONArray
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.FieldEventVS;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class FieldEventVSService {
	
    static transactional = true

    Set<FieldEventVS> saveFieldsEventVS(EventVSElection eventVS, JSONArray fieldsEventVS) {
        log.debug("saveFieldsEventVS - eventVS: ${eventVS.id} - fieldsEventVS: ${fieldsEventVS}")
        Set<FieldEventVS> fieldsEventVSSet = fieldsEventVS.collect { fieldEventItem ->
			eventVS.refresh()
            FieldEventVS fieldEvent = new FieldEventVS(eventVS:eventVS, content:fieldEventItem?.content)
            return fieldEvent.save();
        }
        return fieldsEventVSSet
    }
	
}

