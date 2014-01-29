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

    UserVS systemUser

    public synchronized Map init() throws Exception {
        log.debug("init")
        systemUser = UserVS.findWhere(type:UserVS.Type.SYSTEM)
        if(!systemUser) {
            systemUser = new UserVS(IBAN:"GR16 0110 1250 0000 0001 2300 695", nif:"TICKET_SYSTEM_NIF",
                    type:UserVS.Type.SYSTEM, firstName: "TICKET", lastName: "SYSTEM").save()
        }
        return [systemUser:systemUser]
    }

    public UserVS getSystemUser() {
        if(!systemUser) systemUser = init().systemUser
        return systemUser;
    }

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

    public Map getUserVSDataMap(UserVS userVS){

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

