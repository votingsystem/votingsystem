package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.TypeVS;
import org.votingsystem.util.currency.Payment;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyBatchDto {

    private BigDecimal currencyAmount = BigDecimal.ZERO;

    private TypeVS operation;
    private List<String> currency;
    private String csrCurrency;
    private List<String> hashCertVSCurrency;
    private String toUserIBAN;
    private String toUserName;
    private Payment paymentMethod;
    private String subject;
    private String currencyCode;
    private String tag;
    private String batchUUID;
    private Boolean isTimeLimited = Boolean.FALSE;
    private BigDecimal batchAmount;
    private BigDecimal leftOver;
    private AtomicBoolean initialized = new AtomicBoolean(Boolean.FALSE);
    @JsonIgnore private Currency leftOverCurrency;
    @JsonIgnore private List<Currency> currencyList = new ArrayList<Currency>();


    public CurrencyBatchDto() {}


    public CurrencyBatchDto(CurrencyBatch currencyBatch) {
        this.operation = currencyBatch.getOperation();
        this.paymentMethod = currencyBatch.getPaymentMethod();
        this.subject = currencyBatch.getSubject();
        this.toUserIBAN = currencyBatch.getToUserIBAN();
        this.batchAmount = currencyBatch.getBatchAmount();
        this.currencyAmount = currencyBatch.getCurrencyAmount();
        this.currencyCode = currencyBatch.getCurrencyCode();
        this.tag = currencyBatch.getTag();
        this.currencyList = currencyBatch.getCurrencyList();
        hashCertVSCurrency = new ArrayList<>();
        for(Currency currency : currencyBatch.getCurrencyList()) {
            hashCertVSCurrency.add(currency.getHashCertVS());
        }
        this.isTimeLimited = currencyBatch.getIsTimeLimited();
        this.batchUUID  = currencyBatch.getBatchUUID();
    }

    @JsonIgnore
    public CurrencyBatch loadCurrencyBatch() throws Exception {
        if(getCsrCurrency() != null) {
            PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(getCsrCurrency().getBytes());
            setLeftOverCurrency(new Currency(csr));
        }
        for(String currencyItem : getCurrency()) {
            SMIMEMessage smimeMessage = new SMIMEMessage(new ByteArrayInputStream(
                    Base64.getDecoder().decode(currencyItem.getBytes())));
            smimeMessage.isValidSignature();
            try {
                Currency currency = new Currency(smimeMessage);
                this.currencyAmount = this.currencyAmount.add(currency.getAmount());
                currencyList.add(currency);
                if(!initialized.get()) {
                    initialized.set(Boolean.TRUE);
                    this.operation = currency.getOperation();
                    this.paymentMethod = currency.getPaymentMethod();
                    this.subject = currency.getSubject();
                    this.toUserIBAN = currency.getToUserIBAN();
                    this.batchAmount = currency.getBatchAmount();
                    this.currencyCode = currency.getCurrencyCode();
                    this.tag = currency.getTag().getName();
                    this.isTimeLimited = currency.getIsTimeLimited();
                    this.batchUUID = currency.getBatchUUID();
                } else checkCurrencyData(currency);
            } catch(Exception ex) {
                throw new ExceptionVS("Error with currency : " + ex.getMessage(), ex);
            }
        }
        setLeftOver(getCurrencyAmount().subtract(getBatchAmount()));
        if(getLeftOver().compareTo(BigDecimal.ZERO) < 0) new ValidationExceptionVS(
                "CurrencyTransactionBatch insufficientCash - required '" + getBatchAmount().toString() + "' " + "found '" +
                        getCurrencyAmount().toString() + "'");
        if(getLeftOverCurrency() != null && getLeftOver().compareTo(getLeftOverCurrency().getAmount()) != 0) new ValidationExceptionVS(
                "CurrencyTransactionBatch leftOverMissMatch, expected '" + getLeftOver().toString() +
                        "found '" + getLeftOverCurrency().getAmount().toString() + "'");
        return getCurrencyBatch();
    }

    public CurrencyBatch getCurrencyBatch() throws Exception {
        CurrencyBatch currencyBatch = new CurrencyBatch();
        currencyBatch.setOperation(operation);
        currencyBatch.setPaymentMethod(paymentMethod);
        currencyBatch.setSubject(subject);
        currencyBatch.setToUserIBAN(toUserIBAN);
        currencyBatch.setBatchAmount(batchAmount);
        currencyBatch.setCurrencyAmount(currencyAmount);
        currencyBatch.setCurrencyCode(currencyCode);
        currencyBatch.setTag(tag);
        currencyBatch.setCurrencyList(currencyList);
        currencyBatch.setIsTimeLimited(isTimeLimited);
        currencyBatch.setBatchUUID(batchUUID);
        return currencyBatch;
    }


    public void checkCurrencyData(Currency currency) throws ExceptionVS {
        String currencyData = "Currency with hash '" + currency.getHashCertVS() + "' ";
        if(getOperation() != currency.getOperation()) throw new ValidationExceptionVS(
                currencyData + "expected operation " + getOperation() + " found " + currency.getOperation());
        if(getPaymentMethod() != currency.getPaymentMethod()) throw new ValidationExceptionVS(
                currencyData + "expected paymentOption " + getPaymentMethod() + " found " + currency.getPaymentMethod());
        if(!getSubject().equals(currency.getSubject())) throw new ValidationExceptionVS(
                currencyData + "expected subject " + getSubject() + " found " + currency.getSubject());
        if(!getToUserIBAN().equals(currency.getToUserIBAN())) throw new ValidationExceptionVS(
                currencyData + "expected subject " + getToUserIBAN() + " found " + currency.getToUserIBAN());
        if(getBatchAmount().compareTo(currency.getBatchAmount()) != 0) throw new ValidationExceptionVS(
                currencyData + "expected batchAmount " + getBatchAmount().toString() + " found " + currency.getBatchAmount().toString());
        if(!getCurrencyCode().equals(currency.getCurrencyCode())) throw new ValidationExceptionVS(
                currencyData + "expected currencyCode " + getCurrencyCode() + " found " + currency.getCurrencyCode());
        if(!getTag().equals(currency.getTag().getName())) throw new ValidationExceptionVS(
                currencyData + "expected tag " + getTag() + " found " + currency.getTag().getName());
        if(!getBatchUUID().equals(currency.getBatchUUID())) throw new ValidationExceptionVS(
                currencyData + "expected batchUUID " + getBatchUUID() + " found " + currency.getBatchUUID());
    }

    public BigDecimal getCurrencyAmount() {
        return currencyAmount;
    }

    public void setCurrencyAmount(BigDecimal currencyAmount) {
        this.currencyAmount = currencyAmount;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public List<String> getCurrency() {
        return currency;
    }

    public void setCurrency(List<String> currency) {
        this.currency = currency;
    }

    public String getCsrCurrency() {
        return csrCurrency;
    }

    public void setCsrCurrency(String csrCurrency) {
        this.csrCurrency = csrCurrency;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public Currency getLeftOverCurrency() {
        return leftOverCurrency;
    }

    public void setLeftOverCurrency(Currency leftOverCurrency) {
        this.leftOverCurrency = leftOverCurrency;
    }

    public Payment getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Payment paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public Boolean isTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public BigDecimal getLeftOver() {
        return leftOver;
    }

    public void setLeftOver(BigDecimal leftOver) {
        this.leftOver = leftOver;
    }

    public AtomicBoolean getInitialized() {
        return initialized;
    }

    public void setInitialized(AtomicBoolean initialized) {
        this.initialized = initialized;
    }

    public List<Currency> getCurrencyList() {
        return currencyList;
    }

    public void setCurrencyList(List<Currency> currencyList) {
        this.currencyList = currencyList;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }
}