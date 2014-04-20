package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONSerializer
import org.bouncycastle.asn1.DERTaggedObject
import org.bouncycastle.asn1.DERUTF8String
import org.bouncycastle.util.encoders.Base64
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
import org.votingsystem.model.VoteVS
import org.votingsystem.model.vicket.Vicket
import org.votingsystem.model.vicket.TransactionVS
import org.votingsystem.signature.smime.SMIMEMessageWrapper
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS

import java.math.RoundingMode
import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
class TransactionVSService {

    private final Set<String> listenerSet = Collections.synchronizedSet(new HashSet<String>());

    def signatureVSService
    def messageSource
    def userVSService
    def sessionFactory
    def grailsLinkGenerator
    def grailsApplication
    def webSocketService
    def csrService


    public ResponseVS cancelVicketDeposit(MessageSMIME messageSMIMEReq, Locale locale) {
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        //messageSMIMEReq?.getSmimeMessage()?.getSigner()?.certificate
        String msg;
        try {
            log.debug(smimeMessageReq.getSignedContent())
            String fromUser = grailsApplication.config.VotingSystem.serverName
            String toUser = smimeMessageReq.getFrom().toString()
            String subject = messageSource.getMessage('vicketReceiptSubject', null, locale)
            SMIMEMessageWrapper smimeMessageResp = signatureVSService.getMultiSignedMimeMessage(fromUser, toUser,
                    smimeMessageReq, subject)
            MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.RECEIPT, smimeParent:messageSMIMEReq,
                    content:smimeMessageResp.getBytes()).save()
            return new ResponseVS(statusCode:ResponseVS.SC_OK, message:msg, type:TypeVS.VICKET_CANCEL, data:messageSMIMEResp,
                    contentType: ContentTypeVS.JSON_SIGNED_AND_ENCRYPTED)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            msg = messageSource.getMessage('depositDataError', null, locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VICKET_CANCEL_ERROR)
        }
    }

    public void addTransactionListener (String listenerId) {
        listenerSet.add(listenerId)
    }

    public ResponseVS processDeposit(MessageSMIME messageSMIMEReq, Locale locale) {
        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS signer = messageSMIMEReq.userVS
        String msg;
        try {
            def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage().getSignedContent())
            TypeVS transactionType = TypeVS.valueOf(messageJSON.typeVS)
            switch(transactionType) {
                case TypeVS.VICKET_USER_ALLOCATION:
                    processUserAllocation(messageSMIMEReq, messageJSON, locale)
                    break;
                default:
                    log.debug("Unprocessed transactionType: " + transactionType);
            }

        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            msg = messageSource.getMessage('depositDataError', null, locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VICKET_DEPOSIT_ERROR)
        }
    }


    public void notifyListeners(TransactionVS transactionVS) {
        Map messageMap = getTransactionMap(transactionVS)
        log.debug("notifyListeners - transactionVS.id: ${transactionVS.id} - messageMap: ${messageMap as JSON}")
        Map broadcastResul = webSocketService.broadcastList(messageMap, listenerSet);
        if(ResponseVS.SC_OK != broadcastResul.statusCode) {
            broadcastResul.errorList.each {listenerSet.remove(it)}
        }
    }

    private ResponseVS processUserAllocation(MessageSMIME messageSMIMEReq, JSONObject messageJSON, Locale locale) {

        SMIMEMessageWrapper smimeMessageReq = messageSMIMEReq.getSmimeMessage()
        UserVS signer = messageSMIMEReq.userVS
        String msg;
        try {
            if(!userVSService.isUserAdmin(signer.getNif())) {
                msg = messageSource.getMessage('userAllocationAdminErrorMsg', [signer.getNif()].toArray(), locale)
                return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VICKET_DEPOSIT_ERROR)
            }
            Calendar weekFromCalendar = DateUtils.getMonday(Calendar.getInstance())
            Calendar weekToCalendar = weekFromCalendar.clone();
            weekToCalendar.add(Calendar.DAY_OF_YEAR, 7)

            int numUsers = UserVS.createCriteria().count {
                eq("type", UserVS.Type.USER)
                le("dateCreated", weekFromCalendar.getTime())
            }
            def dataMapToSignBySystem = [numUsers:numUsers, amount: messageJSON.amount,
                    currency:messageJSON.currency, allocationMessage:new String(Base64.encode(messageSMIMEReq.content))]
            String dataToSignBySystem = new JSONObject(dataMapToSignBySystem).toString()
            ResponseVS responseVS = signatureVSService.getTimestampedSignedMimeMessage(userVSService.getSystemUser().getNif(),
                    null, dataToSignBySystem, messageSource.getMessage("userAllocationReceiptMsg", null, locale))
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS
            MessageSMIME messageSMIMEResp = new MessageSMIME(type:TypeVS.VICKET_USER_ALLOCATION_RECEIPT,
                    smimeParent: messageSMIMEReq,
                    content:responseVS.getSmimeMessage().getBytes())
            MessageSMIME.withTransaction { messageSMIMEResp.save(); }

            BigDecimal numUsersBigDecimal = new BigDecimal(numUsers)
            CurrencyVS currency = CurrencyVS.valueOf(messageJSON.currency)
            String subject = messageJSON.subject
            BigDecimal amount = new BigDecimal(messageJSON.amount)
            BigDecimal userPart = amount.divide(numUsersBigDecimal, 2, RoundingMode.FLOOR)
            BigDecimal totalUsers = userPart.multiply(numUsersBigDecimal)
            log.debug("processUserAllocation - ${messageJSON}")
            if(!currency || !amount) throw new ExceptionVS(messageSource.getMessage('depositDataError', null, locale))

            TransactionVS transactionParent = new TransactionVS(amount: amount, messageSMIME:messageSMIMEResp,
                    state:TransactionVS.State.OK, validTo: weekToCalendar.getTime(),
                    subject:subject, fromUserVS:signer, toUserVS: userVSService.getSystemUser(),
                    currency:currency, type:TransactionVS.Type.USER_ALLOCATION).save()

            def usersToDeposit = UserVS.createCriteria().scroll {
                eq("type", UserVS.Type.USER)
                le("dateCreated", weekFromCalendar.getTime())
            }
            UserVS systemUser = UserVS.findWhere(type: UserVS.Type.SYSTEM)
            while (usersToDeposit.next()) {
                UserVS userVS = (UserVS) usersToDeposit.get(0);
                TransactionVS userTransaction = new TransactionVS(transactionParent:transactionParent, amount:userPart,
                        state:TransactionVS.State.OK, validTo: weekToCalendar.getTime(),
                        subject:subject, fromUserVS: systemUser, toUserVS:userVS, currency:currency,
                        type:TransactionVS.Type.USER_ALLOCATION_INPUT).save()
                if((usersToDeposit.getRowNumber() % 100) == 0) {
                    sessionFactory.currentSession.flush()
                    sessionFactory.currentSession.clear()
                    log.debug("processed ${usersToDeposit.getRowNumber()}/${numUsers} user allocation for transaction ${transactionParent.id}");
                }
            }
            return new ResponseVS(statusCode:ResponseVS.SC_OK, message:"Transaction OK", type:TypeVS.VICKET_USER_ALLOCATION)
        } catch(Exception ex) {
            log.error(ex.getMessage(), ex);
            msg = messageSource.getMessage('depositDataError', null, locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST, message:msg, type:TypeVS.VICKET_DEPOSIT_ERROR)
        }
    }

    public ResponseVS processVicketRequest(MessageSMIME messageSMIMEReq, byte[] csrRequest, Locale locale) {
        log.debug("processVicketRequest");
        //To avoid circular references issues
        ResponseVS responseVS = ((VicketService)grailsApplication.mainContext.getBean("vicketService")).processRequest(
                messageSMIMEReq, locale)
        if (ResponseVS.SC_OK == responseVS.statusCode) {
            ResponseVS vicketGenBatchResponse = csrService.signVicketBatchRequest(csrRequest,
                    responseVS.data.amount, responseVS.data.currency, locale)
            if (ResponseVS.SC_OK == vicketGenBatchResponse.statusCode) {
                UserVS userVS = messageSMIMEReq.userVS
                TransactionVS userTransaction = new TransactionVS(amount:responseVS.data.amount,
                        state:TransactionVS.State.OK, currency:responseVS.data.currency,
                        subject: messageSource.getMessage('vicketRequest', null, locale), messageSMIME: messageSMIMEReq,
                        fromUserVS: userVS, toUserVS: userVS, type:TransactionVS.Type.VICKET_REQUEST).save()

                Map transactionMap = getTransactionMap(userTransaction)
                Map resultMap = [transactionList:[transactionMap], issuedVickets:vicketGenBatchResponse.data]
                return new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON_ENCRYPTED,
                        type:TypeVS.VICKET_REQUEST, messageBytes:"${resultMap as JSON}".getBytes());
            } else return vicketGenBatchResponse
        } else return responseVS
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
        Calendar weekToCalendar = mondayLapse.clone();
        weekToCalendar.add(Calendar.DAY_OF_YEAR, 7)
        def userInputTransactions = TransactionVS.createCriteria().scroll {
            eq("toUserVS", userVS)
            or {
                eq("type", TransactionVS.Type.USER_ALLOCATION_INPUT)
                eq("type", TransactionVS.Type.VICKET_CANCELLATION)
            }
            ge("dateCreated", mondayLapse.getTime())
            le("dateCreated", weekToCalendar.getTime())
        }

        String dirPath = DateUtils.getDirPath(mondayLapse.getTime())
        //int year =  calendar.get(Calendar.YEAR);
        //int month = calendar.get(Calendar.MONTH) + 1; // Note: zero based!
        //int day = calendar.get(Calendar.DAY_OF_MONTH);
        def userOutputTransactions = TransactionVS.createCriteria().scroll {
            eq("fromUserVS", userVS)
            eq("type", TransactionVS.Type.VICKET_REQUEST)
            ge("dateCreated", mondayLapse.getTime())
        }

        Map resultMap = [:]
        Map dateResultMap = [:]
        dateResultMap[CurrencyVS.EURO.toString()] = [totalOutputs: new BigDecimal(0),
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