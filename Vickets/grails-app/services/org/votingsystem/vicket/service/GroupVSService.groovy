package org.votingsystem.vicket.service

import grails.converters.JSON
import org.votingsystem.model.GroupVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.SubscriptionVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.vicket.Reason

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
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:Reason.cancelVicketGroup_ERROR_userWithoutPrivilege,
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvsName || !messageJSON.id ||
                (TypeVS.VICKET_GROUP_CANCEL != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "cancelGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:Reason.cancelVicketGroup_ERROR_params,
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        groupVS.state = GroupVS.State.CLOSED
        groupVS.save()
        return new ResponseVS(type:TypeVS.VICKET_GROUP_CANCEL, message:msg, reason:Reason.cancelVicketGroup_OK + groupVS.id,
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
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    reason:Reason.editVicketGroup_ERROR_userWithoutPrivileges)
        }
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvsName || !messageJSON.groupvsInfo ||!messageJSON.id ||
                (TypeVS.VICKET_GROUP_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "editGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:Reason.editVicketGroup_ERROR_params,
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        if(Long.valueOf(messageJSON.id) != groupVS.id) {
            msg = messageSource.getMessage('identifierErrorMsg', [groupVS.id, messageJSON.id].toArray(), locale)
            log.error "editGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, reason:Reason.editVicketGroup_ERROR_id,
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        groupVS.setDescription(messageJSON.groupvsInfo)
        groupVS.save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_EDIT, data:groupVS,
                reason:Reason.editVicketGroup_OK + groupVS.id)
    }

    public ResponseVS deActivateUser(MessageSMIME messageSMIMEReq, Locale locale) {
        GroupVS groupVS = null
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("saveGroup - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        log.debug("activateUser - documentStr: ${documentStr}")
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvsName || !messageJSON.groupvsInfo ||
                (TypeVS.VICKET_GROUP_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    reason:Reason.deActivateVicketGroupUser_ERROR_params, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }

        groupVS = GroupVS.findWhere(name:messageJSON.groupvsName.trim())
        return responseVS
    }

    public ResponseVS activateUser(MessageSMIME messageSMIMEReq, Locale locale) {
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("saveGroup - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvs.name || !messageJSON.groupvs.id ||
            !messageJSON.uservs.name || !messageJSON.uservs.NIF ||
                (TypeVS.VICKET_GROUP_USER_ACTIVATE != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    reason:Reason.activateVicketGroupUser_ERROR_params, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        GroupVS groupVS = GroupVS.get(Long.valueOf(messageJSON.groupvs.id))
        if(!groupVS || !messageJSON.groupvs.name.equals(groupVS.name)) {
            msg = messageSource.getMessage('itemNotFoundMsg', [messageJSON.groupvs.id].toArray(), locale)
            log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    reason:Reason.activateVicketGroupUser_ERROR_groupNotFound, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        if(!groupVS.getGroupRepresentative().nif.equals(messageSMIMEReq.userVS.nif) && !userVSService.isUserAdmin(
                messageSMIMEReq.userVS.nif)) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                 TypeVS.VICKET_GROUP_USER_ACTIVATE.toString(), groupVS.name].toArray(), locale)
            log.error "activateUser - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    reason:Reason.activateVicketGroupUser_ERROR_userWithoutPrivilege, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        UserVS userToActivate = UserVS.findWhere(nif:messageJSON.uservs.NIF)
        SubscriptionVS subscription = SubscriptionVS.findWhere(groupVS: groupVS, userVS:userToActivate)
        if(!userToActivate || SubscriptionVS.State.PENDING != subscription.state) {
            msg = messageSource.getMessage('groupUserNotPendingErrorMsg',
                    [groupVS.name, userSigner.getNif()].toArray(), locale)
            log.error "activateUser - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    reason:Reason.activateVicketGroupUser_ERROR_groupUserNotPending)
        }
        messageSMIMEReq.setSubscriptionVS(subscription)
        log.debug("activateUser OK - userToActivate: ${userToActivate.nif} - group: ${groupVS.name}")
        return new ResponseVS(type:TypeVS.VICKET_GROUP_USER_ACTIVATE, message:msg, statusCode:ResponseVS.SC_OK,
                reason:Reason.activateVicketGroupUser_OK + subscription.id, data:subscription)
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
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    reason:Reason.saveVicketGroup_ERROR_params, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }

        groupVS = GroupVS.findWhere(name:messageJSON.groupvsName.trim())
        if(groupVS) {
            msg = messageSource.getMessage('nameGroupRepeatedMsg', [messageJSON.groupvsName].toArray(), locale)
            log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST,
                    reason:Reason.saveVicketGroup_ERROR_nameGroupRepeatedMsg)
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
                reason: Reason.saveVicketGroup_OK + groupVS.id)
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
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    reason:Reason.subscribeToVicketGroup_ERROR_params, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        GroupVS groupVS = GroupVS.get(messageJSON.groupvs.id)

        if(groupVS.getGroupRepresentative().nif.equals(userSigner.nif)) {
            msg = messageSource.getMessage('representativeSubscribedErrorMsg',
                    [groupVS.groupRepresentative.nif, groupVS.name].toArray(), locale)
            log.error "subscribe - ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST,
                    reason:Reason.subscribeToVicketGroup_ERROR_representativeSubscribed)
        }

        subscriptionVS = SubscriptionVS.findWhere(groupVS:groupVS, userVS:userSigner)
        if(subscriptionVS) {
            msg = messageSource.getMessage('userAlreadySubscribedErrorMsg', [userSigner.nif, groupVS.name].toArray(), locale)
            log.error "subscribe - ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    reason:Reason.subscribeToVicketGroup_ERROR_userAlreadySubscribed)
        }
        subscriptionVS = new SubscriptionVS(userVS:userSigner, groupVS:groupVS, state:SubscriptionVS.State.PENDING,
                subscriptionSMIME: messageSMIMEReq).save()
        log.debug("subscribe - OK subsscription: ${subscriptionVS.id} to groupVS: ${groupVS.id}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, reason:Reason.subscribeToVicketGroup_OK + subscriptionVS.id)
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

