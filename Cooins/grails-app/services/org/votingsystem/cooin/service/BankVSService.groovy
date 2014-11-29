package org.votingsystem.cooin.service

import grails.converters.JSON
import grails.transaction.Transactional
import net.sf.json.JSONObject
import org.iban4j.Iban
import org.votingsystem.model.*
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.MetaInfMsg
import org.votingsystem.util.ValidationExceptionVS
import org.votingsystem.cooin.model.UserVSAccount
import org.votingsystem.cooin.util.IbanVSUtil

import java.security.cert.X509Certificate

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

@Transactional
class BankVSService {

    private class SaveBankRequest {
        String info, certChainPEM;
        Iban IBAN;
        TypeVS operation;
        public SaveBankRequest(String signedContent) throws ExceptionVS {
            JSONObject messageJSON = JSON.parse(signedContent)
            IBAN = Iban.valueOf(messageJSON.IBAN);
            info = messageJSON.info;
            certChainPEM = messageJSON.certChainPEM
            if(!info) throw new ValidationExceptionVS(this.getClass(), "missing param 'info'");
            if(!certChainPEM) throw new ValidationExceptionVS(this.getClass(), "missing param 'certChainPEM'")
            if(TypeVS.BANKVS_NEW != TypeVS.valueOf(messageJSON.operation)) throw new ValidationExceptionVS(this.getClass(),
                    "Operation expected: 'BANKVS_NEW' - operation found: " + messageJSON.operation)
        }
    }

    def userVSService
    def transactionVSService
    def messageSource
    def subscriptionVSService
    def systemService
    def signatureVSService
    def grailsLinkGenerator

    public ResponseVS saveBankVS(MessageSMIME messageSMIMEReq) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS userSigner = messageSMIMEReq.getUserVS()
        log.debug("${methodName} - signer: ${userSigner?.nif}")
        String msg = null
        SaveBankRequest request = new SaveBankRequest(messageSMIMEReq.getSMIME()?.getSignedContent())
        if(!systemService.isUserAdmin(userSigner.getNif())) {
            msg = messageSource.getMessage('userWithoutPrivilegesErrorMsg', [userSigner.getNif(),
                                         TypeVS.BANKVS_NEW.toString()].toArray(), locale)
            log.error "${methodName} - ${msg}"
            return new ResponseVS(type:TypeVS.ERROR, message:msg, statusCode:ResponseVS.SC_ERROR_REQUEST,
                    metaInf:MetaInfMsg.getErrorMsg(methodName, "userWithoutPrivileges"))
        }
        Collection<X509Certificate> certChain = CertUtils.fromPEMToX509CertCollection(request.certChainPEM.getBytes());
        X509Certificate x509Certificate = certChain.iterator().next();
        BankVS bankVS = BankVS.getUserVS(x509Certificate)
        signatureVSService.verifyUserCertificate(bankVS)
        String validatedNIF = org.votingsystem.util.NifUtils.validate(bankVS.getNif())
        def bankVSDB = UserVS.findWhere(nif:validatedNIF)
        if(bankVSDB instanceof UserVS) throw new ExceptionVS("The userVS '${bankVSDB.id}' has the same NIF '${bankVSDB.nif}'")
        if(!bankVSDB) {
            bankVSDB = bankVS.setDescription(request.info).setIBAN(request.IBAN.toString()).save()
            new BankVSInfo(bankVS:bankVSDB, bankCode:request.IBAN.bankCode).save();
            log.debug("${methodName} - NEW bankVS id '${bankVSDB.id}'")
        } else {
            bankVSDB.setDescription(request.info).setCertificateCA(bankVS.getCertificateCA())
            bankVSDB.setCertificate(bankVS.getCertificate())
            bankVSDB.setTimeStampToken(bankVS.getTimeStampToken())
        }
        subscriptionVSService.setUserCertificate(bankVSDB, null)
        new UserVSAccount(currencyCode: Currency.getInstance('EUR').getCurrencyCode(), userVS:bankVSDB, balance:BigDecimal.ZERO,
                IBAN:IbanVSUtil.getInstance().getIBAN(bankVSDB.id), tag:systemService.getWildTag()).save()
        bankVSDB.save()
        msg = messageSource.getMessage('newBankVSOKMsg', [x509Certificate.subjectDN].toArray(), locale)
        String metaInfMsg = MetaInfMsg.getOKMsg(this.class.getSimpleName(), methodName,
                "bankVS_${bankVSDB.id}_certificateVS_${bankVSDB.certificateVS.id}")
        String bankVSURL = "${grailsLinkGenerator.link(controller:"userVS", absolute:true)}/${bankVSDB.id}"
        log.debug("${metaInfMsg}")
        return new ResponseVS(statusCode:ResponseVS.SC_OK, type:TypeVS.BANKVS_NEW, message:msg, metaInf:metaInfMsg,
                data:[message:msg, URL:bankVSURL, statusCode:ResponseVS.SC_OK], contentType:ContentTypeVS.JSON)
    }

    public Map getDataWithBalancesMap(BankVS bankVS, DateUtils.TimePeriod timePeriod) {
        Map resultMap = [timePeriod:timePeriod.getMap()]
        resultMap.userVS = userVSService.getUserVSDataMap(bankVS, false)
        Map transactionsWithBalancesMap = transactionVSService.getTransactionFromListWithBalances(bankVS, timePeriod)
        resultMap.transactionFromList = transactionsWithBalancesMap.transactionFromList
        resultMap.balancesFrom = transactionsWithBalancesMap.balancesFrom
        return resultMap
    }

    public void refreshBankInfoData() {
        List<BankVS> bankVSList = BankVS.getAll()
        for(BankVS bankVS : bankVSList) {
            BankVSInfo bankVSInfo = BankVSInfo.findWhere(bankVS:bankVS)
            Iban iban = Iban.valueOf(bankVS.IBAN);
            if(bankVSInfo) {
                bankVSInfo.setBankCode(iban.bankCode).save();
            } else {
                bankVSInfo = new BankVSInfo(bankVS:bankVS, bankCode:iban.bankCode).save();
            }
        }
    }
}
