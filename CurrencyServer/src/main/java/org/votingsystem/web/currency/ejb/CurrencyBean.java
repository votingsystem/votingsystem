package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.model.BatchVS;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertExtensionCheckerVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.CurrencyExpendedException;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class CurrencyBean {

    private static Logger log = Logger.getLogger(CurrencyBean.class.getSimpleName());

    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject TransactionVSBean transactionVSBean;
    @Inject SignatureBean signatureBean;
    @Inject UserVSBean userVSBean;
    @Inject CSRBean csrBean;
    @Inject WalletBean walletBean;
    @Inject TimeStampBean timeStampBean;


    public CurrencyBatchResponseDto processCurrencyBatch(CurrencyBatchDto batchDto) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        List<Currency> validatedCurrencyList = new ArrayList<>();
        CurrencyBatch currencyBatch = batchDto.validateRequest(new Date());
        currencyBatch.setTagVS(config.getTag(batchDto.getTag()));
        for(Currency currency : batchDto.getCurrencyList()) {
            validatedCurrencyList.add(validateBatchItem(currency));
        }
        Date validTo = null;
        if(currencyBatch.getTimeLimited() == true) {
            TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
            validTo = timePeriod.getDateTo();
        }
        if(batchDto.getOperation() == TypeVS.CURRENCY_CHANGE) {
            return processAnonymousCurrencyBatch(batchDto, currencyBatch, validatedCurrencyList, validTo);
        }
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", batchDto.getToUserIBAN());
        UserVS toUserVS = dao.getSingleResult(UserVS.class, query);
        currencyBatch.setToUserVS(toUserVS);
        if(toUserVS == null) throw new ExceptionVS("CurrencyTransactionBatch:" + currencyBatch.getBatchUUID() +
                " has wrong receptor IBAN '" + batchDto.getToUserIBAN());
        String leftOverCert = null;
        if(batchDto.getLeftOverPKCS10() != null) {
            Currency leftOver = csrBean.signCurrencyRequest(batchDto.getLeftOverPKCS10(), Currency.Type.LEFT_OVER,
                    currencyBatch.getBatchUUID(), currencyBatch.getTagVS());
            currencyBatch.setLeftOver(leftOver);
            leftOverCert = new String(CertUtils.getPEMEncoded(leftOver.getX509AnonymousCert()));
        }
        SMIMEMessage receipt = signatureBean.getSMIMETimeStamped(signatureBean.getSystemUser().getName(),
                currencyBatch.getBatchUUID(), JSON.getMapper().writeValueAsString(batchDto),
                currencyBatch.getSubject());
        receipt.setHeader("TypeVS", batchDto.getOperation().toString());
        MessageSMIME messageSMIME =  dao.persist(new MessageSMIME(receipt, TypeVS.BATCH_RECEIPT));
        dao.persist(currencyBatch.setMessageSMIME(messageSMIME).setState(BatchVS.State.OK));
        log.info("currencyBatch:" + currencyBatch.getId() + " - messageSMIME:" + messageSMIME.getId());
        TransactionVS transactionVS = dao.persist(TransactionVS.CURRENCY_SEND(
                currencyBatch, toUserVS, validTo, messageSMIME, currencyBatch.getTagVS()));
        transactionVSBean.newTransactionVS(transactionVS);
        for(Currency currency : validatedCurrencyList) {
            dao.merge(currency.setState(Currency.State.EXPENDED).setTransactionVS(transactionVS));
        }
        CurrencyBatchResponseDto responseDto = new CurrencyBatchResponseDto(receipt, leftOverCert);
        responseDto.setMessage(messages.get("currencyBatchOKMsg", batchDto.getBatchAmount() + " " +
                batchDto.getCurrencyCode(), toUserVS.getFullName()));
        return responseDto;
    }

    public CurrencyBatchResponseDto processAnonymousCurrencyBatch(CurrencyBatchDto batchDto,
                  CurrencyBatch currencyBatch, List<Currency> validatedCurrencyList, Date validTo) throws Exception {
        Currency leftOver = null;
        if(batchDto.getLeftOverPKCS10() != null) {
            leftOver = csrBean.signCurrencyRequest(batchDto.getLeftOverPKCS10(), Currency.Type.LEFT_OVER,
                    batchDto.getBatchUUID(), currencyBatch.getTagVS());
        }
        Currency currencyChange = csrBean.signCurrencyRequest(batchDto.getCurrencyChangePKCS10(), Currency.Type.CHANGE,
                batchDto.getBatchUUID(), currencyBatch.getTagVS());
        SMIMEMessage receipt = signatureBean.getSMIMETimeStamped(signatureBean.getSystemUser().getName(),
                currencyBatch.getBatchUUID(), JSON.getMapper().writeValueAsString(batchDto),
                currencyBatch.getSubject());
        receipt.setHeader("TypeVS", batchDto.getOperation().toString());
        MessageSMIME messageSMIME =  dao.persist(new MessageSMIME(receipt, TypeVS.BATCH_RECEIPT));
        currencyBatch.setLeftOver(leftOver);
        currencyBatch.setCurrencyChange(currencyChange);
        dao.persist(currencyBatch.setMessageSMIME(messageSMIME).setState(BatchVS.State.OK));
        TransactionVS transactionVS = dao.persist(TransactionVS.CURRENCY_CHANGE(
                currencyBatch, validTo, messageSMIME, currencyBatch.getTagVS()));
        transactionVSBean.newTransactionVS(transactionVS);
        for(Currency currency : validatedCurrencyList) {
            dao.merge(currency.setState(Currency.State.EXPENDED).setTransactionVS(transactionVS).setBatchUUID(
                    currencyBatch.getBatchUUID()));
        }
        return new CurrencyBatchResponseDto(receipt, new String(CertUtils.getPEMEncoded(leftOver.getX509AnonymousCert())),
                new String(CertUtils.getPEMEncoded(currencyChange.getX509AnonymousCert())));
    }

    public Currency validateBatchItem(Currency currency) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        SMIMEMessage smimeMessage = currency.getSMIME();
        Query query = dao.getEM().createQuery("SELECT c FROM Currency c WHERE c.serialNumber =:serialNumber and c.hashCertVS =:hashCertVS")
                .setParameter("serialNumber", currency.getX509AnonymousCert().getSerialNumber().longValue())
                .setParameter("hashCertVS", currency.getHashCertVS());
        Currency currencyDB = dao.getSingleResult(Currency.class, query);
        if(currencyDB == null) throw new ExceptionVS(
                messages.get("hashCertVSCurrencyInvalidErrorMsg", currency.getHashCertVS()));
        if(currencyDB.getState() == Currency.State.EXPENDED) {
            throw new CurrencyExpendedException(currency.getHashCertVS());
        } else if(currencyDB.getState() == Currency.State.OK) {
            currency = currencyDB.checkRequestWithDB(currency);
            UserVS userVS = smimeMessage.getSigner(); //anonymous signer
            timeStampBean.validateToken(userVS.getTimeStampToken());
            CertUtils.CertValidatorResultVS certValidatorResult = CertUtils.verifyCertificate(
                    signatureBean.getCurrencyAnchors(), false, Arrays.asList(userVS.getCertificate()));
            X509Certificate certCaResult = certValidatorResult.getResult().getTrustAnchor().getTrustedCert();
            CertExtensionCheckerVS extensionChecker = certValidatorResult.getChecker();
            //if (extensionChecker.isAnonymousSigner()) { }
        } else  throw new ExceptionVS(messages.get("currencyStateErrorMsg", currencyDB.getId().toString(),
                currencyDB.getState().toString()));
        currency.setAuthorityCertificateVS(signatureBean.getServerCertificateVS());
        return currency;
    }

    public ResultListDto<String> processCurrencyRequest(CurrencyRequestDto requestDto) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS fromUserVS = requestDto.getMessageSMIME().getUserVS();
        //check cash available for user
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                fromUserVS.getIBAN(), requestDto.getTagVS(), requestDto.getTotalAmount(), requestDto.getCurrencyCode());
        Set<String> currencyCertSet = csrBean.signCurrencyRequest(requestDto);
        TransactionVS userTransaction = requestDto.getTransactionVS(messages.get("currencyRequestLbl"), accountFromMovements);
        transactionVSBean.newTransactionVS(userTransaction);
        ResultListDto resultListDto = new ResultListDto(currencyCertSet);
        resultListDto.setMessage(messages.get("withdrawalMsg", requestDto.getTotalAmount().toString(),
                requestDto.getCurrencyCode()) + " " + messages.getTagMessage(requestDto.getTagVS().getName()));
        SMIMEMessage receipt = signatureBean.getSMIMEMultiSigned(signatureBean.getSystemUser().getName(),
                fromUserVS.getNif(), requestDto.getMessageSMIME().getSMIME(), null);
        dao.merge(requestDto.getMessageSMIME().setSMIME(receipt).refresh());
        return resultListDto;
    }

    public Map<String, Currency.State> checkBundleState(List<String> hashCertVSList) {
        Map<String, Currency.State> result = new HashMap<>();
        Query query = dao.getEM().createQuery("SELECT c FROM Currency c WHERE c.hashCertVS =:hashCertVS");
        for(String hashCertVS : hashCertVSList) {
            query.setParameter("hashCertVS", hashCertVS);
            Currency currency = dao.getSingleResult(Currency.class, query);
            if(currency != null) {
                result.put(hashCertVS, currency.getState());
            } else result.put(hashCertVS, Currency.State.UNKNOWN);
        }
        return result;
    }

}