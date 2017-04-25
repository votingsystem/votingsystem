package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.crypto.CertificateUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.currency.CurrencyBatch;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.*;
import org.votingsystem.xml.XML;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
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
    private org.votingsystem.model.currency.Currency leftOverCurrency;
    @JsonIgnore
    private PKCS10CertificationRequest leftOverCsr;
    @JsonIgnore
    private PKCS10CertificationRequest currencyChangeCsr;
    @JsonIgnore
    private List<org.votingsystem.model.currency.Currency> currencyList;
    @JsonIgnore
    private byte[] content;


    public CurrencyBatchDto() {}

    public CurrencyBatchDto(CurrencyBatch currencyBatch) {
        this.subject = currencyBatch.getSubject();
        this.toUserIBAN = currencyBatch.getToUser().getIBAN();
        this.batchAmount = currencyBatch.getBatchAmount();
        this.currencyCode = currencyBatch.getCurrencyCode();
        this.batchUUID  = currencyBatch.getBatchUUID();
    }

    public static CurrencyBatchDto FROM_BYTES(byte[] content) throws IOException {
        CurrencyBatchDto currencyBatchDto = JSON.getMapper().readValue(content, CurrencyBatchDto.class);
        currencyBatchDto.setContent(content);
        return currencyBatchDto;
    }

    public static CurrencyBatchDto NEW(String subject, String toUserIBAN, BigDecimal batchAmount,
            CurrencyCode currencyCode, List<org.votingsystem.model.currency.Currency> currencyList,
            String currencyEntityId, String timestampEntityId) throws Exception {
        CurrencyBatchDto batchDto = new CurrencyBatchDto();
        batchDto.subject = subject;
        batchDto.toUserIBAN = toUserIBAN;
        batchDto.batchAmount = batchAmount;
        batchDto.currencyCode = currencyCode;
        batchDto.currencyList = currencyList;
        batchDto.batchUUID = UUID.randomUUID().toString();
        BigDecimal accumulated = BigDecimal.ZERO;
        for (org.votingsystem.model.currency.Currency currency : currencyList) {
            accumulated = accumulated.add(currency.getAmount());
        }
        if(batchAmount.compareTo(accumulated) > 0) {
            throw new ValidationException(MessageFormat.format("''{0}'' batchAmount exceeds currency sum ''{1}''",
                    batchAmount, accumulated));
        } else if(batchAmount.compareTo(accumulated) != 0){
            batchDto.leftOver = accumulated.subtract(batchAmount);
            batchDto.leftOverCurrency = new org.votingsystem.model.currency.Currency(currencyEntityId, batchDto.leftOver, currencyCode);
            batchDto.leftOverCSR = new String(batchDto.leftOverCurrency.getCertificationRequest().getCsrPEM());
        }
        batchDto.currencySet = new HashSet<>();
        for (org.votingsystem.model.currency.Currency currency : currencyList) {
            byte[] xmlToSign = XML.getMapper().writeValueAsBytes(
                    CurrencyDto.BATCH_ITEM(batchDto, currency));
            byte[] xmlSigned = currency.getCertificationRequest().signDataWithTimeStamp(xmlToSign,
                    OperationType.TIMESTAMP_REQUEST.getUrl(timestampEntityId));
            currency.setContent(xmlSigned);
            batchDto.currencySet.add(new String(xmlSigned));
        }
        return batchDto;
    }

    @JsonIgnore
    public CurrencyBatch validateRequest(LocalDateTime checkDate) throws Exception {
        BigDecimal accumulated = BigDecimal.ZERO;
        BigDecimal wildTagAccumulated = BigDecimal.ZERO;
        currencyList = null;
        for(String currencyItem : currencySet) {
            try {
                SignedDocument signedDocument = null;
                //TODO create SignedDocument from currencyItem
                org.votingsystem.model.currency.Currency currency = new org.votingsystem.model.currency.Currency(signedDocument);
                if(currencyList == null) {
                    currencyList = new ArrayList<>();
                }
                checkBatchItem(currency.getBatchItemDto());
                if(checkDate.isAfter(currency.getValidTo())) throw new ValidationException(MessageFormat.format(
                        "currency ''{0}'' is lapsed", currency.getRevocationHash()));
                accumulated = accumulated.add(currency.getAmount());
                currencyList.add(currency);
            } catch(Exception ex) {
                throw new ValidationException("Error with currency : " + ex.getMessage(), ex);
            }
        }
        if(currencyList == null || currencyList.isEmpty())
            throw new ValidationException("CurrencyBatch without signed transactions");
        CurrencyCertExtensionDto certExtensionDto = null;
        if(leftOverCSR != null) {
            leftOverCsr = PEMUtils.fromPEMToPKCS10CertificationRequest(leftOverCSR.getBytes());
            certExtensionDto = CertificateUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                    leftOverCsr, Constants.CURRENCY_OID);
            if(leftOver.compareTo(certExtensionDto.getAmount()) != 0) throw new ValidationException(
                    "leftOver 'amount' mismatch - request: " + leftOver + " - csr: " + certExtensionDto.getAmount());
            if(!certExtensionDto.getCurrencyCode().equals(currencyCode)) throw new ValidationException(
                    "leftOver 'currencyCode' mismatch - request: " + currencyCode + " - csr: " + certExtensionDto.getCurrencyCode());
            BigDecimal leftOverCalculated = accumulated.subtract(batchAmount);
            if(leftOverCalculated.compareTo(leftOver) != 0) throw new ValidationException(
                    "leftOverCalculated: " + leftOverCalculated + " - leftOver: " + leftOver);
        } else if(leftOver.compareTo(BigDecimal.ZERO) != 0) throw new ValidationException(
                "leftOver request: " + leftOver + " without CSR");
        if(currencyChangeCSR != null) {
            currencyChangeCsr = PEMUtils.fromPEMToPKCS10CertificationRequest(currencyChangeCSR.getBytes());
            certExtensionDto = CertificateUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                    currencyChangeCsr, Constants.CURRENCY_OID);
            if(certExtensionDto.getAmount().compareTo(this.batchAmount) != 0) throw new ValidationException(
                    "currencyChange 'amount' mismatch - request: " + this.batchAmount +
                    " - csr: " + certExtensionDto.getAmount());
            if(!certExtensionDto.getCurrencyCode().equals(currencyCode)) throw new ValidationException(
                    "currencyChange 'currencyCode' mismatch - request: " + currencyCode +
                    " - csr: " + certExtensionDto.getCurrencyCode());
        }
        CurrencyBatch currencyBatch = new CurrencyBatch();
        currencyBatch.setType(operation);
        currencyBatch.setSubject(subject);
        currencyBatch.setBatchAmount(batchAmount);
        currencyBatch.setCurrencyCode(currencyCode);
        currencyBatch.setContent(content);
        currencyBatch.setBatchUUID(batchUUID);
        return currencyBatch;
    }

    @JsonIgnore
    public void validateResponse(CurrencyBatchResponseDto responseDto, Set<TrustAnchor> trustAnchor)
            throws Exception {
        log.severe("============= TODO");
        //responseDto.getReceipt();
        SignedDocument signedDocument = null;
        //CertificateUtils.verifyCertificate(trustAnchor, false, new ArrayList<>(receipt.getSignersCerts()));
        if(responseDto.getLeftOverCert() != null) {
            leftOverCurrency.initSigner(responseDto.getLeftOverCert().getBytes());
        }
        CurrencyBatchDto signedDto = signedDocument.getSignedContent(CurrencyBatchDto.class);
        if(signedDto.getBatchAmount().compareTo(batchAmount) != 0) throw new ValidationException(MessageFormat.format(
            "ERROR - batchAmount ''{0}'' - receipt amount ''{1}''",  batchAmount, signedDto.getBatchAmount()));
        if(!signedDto.getCurrencyCode().equals(signedDto.getCurrencyCode())) throw new ValidationException(MessageFormat.format(
             "ERROR - batch currencyCode ''{0}'' - receipt currencyCode ''{1}''",  currencyCode, signedDto.getCurrencyCode()));
        if(!currencySet.equals(signedDto.getCurrencySet())) throw new ValidationException("ERROR - currencySet mismatch");
    }

    @JsonIgnore
    public Map<String, org.votingsystem.model.currency.Currency> getCurrencyMap() throws ValidationException {
        if(currencyList == null)
            throw new ValidationException("Empty currencyList");
        Map<String, org.votingsystem.model.currency.Currency> result = new HashMap<>();
        for(org.votingsystem.model.currency.Currency currency : currencyList) {
            result.put(currency.getRevocationHash(), currency);
        }
        return result;
    }

    @JsonIgnore
    public void checkBatchItem(CurrencyDto batchItem) throws ValidationException {
        String currencyData = "batchItem with hash '" + batchItem.getRevocationHash() + "' ";
        if(!subject.equals(batchItem.getSubject())) throw new ValidationException(
                currencyData + "expected subject " + subject + " found " + batchItem.getSubject());
        if(toUserIBAN != null) {
            if(!toUserIBAN.equals(batchItem.getToUserIBAN())) throw new ValidationException(
                    currencyData + "expected toUserIBAN " + toUserIBAN + " found " + batchItem.getToUserIBAN());
        }
        if(!currencyCode.equals(batchItem.getCurrencyCode())) throw new ValidationException(
                currencyData + "expected currencyCode " + currencyCode + " found " + batchItem.getCurrencyCode());
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

    public List<org.votingsystem.model.currency.Currency> getCurrencyList() {
        return currencyList;
    }

    public void setCurrencyList(List<org.votingsystem.model.currency.Currency> currencyList) {
        this.currencyList = currencyList;
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

    public PKCS10CertificationRequest getLeftOverCsr() {
        return leftOverCsr;
    }

    public void setLeftOverCsr(PKCS10CertificationRequest leftOverCsr) {
        this.leftOverCsr = leftOverCsr;
    }

    public org.votingsystem.model.currency.Currency getLeftOverCurrency() {
        return leftOverCurrency;
    }

    public void setLeftOverCurrency(org.votingsystem.model.currency.Currency leftOverCurrency) {
        this.leftOverCurrency = leftOverCurrency;
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
}