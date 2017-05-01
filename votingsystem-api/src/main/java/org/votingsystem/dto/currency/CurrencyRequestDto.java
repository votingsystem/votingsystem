package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;

import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyRequestDto {

    private static Logger log = Logger.getLogger(CurrencyRequestDto.class.getName());

    private CurrencyOperation operation = CurrencyOperation.CURRENCY_REQUEST;
    private String subject;
    private String entityId;
    private CurrencyCode currencyCode;
    private BigDecimal totalAmount;
    private String UUID;

    @JsonIgnore
    private SignedDocument signedDocument;
    @JsonIgnore
    private Map<String, CurrencyDto> currencyDtoMap;
    @JsonIgnore
    private Map<String, org.votingsystem.model.currency.Currency> currencyMap;
    @JsonIgnore
    private Set<String> requestCSRSet;

    public CurrencyRequestDto() {}

    public CurrencyRequestDto(BigDecimal totalAmount, CurrencyCode currencyCode, String subject, String entityId) {
        this.entityId = entityId;
        this.totalAmount = totalAmount;
        this.currencyCode = currencyCode;
        this.subject = subject;
    }

    public static CurrencyRequestDto CREATE_REQUEST(TransactionDto transactionDto, BigDecimal currencyValue,
                                                    String currencyEntity) throws Exception {
        CurrencyRequestDto currencyRequestDto = new CurrencyRequestDto(transactionDto.getAmount(),
                transactionDto.getCurrencyCode(), transactionDto.getSubject(), currencyEntity);
        currencyRequestDto.UUID = java.util.UUID.randomUUID().toString();
        Map<String, Currency> currencyMap = new HashMap<>();
        Set<String> requestCSRSet = new HashSet<>();
        BigDecimal divideAndRemainder[] = transactionDto.getAmount().divideAndRemainder(currencyValue);
        if(divideAndRemainder[1].compareTo(BigDecimal.ZERO) != 0) throw new ValidationException(MessageFormat.format(
                "request with remainder - requestAmount ''{0}''  currencyValue ''{{1}}'' remainder ''{{2}}''",
                transactionDto.getAmount(), currencyValue, divideAndRemainder[1]));
        for(int i = 0; i < divideAndRemainder[0].intValue(); i++) {
            Currency currency = Currency.build(currencyEntity, currencyValue, transactionDto.getCurrencyCode());
            requestCSRSet.add(new String(currency.getCertificationRequest().getCsrPEM()));
            currencyMap.put(currency.getRevocationHash(), currency);
        }
        currencyRequestDto.requestCSRSet = requestCSRSet;
        currencyRequestDto.setCurrencyMap(currencyMap);
        return currencyRequestDto;
    }


    public void loadCurrencyCerts(Collection<String> currencyCerts) throws Exception {
        log.info("loadCurrencyCerts - Num IssuedCurrency: " + currencyCerts.size());
        if(currencyCerts.size() != currencyMap.size()) {
            log.log(Level.SEVERE, "Num currency requested: " + currencyMap.size() +
                    " - num. currency received: " + currencyCerts.size());
        }
        for(String pemCert:currencyCerts) {
            Currency currency = loadCurrencyCert(pemCert);
            currencyMap.replace(currency.getRevocationHash(), currency);
        }
    }

    public Currency loadCurrencyCert(String x509CertificatePEM) throws Exception {
        X509Certificate x509Certificate = PEMUtils.fromPEMToX509Cert(x509CertificatePEM.getBytes());
        CurrencyCertExtensionDto certExtensionDto = CertificateUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509Certificate, Constants.CURRENCY_OID);
        Currency currency = currencyMap.get(certExtensionDto.getRevocationHash())
                .setState(Currency.State.OK);
        currency.initSigner(x509CertificatePEM.getBytes());
        return currency;
    }

    public static CurrencyRequestDto validateRequest(Set<String> requestCSRSet, SignedDocument signedDocument,
                                                     String entityId) throws Exception {
        CurrencyRequestDto requestDto = signedDocument.getSignedContent(CurrencyRequestDto.class).setRequestCSRSet(requestCSRSet);
        requestDto.signedDocument = signedDocument;
        if(CurrencyOperation.CURRENCY_REQUEST != requestDto.getOperation()) throw new ValidationException(
                "Expected operation 'CURRENCY_REQUEST' found: " + requestDto.getOperation());
        if(!entityId.equals(requestDto.getEntityId())) throw new ValidationException("Expected serverURL '" + entityId +
                "' found: " + requestDto.getEntityId());
        BigDecimal csrRequestAmount = BigDecimal.ZERO;
        Map<String, CurrencyDto> currencyDtoMap = new HashMap<>();
        for(String currencyCSR : requestDto.requestCSRSet) {
            CurrencyDto currencyDto = new CurrencyDto(PEMUtils.fromPEMToPKCS10CertificationRequest(currencyCSR.getBytes()));
            if(!currencyDto.getCurrencyCode().equals(requestDto.getCurrencyCode())) throw new ValidationException(
                    "currency error - CurrencyCSRDto currencyCode: " + currencyDto.getCurrencyCode() +
                            " - csr currencyCode: " + currencyDto.getCurrencyCode());
            if (!entityId.equals(currencyDto.getCurrencyEntity()))  throw new ValidationException("serverURL error - " +
                    " serverURL: " + entityId + " - csr serverURL: " + currencyDto.getCurrencyEntity());
            csrRequestAmount = csrRequestAmount.add(currencyDto.getAmount());
            currencyDtoMap.put(currencyDto.getRevocationHash(), currencyDto);
        }
        if(requestDto.getTotalAmount().compareTo(csrRequestAmount) != 0) throw new ValidationException(MessageFormat.format(
                "Amount mismatch - CurrencyRequestDto ''{0}'' - csr ''{1}''", requestDto.getTotalAmount(), csrRequestAmount));
        requestDto.setCurrencyDtoMap(currencyDtoMap);
        return requestDto;
    }

    public CurrencyOperation getOperation() {
        return operation;
    }

    public void setOperation(CurrencyOperation operation) {
        this.operation = operation;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setServerURL(String entityId) {
        this.entityId = entityId;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Set<String> getRequestCSRSet() {
        return requestCSRSet;
    }

    public CurrencyRequestDto setRequestCSRSet(Set<String> requestCSRSet) {
        this.requestCSRSet = requestCSRSet;
        return this;
    }

    public Map<String, CurrencyDto> getCurrencyDtoMap() {
        return currencyDtoMap;
    }

    public void setCurrencyDtoMap(Map<String, CurrencyDto> currencyDtoMap) {
        this.currencyDtoMap = currencyDtoMap;
    }

    public void setCurrencyMap(Map<String, Currency> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public Map<String, Currency> getCurrencyMap() {
        return currencyMap;
    }

    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public void setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
    }

}
