package org.votingsystem.ticket.service

import org.votingsystem.model.UserVS

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class UserVSService {
	
	static transactional = true
	
	List<String> systemAdmins
	def grailsApplication

	public Map getUserVS(Date fromDate){
		def usersVS
		UserVS.withTransaction {
			def criteria = UserVS.createCriteria()
			usersVS = criteria.list {
				gt("dateCreated", fromDate)
			}
		}
		int numUsers = UserVS.countByDateCreatedGreaterThan(fromDate)
		return [totalNumUsu:numUsers]
	}

	boolean isUserAdmin(String nif) {
		if(!systemAdmins) {
            systemAdmins = new ArrayList<String>();
            "${grailsApplication.config.VotingSystem.adminsDNI}".split(",")?.each {
                systemAdmins.add(it.trim())
            }
		}
		return systemAdmins.contains(nif)
	}

}

