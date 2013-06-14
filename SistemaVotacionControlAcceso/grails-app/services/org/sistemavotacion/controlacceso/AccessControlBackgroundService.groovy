package org.sistemavotacion.controlacceso

import org.codehaus.groovy.grails.web.json.JSONArray
import org.sistemavotacion.controlacceso.modelo.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class AccessControlBackgroundService {
	
	static transactional = true

	public Respuesta getUsuario(Date fromDate){
		def usuarios 
		Usuario.withTransaction {
			
		}
	}
	
	public void checkRepresentativeAccreditations() {
		def representatives = Usuario.findAllWhere(type:Usuario.Type.REPRESENTATIVE)
		representatives.each {representative ->
			def representations = Usuario.countByRepresentative(representative)
			representative.setNumRepresentations(representations)
			representative.save()
			log.debug("checkRepresentativeAccreditations - representative: ${representative.nif} has ${representations} representations")
		}
		
		
		
		
	}
	
	
	//check user.metainf num representations
}

