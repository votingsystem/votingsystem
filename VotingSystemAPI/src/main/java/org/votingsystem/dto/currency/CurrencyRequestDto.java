package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyAccount;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

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
public class CurrencyRequestDto {

    private static Logger log = Logger.getLogger(CurrencyRequestDto.class.getSimpleName());

    private TypeVS operation = TypeVS.CURRENCY_REQUEST;
    private String subject;
    private String serverURL;
    private String currencyCode;
    private TagVS tagVS;
    private String UUID;
    private BigDecimal totalAmount;
    private Boolean timeLimited;

    @JsonIgnore private MessageSMIME messageSMIME;
    @JsonIgnore private Map<String, CurrencyDto> currencyDtoMap;
    @JsonIgnore private Map<String, Currency> currencyMap;
    @JsonIgnore private Set<String> requestCSRSet;

    public CurrencyRequestDto() {}


    public static CurrencyRequestDto CREATE_REQUEST(TransactionVSDto transactionVSDto, BigDecimal currencyValue,
                                                    String serverURL) throws Exception {
        CurrencyRequestDto currencyRequestDto = new CurrencyRequestDto();
        currencyRequestDto.serverURL = serverURL;
        currencyRequestDto.subject = transactionVSDto.getSubject();
        currencyRequestDto.totalAmount = transactionVSDto.getAmount();
        currencyRequestDto.currencyCode = transactionVSDto.getCurrencyCode();
        currencyRequestDto.timeLimited = transactionVSDto.isTimeLimited();
        currencyRequestDto.tagVS = new TagVS(transactionVSDto.getTagName());;
        currencyRequestDto.UUID = java.util.UUID.randomUUID().toString();

        Map<String, Currency> currencyMap = new HashMap<>();
        Set<String> requestCSRSet = new HashSet<>();
        BigDecimal divideAndRemainder[] = transactionVSDto.getAmount().divideAndRemainder(currencyValue);
        if(divideAndRemainder[1].compareTo(BigDecimal.ZERO) != 0) throw new ValidationExceptionVS(MessageFormat.format(
                "request with remainder - requestAmount ''{0}''  currencyValue ''{{1}}'' remainder ''{{2}}''",
                transactionVSDto.getAmount(), currencyValue, divideAndRemainder[1]));
        for(int i = 0; i < divideAndRemainder[0].intValue(); i++) {
            Currency currency = new Currency(serverURL, currencyValue, transactionVSDto.getCurrencyCode(),
                    currencyRequestDto.tagVS);
            currency.setTimeLimited(transactionVSDto.isTimeLimited());
            requestCSRSet.add(new String(currency.getCertificationRequest().getCsrPEM()));
            currencyMap.put(currency.getHashCertVS(), currency);
        }
        currencyRequestDto.requestCSRSet = requestCSRSet;
        currencyRequestDto.setCurrencyMap(currencyMap);
        return currencyRequestDto;
    }


    public void loadCurrencyCerts(Collection<String> currencyCerts) throws Exception {
        log.info("CurrencyRequestBatch - Num IssuedCurrency: " + currencyCerts.size());
        if(currencyCerts.size() != currencyMap.size()) {
            log.log(Level.SEVERE, "Num currency requested: " + currencyMap.size() +
                    " - num. currency received: " + currencyCerts.size());
        }
        for(String pemCert:currencyCerts) {
            Currency currency = loadCurrencyCert(pemCert);
            getCurrencyMap().replace(currency.getHashCertVS(), currency);
        }
    }

    public Currency loadCurrencyCert(String pemCert) throws Exception {
        Collection<X509Certificate> certificates = CertUtils.fromPEMToX509CertCollection(pemCert.getBytes());
        if(certificates.isEmpty()) throw new ExceptionVS("Unable to init Currency. Certs not found on signed CSR");
        X509Certificate x509Certificate = certificates.iterator().next();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                x509Certificate, ContextVS.CURRENCY_OID);
        Currency currency = currencyMap.get(certExtensionDto.getHashCertVS()).setState(Currency.State.OK);
        currency.initSigner(pemCert.getBytes());
        return currency;
    }

    public static CurrencyRequestDto validateRequest(byte[] currencyBatchRequest, MessageSMIME messageSMIME,
                           String contextURL) throws Exception {
        CurrencyRequestDto requestDto = messageSMIME.getSignedContent(CurrencyRequestDto.class);
        requestDto.messageSMIME = messageSMIME;
        if(TypeVS.CURRENCY_REQUEST != requestDto.getOperation()) throw new ExceptionVS(
                "Expected operation 'CURRENCY_REQUEST' found: " + requestDto.getOperation());
        if(!contextURL.equals(requestDto.getServerURL())) throw new ExceptionVS("Expected serverURL '" + contextURL +
                "' found: " + requestDto.getServerURL());
        requestDto.requestCSRSet = JSON.getMapper().readValue(currencyBatchRequest, new TypeReference<Set<String>>() {});
        BigDecimal csrRequestAmount = BigDecimal.ZERO;
        Map<String, CurrencyDto> currencyDtoMap = new HashMap<>();
        for(String currencyCSR : requestDto.requestCSRSet) {
            CurrencyDto currencyDto = new CurrencyDto(CertUtils.fromPEMToPKCS10CertificationRequest(currencyCSR.getBytes()));
            if(!currencyDto.getTag().equals(requestDto.getTagVS().getName())) throw new ValidationExceptionVS(
                    "requestDto tag: " + requestDto.getTagVS().getName() + " - CurrencyCSRDto tag: " + currencyDto.getTag());
            if(currencyDto.getAmount().compareTo(currencyDto.getAmount()) != 0) throw new ValidationExceptionVS(
                    "amount error - CurrencyCSRDto amount: " + currencyDto.getAmount() + " - csr amount: " +
                            currencyDto.getAmount());
            if(!currencyDto.getCurrencyCode().equals(currencyDto.getCurrencyCode())) throw new ValidationExceptionVS(
                    "currency error - CurrencyCSRDto currencyCode: " + currencyDto.getCurrencyCode() +
                            " - csr currencyCode: " + currencyDto.getCurrencyCode());
            if(!currencyDto.getTag().equals(currencyDto.getTag())) throw new ValidationExceptionVS(
                    "tag error - CurrencyCSRDto tag: " + currencyDto.getTag() + " - csr tag: " + currencyDto.getTag());
            if (!contextURL.equals(currencyDto.getCurrencyServerURL()))  throw new ExceptionVS("serverURL error - " +
                    " serverURL: " + contextURL + " - csr serverURL: " + currencyDto.getCurrencyServerURL());
            csrRequestAmount = csrRequestAmount.add(currencyDto.getAmount());
            currencyDtoMap.put(currencyDto.getHashCertVS(), currencyDto);
        }
        requestDto.setCurrencyDtoMap(currencyDtoMap);
        return requestDto;
    }


    public void setTagVSDB(TagVS tagVS) {
        this.tagVS = tagVS;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
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

    public TagVS getTagVS() {
        return tagVS;
    }

    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
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

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public void setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
    }

    public TransactionVS getTransactionVS(String subject, Map<CurrencyAccount, BigDecimal> accountFromMovements) {
        TransactionVS transaction = new TransactionVS();
        transaction.setType(TransactionVS.Type.CURRENCY_REQUEST);
        transaction.setState(TransactionVS.State.OK);
        transaction.setAmount(totalAmount);
        transaction.setCurrencyCode(currencyCode);
        transaction.setTag(tagVS);
        transaction.setSubject(subject);
        transaction.setMessageSMIME(messageSMIME);
        transaction.setFromUserVS(messageSMIME.getUserVS());
        transaction.setAccountFromMovements(accountFromMovements);
        return transaction;
    }

    public void setCurrencyMap(Map<String, Currency> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public Map<String, Currency> getCurrencyMap() {
        return currencyMap;
    }
}
