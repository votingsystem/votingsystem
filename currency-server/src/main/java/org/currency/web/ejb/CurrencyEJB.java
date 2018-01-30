package org.currency.web.ejb;

import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.crypto.SignedDocumentType;
import org.votingsystem.crypto.cms.CMSSignedMessage;
import org.votingsystem.currency.AccountMovements;
import org.votingsystem.currency.CurrencyChangeTransactionRequest;
import org.votingsystem.currency.CurrencyTransactionRequest;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.dto.ResultListDto;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyBatchResponseDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.dto.currency.CurrencyStateDto;
import org.votingsystem.ejb.CmsEJB;
import org.votingsystem.ejb.SignatureServiceEJB;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.model.currency.Transaction;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.Messages;
import org.votingsystem.xml.XML;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.text.MessageFormat;
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
    @EJB private ConfigCurrencyServer config;
    @EJB private TransactionEJB transactionBean;
    @EJB private SignatureServiceEJB signatureService;
    @EJB private UserEJB userBean;
    @EJB private CurrencyIssuerEJB csrBean;
    @EJB private CmsEJB cmsEJB;


    /**
     * Processes currency-change transactions to issue new currency
     *
     * @param transactionRequest
     * @return
     * @throws Exception
     */
    public CurrencyBatchResponseDto processCurrencyChangeTransaction(CurrencyChangeTransactionRequest transactionRequest)
            throws Exception {
        CurrencyBatchDto batchDto = transactionRequest.getBatchDto();
        Set<Currency> validatedCurrencySet = new HashSet<>();
        CurrencyBatch currencyBatch = transactionRequest.getCurrencyBatch();
        for(Currency currency : transactionRequest.getCurrencySet()) {
            validatedCurrencySet.add(validateBatchItem(currency));
        }
        currencyBatch.setValidatedCurrencySet(validatedCurrencySet);
        String leftOverCert = null;
        if(transactionRequest.getLeftOverCsr() != null) {
            Currency leftOver = csrBean.signCurrencyRequest(transactionRequest.getLeftOverCsr(), Currency.Type.LEFT_OVER,
                    currencyBatch, Constants.CURRENY_ISSUED_LIVE_IN_YEARS);
            currencyBatch.setLeftOver(leftOver);
            leftOverCert = new String(PEMUtils.getPEMEncoded(leftOver.getCurrencyCertificate()));
        }
        Currency currencyChange = csrBean.signCurrencyRequest(batchDto.getCurrencyChangeCsr(),
                Currency.Type.CHANGE, currencyBatch, Constants.CURRENY_ISSUED_LIVE_IN_YEARS);

        SignatureParams signatureParams = new SignatureParams(config.getEntityId(), User.Type.CURRENCY_SERVER,
                SignedDocumentType.CURRENCY_CHANGE_RECEIPT).setWithTimeStampValidation(false);
        byte[] documentToSign = XML.getMapper().writeValueAsBytes(batchDto);
        SignedDocument receipt = signatureService.signXAdESAndSave(documentToSign, signatureParams);

        currencyBatch.setCurrencyChange(currencyChange);
        currencyBatch.setValidatedCurrencySet(validatedCurrencySet);
        em.persist(currencyBatch.setSignedDocument(receipt).setState(CurrencyBatch.State.OK));
        Transaction transaction = Transaction.CURRENCY_CHANGE(currencyBatch, receipt);
        em.persist(transaction);
        transactionBean.updateCurrencyAccounts(transaction.setCurrencyBatch(currencyBatch));
        for(Currency currency : validatedCurrencySet) {
            em.merge(currency.setState(Currency.State.EXPENDED).setTransaction(transaction));
        }
        return new CurrencyBatchResponseDto(receipt.getBody().getBytes(), leftOverCert  ,
                new String(PEMUtils.getPEMEncoded(currencyChange.getCurrencyCertificate())));
    }

    /**
     * Processes currency-paid transactions to enter the amount to a local user IBAN
     *
     * @param transactionRequest
     * @return
     * @throws Exception
     */
    public CurrencyBatchResponseDto processCurrencyTransaction(CurrencyTransactionRequest transactionRequest)
            throws Exception {
        CurrencyBatchDto batchDto = transactionRequest.getBatchDto();
        Set<Currency> validatedCurrencySet = new HashSet<>();
        CurrencyBatch currencyBatch = transactionRequest.getCurrencyBatch();
        for(Currency currency : transactionRequest.getCurrencySet()) {
            validatedCurrencySet.add(validateBatchItem(currency));
        }
        currencyBatch.setValidatedCurrencySet(validatedCurrencySet);
        List<User> userList = em.createNamedQuery(User.FIND_USER_BY_IBAN)
                .setParameter("IBAN", batchDto.getToUserIBAN()).getResultList();
        if(userList.isEmpty()) throw new ValidationException(MessageFormat.format(
                "The IBAN ''{0}'' doesn't belong to this bank", batchDto.getToUserIBAN()));
        currencyBatch.setToUser(userList.iterator().next());
        String leftOverCert = null;
        if(transactionRequest.getLeftOverCsr() != null) {
            Currency leftOver = csrBean.signCurrencyRequest(transactionRequest.getLeftOverCsr(), Currency.Type.LEFT_OVER,
                    currencyBatch, Constants.CURRENY_ISSUED_LIVE_IN_YEARS);
            currencyBatch.setLeftOver(leftOver);
            leftOverCert = new String(PEMUtils.getPEMEncoded(leftOver.getCurrencyCertificate()));
        }
        SignatureParams signatureParams = new SignatureParams(config.getEntityId(), User.Type.CURRENCY_SERVER,
                SignedDocumentType.CURRENCY_SEND_RECEIPT).setWithTimeStampValidation(false);
        byte[] documentToSign = XML.getMapper().writeValueAsBytes(batchDto);
        SignedDocument receipt = signatureService.signXAdESAndSave(documentToSign, signatureParams);
        em.persist(currencyBatch.setSignedDocument(receipt).setState(CurrencyBatch.State.OK));
        log.info("currencyBatch:" + currencyBatch.getId() + " - receipt:" + receipt.getId());
        Transaction transaction = Transaction.CURRENCY_SEND(currencyBatch, currencyBatch.getToUser(), receipt);
        em.persist(transaction);
        transactionBean.updateCurrencyAccounts(transaction.setCurrencyBatch(currencyBatch));
        for(Currency currency : validatedCurrencySet) {
            em.merge(currency.setState(Currency.State.EXPENDED).setTransaction(transaction));
        }
        CurrencyBatchResponseDto responseDto = new CurrencyBatchResponseDto(receipt.getBody().getBytes(), leftOverCert);
        responseDto.setMessage(Messages.currentInstance().get("currencyBatchOKMsg", batchDto.getBatchAmount() + " " +
                batchDto.getCurrencyCode(), currencyBatch.getToUser().getFullName()));
        return responseDto;
    }

    public Currency validateBatchItem(Currency currency) throws Exception {
        CMSSignedMessage cmsMessage = currency.getCmsSignedMessage();
        String currencyCertHash = CertificateUtils.getHash(cmsMessage.getCurrencyCert());
        List<Currency> currencyList = em.createQuery(
                "SELECT c FROM Currency c WHERE c.UUID =:currencyCertHash")
                .setParameter("currencyCertHash", currencyCertHash).getResultList();
        if(currencyList.isEmpty()) throw new ValidationException(
                Messages.currentInstance().get("currencyRevocationHashInvalidErrorMsg", currency.getRevocationHash()));
        Currency currencyDb = currencyList.iterator().next();
        if(Currency.State.OK !=  currencyDb.getState())
            throw new ValidationException(MessageFormat.format("The currency with revocation hash ''{0}'' has state ''{1}''",
                    currency.getRevocationHash(), currencyDb.getState()));
        cmsEJB.validateToken(cmsMessage.getTimeStampToken());
        return currency;
    }

    public ResultListDto<String> processCurrencyRequest(CurrencyRequestDto requestDto) throws Exception {
        User fromUser = requestDto.getSignedDocument().getFirstSignature().getSigner();
        //check cash available for user
        AccountMovements accountMovements = transactionBean.getAccountMovementsForTransaction(
                fromUser, requestDto.getTotalAmount(), requestDto.getCurrencyCode());
        if(!accountMovements.isTransactionApproved())
            return new ResultListDto(ResponseDto.SC_PRECONDITION_FAILED, accountMovements.getMessage());
        Map<CurrencyAccount, BigDecimal> accountFromMovements = accountMovements.getAccountFromMovements();
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