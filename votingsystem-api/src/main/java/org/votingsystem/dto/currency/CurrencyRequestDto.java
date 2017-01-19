package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.JSON;

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
    private String serverURL;
    private CurrencyCode currencyCode;
    private Tag tag;
    private BigDecimal totalAmount;
    private Boolean timeLimited;
    private String UUID;

    @JsonIgnore
    private SignedDocument signedDocument;
    @JsonIgnore
    private Map<String, CurrencyDto> currencyDtoMap;
    @JsonIgnore
    private Map<String, Currency> currencyMap;
    @JsonIgnore
    private Set<String> requestCSRSet;

    public CurrencyRequestDto() {}


    public static CurrencyRequestDto CREATE_REQUEST(TransactionDto transactionDto, BigDecimal currencyValue,
                                                    String serverURL) throws Exception {
        CurrencyRequestDto currencyRequestDto = new CurrencyRequestDto();
        currencyRequestDto.serverURL = serverURL;
        currencyRequestDto.subject = transactionDto.getSubject();
        currencyRequestDto.totalAmount = transactionDto.getAmount();
        currencyRequestDto.currencyCode = transactionDto.getCurrencyCode();
        currencyRequestDto.timeLimited = transactionDto.isTimeLimited();
        currencyRequestDto.tag = new Tag(transactionDto.getTagName());;
        currencyRequestDto.UUID = java.util.UUID.randomUUID().toString();

        Map<String, Currency> currencyMap = new HashMap<>();
        Set<String> requestCSRSet = new HashSet<>();
        BigDecimal divideAndRemainder[] = transactionDto.getAmount().divideAndRemainder(currencyValue);
        if(divideAndRemainder[1].compareTo(BigDecimal.ZERO) != 0) throw new ValidationException(MessageFormat.format(
                "request with remainder - requestAmount ''{0}''  currencyValue ''{{1}}'' remainder ''{{2}}''",
                transactionDto.getAmount(), currencyValue, divideAndRemainder[1]));
        for(int i = 0; i < divideAndRemainder[0].intValue(); i++) {
            Currency currency = new Currency(serverURL, currencyValue, transactionDto.getCurrencyCode(),
                    transactionDto.isTimeLimited(), currencyRequestDto.tag);
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
        Collection<X509Certificate> certificates = PEMUtils.fromPEMToX509CertCollection(x509CertificatePEM.getBytes());
        if(certificates.isEmpty())
            throw new ValidationException("Unable to init Currency. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509Certificate, Constants.CURRENCY_OID);
        Currency currency = currencyMap.get(certExtensionDto.getHashCertVS()).setState(Currency.State.OK);
        currency.initSigner(x509CertificatePEM.getBytes());
        return currency;
    }

    public static CurrencyRequestDto validateRequest(byte[] currencyBatchRequest, SignedDocument signedDocument,
                                                     String entityId) throws Exception {
        CurrencyRequestDto requestDto = signedDocument.getSignedContent(CurrencyRequestDto.class);
        requestDto.signedDocument = signedDocument;
        if(CurrencyOperation.CURRENCY_REQUEST != requestDto.getOperation()) throw new ValidationException(
                "Expected operation 'CURRENCY_REQUEST' found: " + requestDto.getOperation());
        if(!entityId.equals(requestDto.getServerURL())) throw new ValidationException("Expected serverURL '" + entityId +
                "' found: " + requestDto.getServerURL());
        requestDto.requestCSRSet = JSON.getMapper().readValue(currencyBatchRequest, new TypeReference<Set<String>>() {});
        BigDecimal csrRequestAmount = BigDecimal.ZERO;
        Map<String, CurrencyDto> currencyDtoMap = new HashMap<>();
        for(String currencyCSR : requestDto.requestCSRSet) {
            CurrencyDto currencyDto = new CurrencyDto(PEMUtils.fromPEMToPKCS10CertificationRequest(currencyCSR.getBytes()));
            if(!currencyDto.getCurrencyCode().equals(requestDto.getCurrencyCode())) throw new ValidationException(
                    "currency error - CurrencyCSRDto currencyCode: " + currencyDto.getCurrencyCode() +
                            " - csr currencyCode: " + currencyDto.getCurrencyCode());
            if(!currencyDto.getTag().equals(requestDto.getTag().getName())) throw new ValidationException(
                    "requestDto tag: " + requestDto.getTag().getName() + " - CurrencyCSRDto tag: " + currencyDto.getTag());
            if (!entityId.equals(currencyDto.getCurrencyServerURL()))  throw new ValidationException("serverURL error - " +
                    " serverURL: " + entityId + " - csr serverURL: " + currencyDto.getCurrencyServerURL());
            csrRequestAmount = csrRequestAmount.add(currencyDto.getAmount());
            currencyDtoMap.put(currencyDto.getRevocationHashBase64(), currencyDto);
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

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
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

    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public Set<String> getRequestCSRSet() {
        return requestCSRSet;
    }

    public void setRequestCSRSet(Set<String> requestCSRSet) {
        this.requestCSRSet = requestCSRSet;
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
