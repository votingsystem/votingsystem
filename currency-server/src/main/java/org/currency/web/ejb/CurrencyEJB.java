package org.currency.web.ejb;

import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.ejb.SignatureService;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.Messages;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public CurrencyBatchResponseDto processCurrencyBatch(SignedDocument signedDocument) throws Exception {
        CurrencyBatchDto batchDto = signedDocument.getSignedContent(CurrencyBatchDto.class);
        Set<Currency> validatedCurrencySet = new HashSet<>();
        CurrencyBatch currencyBatch = batchDto.validateRequest(LocalDateTime.now());
        for(Currency currency : batchDto.getCurrencyList()) {
            validatedCurrencySet.add(validateBatchItem(currency));
        }
        ZonedDateTime validTo = null;
        if(batchDto.getOperation() == CurrencyOperation.CURRENCY_CHANGE) {
            //return processAnonymousCurrencyBatch(batchDto, currencyBatch, validatedCurrencySet, validTo);
            Currency leftOver = null;
            if(batchDto.getLeftOverCsr() != null) {
                leftOver = csrBean.signCurrencyRequest(batchDto.getLeftOverCsr(), Currency.Type.LEFT_OVER, currencyBatch,
                        Constants.CURRENY_ISSUED_LIVE_IN_YEARS);
            }
            Currency currencyChange = csrBean.signCurrencyRequest(batchDto.getCurrencyChangeCsr(),
                    Currency.Type.CHANGE, currencyBatch, Constants.CURRENY_ISSUED_LIVE_IN_YEARS);

            SignatureParams signatureParams = new SignatureParams(config.getEntityId(), User.Type.CURRENCY_SERVER,
                    SignedDocumentType.CURRENCY_CHANGE_RECEIPT).setWithTimeStampValidation(false);
            SignedDocument receipt = signatureService.signXAdESAndSave(signedDocument.getBody().getBytes(), signatureParams);

            currencyBatch.setLeftOver(leftOver);
            currencyBatch.setCurrencyChange(currencyChange);
            currencyBatch.setValidatedCurrencySet(validatedCurrencySet);
            em.persist(currencyBatch.setSignedDocument(receipt).setState(CurrencyBatch.State.OK));
            Transaction transaction = Transaction.CURRENCY_CHANGE(currencyBatch, receipt);
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
            if(batchDto.getLeftOverCsr() != null) {
                Currency leftOver = csrBean.signCurrencyRequest(batchDto.getLeftOverCsr(), Currency.Type.LEFT_OVER,
                        currencyBatch, Constants.CURRENY_ISSUED_LIVE_IN_YEARS);
                currencyBatch.setLeftOver(leftOver);
                leftOverCert = new String(PEMUtils.getPEMEncoded(leftOver.getX509AnonymousCert()));
            }
            SignatureParams signatureParams = new SignatureParams(config.getEntityId(), User.Type.CURRENCY_SERVER,
                    SignedDocumentType.CURRENCY_CHANGE_RECEIPT).setWithTimeStampValidation(false);
            SignedDocument receipt = signatureService.signXAdESAndSave(signedDocument.getBody().getBytes(), signatureParams);
            em.persist(currencyBatch.setSignedDocument(receipt).setState(CurrencyBatch.State.OK));
            log.info("currencyBatch:" + currencyBatch.getId() + " - receipt:" + receipt.getId());
            Transaction transaction = Transaction.CURRENCY_SEND(currencyBatch, currencyBatch.getToUser(), receipt);
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
                "SELECT c FROM Currency c WHERE c.serialNumber =:serialNumber and c.revocationHash=:revocationHash and c.state=:state")
                .setParameter("serialNumber", currency.getX509AnonymousCert().getSerialNumber().longValue())
                .setParameter("state", Currency.State.OK)
                .setParameter("revocationHash", currency.getRevocationHash()).getResultList();
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
        Map<CurrencyAccount, BigDecimal> accountFromMovements = transactionBean.getAccountMovementsForTransaction(
                fromUser, requestDto.getTotalAmount(), requestDto.getCurrencyCode());
        Set<String> currencyCertSet = csrBean.signCurrencyRequest(requestDto);
        Transaction userTransaction = Transaction.CURRENCY_REQUEST(Messages.currentInstance().get("currencyRequestLbl"),
                accountFromMovements, requestDto, fromUser);
        transactionBean.updateCurrencyAccounts(userTransaction);
        ResultListDto resultListDto = new ResultListDto(currencyCertSet);
        resultListDto.setMessage(Messages.currentInstance().get("withdrawalMsg", requestDto.getTotalAmount().toString(),
                requestDto.getCurrencyCode()));
        /*CMSSignedMessage receipt = cmsBean.addSignature(requestDto.getCmsMessage().getCMS());
        em.merge(requestDto.getCmsMessage().setType(TypeVS.CURRENCY_REQUEST).setCMS(receipt));*/
        return resultListDto;
    }

    public Set<CurrencyStateDto> checkBundleState(Set<String> revocationHashSet) throws Exception {
        Set<CurrencyStateDto> result = new HashSet<>();
        Query query = em.createQuery("SELECT c FROM Currency c WHERE c.revocationHash =:revocationHash");
        for(String revocationHash : revocationHashSet) {
            List<Currency> currencyList = query.setParameter("revocationHash", revocationHash).getResultList();
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

}