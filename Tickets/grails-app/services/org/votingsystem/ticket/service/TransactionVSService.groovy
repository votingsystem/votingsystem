package org.votingsystem.ticket.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONSerializer
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.x509.extension.X509ExtensionUtil
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.ContextVS
import org.votingsystem.model.CurrencyVS
import org.votingsystem.model.EventVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.model.ticket.TicketVS
import org.votingsystem.model.ticket.TransactionVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import sun.security.krb5.internal.Ticket

import java.math.RoundingMode
import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class TransactionVSService {

    def signatureVSService
    def messageSource
    def userVSService
    def sessionFactory
    def grailsLinkGenerator
    def grailsApplication

    public ResponseVS cancelTicketDeposit(MessageSMIME messageSMIMEReq, Locale locale) {
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        //messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate
        String msg;
        try {
            log.debug(smimeMessageReq.getSignedContent())
            String fromUser = grailsApplication.config.VotingSystem.serverName
            String toUser = smimeMessageReq.getFrom().toString()
            String subject = messageSource.getMessage('ticketReceiptSubject', null, locale)




            SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
                    smimeMessageReq, subject)

            MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                    content:smimeMessageResp.getBytes()).save()
            return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, type:TypeVS.TICKET_CANCEL, data:messageSMIMEResp,
                    contentType: ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            msg = messageSource.getMessage('depositDataError', null, locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.TICKET_CANCEL_ERROR)
        }
    }



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
            CurrencyVS currency = CurrencyVS.valueOf(messageJSON.currency)
            String subject = messageJSON.subject
            BigDecimal amount = new BigDecimal(messageJSON.amount)
            BigDecimal userPart = amount.divide(numUsersBigDecimal, 2, RoundingMode.FLOOR)
            BigDecimal totalUsers = userPart.multiply(numUsersBigDecimal)
            log.debug("processUserAllocation - ${messageJSON}")
            if(!currency || !amount) throw new ExceptionVS(messageSource.getMessage('depositDataError', null, locale))
            TransactionVS transactionParent = new TransactionVS(amount: amount, messageSMIME:messageSMIMEReq,
                    subject:subject, fromUserVS:signer, toUserVS: userVSService.getSystemUser(),
                    currency:currency, type:TransactionVS.Type.USER_ALLOCATION).save()

            def criteria = UserVS.createCriteria()
            def usersToDeposit = criteria.scroll { eq("type", UserVS.Type.USER) }
            UserVS systemUser = UserVS.findWhere(type: UserVS.Type.SYSTEM)
            while (usersToDeposit.next()) {
                UserVS userVS = (UserVS) usersToDeposit.get(0);
                TransactionVS userTransaction = new TransactionVS(transactionParent:transactionParent, amount:userPart,
                        subject:subject, fromUserVS: systemUser, toUserVS:userVS, currency:currency,
                        type:TransactionVS.Type.USER_INPUT).save()
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
        if(transaction.fromUserVS) {
            String fromUserVSName = "${transaction.fromUserVS.firstName} ${transaction.fromUserVS.lastName}"
            transactionMap.fromUserVS = [nif:transaction.fromUserVS.nif, name:fromUserVSName]
        }
        if(transaction.toUserVS) {
            String toUserVSName = "${transaction.toUserVS.firstName} ${transaction.toUserVS.lastName}"
            transactionMap.toUserVS = [nif:transaction.toUserVS.nif, name:toUserVSName]
        }
        transactionMap.dateCreated = DateUtils.getStringFromDate(transaction.dateCreated)
        if(transaction.validTo) transactionMap.validTo = DateUtils.getStringFromDate(transaction.validTo)
        transactionMap.id = transaction.id
        transactionMap.subject = transaction.subject
        transactionMap.type = transaction.getType().toString()
        transactionMap.amount = transaction.amount
        transactionMap.currency = transaction.currency.toString()

        String messageSMIMEURL = null
        TransactionVS.withTransaction {
            if(transaction.transactionParent) {
                messageSMIMEURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${transaction.transactionParent.messageSMIME?.id}"
            } else messageSMIMEURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${transaction.getMessageSMIME()?.id}"
            transactionMap.messageSMIMEURL = messageSMIMEURL
        }
        return transactionMap
    }

    public Map getUserInfoMap(UserVS userVS, Calendar mondayLapse) {
        def inputCriteria = TransactionVS.createCriteria()
        def userInputTransactions = inputCriteria.scroll {
            eq("toUserVS", userVS)
            or {
                eq("type", TransactionVS.Type.USER_INPUT)
                eq("type", TransactionVS.Type.TICKET_CANCELLATION)
            }
            ge("dateCreated", mondayLapse.getTime())
        }


        String dirPath = DateUtils.getDirPath(mondayLapse.getTime())
        //int year =  calendar.get(Calendar.YEAR);
        //int month = calendar.get(Calendar.MONTH) + 1; // Note: zero based!
        //int day = calendar.get(Calendar.DAY_OF_MONTH);

        def outputCriteria = TransactionVS.createCriteria()
        def userOutputTransactions = outputCriteria.scroll {
            eq("fromUserVS", userVS)
            eq("type", TransactionVS.Type.USER_OUTPUT)
            ge("dateCreated", mondayLapse.getTime())
        }

        Map resultMap = [:]
        Map dateResultMap = [:]
        dateResultMap[CurrencyVS.Euro.toString()] = [totalOutputs: new BigDecimal(0),
                totalInputs:new BigDecimal(0), transactionList:[]]

        while (userInputTransactions.next()) {
            TransactionVS transactionVS = (TransactionVS) userInputTransactions.get(0);
            CurrencyVS currencyVS = transactionVS.getCurrency()
            Map currencyMap = dateResultMap.get(currencyVS.toString())
            if(!currencyMap) {
                currencyMap = [totalOutputs: new BigDecimal(0), totalInputs:new BigDecimal(0), transactionList:[]]
                dateResultMap.put(currencyVS.toString(), currencyMap)
            }
            currencyMap.totalInputs = currencyMap.totalInputs.add(transactionVS.amount)
            currencyMap.transactionList.add(getTransactionMap(transactionVS))
        }

        while (userOutputTransactions.next()) {
            TransactionVS transactionVS = (TransactionVS) userOutputTransactions.get(0);
            CurrencyVS currencyVS = transactionVS.getCurrency()
            Map currencyMap = dateResultMap.get(currencyVS.toString())
            if(!currencyMap) {
                currencyMap = [totalOutputs: new BigDecimal(0), totalInputs:new BigDecimal(0), transactionList:[]]
                dateResultMap.put(currencyVS.toString(), currencyMap)
            }
            currencyMap.totalOutputs = currencyMap.totalOutputs.add(transactionVS.amount)
            currencyMap.transactionList.add(getTransactionMap(transactionVS))
        }
        resultMap.date = DateUtils.getStringFromDate(Calendar.getInstance().getTime())
        resultMap[dirPath] = dateResultMap
        return resultMap
    }

}