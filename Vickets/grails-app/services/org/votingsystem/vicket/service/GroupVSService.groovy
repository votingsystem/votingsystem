package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.springframework.context.i18n.LocaleContextHolder
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.util.IbanVSUtil

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class GroupVSService {

    private static final CLASS_NAME = CsrService.class.getSimpleName()
    private class GroupVSRequest {
        String groupvsName, groupvsInfo;
        TypeVS operation;
        Set<TagVS> tagSet = new HashSet<TagVS>();
        public GroupVSRequest(String signedContent) throws ExceptionVS {
            def messageJSON = JSON.parse(signedContent)
            groupvsName = messageJSON.groupvsName;
            groupvsInfo = messageJSON.groupvsInfo
            if(!groupvsName) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvsName'");
            if(!groupvsInfo) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvsInfo'")
            if(TypeVS.VICKET_GROUP_NEW != TypeVS.valueOf(messageJSON.operation)) throw ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'VICKET_GROUP_NEW' - operation found: " + messageJSON.operation)
            messageJSON.tags?.each {tag ->
                TagVS tagVS = TagVS.findWhere(name:tag.name)
                if(tagVS) tagSet.add(tagVS)
                else throw new ValidationExceptionVS(this.getClass(), "Tag '${tag}' not found");
            }
        }
    }

    def userVSService
    def messageSource
    def grailsApplication
    def signatureVSService
    def subscriptionVSService
    def transactionVSService
    def systemService
    def userVSAccountService


	public void init() { }

    public ResponseVS cancelGroup(GroupVS groupVS, MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("cancelGroup '${groupVS.id}' - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        if(!groupVS.getRepresentative().nif.equals(userSigner.nif) && !systemService.isUserAdmin(userSigner.nif)) {
            msg = messageSource.getMessage('userWithoutGroupPrivilegesErrorMsg', [userSigner.getNif(),
                             TypeVS.VICKET_GROUP_CANCEL.toString(), groupVS.name].toArray(), LocaleContextHolder.locale)
            log.error "cancelGroup - ${msg}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, type:TypeVS.ERROR, message:msg,
                    metaInf: MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivilege"))
        }
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvsName || !messageJSON.id ||
                (TypeVS.VICKET_GROUP_CANCEL != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, LocaleContextHolder.locale)
            log.error "${methodName} - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST , type:TypeVS.ERROR,
                    message:msg, metaInf: MetaInfMsg.getErrorMsg(methodName, "params"))
        }
        groupVS.state = UserVS.State.CANCELLED
        groupVS.save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_CANCEL, message:msg,
                metaInf:MetaInfMsg.getOKMsg(methodName, "groupVS_${groupVS.id}"))
    }


    public ResponseVS editGroup(GroupVS groupVS, MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        String msg = null
        ResponseVS responseVS = null
        if(!groupVS.getRepresentative().nif.equals(messageSMIMEReq.userVS.nif) &&
                !systemService.isUserAdmin(messageSMIMEReq.userVS.nif)) {
            msg = messageSource.getMessage('userWithoutGroupPrivilegesErrorMsg', [userSigner.getNif(),
                 TypeVS.VICKET_GROUP_EDIT.toString(), groupVS.name].toArray(), LocaleContextHolder.locale)
            log.error "editGroup - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }
        def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage()?.getSignedContent())
        if (!messageJSON.groupvsName || !messageJSON.groupvsInfo ||!messageJSON.id ||
                (TypeVS.VICKET_GROUP_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, LocaleContextHolder.locale)
            log.error "editGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, metaInf:MetaInfMsg.getErrorMsg(methodName, "params"),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        if(Long.valueOf(messageJSON.id) != groupVS.id) {
            msg = messageSource.getMessage('identifierErrorMsg', [groupVS.id, messageJSON.id].toArray(), LocaleContextHolder.locale)
            log.error "editGroup - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "groupVS_${groupVS?.id}"),
                    statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        groupVS.setDescription(messageJSON.groupvsInfo)
        groupVS.save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_EDIT, data:groupVS,
                metaInf:MetaInfMsg.getOKMsg(methodName, "groupVS_${groupVS.id}"))
    }

    @Transactional
    public ResponseVS saveGroup(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("saveGroup - signer: ${userSigner?.nif}")
        GroupVSRequest request = new GroupVSRequest(messageSMIMEReq.getSmimeMessage()?.getSignedContent())
        GroupVS groupVS = GroupVS.findWhere(name:request.groupvsName.trim())
        if(groupVS) {
            throw new ExceptionVS(messageSource.getMessage('nameGroupRepeatedMsg', [request.groupvsName].toArray(),
                    LocaleContextHolder.locale), MetaInfMsg.getErrorMsg(methodName, "nameGroupRepeatedMsg"))
        }
        subscriptionVSService.checkUserVSAccount(userSigner)
        groupVS = new GroupVS(name:request.groupvsName.trim(), state:UserVS.State.ACTIVE, representative:userSigner,
                description:request.groupvsInfo,tagVSSet:request.tagSet).save()
        groupVS.setIBAN(IbanVSUtil.getInstance().getIBAN(groupVS.id))
        new UserVSAccount(currencyCode: Currency.getInstance('EUR').getCurrencyCode(), userVS:groupVS,
                balance:BigDecimal.ZERO, IBAN:groupVS.getIBAN(), tag:systemService.getWildTag()).save()
        String metaInf =  MetaInfMsg.getOKMsg(methodName, "groupVS_${groupVS.id}")
        String fromUser = grailsApplication.config.VotingSystem.serverName
        String toUser = userSigner.getNif()
        String subject = messageSource.getMessage('newGroupVSReceiptSubject', null, LocaleContextHolder.locale)
        SMIMEMessage smimeMessageResp = signatureVSService.getSMIMEMessage(fromUser, toUser,
                messageSMIMEReq.getSmimeMessage()?.getSignedContent(), subject, null)
        log.debug("${metaInf}")
        messageSMIMEReq.setContent(smimeMessageResp.getBytes()).setType(TypeVS.RECEIPT)
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_NEW, data:groupVS)
    }

    @Transactional
    public ResponseVS subscribe(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SubscriptionVS subscriptionVS = null
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("subscribe - signer: ${userSigner?.nif}")
        String msg = null
        ResponseVS responseVS = null
        String documentStr = messageSMIMEReq.getSmimeMessage()?.getSignedContent()
        def messageJSON = JSON.parse(documentStr)
        if (!messageJSON.groupvs || (TypeVS.VICKET_GROUP_SUBSCRIBE != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, LocaleContextHolder.locale)
            log.error "subscribe - DATA ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "params"), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        GroupVS groupVS = GroupVS.get(messageJSON.groupvs.id)

        if(groupVS.getRepresentative().nif.equals(userSigner.nif)) {
            msg = messageSource.getMessage('representativeSubscribedErrorMsg',
                    [groupVS.representative.nif, groupVS.name].toArray(), LocaleContextHolder.locale)
            log.error "subscribe - ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg,statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "representativeSubscribed"))
        }

        subscriptionVS = SubscriptionVS.findWhere(groupVS:groupVS, userVS:userSigner)
        if(subscriptionVS) {
            msg = messageSource.getMessage('userAlreadySubscribedErrorMsg', [userSigner.nif, groupVS.name].toArray(), LocaleContextHolder.locale)
            log.error "subscribe - ERROR - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userAlreadySubscribed"))
        }
        subscriptionVS = new SubscriptionVS(userVS:userSigner, groupVS:groupVS, state:SubscriptionVS.State.PENDING,
                subscriptionSMIME: messageSMIMEReq).save()
        msg = messageSource.getMessage('groupvsSubscriptionOKMsg', [userSigner.nif, groupVS.name].toArray(), LocaleContextHolder.locale)
        log.debug("subscribe - OK subsscription: ${subscriptionVS.id} to groupVS: ${groupVS.id}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type: TypeVS.VICKET_GROUP_SUBSCRIBE, message: msg,
                metaInf:MetaInfMsg.getOKMsg(methodName, "subscriptionVS_${subscriptionVS.id}"))
    }

    @Transactional
 	public Map getGroupVSDataMap(GroupVS groupVS){
        Map resultMap = [id:groupVS.id, IBAN:groupVS.IBAN, name:groupVS.name, description:groupVS.description,
            state:groupVS.state.toString(), dateCreated:groupVS.dateCreated,
            representative:userVSService.getUserVSDataMap(groupVS.representative, false), type:groupVS.type.toString()]
        if(groupVS.tagVSSet) {
            List tagList = []
            groupVS.tagVSSet.each {tag ->
                tagList.add([id:tag.id, name:tag.name])
            }
            resultMap.tags = tagList
        }
        resultMap.numActiveUsers = SubscriptionVS.countByGroupVSAndState(groupVS, SubscriptionVS.State.ACTIVE)
        resultMap.numPendingUsers = SubscriptionVS.countByGroupVSAndState(groupVS, SubscriptionVS.State.PENDING)
        return resultMap
	}

    @Transactional
    public Map getDataMap(GroupVS groupVS, DateUtils.TimePeriod timePeriod){
        Map resultMap = [timePeriod:[dateFrom:timePeriod.getDateFrom(), dateTo:timePeriod.getDateTo()]]
        resultMap.userVS = getGroupVSDataMap(groupVS)
        return resultMap
    }

    @Transactional
    public Map getDataWithBalancesMap(GroupVS groupVS, DateUtils.TimePeriod timePeriod){
        Map resultMap = [timePeriod:[dateFrom:timePeriod.getDateFrom(), dateTo:timePeriod.getDateTo()]]
        resultMap.userVS = getGroupVSDataMap(groupVS)

        Map transactionsFromWithBalancesMap = transactionVSService.getTransactionFromListWithBalances(groupVS, timePeriod)
        resultMap.transactionFromList = transactionsFromWithBalancesMap.transactionFromList
        resultMap.balancesFrom = transactionsFromWithBalancesMap.balancesFrom

        Map transactionsToWithBalancesMap = transactionVSService.getTransactionToListWithBalances(groupVS, timePeriod)
        resultMap.transactionToList = transactionsToWithBalancesMap.transactionToList
        resultMap.balancesTo = transactionsToWithBalancesMap.balancesTo

        resultMap.balancesCash = transactionVSService.balancesCash(resultMap.balancesTo, resultMap.balancesFrom)
        userVSAccountService.checkBalancesMap(groupVS, resultMap.balancesCash)
        return resultMap
    }

}

