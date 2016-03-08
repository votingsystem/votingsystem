package org.votingsystem.web.currency.ejb;

import org.apache.commons.io.IOUtils;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.*;
import org.votingsystem.model.BatchVS;
import org.votingsystem.model.MessageCMS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.throwable.CurrencyExpendedException;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.Interval;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.crypto.CertUtils;
import org.votingsystem.util.crypto.PEMUtils;
import org.votingsystem.web.currency.util.ReportFiles;
import org.votingsystem.web.ejb.DAOBean;
import org.votingsystem.web.ejb.SignatureBean;
import org.votingsystem.web.ejb.TimeStampBean;
import org.votingsystem.web.util.ConfigVS;
import org.votingsystem.web.util.MessagesVS;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class CurrencyBean {

    private static Logger log = Logger.getLogger(CurrencyBean.class.getName());

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
        Set<Currency> validatedCurrencySet = new HashSet<>();
        CurrencyBatch currencyBatch = batchDto.validateRequest(new Date());
        currencyBatch.setTagVS(config.getTag(batchDto.getTag()));
        for(Currency currency : batchDto.getCurrencyList()) {
            validatedCurrencySet.add(validateBatchItem(currency));
        }
        Date validTo = null;
        if(currencyBatch.getTimeLimited() == true) {
            Interval timePeriod = DateUtils.getCurrentWeekPeriod();
            validTo = timePeriod.getDateTo();
        }
        if(batchDto.getOperation() == TypeVS.CURRENCY_CHANGE) {
            //return processAnonymousCurrencyBatch(batchDto, currencyBatch, validatedCurrencySet, validTo);
            Currency leftOver = null;
            if(batchDto.getLeftOverPKCS10() != null) {
                leftOver = csrBean.signCurrencyRequest(batchDto.getLeftOverPKCS10(), Currency.Type.LEFT_OVER, currencyBatch);
            }
            Currency currencyChange = csrBean.signCurrencyRequest(batchDto.getCurrencyChangePKCS10(), Currency.Type.CHANGE,
                    currencyBatch);
            CMSSignedMessage receipt = signatureBean.signDataWithTimeStamp(JSON.getMapper().writeValueAsString(batchDto));
            MessageCMS messageCMS =  dao.persist(new MessageCMS(receipt, TypeVS.BATCH_RECEIPT));
            currencyBatch.setLeftOver(leftOver);
            currencyBatch.setCurrencyChange(currencyChange);
            currencyBatch.setValidatedCurrencySet(validatedCurrencySet);
            dao.persist(currencyBatch.setMessageCMS(messageCMS).setState(BatchVS.State.OK));
            TransactionVS transactionVS = dao.persist(TransactionVS.CURRENCY_CHANGE(
                    currencyBatch, validTo, messageCMS, currencyBatch.getTagVS()));
            transactionVSBean.updateCurrencyAccounts(transactionVS.setCurrencyBatch(currencyBatch));
            for(Currency currency : validatedCurrencySet) {
                dao.merge(currency.setState(Currency.State.EXPENDED).setTransactionVS(transactionVS));
            }
            return new CurrencyBatchResponseDto(receipt, new String(PEMUtils.getPEMEncoded(leftOver.getX509AnonymousCert())),
                    new String(PEMUtils.getPEMEncoded(currencyChange.getX509AnonymousCert())));
        } else {
            Query query = dao.getEM().createNamedQuery("findUserByIBAN").setParameter("IBAN", batchDto.getToUserIBAN());
            UserVS toUserVS = dao.getSingleResult(UserVS.class, query);
            currencyBatch.setToUserVS(toUserVS);
            if(toUserVS == null) throw new ExceptionVS("CurrencyTransactionBatch:" + currencyBatch.getBatchUUID() +
                    " has wrong receptor IBAN '" + batchDto.getToUserIBAN());
            String leftOverCert = null;
            if(batchDto.getLeftOverPKCS10() != null) {
                Currency leftOver = csrBean.signCurrencyRequest(batchDto.getLeftOverPKCS10(), Currency.Type.LEFT_OVER,
                        currencyBatch);
                currencyBatch.setLeftOver(leftOver);
                leftOverCert = new String(PEMUtils.getPEMEncoded(leftOver.getX509AnonymousCert()));
            }
            CMSSignedMessage receipt = signatureBean.signDataWithTimeStamp(JSON.getMapper().writeValueAsString(batchDto));
            MessageCMS messageCMS =  dao.persist(new MessageCMS(receipt, TypeVS.BATCH_RECEIPT));
            dao.persist(currencyBatch.setMessageCMS(messageCMS).setState(BatchVS.State.OK));
            log.info("currencyBatch:" + currencyBatch.getId() + " - messageCMS:" + messageCMS.getId());
            TransactionVS transactionVS = dao.persist(TransactionVS.CURRENCY_SEND(
                    currencyBatch, toUserVS, validTo, messageCMS, currencyBatch.getTagVS()));
            currencyBatch.setValidatedCurrencySet(validatedCurrencySet);
            transactionVSBean.updateCurrencyAccounts(transactionVS.setCurrencyBatch(currencyBatch));
            for(Currency currency : validatedCurrencySet) {
                dao.merge(currency.setState(Currency.State.EXPENDED).setTransactionVS(transactionVS));
            }
            CurrencyBatchResponseDto responseDto = new CurrencyBatchResponseDto(receipt, leftOverCert);
            responseDto.setMessage(messages.get("currencyBatchOKMsg", batchDto.getBatchAmount() + " " +
                    batchDto.getCurrencyCode(), toUserVS.getFullName()));
            return responseDto;
        }
    }

    public Currency validateBatchItem(Currency currency) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        CMSSignedMessage cmsMessage = currency.getCMS();
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
            UserVS userVS = cmsMessage.getSigner(); //anonymous signer
            timeStampBean.validateToken(userVS.getTimeStampToken());
            PKIXCertPathValidatorResult certValidatorResult = CertUtils.verifyCertificate(
                    signatureBean.getCurrencyAnchors(), false, Arrays.asList(userVS.getCertificate()));
            X509Certificate certCaResult = certValidatorResult.getTrustAnchor().getTrustedCert();
        } else  throw new ExceptionVS(messages.get("currencyStateErrorMsg", currencyDB.getId().toString(),
                currencyDB.getState().toString()));
        currency.setAuthorityCertificateVS(signatureBean.getServerCertificateVS());
        return currency;
    }

    public ResultListDto<String> processCurrencyRequest(CurrencyRequestDto requestDto) throws Exception {
        MessagesVS messages = MessagesVS.getCurrentInstance();
        UserVS fromUserVS = requestDto.getMessageCMS().getUserVS();
        //check cash available for user
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                fromUserVS.getIBAN(), requestDto.getTagVS(), requestDto.getTotalAmount(), requestDto.getCurrencyCode());
        Set<String> currencyCertSet = csrBean.signCurrencyRequest(requestDto);
        TransactionVS userTransaction = TransactionVS.CURRENCY_REQUEST(messages.get("currencyRequestLbl"),
                accountFromMovements, requestDto);
        transactionVSBean.updateCurrencyAccounts(userTransaction);
        ResultListDto resultListDto = new ResultListDto(currencyCertSet);
        resultListDto.setMessage(messages.get("withdrawalMsg", requestDto.getTotalAmount().toString(),
                requestDto.getCurrencyCode()) + " " + messages.getTagMessage(requestDto.getTagVS().getName()));
        CMSSignedMessage receipt = signatureBean.addSignature(requestDto.getMessageCMS().getCMS());
        dao.merge(requestDto.getMessageCMS().setType(TypeVS.CURRENCY_REQUEST).setCMS(receipt));
        return resultListDto;
    }

    public Set<CurrencyStateDto> checkBundleState(Set<String> hashSet) throws Exception {
        Set<CurrencyStateDto> result = new HashSet<>();
        Query query = dao.getEM().createQuery("SELECT c FROM Currency c WHERE c.hashCertVS =:hashCertVS");
        for(String hashCertVS : hashSet) {
            query.setParameter("hashCertVS", hashCertVS);
            Currency currency = dao.getSingleResult(Currency.class, query);
            if(currency != null) {
                CurrencyStateDto currencyStateDto = new CurrencyStateDto(currency);
                if(currency.getCurrencyBatch() != null) {
                    query = dao.getEM().createQuery("select c from Currency c where c.currencyBatch =:currencyBatch")
                            .setParameter("currencyBatch", currency.getCurrencyBatch());
                    try {
                        currencyStateDto.setBatchResponseCerts(query.getResultList());
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                }
                result.add(currencyStateDto);
            } else result.add(new CurrencyStateDto(hashCertVS, Currency.State.UNKNOWN));
        }
        return result;
    }

    public CurrencyPeriodResultDto createPeriodBackup(Interval timePeriod) throws IOException {
        ReportFiles reportFiles = ReportFiles.CURRENCY_PERIOD(timePeriod, config.getServerDir().getAbsolutePath());
        if(reportFiles.getJsonFile().exists())
                return JSON.getMapper().readValue(reportFiles.getJsonFile(), CurrencyPeriodResultDto.class);

        Map<String, Map<String, IncomesDto>> leftOverMap = new HashMap<>();
        Map<String, Map<String, IncomesDto>> changeMap = new HashMap<>();
        Map<String, Map<String, IncomesDto>> requestMap = new HashMap<>();

        Set<String> leftOverSet = new HashSet<>();
        Set<String> changeSet = new HashSet<>();
        Set<String> requestSet = new HashSet<>();
        File currencyRequestDir = new File(reportFiles.getBaseDir().getAbsolutePath() + "/request");
        File currencyBatchDir = new File(reportFiles.getBaseDir().getAbsolutePath() + "/batch");

        Query query = dao.getEM().createQuery("SELECT m FROM MessageCMS m WHERE m.type =:typeVS")
                .setParameter("typeVS",  TypeVS.CURRENCY_REQUEST);
        List<MessageCMS> resultList = query.getResultList();
        for(MessageCMS messageCMS : resultList) {
            File cmsFile = new File(format("{0}/messageCMS_{1}.p7m", currencyRequestDir.getAbsolutePath(), messageCMS.getId()));
            IOUtils.write(messageCMS.getContent(), new FileOutputStream(cmsFile));
        }
        query = dao.getEM().createQuery("SELECT c FROM CurrencyBatch c WHERE c.state =:state and c.type =:typeVS")
                .setParameter("typeVS",  TypeVS.CURRENCY_SEND).setParameter("state", BatchVS.State.OK);
        List<CurrencyBatch> currencyBatchList = query.getResultList();
        for(CurrencyBatch currencyBatch : currencyBatchList) {
            File cmsFile = new File(format("{0}/CURRENCY_SEND_messageCMS_{1}.p7m", currencyBatchDir.getAbsolutePath(),
                    currencyBatch.getMessageCMS().getId()));
            IOUtils.write(currencyBatch.getMessageCMS().getContent(), new FileOutputStream(cmsFile));
        }

        query = dao.getEM().createQuery("SELECT c FROM CurrencyBatch c WHERE c.state =:state and c.type =:typeVS")
                .setParameter("typeVS",  TypeVS.CURRENCY_CHANGE).setParameter("state", BatchVS.State.OK);
        currencyBatchList = query.getResultList();
        for(CurrencyBatch currencyBatch : currencyBatchList) {
            File cmsFile = new File(format("{0}/CURRENCY_CHANGE_messageCMS_{1}.p7m", currencyBatchDir.getAbsolutePath(),
                    currencyBatch.getMessageCMS().getId()));
            IOUtils.write(currencyBatch.getMessageCMS().getContent(), new FileOutputStream(cmsFile));
        }


        //TODO


        Set<X509Certificate> systemTrustedCerts = signatureBean.getTrustedCerts();
        File systemTrustedCertsFile = new File(format("{0}/systemTrustedCerts.pem", reportFiles.getBaseDir().getAbsolutePath()));
        IOUtils.write(PEMUtils.getPEMEncoded(systemTrustedCerts), new FileOutputStream(systemTrustedCertsFile));


        CurrencyPeriodResultDto currencyPeriodResultDto = new CurrencyPeriodResultDto();
        currencyPeriodResultDto.setLeftOverMap(leftOverMap);
        currencyPeriodResultDto.setChangeMap(changeMap);
        currencyPeriodResultDto.setRequestMap(requestMap);
        currencyPeriodResultDto.setLeftOverSet(leftOverSet);
        currencyPeriodResultDto.setChangeSet(changeSet);
        currencyPeriodResultDto.setRequestSet(requestSet);
        return currencyPeriodResultDto;
    }

}