package org.sistemavotacion.controlacceso

import org.codehaus.groovy.grails.web.json.JSONArray
import org.sistemavotacion.controlacceso.modelo.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
*/
class AccessControlBackgroundService {
	
	static transactional = true

	public Respuesta getUsuario(Date fromDate){
		def usuarios 
		Usuario.withTransaction {
			
		}
	}
	
}

