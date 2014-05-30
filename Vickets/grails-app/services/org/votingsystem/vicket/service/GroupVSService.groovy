package org.votingsystem.vicket.service

import grails.converters.JSON
import org.votingsystem.model.*
import org.votingsystem.model.vicket.MetaInfMsg

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
    def subscriptionVSService

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
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.VICKET_GROUP_ERROR, message:msg,
                    metaInf: MetaInfMsg.cancelVicketGroup_ERROR_userWithoutPrivilege)
        }
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvsName || !messageJSON.id ||
                (TypeVS.VICKET_GROUP_CANCEL != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "cancelGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.VICKET_GROUP_ERROR,
                    message:msg, metaInf: MetaInfMsg.cancelVicketGroup_ERROR_params)
        }
        groupVS.state = UserVS.State.CLOSED
        groupVS.save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_CANCEL, message:msg,
                metaInf:MetaInfMsg.cancelVicketGroup_OK + groupVS.id)
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
                    metaInf:MetaInfMsg.editVicketGroup_ERROR_userWithoutPrivileges)
        }
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvsName || !messageJSON.groupvsInfo ||!messageJSON.id ||
                (TypeVS.VICKET_GROUP_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "editGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, metaInf:MetaInfMsg.editVicketGroup_ERROR_params,
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        if(Long.valueOf(messageJSON.id) != groupVS.id) {
            msg = messageSource.getMessage('identifierErrorMsg', [groupVS.id, messageJSON.id].toArray(), locale)
            log.error "editGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, metaInf:MetaInfMsg.editVicketGroup_ERROR_id,
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        groupVS.setDescription(messageJSON.groupvsInfo)
        groupVS.save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_EDIT, data:groupVS,
                metaInf:MetaInfMsg.editVicketGroup_OK + groupVS.id)
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
                    metaInf:MetaInfMsg.saveVicketGroup_ERROR_params, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }

        groupVS = GroupVS.findWhere(name:messageJSON.groupvsName.trim())
        if(groupVS) {
            msg = messageSource.getMessage('nameGroupRepeatedMsg', [messageJSON.groupvsName].toArray(), locale)
            log.error "saveGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.saveVicketGroup_ERROR_nameGroupRepeatedMsg)
        }

        groupVS = new GroupVS(name:messageJSON.groupvsName.trim(), state:UserVS.State.ACTIVE, groupRepresentative:userSigner,
                description:messageJSON.groupvsInfo).save()
        String metaInf =  MetaInfMsg.saveVicketGroup_OK + groupVS.id

        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = userSigner.getNif()
        String subject = messageSource.getMessage('newGroupVSReceiptSubject', null, locale)
        byte[] smimeMessageRespBytes = signatureVSService.getSignedMimeMessage(fromUser, toUser, documentStr, subject, null)

        MessageSMIME.withTransaction { new MessageSMIME(type:TypeVS.RECEIPT, metaInf:metaInf,
                smimeParent:messageSMIMEReq, content:smimeMessageRespBytes).save() }
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_NEW, data:groupVS)
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
                    metaInf:MetaInfMsg.subscribeToVicketGroup_ERROR_params, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        GroupVS groupVS = GroupVS.get(messageJSON.groupvs.id)

        if(groupVS.getGroupRepresentative().nif.equals(userSigner.nif)) {
            msg = messageSource.getMessage('representativeSubscribedErrorMsg',
                    [groupVS.groupRepresentative.nif, groupVS.name].toArray(), locale)
            log.error "subscribe - ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.subscribeToVicketGroup_ERROR_representativeSubscribed)
        }

        subscriptionVS = SubscriptionVS.findWhere(groupVS:groupVS, userVS:userSigner)
        if(subscriptionVS) {
            msg = messageSource.getMessage('userAlreadySubscribedErrorMsg', [userSigner.nif, groupVS.name].toArray(), locale)
            log.error "subscribe - ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.VICKET_GROUP_ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.subscribeToVicketGroup_ERROR_userAlreadySubscribed)
        }
        subscriptionVS = new SubscriptionVS(userVS:userSigner, groupVS:groupVS, state:SubscriptionVS.State.PENDING,
                subscriptionSMIME: messageSMIMEReq).save()
        msg = messageSource.getMessage('groupvsSubscriptionOKMsg', [userSigner.nif, groupVS.name].toArray(), locale)
        log.debug("subscribe - OK subsscription: ${subscriptionVS.id} to groupVS: ${groupVS.id}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type: TypeVS.VICKET_GROUP_SUBSCRIBE, message: msg,
                metaInf:MetaInfMsg.subscribeToVicketGroup_OK + subscriptionVS.id)
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

