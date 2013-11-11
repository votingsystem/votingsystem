package org.votingsystem.controlcenter.service

import java.util.Date;
import java.util.List;

import org.codehaus.groovy.grails.web.json.JSONArray
import org.votingsystem.controlcenter.model.*;
import java.security.cert.X509Certificate;
import org.votingsystem.signature.util.*

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class UsuarioService {
	
	static transactional = true
	
	List<String> administradoresSistema
	def grailsApplication

	public Map getUsuario(Date fromDate){
		def usuarios
		Usuario.withTransaction {
			def criteria = Usuario.createCriteria()
			usuarios = criteria.list {
				gt("dateCreated", fromDate)
			}
		}
		int numUsu = Usuario.countByDateCreatedGreaterThan(fromDate)

		Map datosRespuesta = [totalNumUsu:numUsu]
		return datosRespuesta
	}
	
	
	boolean isUserAdmin(String nif) {
		if(!administradoresSistema) {
			administradoresSistema = Arrays.asList(
			"${grailsApplication.config.VotingSystem.adminsDNI}".split(","))
		}
		return administradoresSistema.contains(nif)
	}
}

