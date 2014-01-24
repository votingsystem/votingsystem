package org.votingsystem.ticket.service

import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ticket.TransactionVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils

import java.math.RoundingMode

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class TransactionVSService {

    def messageSource
    def userVSService
    def sessionFactory
    def grailsLinkGenerator

    public ResponseVS processDeposit(MessageSMIME messageSMIMEReq, Locale locale) {
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS signer = messageSMIMEReq.userVS
        String msg;
        try {
            def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage().getSignedContent())
            TypeVS transactionType = TypeVS.valueOf(messageJSON.typeVS)
            switch(transactionType) {
                case TypeVS.TICKET_USER_ALLOCATION:
                    processUserAllocation(messageSMIMEReq, messageJSON, locale)
                    break;
                default:
                    log.debug("Unprocessed transactionType: " + transactionType);
            }

        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            msg = messageSource.getMessage('depositDataError', null, locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.TICKET_DEPOSIT_ERROR)
        }
    }


    private ResponseVS processUserAllocation(MessageSMIME messageSMIMEReq, JSONObject messageJSON, Locale locale) {

        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS signer = messageSMIMEReq.userVS
        String msg;
        try {
            if(!userVSService.isUserAdmin(signer.getNif())) {
                msg = messageSource.getMessage('userAllocationAdminErrorMsg', [signer.getNif()].toArray(), locale)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.TICKET_DEPOSIT_ERROR)
            }
            int numUsers = UserVS.countByType(UserVS.Type.USER)
            BigDecimal numUsersBigDecimal = new BigDecimal(numUsers)
            BigDecimal amount = new BigDecimal(messageJSON.amount)
            BigDecimal userPart = amount.divide(numUsersBigDecimal, 2, RoundingMode.FLOOR)
            BigDecimal totalUsers = userPart.multiply(numUsersBigDecimal)
            log.debug("processUserAllocation - amount: ${amount} -  numUsers: ${numUsers} - userPart: ${userPart} - totalUsers: ${totalUsers}")
            TransactionVS transactionParent = new TransactionVS(amount: amount, messageSMIME:messageSMIMEReq,
                    fromUserVS:signer, type:TransactionVS.Type.USER_ALLOCATION).save()

            def criteria = UserVS.createCriteria()
            def usersToDeposit = criteria.scroll { eq("type", UserVS.Type.USER) }
            while (usersToDeposit.next()) {
                UserVS userVS = (UserVS) usersToDeposit.get(0);
                TransactionVS userTransaction = new TransactionVS(transactionParent:transactionParent, amount:userPart,
                        fromUserVS: signer, toUserVS:userVS, type:TransactionVS.Type.USER_INPUT).save()
                if((usersToDeposit.getRowNumber() % 100) == 0) {
                    sessionFactory.currentSession.flush()
                    sessionFactory.currentSession.clear()
                    log.debug("processed ${usersToDeposit.getRowNumber()}/${numUsers} user allocation for transaction ${transactionParent.id}");
                }
            }
            return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Transaction OK", type:TypeVS.TICKET_USER_ALLOCATION)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            msg = messageSource.getMessage('depositDataError', null, locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.TICKET_DEPOSIT_ERROR)
        }
    }

    //_ TODO _
    public ResponseVS getUserBalance(UserVS uservs) {
        def userInputTransactions = TransactionVS.findAllWhere(toUserVS: uservs, type:TransactionVS.Type.USER_INPUT)
        BigDecimal totalInputs = new BigDecimal(0)
        userInputTransactions.each {
            totalInputs = totalInputs.add(it.amount)
        }
        def userOutputTransactions = TransactionVS.findAllWhere(toUserVS:uservs,
                type:TransactionVS.Type.USER_OUTPUT)
        BigDecimal totalOutputs = new BigDecimal(0)
        userOutputTransactions.each {
            totalOutputs = totalOutputs.add(it.amount)
        }
        BigDecimal result = totalInputs.add(totalOutputs.negate())
        log.debug("getUserBalance - totalInputs: ${totalInputs} - totalOutputs: ${totalOutputs} - Balance: ${result}")
        return new ResponseVS(statusCode: ResponseVS.SC_OK, data:result)
    }

    public Map getTransactionMap(TransactionVS transaction) {
        Map transactionMap = [:]
        String fromUserVSName = "${transaction.fromUserVS.firstName} ${transaction.fromUserVS.lastName} "
        transactionMap.fromUserVS = [nif:transaction.fromUserVS.nif, name:fromUserVSName]
        transactionMap.dateCreated = DateUtils.getStringFromDate(transaction.dateCreated)
        if(transaction.validTo) transactionMap.validTo = DateUtils.getStringFromDate(transaction.validTo)
        transactionMap.id = transaction.id
        transactionMap.subject = transaction.subject
        transactionMap.type = transaction.getType().toString()
        transactionMap.amount = transaction.amount
        String messageSMIMEURL = null
        if(transaction.transactionParent) {
            messageSMIMEURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${transaction.transactionParent.getMessageSMIME()?.id}"
        } else messageSMIMEURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${transaction.getMessageSMIME()?.id}"
        transactionMap.messageSMIMEURL = messageSMIMEURL
        return transactionMap
    }

    public Map getUserInfoMap(UserVS userVS) {
        def criteria = TransactionVS.createCriteria()
        def userTransactions = criteria.scroll {
            eq("toUserVS", userVS)
            or {
                eq("type", TransactionVS.Type.USER_INPUT)
                eq("type", TransactionVS.Type.USER_OUTPUT)
            }
        }

        Map resultMap = [:]
        List transactionList = []
        BigDecimal totalInputs = new BigDecimal(0)
        BigDecimal totalOutputs = new BigDecimal(0)

        while (userTransactions.next()) {
            TransactionVS transactionVS = (TransactionVS) userTransactions.get(0);
            if(TransactionVS.Type.USER_INPUT == transactionVS.type) {
                totalInputs = totalInputs.add(transactionVS.amount)
            } else if(TransactionVS.Type.USER_OUTPUT == transactionVS.type) {
                totalOutputs = totalOutputs.add(transactionVS.amount)
            }
            transactionList.add(getTransactionMap(transactionVS))
        }
        resultMap.date = DateUtils.getStringFromDate(Calendar.getInstance().getTime())
        resultMap.totalInputs = totalInputs
        resultMap.totalOutputs = totalOutputs
        resultMap.available = totalInputs.add(totalOutputs.negate())
        resultMap.transactions = transactionList
        return resultMap
    }

}