package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.votingsystem.groovy.util.TransactionVSUtils
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.NifUtils

import java.security.cert.X509Certificate

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
@Transactional
class UserVSService {

    def signatureVSService
	def grailsApplication
    def grailsLinkGenerator
    def messageSource
    def subscriptionVSService
    def transactionVSService
    def systemService
    def userVSAccountService

    /*
     * Add users from PEM certs
     */
    @Transactional
    public ResponseVS saveUser(MessageSMIME messageSMIMEReq) {
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

        def messageJSON = JSON.parse(messageSMIMEReq.getSMIME()?.getSignedContent())
        if (!messageJSON.info || !messageJSON.certChainPEM ||
                (TypeVS.CERT_USER_NEW != TypeVS.valueOf(messageJSON.operation))) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName}- ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, metaInf:MetaInfMsg.getErrorMsg(methodName, "params"),
                    reason: msg, statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(messageJSON.certChainPEM.getBytes());
        UserVS newUser = UserVS.getUserVS(certChain.iterator().next())

        signatureVSService.verifyUserCertificate(newUser)
        responseVS = subscriptionVSService.checkUser(newUser)
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
                state:subscriptionVS.state.toString(), dateCreated:DateUtils.getDayWeekDateStr(subscriptionVS.dateCreated)]
        return resultMap
    }

	boolean isUserAdmin(String nif) {
        nif = NifUtils.validate(nif);
        boolean result = grailsApplication.config.vs.adminsDNI.contains(nif)
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
                                     pemCert:new String(CertUtils.getPEMEncoded (x509Cert), "UTF-8")])
            }
        }

        return [id:userVS?.id, nif:userVS?.nif, firstName: userVS.firstName, lastName: userVS.lastName, name:name,
                IBAN:userVS.IBAN, state:userVS.state.toString(), type:userVS.type.toString(), reason:userVS.reason,
                description:userVS.description, certificateList:certificateList]
    }

    @Transactional
    public Map getDataWithBalancesMap(UserVS userVS, DateUtils.TimePeriod timePeriod){
        Map resultMap = [timePeriod:timePeriod.getMap()]
        resultMap.userVS = getUserVSDataMap(userVS, false)

        Map transactionsFromWithBalancesMap = transactionVSService.getTransactionFromListWithBalances(userVS, timePeriod)
        resultMap.transactionFromList = transactionsFromWithBalancesMap.transactionFromList
        resultMap.balancesFrom = transactionsFromWithBalancesMap.balancesFrom

        Map transactionsToWithBalancesMap = transactionVSService.getTransactionToListWithBalances(userVS, timePeriod)
        resultMap.transactionToList = transactionsToWithBalancesMap.transactionToList
        resultMap.balancesTo = transactionsToWithBalancesMap.balancesTo
        resultMap.balancesCash = TransactionVSUtils.balancesCash(resultMap.balancesTo, resultMap.balancesFrom)

        if(UserVS.Type.SYSTEM != userVS.type) userVSAccountService.checkBalancesMap(userVS, resultMap.balancesCash)
        resultMap.balancesFrom = TransactionVSUtils.setBigDecimalToPlainString(resultMap.balancesFrom)
        resultMap.balancesTo = TransactionVSUtils.setBigDecimalToPlainString(resultMap.balancesTo)
        resultMap.balancesCash = TransactionVSUtils.setBigDecimalToPlainString(resultMap.balancesCash)
        return resultMap
    }
}

