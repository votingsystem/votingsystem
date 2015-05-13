package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;
import org.votingsystem.util.StringUtils;
import org.votingsystem.util.TypeVS;

import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.util.*;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyBatchDto {

    private TypeVS operation = TypeVS.CURRENCY_SEND;
    private Set<String> currencySet;
    private String leftOverCSR;
    private String toUserIBAN;
    private String toUserName;
    private String subject;
    private String currencyCode;
    private String tag;
    private String batchUUID;
    private Boolean timeLimited = Boolean.FALSE;
    private BigDecimal batchAmount;
    private BigDecimal leftOver = BigDecimal.ZERO;
    @JsonIgnore private PKCS10CertificationRequest leftOverPKCS10;
    @JsonIgnore private List<Currency> currencyList;


    public CurrencyBatchDto() {}

    public CurrencyBatchDto(CurrencyBatch currencyBatch) {
        this.subject = currencyBatch.getSubject();
        this.toUserIBAN = currencyBatch.getToUserVS().getIBAN();
        this.batchAmount = currencyBatch.getBatchAmount();
        this.currencyCode = currencyBatch.getCurrencyCode();
        this.tag = currencyBatch.getTagVS().getName();
        this.timeLimited = currencyBatch.getTimeLimited();
        this.batchUUID  = currencyBatch.getBatchUUID();
    }

    public CurrencyBatchDto(String subject, String toUserIBAN, BigDecimal batchAmount, String currencyCode,  String tag,
            Boolean timeLimited, List<Currency> currencyList, String timeStampServiceURL) throws Exception {
        this.subject = subject;
        this.toUserIBAN = toUserIBAN;
        this.batchAmount = batchAmount;
        this.currencyCode = currencyCode;
        this.tag = tag;
        this.timeLimited = timeLimited;
        this.currencyList = currencyList;
        this.batchUUID = UUID.randomUUID().toString();
        Set<String> currencySetTemp = new HashSet<>();
        for (Currency currency : currencyList) {
            SMIMEMessage smimeMessage = currency.getCertificationRequest().getSMIME(currency.getHashCertVS(),
                    StringUtils.getNormalized(currency.getToUserName()), JSON.getMapper().writeValueAsString(
                            CurrencyDto.BATCH_ITEM(this, currency)), subject, null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampServiceURL);
            currency.setSMIME(timeStamper.call());
            currencySet.add(Base64.getEncoder().encodeToString(currency.getSMIME().getBytes()));
        }
        this.currencySet = currencySetTemp;
    }


    @JsonIgnore
    public CurrencyBatch validateRequest() throws Exception {
        BigDecimal accumulated = BigDecimal.ZERO;
        for(String currencyItem : currencySet) {
            try {
                Currency currency = new Currency(new SMIMEMessage(Base64.getDecoder().decode(currencyItem.getBytes())));
                if(currencyList == null) {
                    currencyList = new ArrayList<>();
                    this.subject = currency.getSubject();
                    this.toUserIBAN = currency.getToUserIBAN();
                    this.batchAmount = currency.getBatchAmount();
                    this.currencyCode = currency.getCurrencyCode();
                    this.tag = currency.getTagVS().getName();
                    this.timeLimited = currency.getTimeLimited();
                    this.batchUUID = currency.getBatchUUID();
                } else checkCurrencyData(currency);
                accumulated.add(currency.getAmount());
                currencyList.add(currency);
            } catch(Exception ex) {
                throw new ExceptionVS("Error with currency : " + ex.getMessage(), ex);
            }
        }
        CurrencyCertExtensionDto certExtensionDto = null;
        if(leftOverCSR != null) {
            leftOverPKCS10 = CertUtils.fromPEMToPKCS10CertificationRequest(leftOverCSR.getBytes());
            certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                    leftOverPKCS10, ContextVS.CURRENCY_TAG);
            if(leftOver.compareTo(certExtensionDto.getAmount()) != 0) throw new ValidationExceptionVS(
                    "leftOver 'amount' mismatch - request: " + leftOver + " - csr: " + certExtensionDto.getAmount());
            if(!certExtensionDto.getCurrencyCode().equals(currencyCode)) throw new ValidationExceptionVS(
                    "leftOver 'currencyCode' mismatch - request: " + currencyCode + " - csr: " + certExtensionDto.getCurrencyCode());
            if(!certExtensionDto.getTag().equals(tag)) throw new ValidationExceptionVS(
                    "leftOver 'tag' mismatch - request: " + tag + " - csr: " + certExtensionDto.getTag());
            BigDecimal leftOverCalculated = accumulated.subtract(batchAmount);
            if(leftOverCalculated.compareTo(leftOver) != 0) throw new ValidationExceptionVS(
                    "leftOverCalculated: " + leftOverCalculated + " - leftOver: " + leftOver);
        } else if(leftOver.compareTo(BigDecimal.ZERO) != 0) throw new ValidationExceptionVS(
                "leftOver request: " + leftOver + " without CSR");
        CurrencyBatch currencyBatch = new CurrencyBatch();
        currencyBatch.setSubject(subject);
        currencyBatch.setBatchAmount(batchAmount);
        currencyBatch.setCurrencyCode(currencyCode);
        currencyBatch.setTagVS(new TagVS(tag));
        currencyBatch.setTimeLimited(timeLimited);
        currencyBatch.setBatchUUID(batchUUID);
        return currencyBatch;
    }

    public void validateTransactionVSResponse(Map<String, String> dataMap, Set<TrustAnchor> trustAnchor) throws Exception {
        SMIMEMessage receipt = new SMIMEMessage(Base64.getDecoder().decode(((String)dataMap.get("receipt")).getBytes()));
        if(dataMap.containsKey("leftOverCoin")) {

        }


        Map<String, Currency> currencyMap = getCurrencyMap();
        if(currencyMap.size() != dataMap.size()) throw new ExceptionVS("Num. currency: '" +
                currencyMap.size() + "' - num. receipts: " + dataMap.size());
        for(String hashCertVS : dataMap.keySet()) {
            SMIMEMessage smimeReceipt = new SMIMEMessage(Base64.getDecoder().decode(dataMap.get(hashCertVS).getBytes()));
            String signatureHashCertVS = CertUtils.getHashCertVS(smimeReceipt.getCurrencyCert(), ContextVS.CURRENCY_OID);
            Currency currency = currencyMap.remove(signatureHashCertVS);
            currency.validateReceipt(smimeReceipt, trustAnchor);
        }
        if(currencyMap.size() != 0) throw new ExceptionVS(currencyMap.size() + " Currency transactions without receipt");
    }

    public Map<String, Currency> getCurrencyMap() throws ExceptionVS {
        if(currencyList == null) throw new ExceptionVS("Empty currencyList");
        Map<String, Currency> result = new HashMap<>();
        for(Currency currency : currencyList) {
            result.put(currency.getHashCertVS(), currency);
        }
        return result;
    }
    public void checkCurrencyData(Currency currency) throws ExceptionVS {
        String currencyData = "Currency with hash '" + currency.getHashCertVS() + "' ";
        if(!timeLimited && currency.getTimeLimited()) throw new ValidationExceptionVS(
                currencyData + "TimeLimited currency can't go inside NOT TimeLimited batch");
        if(!subject.equals(currency.getSubject())) throw new ValidationExceptionVS(
                currencyData + "expected subject " + subject + " found " + currency.getSubject());
        if(!toUserIBAN.equals(currency.getToUserIBAN())) throw new ValidationExceptionVS(
                currencyData + "expected subject " + toUserIBAN + " found " + currency.getToUserIBAN());
        if(batchAmount.compareTo(currency.getBatchAmount()) != 0) throw new ValidationExceptionVS(
                currencyData + "expected batchAmount " + batchAmount + " found " + currency.getBatchAmount());
        if(!currencyCode.equals(currency.getCurrencyCode())) throw new ValidationExceptionVS(
                currencyData + "expected currencyCode " + currencyCode + " found " + currency.getCurrencyCode());
        if(!tag.equals(currency.getTagVS().getName())) throw new ValidationExceptionVS(
                currencyData + "expected tag " + tag + " found " + currency.getTagVS().getName());
        if(!batchUUID.equals(currency.getBatchUUID())) throw new ValidationExceptionVS(
                currencyData + "expected batchUUID " + batchUUID + " found " + currency.getBatchUUID());
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
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

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public String getLeftOverCSR() {
        return leftOverCSR;
    }

    public void setLeftOverCSR(String leftOverCSR) {
        this.leftOverCSR = leftOverCSR;
    }

    public PKCS10CertificationRequest getLeftOverPKCS10() {
        return leftOverPKCS10;
    }

    public void setLeftOverPKCS10(PKCS10CertificationRequest leftOverPKCS10) {
        this.leftOverPKCS10 = leftOverPKCS10;
    }

}