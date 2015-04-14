package org.votingsystem.web.currency.ejb;

import org.bouncycastle.tsp.TSPException;
import org.votingsystem.model.*;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertExtensionCheckerVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.CurrencyExpendedException;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.TimePeriod;
import org.votingsystem.util.TypeVS;
import org.votingsystem.web.cdi.ConfigVS;
import org.votingsystem.web.cdi.MessagesBean;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
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

    @PersistenceContext EntityManager em;
    @Inject ConfigVS config;
    @Inject DAOBean dao;
    @Inject TransactionVSBean transactionVSBean;
    @Inject SignatureBean signatureBean;
    @Inject UserVSBean userVSBean;
    @Inject CSRBean csrBean;
    @Inject WalletBean walletBean;
    @Inject TimeStampBean timeStampBean;
    @Inject MessagesBean messages;


    public Map processCurrencyTransaction(CurrencyTransactionBatch currencyBatch) throws Exception {
        Map dataMap = new HashMap<>();
        List<Currency> validatedCurrencyList = new ArrayList<>();
        Query query = em.createNamedQuery("findUserByIBAN").setParameter("IBAN", currencyBatch.getToUserIBAN());
        UserVS toUserVS = dao.getSingleResult(UserVS.class, query);
        currencyBatch.setToUserVS(toUserVS);
        TagVS tagVS = config.getTag(currencyBatch.getTag());
        if(tagVS == null) throw new ExceptionVS(
                "CurrencyTransactionBatch:" + currencyBatch.getBatchUUID() + " missing TagVS");
        currencyBatch.setTagVS(tagVS);
        if(toUserVS == null) throw new ExceptionVS("CurrencyTransactionBatch:" + currencyBatch.getBatchUUID() +
                " has wrong receptor IBAN '" + currencyBatch.getToUserIBAN());
        for(Currency currency : currencyBatch.getCurrencyList()) {
            validatedCurrencyList.add(validateCurrency(currency));
        }
        Map batchDataMap = currencyBatch.getDataMap();
        SMIMEMessage receipt = signatureBean.getSMIMETimeStamped(signatureBean.getSystemUser().getName(),
                currencyBatch.getBatchUUID(), batchDataMap.toString(), currencyBatch.getSubject());
        MessageSMIME messageSMIME = new MessageSMIME(receipt, TypeVS.RECEIPT);
        em.persist(messageSMIME);
        em.persist(currencyBatch.setMessageSMIME(messageSMIME).setState(BatchRequest.State.OK));
        log.info("currencyBatch:" + currencyBatch.getId() + " - messageSMIME:" + messageSMIME.getId());
        Date validTo = null;
        //TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        //if(currencyBatch.isTimeLimited == true) validTo = timePeriod.getDateTo()

        TransactionVS transactionVS = TransactionVS.CURRENCY_BATCH(currencyBatch, toUserVS, validTo, messageSMIME);
        em.persist(transactionVS);

        for(Currency currency : validatedCurrencyList) {
            em.merge(currency.setState(Currency.State.EXPENDED).setTransactionVS(transactionVS));
        }
        if(currencyBatch.getLeftOverCurrency() != null) {
            currencyBatch.getLeftOverCurrency().setTag(config.getTag(currencyBatch.getLeftOverCurrency().getCertTagVS()));
            Currency leftOverCoin = csrBean.signCurrencyRequest(currencyBatch.getLeftOverCurrency());
            dataMap.put("leftOverCoin", new String(leftOverCoin.getIssuedCertPEM(), "UTF-8"));
        }
        dataMap.put("receipt", Base64.getEncoder().encodeToString(receipt.getBytes()));
        //return new ResponseVS(statusCode:ResponseVS.SC_OK, contentType: ContentTypeVS.JSON, data: dataMap)
        return dataMap;
    }

    public Currency validateCurrency(Currency currency) throws ExceptionVS, TSPException {
        SMIMEMessage smimeMessage = currency.getSMIME();
        Query query = em.createNamedQuery("findCryptocurrBySerialNumberAndHashCert")
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

    public Map processCurrencyRequest(CurrencyRequestBatch currencyBatch) throws Exception {
        UserVS fromUserVS = currencyBatch.getMessageSMIME().getUserVS();
        TimePeriod timePeriod = DateUtils.getWeekPeriod(Calendar.getInstance());
        //Check cash available for user
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                fromUserVS.getIBAN(), currencyBatch.getTagVS(), currencyBatch.getRequestAmount(), currencyBatch.getCurrencyCode());
        currencyBatch = csrBean.signCurrencyBatchRequest(currencyBatch);
        TransactionVS userTransaction = currencyBatch.getTransactionVS(messages.get("currencyRequestLbl"), accountFromMovements);
        em.persist(userTransaction);
        String message = messages.get("withdrawalMsg", currencyBatch.getRequestAmount().toString(),
                currencyBatch.getCurrencyCode()) + " " + messages.getTagMessage(currencyBatch.getTag());
        Map dataMap = new HashMap<>();
        dataMap.put("statusCode", ResponseVS.SC_OK);
        dataMap.put("message", message);
        dataMap.put("issuedCurrency", currencyBatch.getIssuedCurrencyListPEM());
        SMIMEMessage receipt = signatureBean.getSMIMEMultiSigned(signatureBean.getSystemUser().getName(),
                fromUserVS.getNif(), currencyBatch.getMessageSMIME().getSMIME(), null);
        em.merge(currencyBatch.getMessageSMIME().setSMIME(receipt).refresh());
        return dataMap;
    }

    public List<Map> checkBundleState(List<String> hashCertVSList) {
        List<Map> resultList = new ArrayList<>();
        Query query = em.createNamedQuery("findCryptocurrByHashCert");
        for(String hashCertVS : hashCertVSList) {
            Map dataMap = new HashMap<>();
            Currency currency = (Currency) query.setParameter("hashCertVS", hashCertVS).getSingleResult();
            if(currency != null) {
                dataMap.put("hashCertVS", hashCertVS);
                dataMap.put("state", currency.getState().toString());
                resultList.add(dataMap);
            }
        }
        return resultList;
    }

}
