package org.votingsystem.vicket.service

import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.UserVS

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class UserVSService {
	
	//static transactional = true
	
	List<String> systemAdmins
	def grailsApplication
    def grailsLinkGenerator

    UserVS systemUser

    public synchronized Map init() throws Exception {
        log.debug("init")
        systemUser = UserVS.findWhere(type:UserVS.Type.SYSTEM)
        if(!systemUser) {
            systemUser = new UserVS(IBAN:"GR16 0110 1250 0000 0001 2300 695", nif:"VICKET_SYSTEM_NIF",
                    type:UserVS.Type.SYSTEM, firstName: "VICKET", lastName: "SYSTEM").save()
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
			usersVS = UserVS.createCriteria().list(offset: 0) {
				gt("dateCreated", fromDate)
			}
		}
		return [totalNumUsu:usersVS?usersVS.getTotalCount():0]
	}

    public Map getUserVSDataMap(UserVS userVS){
        return [id:userVS?.id, nif:userVS?.nif, firstName: userVS.firstName, lastName: userVS.lastName, nif:userVS?.nif]
    }

    public Map getSubscriptionVSDataMap(SubscriptionVS subscriptionVS){
        Map resultMap = [subscriptionId:subscriptionVS.id, id:subscriptionVS.userVS.id, nif:subscriptionVS.userVS.nif,
                name:"${subscriptionVS.userVS.firstName} ${subscriptionVS.userVS.lastName}",
                         state:subscriptionVS.state.toString(), subscriptionDateCreated:subscriptionVS.dateCreated]
        return resultMap
    }

    public Map getSubscriptionVSDetailedDataMap(SubscriptionVS subscriptionVS){
        String subscriptionMessageURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${subscriptionVS.subscriptionSMIME.id}"
        def adminMessages = []
        subscriptionVS.adminMessageSMIMESet.each {adminMessage ->
            adminMessages.add("${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${adminMessage.id}")
        }
        Map resultMap = [subscriptionId:subscriptionVS.id, id:subscriptionVS.userVS.id, nif:subscriptionVS.userVS.nif,
                name:"${subscriptionVS.userVS.firstName} ${subscriptionVS.userVS.lastName}",
                state:subscriptionVS.state.toString(), subscriptionDateCreated:subscriptionVS.dateCreated,
                subscriptionDateActivated:subscriptionVS.dateActivated, subscriptionDateCancelled:subscriptionVS.dateCancelled,
                subscriptionLastUpdated:subscriptionVS.lastUpdated, subscriptionMessageURL:subscriptionMessageURL,
                adminMessages:adminMessages]
        return resultMap
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

