package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;

import java.math.BigDecimal;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyBatchDto {

    private static Logger log = Logger.getLogger(TransactionDto.class.getName());

    private CurrencyOperation operation = CurrencyOperation.CURRENCY_SEND;
    private Set<String> currencySet;
    private String leftOverCSR;
    private String currencyChangeCSR;
    private String toUserIBAN;
    private String toUserName;
    private String subject;
    private CurrencyCode currencyCode;
    private String batchUUID;
    private BigDecimal batchAmount;
    private BigDecimal leftOver = BigDecimal.ZERO;

    @JsonIgnore
    private PKCS10CertificationRequest currencyChangeCsr;
    @JsonIgnore
    private byte[] content;


    public CurrencyBatchDto(String subject, String toUserIBAN, BigDecimal batchAmount, CurrencyCode currencyCode) {
        this.subject = subject;
        this.toUserIBAN = toUserIBAN;
        this.batchAmount = batchAmount;
        this.currencyCode = currencyCode;
    }

    public CurrencyBatchDto(CurrencyBatch currencyBatch) {
        this.subject = currencyBatch.getSubject();
        this.toUserIBAN = currencyBatch.getToUser().getIBAN();
        this.batchAmount = currencyBatch.getBatchAmount();
        this.currencyCode = currencyBatch.getCurrencyCode();
        this.batchUUID  = currencyBatch.getBatchUUID();
    }

    public CurrencyOperation getOperation() {
        return operation;
    }

    public void setOperation(CurrencyOperation operation) {
        this.operation = operation;
    }

    public Set<String> getCurrencySet() {
        return currencySet;
    }

    public void setCurrencySet(Set<String> currencySet) {
        this.currencySet = currencySet;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
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

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getLeftOverCSR() {
        return leftOverCSR;
    }

    public void setLeftOverCSR(String leftOverCSR) {
        this.leftOverCSR = leftOverCSR;
    }

    public String getCurrencyChangeCSR() {
        return currencyChangeCSR;
    }

    public void setCurrencyChangeCSR(String currencyChangeCSR) {
        this.currencyChangeCSR = currencyChangeCSR;
    }

    public PKCS10CertificationRequest getCurrencyChangeCsr() {
        return currencyChangeCsr;
    }

    public void setCurrencyChangeCsr(PKCS10CertificationRequest currencyChangeCsr) {
        this.currencyChangeCsr = currencyChangeCsr;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public CurrencyDto buildBatchItem(Currency currency) throws ValidationException {
        if(currencyCode != currency.getCurrencyCode()) throw new ValidationException(
                "ERROR - Batch currencyCode: " + currencyCode + " - Currency currencyCode: " + currency.getCurrencyCode());
        CurrencyDto currencyDto = new CurrencyDto(currency);
        currencyDto.setSubject(subject).setToUserIBAN(toUserIBAN).setToUserName(toUserName).setBatchAmount(batchAmount)
                .setBatchUUID(batchUUID);
        return currencyDto;
    }

}