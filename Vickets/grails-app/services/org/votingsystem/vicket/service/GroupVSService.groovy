package org.votingsystem.vicket.service

import org.votingsystem.model.EventVS
import org.votingsystem.model.GroupVS
import org.votingsystem.model.TypeVS
import org.votingsystem.util.DateUtils
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class GroupVSService {
	
	static transactional = true

    def userVSService

	public void init() {

	}

 	public Map getGroupVSDataMap(GroupVS groupVS){
        Map resultMap = [id:groupVS.id, name:groupVS.name, description:groupVS.description]
        List userList = []
        groupVS.userVSSet.each { userVS ->
            userList.add(userVSService.getUserVSDataMap(userVS))
        }
        resultMap.users = userList
        return resultMap
	}

}

