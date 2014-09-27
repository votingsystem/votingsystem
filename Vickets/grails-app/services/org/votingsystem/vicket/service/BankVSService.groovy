package org.votingsystem.vicket.service

import grails.converters.JSON
import grails.transaction.Transactional
import org.votingsystem.model.BankVS
import org.votingsystem.model.CertificateVS
import org.votingsystem.model.ContentTypeVS
import org.votingsystem.model.MessageSMIME
import org.votingsystem.model.ResponseVS
import org.votingsystem.model.TypeVS
import org.votingsystem.model.UserVS
import org.votingsystem.signature.util.CertUtil
import org.votingsystem.util.DateUtils
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.vicket.model.UserVSAccount
import org.votingsystem.vicket.util.IbanVSUtil

import java.security.cert.X509Certificate

@Transactional
class BankVSService {

    private static final CLASS_NAME = BankVSService.class.getSimpleName()

    def userVSService
    def transactionVSService
    def messageSource
    def subscriptionVSService
    def systemService
    def signatureVSService
    def grailsLinkGenerator

    public ResponseVS saveBankVS(MessageSMIME messageSMIMEReq, Locale locale) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("${methodName} - signer: ${userSigner?.nif}")
        String msg = null
        def messageJSON = JSON.parse(messageSMIMEReq.getSmimeMessage()?.getSignedContent())
        IbanVSUtil.validate(messageJSON.IBAN)
        if (!messageJSON.info || (TypeVS.BANKVS_NEW != TypeVS.valueOf(messageJSON.operation)) ||
                !messageJSON.certChainPEM) {
            msg = messageSource.getMessage('paramsErrorMsg', null, locale)
            log.error "${methodName} - PARAMS ERROR - ${msg} - messageJSON: ${messageJSON}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, reason:msg,
                    metaInf:MetaInfMsg.getErrorMsg(methodName), statusCode:ResponseVS.SC_ERROR_REQUEST)
        }
        if(!systemService.isUserAdmin(userSigner.getNif())) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                                         TypeVS.BANKVS_NEW.toString()].toArray(), locale)
            log.error "${methodName} - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }
        Collection<X509Certificate> certChain = CertUtil.fromPEMToX509CertCollection(messageJSON.certChainPEM.getBytes());
        X509Certificate x509Certificate = certChain.iterator().next();
        BankVS bankVS = BankVS.getUserVS(x509Certificate)
        ResponseVS responseVS = signatureVSService.verifyUserCertificate(bankVS)
        if(ResponseVS.SC_OK != responseVS.statusCode) return responseVS
        String validatedNIF = org.votingsystem.util.NifUtils.validate(bankVS.getNif())
        def bankVSDB = UserVS.findWhere(nif:validatedNIF)
        if(!bankVSDB || (bankVSDB instanceof UserVS)) {
            if(bankVSDB instanceof UserVS) {
                bankVSDB.setState(BankVS.State.SUSPENDED)
                bankVSDB.setReason("Updated to BankVS")
                bankVSDB.save()
                log.debug("${methodName} - UserVS: '${bankVSDB.id}' updated to bank")
            }
            bankVS.description = messageJSON.info
            bankVS.setIBAN(messageJSON.IBAN)
            bankVSDB = bankVS.save()
            log.debug("${methodName} - NEW bankVS.id: '${bankVSDB.id}'")
        } else {
            bankVSDB.description = messageJSON.info
            bankVSDB.setCertificateCA(bankVS.getCertificateCA())
            bankVSDB.setCertificate(bankVS.getCertificate())
            bankVSDB.setTimeStampToken(bankVS.getTimeStampToken())
        }
        CertificateVS certificateVS = subscriptionVSService.saveUserCertificate(bankVSDB, null)
        new UserVSAccount(currencyCode: Currency.getInstance('EUR').getCurrencyCode(), userVS:bankVSDB, balance:BigDecimal.ZERO,
                IBAN:IbanVSUtil.getInstance().getIBAN(bankVSDB.id), tag:systemService.getWildTag()).save()
        bankVSDB.save()
        msg = messageSource.getMessage('newBankVSOKMsg', [x509Certificate.subjectDN].toArray(), locale)
        String metaInfMsg = MetaInfMsg.getOKMsg(CLASS_NAME, methodName,
                "bankVS_${bankVSDB.id}_certificateVS_${certificateVS.id}")
        String bankVSURL = "${grailsLinkGenerator.link(controller:"userVS", absolute:true)}/${bankVSDB.id}"
        log.debug("${metaInfMsg}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.BANKVS_NEW, message:msg, metaInf:metaInfMsg,
                data:[message:msg, URL:bankVSURL, statusCode:ResponseVS.SC_OK], contentType:ContentTypeVS.JSON)
    }

    @Transactional
    public Map getDetailedDataMap(BankVS bankVS, DateUtils.TimePeriod timePeriod, Map params, Locale locale) {
        Map resultMap = userVSService.getUserVSDataMap(bankVS, false)
        resultMap.transactionVSMap = transactionVSService.getUserVSTransactionVSMap(bankVS, timePeriod, params, locale)
        return resultMap
    }

    public Map getDetailedDataMapWithBalances(BankVS bankVS, DateUtils.TimePeriod timePeriod) {
        Map resultMap = [timePeriod:[dateFrom:timePeriod.getDateFrom(), dateTo:timePeriod.getDateTo()]]
        resultMap.userVS = userVSService.getUserVSDataMap(bankVS, false)
        Map transactionsWithBalancesMap = transactionVSService.getTransactionFromListWithBalances(bankVS, timePeriod)
        resultMap.transactionFromList = transactionsWithBalancesMap.transactionFromList
        resultMap.balancesFrom = transactionsWithBalancesMap.balancesFrom
        return resultMap
    }

}
