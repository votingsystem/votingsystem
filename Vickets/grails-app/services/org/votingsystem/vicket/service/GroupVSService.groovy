package org.votingsystem.vicket.service

import grails.converters.JSON
import org.votingsystem.model.EventVS
import org.votingsystem.model.EventVSElection
import org.votingsystem.model.GroupVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubscriptionVS
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

	public void init() { }

    public ResponseVS cancelGroup(GroupVS groupVS, MessageSMIME messageSMIMEReq, Locale locale) {
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("cancelGroup '${groupVS.id}' - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        if(!groupVS.getGroupRepresentative().nif.equals(messageSMIMEReq.userVS.nif) && !userVSService.isUserAdmin()) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                             TypeVS.VICKET_GROUP_CANCEL.toString(), groupVS.name].toArray(), locale)
            log.error "cancelGroup - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:"cancelGroup_userWithoutPrivilegesErrorMsg",
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvsName || !messageJSON.id ||
                (TypeVS.VICKET_GROUP_CANCEL != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "cancelGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:"cancelGroup_paramsErrorMsg",
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        groupVS.state = GroupVS.State.CLOSED
        groupVS.save()
        return new ResponseVS(type:TypeVS.VICKET_GROUP_CANCEL, message:msg, reason:"cancelGroup_groupvs_id_${groupVS.id}",
                statusCode:ResponseVS.SC_OK)
    }


    public ResponseVS editGroup(GroupVS groupVS, MessageSMIME messageSMIMEReq, Locale locale) {
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("editGroup '${groupVS.id}' - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        if(!groupVS.getGroupRepresentative().nif.equals(messageSMIMEReq.userVS.nif) && !userVSService.isUserAdmin()) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                 TypeVS.VICKET_GROUP_EDIT.toString(), groupVS.name].toArray(), locale)
            log.error "editGroup - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:"editGroup_userWithoutPrivilegesErrorMsg",
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvsName || !messageJSON.groupvsInfo ||!messageJSON.id ||
                (TypeVS.VICKET_GROUP_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "editGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:"editGroup_paramsErrorMsg",
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        if(Long.valueOf(messageJSON.id) != groupVS.id) {
            msg = messageSource.getMessage('identifierErrorMsg', [groupVS.id, messageJSON.id].toArray(), locale)
            log.error "editGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:"editGroup_identifierErrorMsg",
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        groupVS.setDescription(messageJSON.groupvsInfo)
        groupVS.save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_EDIT, data:groupVS,
                reason: "editGroup_${groupVS.id}")
    }

    public ResponseVS saveGroup(MessageSMIME messageSMIMEReq, Locale locale) {
        GroupVS groupVS = null
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("saveGroup - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvsName || !messageJSON.groupvsInfo ||
                (TypeVS.VICKET_GROUP_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:"saveGroup_paramsErrorMsg",
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }

        groupVS = GroupVS.findWhere(name:messageJSON.groupvsName.trim())
        if(groupVS) {
            msg = messageSource.getMessage('nameGroupRepeatedMsg', [messageJSON.groupvsName].toArray(), locale)
            log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:"saveGroup_nameGroupRepeatedMsg",
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
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_NEW, data:groupVS,
                reason: "saveGroup_${groupVS.id}")
    }

    public ResponseVS subscribe(MessageSMIME messageSMIMEReq, Locale locale) {
        SubscriptionVS subscriptionVS = null
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("subscribe - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvs || (TypeVS.VICKET_GROUP_SUBSCRIBE != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "subscribe - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:"subscribe_paramsErrorMsg",
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        GroupVS groupVS = GroupVS.get(messageJSON.groupvs.id)

        if(groupVS.getGroupRepresentative().nif.equals(userSigner.nif)) {
            msg = messageSource.getMessage('representativeSubscribedErrorMsg',
                    [groupVS.groupRepresentative.nif, groupVS.name].toArray(), locale)
            log.error "subscribe - ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST,
                    reason:"subscribe_representativeSubscribedErrorMsg")
        }

        subscriptionVS = SubscriptionVS.findWhere(groupVS:groupVS, userVS:userSigner)
        if(subscriptionVS) {
            msg = messageSource.getMessage('userAlreadySubscribedErrorMsg', [userSigner.nif, groupVS.name].toArray(), locale)
            log.error "subscribe - ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:"subscribe_userAlreadySubscribedErrorMsg",
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        subscriptionVS = new SubscriptionVS(userVS:userSigner, groupVS:groupVS, state:SubscriptionVS.State.PENDING,
                subscriptionSMIME: messageSMIMEReq).save()
        log.debug("subscribe - OK subsscription: ${subscriptionVS.id} to groupVS: ${groupVS.id}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK)
    }

    public ResponseVS updateSubscription(MessageSMIME messageSMIMEReq, Locale locale) {
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("updateSubscription - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if ((TypeVS.VICKET_GROUP_UPDATE_SUBSCRIPTION != TypeVS.valueOf(messageJSON.operation))
            || !messageJSON.groupId || messageJSON.groupName || !messageJSON.subscriptionId || messageJSON.subscriberNIF) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "updateSubscription - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:"updateSubscription_paramsErrorMsg",
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        GroupVS groupVS = GroupVS.get(messageJSON.groupId)
        if(!groupVS.representative?.nif.equals(userSigner.getNif())) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                TypeVS.VICKET_GROUP_UPDATE_SUBSCRIPTION.toString(), groupVS.name].toArray(), locale)
            log.error "updateSubscription - ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    reason:"updateSubscription_userWithoutPrivilegesErrorMsg", statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        SubscriptionVS subscriptionVS = SubscriptionVS.get(messageJSON.subscriptionId)
        subscriptionVS.setState(SubscriptionVS.State.valueOf(messageJSON.subscriptionState))
        messageSMIMEReq.setSubscriptionVS(subscriptionVS)
        subscriptionVS.save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK)
    }


 	public Map getGroupVSDataMap(GroupVS groupVS){
        Map resultMap = [id:groupVS.id, name:groupVS.name, description:groupVS.description, state:groupVS.state.toString(),
            dateCreated:groupVS.dateCreated, representative:userVSService.getUserVSDataMap(groupVS.groupRepresentative)]
        SubscriptionVS.withTransaction {
            def result = SubscriptionVS.createCriteria().list(offset: 0) {
                eq("groupVS", groupVS)
                eq("state", SubscriptionVS.State.ACTIVE)
            }
            resultMap.numActiveUsers = result.totalCount
            result = SubscriptionVS.createCriteria().list(offset: 0) {
                eq("groupVS", groupVS)
                eq("state", SubscriptionVS.State.PENDING)
            }
            resultMap.numPendingUsers = result.totalCount
        }
        return resultMap
	}

}

