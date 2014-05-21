package org.votingsystem.vicket.service

import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.NifUtils

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class UserVSService {
	
	static transactional = false
	
	def systemAdmins
	def grailsApplication
    def grailsLinkGenerator

    private UserVS systemUser

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
        Map resultMap = [id:subscriptionVS.id, dateActivated:subscriptionVS.dateActivated,
             dateCancelled:subscriptionVS.dateCancelled, lastUpdated:subscriptionVS.lastUpdated,
             uservs:[id:subscriptionVS.userVS.id, NIF:subscriptionVS.userVS.nif,
                   name:"${subscriptionVS.userVS.firstName} ${subscriptionVS.userVS.lastName}"],
             groupvs:[name:subscriptionVS.groupVS.name, id:subscriptionVS.groupVS.id],
                state:subscriptionVS.state.toString(), dateCreated:subscriptionVS.dateCreated]
        return resultMap
    }

    public Map getSubscriptionVSDetailedDataMap(SubscriptionVS subscriptionVS){
        String subscriptionMessageURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${subscriptionVS.subscriptionSMIME.id}"
        def adminMessages = []
        subscriptionVS.adminMessageSMIMESet.each {adminMessage ->
            adminMessages.add("${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${adminMessage.id}")
        }

        Map resultMap = [id:subscriptionVS.id, dateActivated:subscriptionVS.dateActivated,
                dateCancelled:subscriptionVS.dateCancelled, lastUpdated:subscriptionVS.lastUpdated,
                messageURL:subscriptionMessageURL,adminMessages:adminMessages,
                uservs:[id:subscriptionVS.userVS.id, NIF:subscriptionVS.userVS.nif,
                      name:"${subscriptionVS.userVS.firstName} ${subscriptionVS.userVS.lastName}"],
                groupvs:[name:subscriptionVS.groupVS.name, id:subscriptionVS.groupVS.id],
                state:subscriptionVS.state.toString(), dateCreated:subscriptionVS.dateCreated]
        return resultMap
    }

	boolean isUserAdmin(String nif) {
        nif = NifUtils.validate(nif);
		if(!systemAdmins) {
            systemAdmins = new ArrayList<String>();
            "${grailsApplication.config.VotingSystem.adminsDNI}".split(",")?.each {
                systemAdmins.add(NifUtils.validate(it.trim()))
            }
		}
        boolean result = systemAdmins.contains(nif)
        if(result) log.debug("isUserAdmin - nif: ${nif}")
		return result
	}

}

