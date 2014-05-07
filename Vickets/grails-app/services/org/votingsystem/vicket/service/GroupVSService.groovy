package org.votingsystem.vicket.service

import grails.converters.JSON
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.GroupVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils

import java.security.acl.Group

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class GroupVSService {

	//static transactional = true

    def userVSService
    def messageSource
    def grailsApplication
    def signatureVSService

	public void init() {

	}

    ResponseVS saveGroup(MessageSMIME messageSMIMEReq, Locale locale) {
        GroupVS groupVS = null
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("saveGroup - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        try {
            String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
            def messageJSON = JSON.parse(documentStr)
            if (!messageJSON.groupvsName || !messageJSON.groupvsInfo ||
                    (TypeVS.VICKET_NEWGROUP != TypeVS.valueOf(messageJSON.operation))) {
                msg = messageSource.getMessage('paramsErrorMsg', null, locale)
                log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
                return new ResponseVS(type:TypeVS.VICKET_ERROR, message:msg, reason:msg,
                        statusCode:ResponseVS.SC_ERROR_REQUEST)
            }

            groupVS = GroupVS.findWhere(name:messageJSON.groupvsName.trim())
            if(groupVS) {
                msg = messageSource.getMessage('nameGroupRepeatedMsg', [messageJSON.groupvsName].toArray(), locale)
                log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
                return new ResponseVS(type:TypeVS.VICKET_ERROR, message:msg, reason:msg,
                        statusCode:ResponseVS.SC_ERROR_REQUEST)
            }

            groupVS = new GroupVS(name:messageJSON.groupvsName.trim(), state:GroupVS.State.ACTIVE, groupRepresentative:userSigner,
                    description:messageJSON.groupvsInfo, type:UserVS.Type.GROUP).save()

            String fromUser = grailsApplication.config.VotingSystem.serverName
            String toUser = userSigner.getNif()
            String subject = messageSource.getMessage('newGroupVSReceiptSubject', null, locale)
            byte[] smimeMessageRespBytes = signatureVSService.getSignedMimeMessage(
                    fromUser, toUser, documentStr, subject, null)

            MessageSMIME.withTransaction { new MessageSMIME(type:TypeVS.RECEIPT,
                    smimeParent:messageSMIMEReq, content:smimeMessageRespBytes).save() }
            return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_NEWGROUP, data:groupVS)
        } catch(Exception ex) {
            log.error (ex.getMessage(), ex)
            msg = messageSource.getMessage('publishVotingErrorMessage', null, locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR,
                    message:msg, type:TypeVS.VOTING_EVENT_ERROR, eventVS:eventVS)
        }
    }

 	public Map getGroupVSDataMap(GroupVS groupVS){
        Map resultMap = [id:groupVS.id, name:groupVS.name, description:groupVS.description, state:groupVS.state.toString(),
            representative:userVSService.getUserVSDataMap(groupVS.groupRepresentative)]
        List userList = []
        groupVS.userVSSet.each { userVS ->
            userList.add(userVSService.getUserVSDataMap(userVS))
        }
        resultMap.users = userList
        return resultMap
	}

}

