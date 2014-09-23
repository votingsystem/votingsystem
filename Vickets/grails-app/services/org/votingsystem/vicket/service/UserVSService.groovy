package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.NifUtils
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.util.IbanVSUtil
import org.votingsystem.util.MetaInfMsg

import java.security.cert.X509Certificate

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class UserVSService {

    private static final CLASS_NAME = UserVSService.class.getSimpleName()

    def signatureVSService
	def grailsApplication
    def grailsLinkGenerator
    def messageSource
    def subscriptionVSService
    def transactionVSService
    def systemService

    public ResponseVS saveBankVS(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("${methodName} - signer: ${userSigner?.nif}")
        String msg = null
        def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage()?.getSignedContent())
        IbanVSUtil.validate(messageJSON.bankIBAN)
        if (!messageJSON.info || (TypeVS.BANKVS_NEW != TypeVS.valueOf(messageJSON.operation)) ||
                !messageJSON.certChainPEM) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - PARAMS ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, reason:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        if(!isUserAdmin(userSigner.getNif())) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                    TypeVS.BANKVS_NEW.toString()].toArray(), locale)
            log.error "${methodName} - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }
        Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(messageJSON.certChainPEM.getBytes());
        ResponseVS responseVS = signatureVSService.validateCertificates(new ArrayList(certChain))
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        X509Certificate x509Certificate = certChain.iterator().next();
        BankVS bankVS = BankVS.getUserVS(x509Certificate)
        String validatedNIF = org.votingsystem.util.NifUtils.validate(bankVS.getNif())
        def bankVSDB = BankVS.findWhere(nif:validatedNIF)
        if(!bankVSDB) {
            bankVS.description = messageJSON.info
            bankVSDB = bankVS.save()
            bankVSDB.setIBAN(IbanVSUtil.getInstance().getIBAN(bankVSDB.id))
            log.debug("${methodName} - NEW bankVS.id: '${bankVSDB.id}'")
        } else {
            log.debug("${methodName} - updating bankVS.id: '${bankVSDB.id}'")
            bankVSDB.description = messageJSON.info
            bankVSDB.setCertificateCA(bankVS.getCertificateCA())
            bankVSDB.setCertificate(bankVS.getCertificate())
            bankVSDB.setTimeStampToken(bankVS.getTimeStampToken())
        }
        CertificateVS certificateVS = subscriptionVSService.saveUserCertificate(bankVSDB, null)
        new UserVSAccount(currencyCode: Currency.getInstance('EUR').getCurrencyCode(), userVS:bankVSDB, balance:BigDecimal.ZERO,
                type: UserVSAccount.Type.EXTERNAL, IBAN:messageJSON.bankIBAN, tag:systemService.getWildTag()).save()
        bankVSDB.save()
        msg = messageSource.getMessage('newBankVSOKMsg', [x509Certificate.subjectDN].toArray(), locale)
        String metaInfMsg = MetaInfMsg.getOKMsg(methodName, "bankVS_${bankVSDB.id}_certificateVS_${certificateVS.id}")
        String bankVSURL = "${grailsLinkGenerator.link(controller:"userVS", absolute:true)}/${bankVSDB.id}"
        log.debug("${metaInfMsg}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.BANKVS_NEW, message:msg, metaInf:metaInfMsg,
            data:[message:msg, URL:bankVSURL, statusCode:ResponseVS.SC_OK], contentType:ContentTypeVS.JSON)
    }

    /*
     * Método para poder añadir usuarios a partir de un certificado en formato PEM
     */
    @Transactional
    public ResponseVS saveUser(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        log.debug(methodName);
        /*if(grails.util.Environment.PRODUCTION  ==  grails.util.Environment.current) {
            log.debug(" ### ADDING CERTS NOT ALLOWED IN PRODUCTION ENVIRONMENTS ###")
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR_REQUEST,
                    message: messageSource.getMessage('serviceDevelopmentModeMsg', null, locale))
        }*/
        ResponseVS responseVS = null;
        UserVS userSigner = messageSMIMEReq.getUserVS()
        String msg
        if(!isUserAdmin(userSigner.getNif())) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                     TypeVS.CERT_CA_NEW.toString()].toArray(), locale)
            log.error "${methodName} - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }

        def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage()?.getSignedContent())
        if (!messageJSON.info || !messageJSON.certChainPEM ||
                (TypeVS.CERT_USER_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName}- ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, metaInf:MetaInfMsg.getErrorMsg(methodName, "params"),
                    reason: msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(messageJSON.certChainPEM.getBytes());
        UserVS newUser = UserVS.getUserVS(certChain.iterator().next())

        responseVS = signatureVSService.verifyUserCertificate(newUser)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        responseVS = subscriptionVSService.checkUser(newUser, locale)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        String userURL = "${grailsLinkGenerator.link(controller:"userVS", absolute:true)}/${responseVS.getUserVS().id}"

        if(!responseVS.data.isNewUser) {
            msg = messageSource.getMessage('certUserNewErrorMsg', [responseVS.getUserVS().getNif()].toArray(), locale)
            return new ResponseVS(statusCode:ResponseVS.SC_ERROR, type:TypeVS.CERT_USER_NEW, contentType:
                    ContentTypeVS.JSON, data:[message:msg, URL:userURL, statusCode:ResponseVS.SC_ERROR], message:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userVS_${responseVS.getUserVS().id}"))
        }

        responseVS.getUserVS().setState(UserVS.State.ACTIVE)
        responseVS.getUserVS().setReason(messageJSON.info)
        responseVS.getUserVS().save()
        msg = messageSource.getMessage('certUserNewMsg', [responseVS.getUserVS().getNif()].toArray(), locale)

        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.CERT_USER_NEW, contentType: ContentTypeVS.JSON,
                data:[message:msg, URL:userURL, statusCode:ResponseVS.SC_OK], message:msg,
                metaInf:MetaInfMsg.getOKMsg(methodName, "userVS_${responseVS.getUserVS().id}"))
    }

	public Map getUserVS(Date fromDate){
        def usersVS = UserVS.createCriteria().list(offset: 0) {
            gt("dateCreated", fromDate)
        }
		return [totalNumUsu:usersVS?usersVS.getTotalCount():0]
	}

    public Map getSubscriptionVSDataMap(SubscriptionVS subscriptionVS){
        Map resultMap = [id:subscriptionVS.id, dateActivated:subscriptionVS.dateActivated,
             dateCancelled:subscriptionVS.dateCancelled, lastUpdated:subscriptionVS.lastUpdated,
             uservs:[id:subscriptionVS.userVS.id, IBAN:subscriptionVS.userVS.IBAN, NIF:subscriptionVS.userVS.nif,
                   name:"${subscriptionVS.userVS.firstName} ${subscriptionVS.userVS.lastName}"],
             groupvs:[name:subscriptionVS.groupVS.name, id:subscriptionVS.groupVS.id],
                state:subscriptionVS.state.toString(), dateCreated:subscriptionVS.dateCreated]
        return resultMap
    }

    public Map getSubscriptionVSDetailedDataMap(SubscriptionVS subscriptionVS){
        String subscriptionMessageURL = "${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${subscriptionVS.subscriptionSMIME.id}"
        def adminMessages = []
        subscriptionVS.adminMessageSMIMESet.each {adminMessage ->
            adminMessages.add("${grailsLinkGenerator.link(controller:"messageSMIME", absolute:true)}/${adminMessage.id}")
        }
        Map resultMap = [id:subscriptionVS.id, dateActivated:subscriptionVS.dateActivated,
                dateCancelled:subscriptionVS.dateCancelled, lastUpdated:subscriptionVS.lastUpdated,
                messageURL:subscriptionMessageURL,adminMessages:adminMessages,
                uservs:[id:subscriptionVS.userVS.id, NIF:subscriptionVS.userVS.nif, IBAN:subscriptionVS.userVS.IBAN,
                      name:"${subscriptionVS.userVS.firstName} ${subscriptionVS.userVS.lastName}"],
                groupvs:[name:subscriptionVS.groupVS.name, id:subscriptionVS.groupVS.id],
                state:subscriptionVS.state.toString(), dateCreated:subscriptionVS.dateCreated]
        return resultMap
    }

	boolean isUserAdmin(String nif) {
        nif = NifUtils.validate(nif);
        boolean result = grailsApplication.config.VotingSystem.adminsDNI.contains(nif)
        if(result) log.debug("isUserAdmin - nif: ${nif}")
		return result
	}

    public Map getUserVSBasicDataMap(UserVS userVS){
        String name = userVS.name
        if(!name) name = "${userVS.firstName} ${userVS.lastName}"
        return [nif:userVS?.nif, name:name]
    }

    @Transactional
    public Map getUserVSDataMap(UserVS userVS, boolean withCerts){
        String name = userVS.name
        if(!userVS.name) name = "${userVS.firstName} ${userVS.lastName}"
        def certificateList = []
        if(withCerts) {
            def certificates = CertificateVS.findAllWhere(userVS:userVS, state:CertificateVS.State.OK)
            certificates.each {certItem ->
                X509Certificate x509Cert = certItem.getX509Cert()
                certificateList.add([serialNumber:"${certItem.serialNumber}",
                                     pemCert:new String(CertUtil.getPEMEncoded (x509Cert), "UTF-8")])
            }
        }

        return [id:userVS?.id, nif:userVS?.nif, firstName: userVS.firstName, lastName: userVS.lastName, name:name,
                IBAN:userVS.IBAN, state:userVS.state.toString(), type:userVS.type.toString(), reason:userVS.reason,
                description:userVS.description, certificateList:certificateList]
    }


    /*@Transactional
    public Map getBankVSDataMap(BankVS bankVS, DateUtils.TimePeriod timePeriod) {
        def transactionList = TransactionVS.createCriteria().list(offset: 0, sort:'dateCreated', order:'desc') {
            eq('fromUserVS', bankVS)
            if(timePeriod) gt('dateCreated', timePeriod.getDateFrom())
            isNotNull('transactionParent')
        }
        def transactionListJSON = []
        transactionList.each { transaction ->
            transactionListJSON.add(transactionVSService.getTransactionMap(transaction))
        }
        Map resultMap = getUserVSDataMap(bankVS)
        resultMap.transactionList = transactionListJSON
        return resultMap
    }*/

    @Transactional
    public Map getBankVSDetailedDataMap(UserVS userVS, DateUtils.TimePeriod timePeriod, Map params, Locale locale){
        Map resultMap = getUserVSDataMap(userVS, false)
        resultMap.transactionVSMap = transactionVSService.getUserVSTransactionVSMap(userVS, timePeriod, params, locale)
        return resultMap
    }

    @Transactional
    public Map getUserVSDetailedDataMap(UserVS userVS, DateUtils.TimePeriod timePeriod, Map params, Locale locale){
        Map resultMap = getUserVSDataMap(userVS, false)
        def subscriptions = SubscriptionVS.findAllWhere(userVS:userVS, state: SubscriptionVS.State.ACTIVE)
        List subscriptionList = []
        subscriptions.each { it->
            subscriptionList.add([id:it.id, groupVS:[id:it.groupVS.id, name:it.groupVS.name]])
        }
        resultMap.subscriptionVSList = subscriptionList
        resultMap.transactionVSMap = transactionVSService.getUserVSTransactionVSMap(userVS, timePeriod, params, locale)
        return resultMap
    }

    @Transactional
    public Map getDetailedDataMap(UserVS userVS, DateUtils.TimePeriod timePeriod){
        Map resultMap = getUserVSDataMap(userVS, false)

        def transactionFromListJSON = []
        transactionVSService.getTransactionFromList(userVS, timePeriod).each { transaction ->
            transactionFromListJSON.add(transactionVSService.getTransactionMap(transaction))
        }

        def transactionToListJSON = []
        transactionVSService.getTransactionToList(userVS, timePeriod).each { transaction ->
            transactionToListJSON.add(transactionVSService.getTransactionMap(transaction))
        }
        resultMap.transactionFromList = transactionFromListJSON
        resultMap.transactionToList = transactionToListJSON
        return resultMap
    }

    @Transactional
    public Map getDetailedDataMapWithBalances(UserVS userVS, DateUtils.TimePeriod timePeriod){
        Map resultMap = [:]
        resultMap.timePeriod = [dateFrom:timePeriod.getDateFrom(), dateTo:timePeriod.getDateTo()]
        resultMap.userVS = getUserVSDataMap(userVS, false)


        Map transactionsFromWithBalancesMap = transactionVSService.getTransactionFromListWithBalances(userVS, timePeriod)
        resultMap.transactionFromList = transactionsFromWithBalancesMap.transactionFromList
        resultMap.balancesFrom = transactionsFromWithBalancesMap.balancesFrom

        Map transactionsToWithBalancesMap = transactionVSService.getTransactionToListWithBalances(userVS, timePeriod)
        resultMap.transactionToList = transactionsToWithBalancesMap.transactionToList
        resultMap.balancesTo = transactionsToWithBalancesMap.balancesTo
        resultMap.balancesToTimeLimited = transactionsToWithBalancesMap.balancesToTimeLimited
        resultMap.balanceResult = transactionVSService.balanceResult(resultMap.balancesTo, resultMap.balancesFrom)
        return resultMap
    }
}

