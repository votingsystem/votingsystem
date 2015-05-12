package org.votingsystem.dto.currency;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.TimePeriod;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurrencyDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private BigDecimal amount;
    private String currencyCode;
    private String currencyServerURL;
    private String hashCertVS;
    private String tag;
    private Boolean timeLimited;
    private String object;
    private String certificationRequest;
    private Date notBefore;
    private Date notAfter;
    private Date dateCreated;

    @JsonIgnore private PKCS10CertificationRequest csr;

    public CurrencyDto() {}


    public CurrencyDto(PKCS10CertificationRequest csr) throws Exception {
        this.setCsr(csr);
        CertificationRequestInfo info = csr.getCertificationRequestInfo();
        String subjectDN = info.getSubject().toString();
        CurrencyCertExtensionDto certExtensionDto = CertUtils.getCertExtensionData(CurrencyCertExtensionDto.class,
                csr, ContextVS.CURRENCY_TAG);
        if(certExtensionDto == null) throw new ValidationExceptionVS("error missing cert extension data");
        currencyServerURL = certExtensionDto.getCurrencyServerURL();
        hashCertVS = certExtensionDto.getHashCertVS();
        amount = certExtensionDto.getAmount();
        currencyCode = certExtensionDto.getCurrencyCode();
        tag = certExtensionDto.getTag();
        CurrencyDto certSubjectDto = getCertSubjectDto(subjectDN, hashCertVS);
        if(!certSubjectDto.getCurrencyServerURL().equals(currencyServerURL))
                throw new ValidationExceptionVS("currencyServerURL: " + currencyServerURL + " - certSubject: " + subjectDN);
        if(certSubjectDto.getAmount().compareTo(amount) != 0)
            throw new ValidationExceptionVS("amount: " + amount + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getCurrencyCode().equals(currencyCode))
            throw new ValidationExceptionVS("currencyCode: " + currencyCode + " - certSubject: " + subjectDN);
        if(!certSubjectDto.getTag().equals(tag)) throw new ValidationExceptionVS("tag: " + tag + " - certSubject: " + subjectDN);
    }

    public CurrencyDto(Currency currency) {
        this.id = currency.getId();
        this.amount = currency.getAmount();
        this.currencyCode = currency.getCurrencyCode();
        this.tag = currency.getTag().getName();
        this.timeLimited = currency.getTimeLimited();
        this.dateCreated = currency.getDateCreated();
        this.notBefore = currency.getValidFrom();
        this.notAfter = currency.getValidTo();
    }

    public CurrencyDto(Boolean timeLimited, String object) {
        this.timeLimited = timeLimited;
        this.object = object;
    }

    public static CurrencyDto serialize(Currency currency) throws Exception {
        CurrencyDto currencyDto = new CurrencyDto();
        currencyDto.setAmount(currency.getAmount());
        currencyDto.setCurrencyCode(currency.getCurrencyCode());
        currencyDto.setHashCertVS(currency.getHashCertVS());
        currencyDto.setTag(currency.getTag().getName());
        currencyDto.setTimeLimited(currency.getTimeLimited());
        currencyDto.setObject(ObjectUtils.serializeObjectToString(currency));
        //if(currency.getCertificationRequest() != null)
        //    currencyDto.setCertificationRequest(new String(currency.getCertificationRequest().getCsrPEM()));
        return currencyDto;
    }


    public static CurrencyDto getCertSubjectDto(String subjectDN, String hashCertVS) {
        CurrencyDto currencyDto = new CurrencyDto();
        if (subjectDN.contains("CURRENCY_CODE:"))
            currencyDto.setCurrencyCode(subjectDN.split("CURRENCY_CODE:")[1].split(",")[0]);
        if (subjectDN.contains("CURRENCY_VALUE:"))
            currencyDto.setAmount(new BigDecimal(subjectDN.split("CURRENCY_VALUE:")[1].split(",")[0]));
        if (subjectDN.contains("TAG:")) currencyDto.setTag(subjectDN.split("TAG:")[1].split(",")[0]);
        if (subjectDN.contains("currencyServerURL:"))
            currencyDto.setCurrencyServerURL(subjectDN.split("currencyServerURL:")[1].split(",")[0]);
        currencyDto.setHashCertVS(hashCertVS);
        return currencyDto;
    }

    public static Set<CurrencyDto> serializeCollection(Collection<Currency> currencyCollection) throws Exception {
        Set<CurrencyDto> result = new HashSet<>();
        for(Currency currency : currencyCollection) {
            result.add(CurrencyDto.serialize(currency));
        }
        return result;
    }

    public Currency deSerialize() throws Exception {
        /*if(certificationRequest != null) {
            CertificationRequestVS certificationRequestVS = (CertificationRequestVS) ObjectUtils.deSerializeObject(
                    certificationRequest.getBytes());
            return Currency.fromCertificationRequestVS(certificationRequestVS);
        } else {
            return (Currency) ObjectUtils.deSerializeObject(object.getBytes());
        }*/
        return (Currency) ObjectUtils.deSerializeObject(object.getBytes());
    }

    public static Set<Currency> deSerializeCollection(Collection<CurrencyDto> currencyCollection) throws Exception {
        Set<Currency> result = new HashSet<>();
        for(CurrencyDto currencyDto : currencyCollection) {
            result.add(currencyDto.deSerialize());
        }
        return result;
    }

    public Currency loadCertData(X509Certificate x509AnonymousCert, TimePeriod timePeriod, TagVS tagVS,
                                 CertificateVS authorityCertificateVS) throws CertificateEncodingException {
        Currency currency = new Currency();
        currency.setAmount(amount);
        currency.setCurrencyCode(currencyCode);
        currency.setTag(tagVS);
        currency.setHashCertVS(hashCertVS);
        currency.setTimeLimited(timeLimited);
        currency.setX509AnonymousCert(x509AnonymousCert);
        currency.setSerialNumber(x509AnonymousCert.getSerialNumber().longValue());
        currency.setContent(x509AnonymousCert.getEncoded());
        currency.setState(Currency.State.OK);
        currency.setValidFrom(timePeriod.getDateFrom());
        currency.setValidTo(timePeriod.getDateTo());
        currency.setAuthorityCertificateVS(authorityCertificateVS);
        return currency;
    }

    public Boolean isTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public String getCertificationRequest() {
        return certificationRequest;
    }

    public void setCertificationRequest(String certificationRequest) {
        this.certificationRequest = certificationRequest;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrencyServerURL() {
        return currencyServerURL;
    }

    public void setCurrencyServerURL(String currencyServerURL) {
        this.currencyServerURL = currencyServerURL;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public void setNotBefore(Date notBefore) {
        this.notBefore = notBefore;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public void setNotAfter(Date notAfter) {
        this.notAfter = notAfter;
    }

    public PKCS10CertificationRequest getCsr() {
        return csr;
    }

    public void setCsr(PKCS10CertificationRequest csr) {
        this.csr = csr;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }
}
