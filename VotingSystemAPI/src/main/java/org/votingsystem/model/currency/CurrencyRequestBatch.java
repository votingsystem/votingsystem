package org.votingsystem.model.currency;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.dto.currency.CurrencyCSRDto;
import org.votingsystem.dto.currency.CurrencyCertExtensionDto;
import org.votingsystem.dto.currency.CurrencyRequestDto;
import org.votingsystem.model.BatchRequest;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

import javax.persistence.*;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("CurrencyRequestBatch")
public class CurrencyRequestBatch extends BatchRequest implements Serializable  {

    private static Logger log = Logger.getLogger(CurrencyRequestBatch.class.getSimpleName());

    public static final long serialVersionUID = 1L;

    @OneToOne private MessageSMIME messageSMIME;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="isTimeLimited") private Boolean isTimeLimited;

    @Transient private CurrencyRequestDto requestDto;
    @Transient private Map<String, Currency> currencyMap;
    @Transient private List<CurrencyCSRDto> currencyCSRList;

    public CurrencyRequestBatch() {}

    public CurrencyRequestBatch(CurrencyRequestDto requestDto, MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
        this.requestDto = requestDto;
    }

    public static CurrencyRequestBatch validateRequest(byte[] currencyBatchRequest, MessageSMIME messageSMIME,
               String contextURL) throws Exception {
        CurrencyRequestDto requestDto = messageSMIME.getSignedContent(CurrencyRequestDto.class);
        if(TypeVS.CURRENCY_REQUEST != requestDto.getOperation()) throw new ExceptionVS(
                "Expected operation 'CURRENCY_REQUEST' found: " + requestDto.getOperation());
        if(!contextURL.equals(requestDto.getServerURL())) throw new ExceptionVS("Expected serverURL '" + contextURL +
                "' found: " + requestDto.getServerURL());
        List<CurrencyCSRDto> currencyCSRList = JSON.getMapper().readValue(currencyBatchRequest,
                new TypeReference<List<CurrencyCSRDto>>() {});
        BigDecimal csrRequestAmount = BigDecimal.ZERO;
        Map<String, Currency> currencyMap = new HashMap<>();
        for(CurrencyCSRDto currencyCSRDto : currencyCSRList) {
            if(!currencyCSRDto.getTag().equals(requestDto.getTagVS().getName())) throw new ValidationExceptionVS(
                    "requestDto tag: " + requestDto.getTagVS().getName() + " - CurrencyCSRDto tag: " + currencyCSRDto.getTag());
            PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(currencyCSRDto.getCsr().getBytes());
            Currency currency = new Currency(csr);
            if(currencyCSRDto.getCurrencyValue().compareTo(currency.getAmount()) != 0) throw new ValidationExceptionVS(
                    "amount error - CurrencyCSRDto amount: " + currencyCSRDto.getCurrencyValue() + " - csr amount: " +
                    currency.getAmount());
            if(!currencyCSRDto.getCurrencyCode().equals(currency.getCurrencyCode())) throw new ValidationExceptionVS(
                    "currency error - CurrencyCSRDto currencyCode: " + currencyCSRDto.getCurrencyCode() +
                    " - csr currencyCode: " + currency.getCurrencyCode());
            if(!currencyCSRDto.getTag().equals(currency.getTag().getName())) throw new ValidationExceptionVS(
                    "tag error - CurrencyCSRDto tag: " + currencyCSRDto.getTag() + " - csr tag: " + currency.getTag().getName());
            if (!contextURL.equals(currency.getCurrencyServerURL()))  throw new ExceptionVS("serverURL error - " +
                    " serverURL: " + contextURL + " - csr serverURL: " + currency.getCurrencyServerURL());
            csrRequestAmount = csrRequestAmount.add(currency.getAmount());
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        if(requestDto.getTotalAmount().compareTo(csrRequestAmount) != 0) throw new ExceptionVS("total amount error - " +
            "CurrencyRequestDto amount: " + requestDto.getTotalAmount() + "' - total csr amount: " + csrRequestAmount);
        CurrencyRequestBatch currencyRequestBatch = new CurrencyRequestBatch(requestDto, messageSMIME);
        currencyRequestBatch.setCurrencyCSRList(currencyCSRList);
        currencyRequestBatch.setCurrencyMap(currencyMap);
        return currencyRequestBatch;
    }

    public static CurrencyRequestBatch createRequest(BigDecimal requestAmount, BigDecimal currencyValue, String currencyCode,
                TagVS tagVS, Boolean isTimeLimited, String serverURL) throws Exception {
        if(tagVS == null) tagVS = new TagVS(TagVS.WILDTAG);
        CurrencyRequestDto requestDto = new CurrencyRequestDto(null, requestAmount, currencyCode, isTimeLimited,
                serverURL, tagVS);
        CurrencyRequestBatch currencyRequestBatch = new CurrencyRequestBatch();
        currencyRequestBatch.setRequestDto(requestDto);
        Map<String, Currency> currencyMap = new HashMap<>();
        BigDecimal numCurrency = requestAmount.divide(currencyValue);
        for(int i = 0; i < numCurrency.intValue(); i++) {
            Currency currency = new Currency(serverURL, currencyValue, currencyCode, tagVS);
            currency.setIsTimeLimited(isTimeLimited);
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        currencyRequestBatch.setCurrencyMap(currencyMap);
        List<CurrencyCSRDto> currencyCSRList = new ArrayList<>();
        for(Currency currency : currencyMap.values()) {
            currencyCSRList.add(currency.getCSRDto());
        }
        currencyRequestBatch.setCurrencyCSRList(currencyCSRList);
        return currencyRequestBatch;
    }

    public Map<String, Currency> getCurrencyMap() {
        return currencyMap;
    }

    public List<String> getIssuedCurrencyListPEM() throws IOException {
        List<String> result = new ArrayList<>();
        for(Currency currency : currencyMap.values()) {
            result.add(new String(currency.getIssuedCertPEM()));
        }
        return result;
    }

    public void loadIssuedCurrency(List<String> issuedCurrencyArray) throws Exception {
        log.info("CurrencyRequestBatch - Num IssuedCurrency: " + issuedCurrencyArray.size());
        if(issuedCurrencyArray.size() != currencyMap.size()) {
            log.log(Level.SEVERE, "Num currency requested: " + currencyMap.size() +
                    " - num. currency received: " + issuedCurrencyArray.size());
        }
        for(int i = 0; i < issuedCurrencyArray.size(); i++) {
            Currency currency = loadIssuedCurrency(issuedCurrencyArray.get(i));
            currencyMap.replace(currency.getHashCertVS(), currency);
        }
    }

    public Currency loadIssuedCurrency(String signedCsr) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(signedCsr.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS("Unable to init Currency. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509Certificate, ContextVS.CURRENCY_OID);
        Currency currency = currencyMap.get(certExtensionDto.getHashCertVS()).setState(Currency.State.OK);
        currency.initSigner(signedCsr.getBytes());
        return currency;
    }

    public void setCurrencyMap(Map<String, Currency> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public void setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public List<CurrencyCSRDto> getCurrencyCSRList () {
        return currencyCSRList;
    }

    public TagVS getTagVS() {
        if(tagVS != null) return tagVS;
        else return requestDto.getTagVS();
    }

    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
        for(Currency currency : currencyMap.values()) {
            currency.setTag(tagVS);
        }
    }

    public BigDecimal getRequestAmount() {
        if(requestDto == null) return null;
        else return requestDto.getTotalAmount();
    }

    public String getCurrencyCode() {
        if(requestDto == null) return null;
        else return requestDto.getCurrencyCode();
    }

    public Boolean getIsTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public CurrencyRequestDto getRequestDto() {
        return requestDto;
    }

    public void setRequestDto(CurrencyRequestDto requestDto) {
        this.requestDto = requestDto;
    }

    public void setCurrencyCSRList(List<CurrencyCSRDto> currencyCSRList) {
        this.currencyCSRList = currencyCSRList;
    }

    public TransactionVS getTransactionVS(String subject, Map<CurrencyAccount, BigDecimal> accountFromMovements) {
        TransactionVS transaction = new TransactionVS();
        transaction.setType(TransactionVS.Type.CURRENCY_REQUEST);
        transaction.setState(TransactionVS.State.OK);
        transaction.setAmount(requestDto.getTotalAmount());
        transaction.setCurrencyCode(requestDto.getCurrencyCode());
        transaction.setTag(tagVS);
        transaction.setSubject(subject);
        transaction.setMessageSMIME(messageSMIME);
        transaction.setFromUserVS(messageSMIME.getUserVS());
        transaction.setAccountFromMovements(accountFromMovements);
        return transaction;
    }

}