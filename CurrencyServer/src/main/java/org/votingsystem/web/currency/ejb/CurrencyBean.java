package org.votingsystem.web.currency.ejb;

import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.CurrencyIssuedDto;
import org.votingsystem.model.BatchRequest;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertExtensionCheckerVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.CurrencyExpendedException;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;
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
    private MessagesVS messages = MessagesVS.getCurrentInstance();


    public CurrencyBatchResponseDto processCurrencyTransaction(CurrencyBatch currencyBatch) throws Exception {
        CurrencyBatchResponseDto responseDto = new CurrencyBatchResponseDto();
        List<Currency> validatedCurrencyList = new ArrayList<>();
        Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", currencyBatch.getToUserIBAN());
        TagVS tagVS = config.getTag(currencyBatch.getTag());
        if(tagVS == null) throw new ExceptionVS(
                "CurrencyTransactionBatch:" + currencyBatch.getBatchUUID() + " missing TagVS");
        currencyBatch.setTagVS(tagVS);
        UserVS toUserVS = dao.getSingleResult(UserVS.class, query);
        currencyBatch.setToUserVS(toUserVS);
        if(toUserVS == null) throw new ExceptionVS("CurrencyTransactionBatch:" + currencyBatch.getBatchUUID() +
                " has wrong receptor IBAN '" + currencyBatch.getToUserIBAN());
        for(Currency currency : currencyBatch.getCurrencyList()) {
            validatedCurrencyList.add(validateCurrency(currency));
        }
        CurrencyBatchDto dto = new CurrencyBatchDto(currencyBatch);
        SMIMEMessage receipt = signatureBean.getSMIMETimeStamped(signatureBean.getSystemUser().getName(),
                currencyBatch.getBatchUUID(), JSON.getMapper().writeValueAsString(dto), currencyBatch.getSubject());
        MessageSMIME messageSMIME = new MessageSMIME(receipt, TypeVS.BATCH_RECEIPT);
        dao.persist(messageSMIME);
        dao.persist(currencyBatch.setMessageSMIME(messageSMIME).setState(BatchRequest.State.OK));
        log.info("currencyBatch:" + currencyBatch.getId() + " - messageSMIME:" + messageSMIME.getId());
        Date validTo = null;
        //TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        //if(currencyBatch.isTimeLimited == true) validTo = timePeriod.getDateTo()

        TransactionVS transactionVS = TransactionVS.CURRENCY_BATCH(currencyBatch, toUserVS, validTo, messageSMIME);
        dao.persist(transactionVS);

        for(Currency currency : validatedCurrencyList) {
            dao.merge(currency.setState(Currency.State.EXPENDED).setTransactionVS(transactionVS));
        }
        if(currencyBatch.getLeftOverCurrency() != null) {
            currencyBatch.getLeftOverCurrency().setTag(config.getTag(
                    currencyBatch.getLeftOverCurrency().getCertExtensionDto().getTag()));
            Currency leftOverCoin = csrBean.signCurrencyRequest(currencyBatch.getLeftOverCurrency());
            responseDto.setLeftOverCoin(new String(leftOverCoin.getIssuedCertPEM(), "UTF-8"));
        }
        responseDto.setReceipt(Base64.getEncoder().encodeToString(receipt.getBytes()));
        return responseDto;
    }

    public Currency validateCurrency(Currency currency) throws Exception {
        SMIMEMessage smimeMessage = currency.getSMIME();
        Query query = dao.getEM().createQuery("SELECT c FROM Currency c WHERE c.serialNumber =:serialNumber and c.hashCertVS =:hashCertVS")
                .setParameter("serialNumber", currency.getX509AnonymousCert().getSerialNumber().longValue())
                .setParameter("hashCertVS", currency.getHashCertVS());
        Currency currencyDB = dao.getSingleResult(Currency.class, query);
        if(currencyDB == null) throw new ExceptionVS("hashCertVSCurrencyInvalidErrorMsg - hashCertVS: " + currency.getHashCertVS());
        currency = currencyDB.checkRequestWithDB(currency);
        if(currency.getState() == Currency.State.EXPENDED) {
            throw new CurrencyExpendedException(currency.getHashCertVS());
        } else if(currency.getState() == Currency.State.OK) {
            UserVS userVS = smimeMessage.getSigner(); //anonymous signer
            timeStampBean.validateToken(userVS.getTimeStampToken());
            CertUtils.CertValidatorResultVS certValidatorResult = CertUtils.verifyCertificate(
                    signatureBean.getCurrencyAnchors(), false, Arrays.asList(userVS.getCertificate()));
            X509Certificate certCaResult = certValidatorResult.getResult().getTrustAnchor().getTrustedCert();
            CertExtensionCheckerVS extensionChecker = certValidatorResult.getChecker();
            //if (extensionChecker.isAnonymousSigner()) { }
        } else  throw new ExceptionVS(messages.get("currencyStateErrorMsg", currency.getId().toString(), currency.getState().toString()));
        currency.setAuthorityCertificateVS(signatureBean.getServerCertificateVS());
        return currency;
    }

    public CurrencyIssuedDto processCurrencyRequest(CurrencyRequestBatch currencyBatch) throws Exception {
        UserVS fromUserVS = currencyBatch.getMessageSMIME().getUserVS();
        //Check cash available for user
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                fromUserVS.getIBAN(), currencyBatch.getTagVS(), currencyBatch.getRequestAmount(), currencyBatch.getCurrencyCode());
        currencyBatch = csrBean.signCurrencyBatchRequest(currencyBatch);
        TransactionVS userTransaction = currencyBatch.getTransactionVS(messages.get("currencyRequestLbl"), accountFromMovements);
        dao.persist(userTransaction);
        String message = messages.get("withdrawalMsg", currencyBatch.getRequestAmount().toString(),
                currencyBatch.getCurrencyCode()) + " " + getTagMessage(currencyBatch.getTagVS().getName());
        CurrencyIssuedDto dto = new CurrencyIssuedDto(currencyBatch.getIssuedCurrencyListPEM(), message);
        SMIMEMessage receipt = signatureBean.getSMIMEMultiSigned(signatureBean.getSystemUser().getName(),
                fromUserVS.getNif(), currencyBatch.getMessageSMIME().getSMIME(), null);
        dao.merge(currencyBatch.getMessageSMIME().setSMIME(receipt).refresh());
        return dto;
    }

    public String getTagMessage(String tag) {
        if(TagVS.WILDTAG.equals(tag)) return messages.get("wildTagMsg");
        else return messages.get("tagMsg", tag);
    }

    public Map<String, Currency.State> checkBundleState(List<String> hashCertVSList) {
        Map<String, Currency.State> result = new HashMap<>();
        Query query = dao.getEM().createQuery("SELECT c FROM Currency c WHERE c.hashCertVS =:hashCertVS");
        for(String hashCertVS : hashCertVSList) {
            Currency currency = (Currency) query.setParameter("hashCertVS", hashCertVS).getSingleResult();
            if(currency != null) {
                result.put(hashCertVS, currency.getState());
            }
        }
        return result;
    }

}
