package org.votingsystem.cooin.service

import grails.transaction.Transactional
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject
import org.votingsystem.cooin.model.*
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.signature.util.CertExtensionCheckerVS
import org.votingsystem.signature.util.CertUtils
import org.votingsystem.throwable.CooinExpendedException
import org.votingsystem.throwable.ExceptionVS
import org.votingsystem.util.DateUtils
import org.votingsystem.util.MetaInfMsg

import java.security.cert.X509Certificate

import static org.springframework.context.i18n.LocaleContextHolder.getLocale

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
class CooinService {

    def messageSource
    def transactionVSService
    def grailsApplication
    def signatureVSService
    def userVSService
    def csrService
    def walletVSService
    def timeStampService
    def systemService

    public ResponseVS processCooinTransaction(CooinTransactionBatch cooinBatch) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        Map resultMap = [:]
        List<Cooin> validatedCooinList = new ArrayList<Cooin>()
        UserVS toUserVS = UserVS.findWhere(IBAN:cooinBatch.getToUserIBAN());
        cooinBatch.setToUserVS(toUserVS)
        TagVS tagVS = TagVS.findWhere(name:cooinBatch.getTag())
        if(!tagVS) throw new ExceptionVS(
                "Error - CooinTransactionBatch '${cooinBatch?.getBatchUUID()}' missing TagVS '",
                MetaInfMsg.getErrorMsg(methodName, 'missingTagVS'))
        cooinBatch.setTagVS(tagVS)
        if(!toUserVS) throw new ExceptionVS(
                "Error - CooinTransactionBatch '${cooinBatch?.getBatchUUID()}' has wrong receptor IBAN '" +
                cooinBatch.getToUserIBAN() + "'", MetaInfMsg.getErrorMsg(methodName, 'toUserVSERROR'))
        for(Cooin cooin : cooinBatch.getCooinList()) {
            try {
                validatedCooinList.add(validateCooin(cooin));
            } catch(Exception ex) {
                if(ex instanceof CooinExpendedException) return new ResponseVS(ResponseVS.SC_COOIN_EXPENDED, ex.getMessage())
                else if(ex instanceof ExceptionVS) throw ex
                else throw new ExceptionVS("Error validating Cooin with hashCertVS '${cooin?.hashCertVS}'",
                        MetaInfMsg.getErrorMsg(methodName, 'cooinExpended'), ex)
            }
        }
        JSONObject batchDataJSON = cooinBatch.getDataJSON()
        SMIMEMessage receipt = signatureVSService.getSMIME(systemService.getSystemUser().getName(),
                cooinBatch.getBatchUUID(), batchDataJSON.toString(), cooinBatch.getSubject(), null)
        cooinBatch.setState(BatchRequest.State.OK).save()
        MessageSMIME messageSMIME = new MessageSMIME(smimeMessage:receipt, type:TypeVS.RECEIPT, batchRequest:cooinBatch).save()
        Date validTo = null
        //DateUtils.TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        //if(cooinBatch.isTimeLimited == true) validTo = timePeriod.getDateTo()
        TransactionVS transactionVS = new TransactionVS(amount: cooinBatch.getBatchAmount(), messageSMIME:messageSMIME,
                cooinTransactionBatch:cooinBatch, toUserIBAN:cooinBatch.getToUserIBAN(), state:TransactionVS.State.OK,
                validTo: validTo, subject:cooinBatch.getSubject(), toUserVS: toUserVS, type:TransactionVS.Type.COOIN_SEND,
                currencyCode: cooinBatch.getCurrencyCode(), tag:cooinBatch.getTagVS()).save()
        for(Cooin cooin: validatedCooinList) {
            cooin.setState(Cooin.State.EXPENDED).setTransactionVS(transactionVS).save()
        }
        if(cooinBatch.getLeftOverCooin()) {
            cooinBatch.getLeftOverCooin().setTag(systemService.getTag(cooinBatch.getLeftOverCooin().getCertTagVS()))
            Cooin leftOverCoin = csrService.signCooinRequest(cooinBatch.getLeftOverCooin())
            resultMap.leftOverCoin = new String(leftOverCoin.getIssuedCertPEM(), "UTF-8")
        }
        resultMap.receipt = Base64.getEncoder().encodeToString(receipt.getBytes())
        return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON, data: resultMap)
    }

    public Cooin validateCooin(Cooin cooin) throws ExceptionVS {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        SMIMEMessage smimeMessage = cooin.getSMIME()
        Cooin cooinDB =  Cooin.findWhere(serialNumber:cooin.getX509AnonymousCert().serialNumber.longValue(),
                hashCertVS:cooin.getHashCertVS())
        if(!cooinDB) throw new ExceptionVS(messageSource.getMessage('hashCertVSCooinInvalidErrorMsg',
                [cooin.getHashCertVS()].toArray(), locale),
                MetaInfMsg.getErrorMsg(methodName, 'cooinDBMissing'))
        cooin = cooinDB.checkRequestWithDB(cooin)
        if(cooin.state == Cooin.State.EXPENDED) {
            throw new CooinExpendedException(cooin.getHashCertVS())
        } else if(cooin.state == Cooin.State.OK) {
            UserVS userVS = smimeMessage.getSigner(); //anonymous signer
            CertExtensionCheckerVS extensionChecker
            timeStampService.validateToken(userVS.getTimeStampToken())
            CertUtils.CertValidatorResultVS certValidatorResult = CertUtils.verifyCertificate(
                    signatureVSService.getCooinAnchors(), false, [userVS.getCertificate()])
            X509Certificate certCaResult = certValidatorResult.result.trustAnchor.trustedCert;
            extensionChecker = certValidatorResult.checker
            //if (extensionChecker.isAnonymousSigner()) { }
        } else  throw new ExceptionVS(messageSource.getMessage('cooinStateErrorMsg',
                [cooin.id, cooin.state.toString()].toArray(), locale),
                MetaInfMsg.getErrorMsg(methodName, 'cooinStateError'))
        cooin.setAuthorityCertificateVS(signatureVSService.getServerCertificateVS())
        return cooin
    }

    public ResponseVS processCooinRequest(CooinRequestBatch cooinBatch) {
        String methodName = new Object() {}.getClass().getEnclosingMethod().getName();
        UserVS fromUserVS = cooinBatch.messageSMIME.userVS
        DateUtils.TimePeriod timePeriod = DateUtils.getWeekPeriod(Calendar.getInstance())
        //Check cash available for user
        Map<CooinAccount, BigDecimal> accountFromMovements = walletVSService.getAccountMovementsForTransaction(
                fromUserVS.IBAN, cooinBatch.getTagVS(), cooinBatch.getRequestAmount(), cooinBatch.getCurrencyCode())
        cooinBatch = csrService.signCooinBatchRequest(cooinBatch)
        TransactionVS userTransaction = cooinBatch.getTransactionVS(messageSource.getMessage(
                'cooinRequestLbl', null, locale), accountFromMovements).save()
        String message = messageSource.getMessage('withdrawalMsg', [cooinBatch.getRequestAmount().toString(),
                cooinBatch.getCurrencyCode()].toArray(), locale) + " " + systemService.getTagMessage(cooinBatch.getTag())

        Map resultMap = [statusCode: ResponseVS.SC_OK, message:message, issuedCooins:cooinBatch.getIssuedCooinListPEM()]
        return new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON, data:resultMap,
                type:TypeVS.COOIN_REQUEST);
    }

    @Transactional
    public ResponseVS checkBundleState(JSONArray hashCertVSArray) {
        List resultList = []
        hashCertVSArray.each {it ->
            Cooin cooin = Cooin.findWhere(hashCertVS:it)
            if(cooin) resultList.add([state:cooin.state.toString(), hashCertVS:it])
        }
        return new ResponseVS(statusCode: ResponseVS.SC_OK, contentType: ContentTypeVS.JSON, data:resultList);
    }

}