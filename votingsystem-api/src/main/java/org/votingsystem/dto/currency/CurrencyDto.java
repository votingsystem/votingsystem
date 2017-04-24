package org.votingsystem.dto.currency;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.CertificationRequest;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.CurrencyCode;
import org.votingsystem.util.CurrencyOperation;
import org.votingsystem.util.ObjectUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrencyDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private CurrencyOperation operation = CurrencyOperation.CURRENCY_SEND;
    private BigDecimal amount;
    private BigDecimal batchAmount;
    private CurrencyCode currencyCode;
    private String currencyEntity;
    private String revocationHash;
    private String subject;
    private String toUserIBAN;
    private String toUserName;
    private String batchUUID;
    private String serializedObject;
    private ZonedDateTime notBefore;
    private ZonedDateTime notAfter;
    private ZonedDateTime dateCreated;

    @JsonIgnore
    private PKCS10CertificationRequest csrPKCS10;

    public CurrencyDto() {}

    public static CurrencyDto BATCH_ITEM(CurrencyBatchDto batchDto, Currency currency) throws ValidationException {
        if(!batchDto.getCurrencyCode().equals(currency.getCurrencyCode())) throw new ValidationException(
                "CurrencyBatch currencyCode: " + batchDto.getCurrencyCode()
                + " - Currency currencyCode: " + currency.getCurrencyCode());
        CurrencyDto currencyDto = new CurrencyDto(currency);
        currencyDto.subject = batchDto.getSubject();
        currencyDto.toUserIBAN = batchDto.getToUserIBAN();
        currencyDto.toUserName = batchDto.getToUserName();
        currencyDto.batchAmount = batchDto.getBatchAmount();
        currencyDto.batchUUID = batchDto.getBatchUUID();
        return currencyDto;
    }

    public CurrencyDto(PKCS10CertificationRequest csrPKCS10) throws Exception {
        this.csrPKCS10 = csrPKCS10;
        CertificationRequestInfo info = csrPKCS10.toASN1Structure().getCertificationRequestInfo();
        String subjectDN = info.getSubject().toString();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                csrPKCS10, Constants.CURRENCY_OID);
        if(certExtensionDto == null) throw new ValidationException("error missing cert extension data");
        currencyEntity = certExtensionDto.getCurrencyEntity();
        revocationHash = certExtensionDto.getRevocationHash();
        amount = certExtensionDto.getAmount();
        currencyCode = certExtensionDto.getCurrencyCode();
        CurrencyDto certSubjectDto = getCertSubjectDto(subjectDN, revocationHash);
        if(!certSubjectDto.getCurrencyEntity().equals(currencyEntity))
                throw new ValidationException("currencyEntity: " + currencyEntity + " - certSubject: " + subjectDN);
        if(certSubjectDto.getAmount().compareTo(amount) != 0)
            throw new ValidationException("amount: " + amount + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getCurrencyCode().equals(currencyCode))
            throw new ValidationException("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
    }

    public CurrencyDto(Currency currency) {
        this.id = currency.getId();
        this.revocationHash = currency.getRevocationHash();
        this.amount = currency.getAmount();
        this.currencyCode = currency.getCurrencyCode();
        this.dateCreated = ZonedDateTime.of(currency.getDateCreated(), ZoneId.systemDefault());
        this.notBefore = ZonedDateTime.of(currency.getValidFrom(), ZoneId.systemDefault());
        this.notAfter = ZonedDateTime.of(currency.getValidTo(), ZoneId.systemDefault());
    }

    public static CurrencyDto serialize(Currency currency) throws Exception {
        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setAmount(currency.getAmount());
        currencyDto.setCurrencyCode(currency.getCurrencyCode());
        currencyDto.setRevocationHash(currency.getRevocationHash());
        //CertificationRequest instead of Currency to make it easier deserialization on Android
        currencyDto.setSerialized(ObjectUtils.serializeObjectToString(currency.getCertificationRequest()));
        return currencyDto;
    }

    public static CurrencyDto getCertSubjectDto(String subjectDN, String revocationHash) {
        CurrencyDto currencyDto = new CurrencyDto();
        if (subjectDN.contains("CURRENCY_CODE:"))
            currencyDto.setCurrencyCode(CurrencyCode.valueOf(subjectDN.split("CURRENCY_CODE:")[1].split(",")[0]));
        if (subjectDN.contains("CURRENCY_VALUE:"))
            currencyDto.setAmount(new BigDecimal(subjectDN.split("CURRENCY_VALUE:")[1].split(",")[0]));
        if (subjectDN.contains("currencyEntity:"))
            currencyDto.setCurrencyEntity(subjectDN.split("currencyEntity:")[1].split(",")[0]);
        currencyDto.setRevocationHash(revocationHash);
        return currencyDto;
    }

    public static Set<CurrencyDto> serializeCollection(Collection<Currency> currencyCollection) throws Exception {
        Set<CurrencyDto> result = new HashSet<>();
        for (Currency currency : currencyCollection) {
            result.add(CurrencyDto.serialize(currency));
        }
        return result;
    }

    public Currency deSerialize() throws Exception {
        try {
            CertificationRequest certificationRequest =
                    (CertificationRequest) ObjectUtils.deSerializeObject(serializedObject.getBytes());
            return Currency.fromCertificationRequest(certificationRequest);
        }catch (Exception ex) {
            return (Currency) ObjectUtils.deSerializeObject(serializedObject.getBytes());
        }
    }

    public static Set<Currency> deSerialize(Collection<CurrencyDto> currencyCollection) throws Exception {
        Set<Currency> result = new HashSet<>();
        for(CurrencyDto currencyDto : currencyCollection) {
            result.add(currencyDto.deSerialize());
        }
        return result;
    }

    public String getSerialized() {
        return serializedObject;
    }

    public void setSerialized(String serializedObject) {
        this.serializedObject = serializedObject;
    }

    public String getRevocationHash() {
        return revocationHash;
    }

    public void setRevocationHash(String revocationHash) {
        this.revocationHash = revocationHash;
    }

    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(CurrencyCode currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyEntity() {
        return currencyEntity;
    }

    public void setCurrencyEntity(String currencyEntity) {
        this.currencyEntity = currencyEntity;
    }

    public ZonedDateTime getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(ZonedDateTime notBefore) {
        this.notBefore = notBefore;
    }

    public ZonedDateTime getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(ZonedDateTime notAfter) {
        this.notAfter = notAfter;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ZonedDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(ZonedDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public CurrencyOperation getOperation() {
        return operation;
    }

    public CurrencyDto setOperation(CurrencyOperation operation) {
        this.operation = operation;
        return this;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public PKCS10CertificationRequest getCsrPKCS10() {
        return csrPKCS10;
    }

    public void setCsrPKCS10(PKCS10CertificationRequest csrPKCS10) {
        this.csrPKCS10 = csrPKCS10;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

}
