package org.currency.web.ejb;

import org.apache.commons.io.IOUtils;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.*;
import org.votingsystem.util.*;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.SignedDocumentType;
import org.currency.web.util.ReportFiles;
import org.votingsystem.dto.currency.*;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.text.MessageFormat.format;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Stateless
public class CurrencyEJB {

    private static Logger log = Logger.getLogger(CurrencyEJB.class.getName());

    @PersistenceContext
    private EntityManager em;
    @Inject private ConfigCurrencyServer config;
    @Inject private TransactionEJB transactionBean;
    @Inject private SignatureService signatureService;
    @Inject private UserEJB userBean;
    @Inject private CurrencyIssuerEJB csrBean;
    @Inject private WalletEJB walletBean;

    public CurrencyBatchResponseDto processCurrencyBatch(SignedDocument signedDocument) throws Exception {
        CurrencyBatchDto batchDto = signedDocument.getSignedContent(CurrencyBatchDto.class);
        Set<Currency> validatedCurrencySet = new HashSet<>();
        CurrencyBatch currencyBatch = batchDto.validateRequest(LocalDateTime.now());
        currencyBatch.setTag(config.getTag(batchDto.getTag()));
        for(Currency currency : batchDto.getCurrencyList()) {
            validatedCurrencySet.add(validateBatchItem(currency));
        }
        ZonedDateTime validTo = null;
        if(currencyBatch.getTimeLimited() == true) {
            Interval timePeriod = DateUtils.getCurrentWeekPeriod();
            validTo = timePeriod.getDateTo();
        }
        if(batchDto.getOperation() == CurrencyOperation.CURRENCY_CHANGE) {
            //return processAnonymousCurrencyBatch(batchDto, currencyBatch, validatedCurrencySet, validTo);
            Currency leftOver = null;
            if(batchDto.getLeftOverPKCS10() != null) {
                leftOver = csrBean.signCurrencyRequest(batchDto.getLeftOverPKCS10(), Currency.Type.LEFT_OVER, currencyBatch);
            }
            Currency currencyChange = csrBean.signCurrencyRequest(batchDto.getCurrencyChangePKCS10(),
                    Currency.Type.CHANGE, currencyBatch);

            SignatureParams signatureParams = new SignatureParams(config.getEntityId(), User.Type.CURRENCY_SERVER,
                    SignedDocumentType.CURRENCY_CHANGE_RECEIPT).setWithTimeStampValidation(false);
            SignedDocument receipt = signatureService.signXAdESAndSave(signedDocument.getBody().getBytes(), signatureParams);

            currencyBatch.setLeftOver(leftOver);
            currencyBatch.setCurrencyChange(currencyChange);
            currencyBatch.setValidatedCurrencySet(validatedCurrencySet);
            em.persist(currencyBatch.setSignedDocument(receipt).setState(CurrencyBatch.State.OK));
            Transaction transaction = Transaction.CURRENCY_CHANGE(currencyBatch, validTo.toLocalDateTime(), receipt,
                    currencyBatch.getTag());
            em.persist(transaction);
            transactionBean.updateCurrencyAccounts(transaction.setCurrencyBatch(currencyBatch));
            for(Currency currency : validatedCurrencySet) {
                em.merge(currency.setState(Currency.State.EXPENDED).setTransaction(transaction));
            }
            return new CurrencyBatchResponseDto(receipt.getBody().getBytes(),
                    new String(PEMUtils.getPEMEncoded(leftOver.getX509AnonymousCert())),
                    new String(PEMUtils.getPEMEncoded(currencyChange.getX509AnonymousCert())));
        } else {
            List<User> userList = em.createNamedQuery(User.FIND_USER_BY_IBAN)
                    .setParameter("IBAN", batchDto.getToUserIBAN()).getResultList();
            if(userList.isEmpty()) throw new ValidationException("CurrencyTransactionBatch:" + currencyBatch.getBatchUUID() +
                    " has wrong receptor IBAN '" + batchDto.getToUserIBAN());
            currencyBatch.setToUser(userList.iterator().next());
            String leftOverCert = null;
            if(batchDto.getLeftOverPKCS10() != null) {
                Currency leftOver = csrBean.signCurrencyRequest(batchDto.getLeftOverPKCS10(), Currency.Type.LEFT_OVER,
                        currencyBatch);
                currencyBatch.setLeftOver(leftOver);
                leftOverCert = new String(PEMUtils.getPEMEncoded(leftOver.getX509AnonymousCert()));
            }
            SignatureParams signatureParams = new SignatureParams(config.getEntityId(), User.Type.CURRENCY_SERVER,
                    SignedDocumentType.CURRENCY_CHANGE_RECEIPT).setWithTimeStampValidation(false);
            SignedDocument receipt = signatureService.signXAdESAndSave(signedDocument.getBody().getBytes(), signatureParams);
            em.persist(currencyBatch.setSignedDocument(receipt).setState(CurrencyBatch.State.OK));
            log.info("currencyBatch:" + currencyBatch.getId() + " - receipt:" + receipt.getId());
            Transaction transaction = Transaction.CURRENCY_SEND(currencyBatch, currencyBatch.getToUser(),
                    validTo.toLocalDateTime(), receipt, currencyBatch.getTag());
            em.persist(transactionBean);
            currencyBatch.setValidatedCurrencySet(validatedCurrencySet);
            transactionBean.updateCurrencyAccounts(transaction.setCurrencyBatch(currencyBatch));
            for(Currency currency : validatedCurrencySet) {
                em.merge(currency.setState(Currency.State.EXPENDED).setTransaction(transaction));
            }
            CurrencyBatchResponseDto responseDto = new CurrencyBatchResponseDto(receipt.getBody().getBytes(), leftOverCert);
            responseDto.setMessage(Messages.currentInstance().get("currencyBatchOKMsg", batchDto.getBatchAmount() + " " +
                    batchDto.getCurrencyCode(), currencyBatch.getToUser().getFullName()));
            return responseDto;
        }
    }

    public Currency validateBatchItem(Currency currency) throws Exception {
        log.info("======= TODO");
        /*CMSSignedMessage cmsMessage = currency.getSignedDocument();
        List<Currency> currencyList = em.createQuery(
                "SELECT c FROM Currency c WHERE c.serialNumber =:serialNumber and c.revocationHashBase64=:revocationHashBase64 and c.state=:state")
                .setParameter("serialNumber", currency.getX509AnonymousCert().getSerialNumber().longValue())
                .setParameter("state", Currency.State.OK)
                .setParameter("revocationHashBase64", currency.getRevocationHash()).getResultList();
        if(currencyList.isEmpty()) throw new ValidationException(
                Messages.currentInstance().get("currencyRevocationHashInvalidErrorMsg", currency.getRevocationHash()));
        Currency currencyDB = currencyList.iterator().next();
        currency = currencyDB.checkRequestWithDB(currency);
        User user = cmsMessage.getSigner(); //anonymous signer
        timeStampBean.validateToken(user.getTimeStampToken());
        PKIXCertPathValidatorResult certValidatorResult = CertUtils.verifyCertificate(
                cmsBean.getCurrencyAnchors(), false, Arrays.asList(user.getX509Certificate()));
        X509Certificate certCaResult = certValidatorResult.getTrustAnchor().getTrustedCert();
        currency.setAuthorityCertificate(cmsBean.getServerCertificate());*/
        return currency;
    }

    public ResultListDto<String> processCurrencyRequest(CurrencyRequestDto requestDto) throws Exception {
        User fromUser = requestDto.getSignedDocument().getFirstSignature().getSigner();
        //check cash available for user
        Map<CurrencyAccount, BigDecimal> accountFromMovements = walletBean.getAccountMovementsForTransaction(
                fromUser.getIBAN(), requestDto.getTag(), requestDto.getTotalAmount(), requestDto.getCurrencyCode());
        Set<String> currencyCertSet = csrBean.signCurrencyRequest(requestDto);
        Transaction userTransaction = Transaction.CURRENCY_REQUEST(Messages.currentInstance().get("currencyRequestLbl"),
                accountFromMovements, requestDto, fromUser);
        transactionBean.updateCurrencyAccounts(userTransaction);
        ResultListDto resultListDto = new ResultListDto(currencyCertSet);
        resultListDto.setMessage(Messages.currentInstance().get("withdrawalMsg", requestDto.getTotalAmount().toString(),
                requestDto.getCurrencyCode()) + " " + Messages.currentInstance().getTagMessage(requestDto.getTag().getName()));
        /*CMSSignedMessage receipt = cmsBean.addSignature(requestDto.getCmsMessage().getCMS());
        em.merge(requestDto.getCmsMessage().setType(TypeVS.CURRENCY_REQUEST).setCMS(receipt));*/
        return resultListDto;
    }

    public Set<CurrencyStateDto> checkBundleState(Set<String> revocationHashSet) throws Exception {
        Set<CurrencyStateDto> result = new HashSet<>();
        Query query = em.createQuery("SELECT c FROM Currency c WHERE c.revocationHashBase64 =:revocationHashBase64");
        for(String revocationHash : revocationHashSet) {
            List<Currency> currencyList = query.setParameter("revocationHashBase64", revocationHash).getResultList();
            if(!currencyList.isEmpty()) {
                Currency currency = currencyList.iterator().next();
                CurrencyStateDto currencyStateDto = new CurrencyStateDto(currency);
                if(currency.getCurrencyBatch() != null) {
                    query = em.createQuery("select c from Currency c where c.currencyBatch =:currencyBatch")
                            .setParameter("currencyBatch", currency.getCurrencyBatch());
                    try {
                        currencyStateDto.setBatchResponseCerts(query.getResultList());
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                }
                result.add(currencyStateDto);
            } else result.add(new CurrencyStateDto(revocationHash, Currency.State.UNKNOWN));
        }
        return result;
    }

    public CurrencyPeriodResultDto createPeriodBackup(Interval timePeriod) throws IOException {
        ReportFiles reportFiles = ReportFiles.CURRENCY_PERIOD(timePeriod, config.getApplicationDataPath());
        if(reportFiles.getReportFile().exists())
                return JSON.getMapper().readValue(reportFiles.getReportFile(), CurrencyPeriodResultDto.class);

        Map<String, Map<String, IncomesDto>> leftOverMap = new HashMap<>();
        Map<String, Map<String, IncomesDto>> changeMap = new HashMap<>();
        Map<String, Map<String, IncomesDto>> requestMap = new HashMap<>();

        Set<String> leftOverSet = new HashSet<>();
        Set<String> changeSet = new HashSet<>();
        Set<String> requestSet = new HashSet<>();
        File currencyRequestDir = new File(reportFiles.getBaseDir().getAbsolutePath() + "/request");
        File currencyBatchDir = new File(reportFiles.getBaseDir().getAbsolutePath() + "/batch");

        List<SignedDocument> resultList = em.createQuery(
                "SELECT s FROM SignedDocument s WHERE s.signedDocumentType =:documentType")
                .setParameter("documentType",  SignedDocumentType.CURRENCY_REQUEST).getResultList();

        for(SignedDocument signedDocument : resultList) {
            File cmsFile = new File(format("{0}/currency-request_{1}.xml", currencyRequestDir.getAbsolutePath(),
                    signedDocument.getId()));
            IOUtils.write(signedDocument.getBody().getBytes(), new FileOutputStream(cmsFile));
        }
        List<CurrencyBatch> currencyBatchList = em.createQuery("SELECT c FROM CurrencyBatch c WHERE c.state =:state and c.type =:typeVS")
                .setParameter("typeVS",  CurrencyOperation.CURRENCY_SEND).setParameter("state", CurrencyBatch.State.OK).getResultList();
        for(CurrencyBatch currencyBatch : currencyBatchList) {
            File cmsFile = new File(format("{0}/currency-send_{1}.xml", currencyBatchDir.getAbsolutePath(),
                    currencyBatch.getId()));
            IOUtils.write(currencyBatch.getSignedDocument().getBody(), new FileOutputStream(cmsFile));
        }
        currencyBatchList = em.createQuery("SELECT c FROM CurrencyBatch c WHERE c.state =:state and c.type =:currencyOperation")
                .setParameter("currencyOperation",  CurrencyOperation.CURRENCY_CHANGE)
                .setParameter("state", CurrencyBatch.State.OK).getResultList();
        for(CurrencyBatch currencyBatch : currencyBatchList) {
            File cmsFile = new File(format("{0}/currency-change_{1}.xml", currencyBatchDir.getAbsolutePath(),
                    currencyBatch.getId()));
            IOUtils.write(currencyBatch.getSignedDocument().getBody(), new FileOutputStream(cmsFile));
        }
        //TODO
        //Set<X509Certificate> systemTrustedCerts = cmsBean.getTrustedCerts();
        Set<X509Certificate> systemTrustedCerts = null;
        File systemTrustedCertsFile = new File(format("{0}/system-trusted-certs.pem", reportFiles.getBaseDir().getAbsolutePath()));
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