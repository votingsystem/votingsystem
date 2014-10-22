package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.votingsystem.groovy.util.TransactionVSUtils
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.util.IbanVSUtil

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class GroupVSService {

    def userVSService
    def messageSource
    def grailsApplication
    def signatureVSService
    def subscriptionVSService
    def transactionVSService
    def systemService
    def userVSAccountService
    def grailsLinkGenerator

    public ResponseVS cancelGroup(GroupVS groupVS, MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("$methodName '${groupVS.id}' - signer: ${userSigner?.nif}")
        if(!groupVS.getRepresentative().nif.equals(userSigner.nif) && !systemService.isUserAdmin(userSigner.nif)) {
            throw new ExceptionVS(messageSource.getMessage('userWithoutGroupPrivilegesErrorMsg', [userSigner.getNif(),
                    TypeVS.VICKET_GROUP_CANCEL.toString(), groupVS.name].toArray(), locale),
                    MetaInfMsg.getErrorMsg(methodName, "nameGroupRepeatedMsg"))
        }
        GroupVSRequest request = GroupVSRequest.getCancelRequest(messageSMIMEReq.getSMIME()?.getSignedContent())
        groupVS.setState(UserVS.State.CANCELLED)
        groupVS.save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_CANCEL,
                metaInf:MetaInfMsg.getOKMsg(methodName, "groupVS_${groupVS.id}"))
    }

    public ResponseVS editGroup(GroupVS groupVS, MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        if(!groupVS.getRepresentative().nif.equals(messageSMIMEReq.userVS.nif) &&
                !systemService.isUserAdmin(messageSMIMEReq.userVS.nif)) {
            throw new ExceptionVS(messageSource.getMessage('userWithoutGroupPrivilegesErrorMsg', [userSigner.getNif(),
                    TypeVS.VICKET_GROUP_EDIT.toString(), groupVS.name].toArray(), locale),
                    MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }
        GroupVSRequest request = GroupVSRequest.getEditRequest(messageSMIMEReq.getSMIME()?.getSignedContent())
        if(request.id != groupVS.id) {
            throw new ExceptionVS(messageSource.getMessage('identifierErrorMsg', [groupVS.id, request.id].toArray(),
                locale), MetaInfMsg.getErrorMsg(methodName, "groupVS_${groupVS?.id}"))
        }
        groupVS.setDescription(request.groupvsInfo)
        groupVS.save()
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_EDIT, data:groupVS,
                metaInf:MetaInfMsg.getOKMsg(methodName, "groupVS_${groupVS.id}"))
    }

    @Transactional
    public ResponseVS saveGroup(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("saveGroup - signer: ${userSigner?.nif}")
        GroupVSRequest request = new GroupVSRequest(messageSMIMEReq.getSMIME()?.getSignedContent())
        GroupVS groupVS = GroupVS.findWhere(name:request.groupvsName.trim())
        if(groupVS) {
            throw new ExceptionVS(messageSource.getMessage('nameGroupRepeatedMsg', [request.groupvsName].toArray(),
                    locale), MetaInfMsg.getErrorMsg(methodName, "nameGroupRepeatedMsg"))
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
        String subject = messageSource.getMessage('newGroupVSReceiptSubject', null, locale)
        SMIMEMessage receipt = signatureVSService.getSMIMEMultiSigned(fromUser, toUser,
                messageSMIMEReq.getSMIME(), subject)
        messageSMIMEReq.setSMIME(receipt)

        log.debug("${metaInf}")
        Map resultMap = [statusCode:ResponseVS.SC_OK,
                 message:messageSource.getMessage('newVicketGroupOKMsg', [groupVS.name].toArray(), locale),
                 URL:"${grailsLinkGenerator.link(controller:"groupVS", absolute:true)}/${groupVS.id}"]
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.VICKET_GROUP_NEW, data:resultMap,
                contentType:ContentTypeVS.JSON)
    }

    @Transactional
    public ResponseVS subscribe(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SubscriptionVS subscriptionVS = null
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("$methodName - signer: ${userSigner?.nif}")
        GroupVSRequest request = GroupVSRequest.getSubscribeRequest(messageSMIMEReq.getSMIME()?.getSignedContent())
        GroupVS groupVS = GroupVS.get(request.id)
        if(groupVS.getRepresentative().nif.equals(userSigner.nif)) {
            throw new ExceptionVS(messageSource.getMessage('representativeSubscribedErrorMsg',
                    [groupVS.representative.nif, groupVS.name].toArray(), locale),
                    MetaInfMsg.getErrorMsg(methodName, "representativeSubscribed"))
        }

        subscriptionVS = SubscriptionVS.findWhere(groupVS:groupVS, userVS:userSigner)
        if(subscriptionVS) {
            throw new ExceptionVS(messageSource.getMessage('userAlreadySubscribedErrorMsg',
                    [userSigner.nif, groupVS.name].toArray(), locale),
                    MetaInfMsg.getErrorMsg(methodName, "userAlreadySubscribed"))
        }
        subscriptionVS = new SubscriptionVS(userVS:userSigner, groupVS:groupVS, state:SubscriptionVS.State.PENDING,
                subscriptionSMIME: messageSMIMEReq).save()
        log.debug("$methodName - subscription OK id '${subscriptionVS.id}' to groupVS '${groupVS.id}'")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type: TypeVS.VICKET_GROUP_SUBSCRIBE,
                message: messageSource.getMessage('groupvsSubscriptionOKMsg', [userSigner.nif, groupVS.name].toArray(), locale),
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

        Map transactionsFromWithBalancesMap = transactionVSService.getTransactionFromListWithBalances(((UserVS)groupVS), timePeriod)
        resultMap.transactionFromList = transactionsFromWithBalancesMap.transactionFromList
        resultMap.balancesFrom = transactionsFromWithBalancesMap.balancesFrom

        Map transactionsToWithBalancesMap = transactionVSService.getTransactionToListWithBalances(groupVS, timePeriod)
        resultMap.transactionToList = transactionsToWithBalancesMap.transactionToList
        resultMap.balancesTo = transactionsToWithBalancesMap.balancesTo

        resultMap.balancesCash = TransactionVSUtils.balancesCash(resultMap.balancesTo, resultMap.balancesFrom)
        userVSAccountService.checkBalancesMap(groupVS, resultMap.balancesCash)
        return resultMap
    }

    private static class GroupVSRequest {
        String groupvsName, groupvsInfo;
        TypeVS operation;
        Long id;
        Set<TagVS> tagSet = new HashSet<TagVS>();
        public GroupVSRequest() {}
        public GroupVSRequest(String signedContent) throws ExceptionVS {
            def messageJSON = JSON.parse(signedContent)
            groupvsName = messageJSON.groupvsName;
            groupvsInfo = messageJSON.groupvsInfo
            if(!groupvsName) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvsName'");
            if(!groupvsInfo) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvsInfo'")
            if(TypeVS.VICKET_GROUP_NEW != TypeVS.valueOf(messageJSON.operation)) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'VICKET_GROUP_NEW' - operation found: " + messageJSON.operation)
            messageJSON.tags?.each {tag ->
                TagVS tagVS = TagVS.findWhere(name:tag.name)
                if(tagVS) tagSet.add(tagVS)
                else throw new ValidationExceptionVS(this.getClass(), "Tag '${tag}' not found");
            }
        }

        public static GroupVSRequest getCancelRequest(String signedContent) {
            GroupVSRequest result = new GroupVSRequest()
            def messageJSON = JSON.parse(signedContent)
            if(TypeVS.VICKET_GROUP_CANCEL != TypeVS.valueOf(messageJSON.operation)) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'VICKET_GROUP_CANCEL' - operation found: " + messageJSON.operation)
            result.groupvsName = messageJSON.groupvsName;
            if(!result.groupvsName) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvsName'");
            result.id = Long.valueOf(messageJSON.id)
            return result
        }

        public static GroupVSRequest getEditRequest(String signedContent) {
            GroupVSRequest result = new GroupVSRequest()
            def messageJSON = JSON.parse(signedContent)
            if(TypeVS.VICKET_GROUP_NEW != TypeVS.valueOf(messageJSON.operation)) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'VICKET_GROUP_NEW' - operation found: " + messageJSON.operation)
            result.groupvsName = messageJSON.groupvsName;
            if(!result.groupvsName) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvsName'");
            result.id = Long.valueOf(messageJSON.id)
            result.groupvsName = messageJSON.groupvsName;
            result.groupvsInfo = messageJSON.groupvsInfo
            if(!result.groupvsName) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvsName'");
            if(!result.groupvsInfo) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvsInfo'")
            return result
        }

        public static GroupVSRequest getSubscribeRequest(String signedContent) {
            GroupVSRequest result = new GroupVSRequest()
            def messageJSON = JSON.parse(signedContent)
            if(TypeVS.VICKET_GROUP_SUBSCRIBE != TypeVS.valueOf(messageJSON.operation)) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'VICKET_GROUP_SUBSCRIBE' - operation found: " + messageJSON.operation)
            if(!messageJSON.groupvs?.id) throw new ValidationExceptionVS(this.getClass(), "missing param 'groupvs.id'");
            result.id = Long.valueOf(messageJSON.groupvs.id)
            return result
        }

    }

}

