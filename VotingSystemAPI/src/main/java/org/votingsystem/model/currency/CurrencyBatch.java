package org.votingsystem.model.currency;

import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.model.BatchRequest;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;
import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.util.*;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("CurrencyBatch")
public class CurrencyBatch extends BatchRequest implements Serializable {

    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(CurrencyBatch.class.getSimpleName());

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;
    @Column(name="batchAmount") private BigDecimal batchAmount = null;
    @Column(name="currencyAmount") private BigDecimal currencyAmount = null;
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="timeLimited") private Boolean timeLimited = Boolean.FALSE;
    @OneToOne private MessageSMIME messageSMIME;
    @Column(name="batchUUID") private String batchUUID;
    @Column(name="subject") private String subject;

    @Transient private List<Currency> currencyList;
    @Transient private CurrencyDto leftOverCurrency;
    @Transient private BigDecimal leftOver;
    @Transient private String currencyCode;
    @Transient private String toUserIBAN;

    public CurrencyBatch() {}

    public CurrencyBatch(List<Currency> currencyList) {
        this.currencyList = currencyList;
    }

    public void addCurrency(Currency currency) {
        if(currencyList == null) currencyList = new ArrayList<Currency>();
        currencyList.add(currency);
    }

    public CurrencyBatchDto getDto(String subject, String toUserIBAN,
            BigDecimal batchAmount, String currencyCode, String tag, Boolean isTimeLimited, String timeStampServiceURL)
            throws Exception {
        this.subject = subject;
        this.toUserIBAN = toUserIBAN;
        this.setBatchAmount(batchAmount);
        this.currencyCode = currencyCode;
        this.tagVS = new TagVS(tag);
        this.batchUUID = UUID.randomUUID().toString();
        CurrencyBatchDto dto = new CurrencyBatchDto(this);
        List<String> currencyTransactionBatch = new ArrayList<String>();
        for (Currency currency : currencyList) {
            SMIMEMessage smimeMessage = currency.getCertificationRequest().getSMIME(currency.getHashCertVS(),
                    StringUtils.getNormalized(currency.getToUserName()), JSON.getMapper().writeValueAsString(dto),
                    subject, null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampServiceURL);
            currency.setSMIME(timeStamper.call());
            currencyTransactionBatch.add(Base64.getEncoder().encodeToString(currency.getSMIME().getBytes()));
        }
        dto.setCurrency(currencyTransactionBatch);
        return dto;
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

    public CurrencyDto getLeftOverCurrency() {
        return leftOverCurrency;
    }

    public List<Currency> getCurrencyList() {
        return currencyList;
    }


    public void setCurrencyList(List<Currency> currencyList) {
        this.currencyList = currencyList;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public BigDecimal getCurrencyAmount() {
        return currencyAmount;
    }

    public void setCurrencyAmount(BigDecimal currencyAmount) {
        this.currencyAmount = currencyAmount;
    }

    public TagVS getTagVS() {
        return tagVS;
    }

    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
    }

    public String getToUserIBAN() {
        return toUserIBAN;
    }

    public void setToUserIBAN(String toUserIBAN) {
        this.toUserIBAN = toUserIBAN;
    }

    public String getBatchUUID() {
        return batchUUID;
    }

    public void setBatchUUID(String batchUUID) {
        this.batchUUID = batchUUID;
    }

    public UserVS getToUserVS() {
        return toUserVS;
    }

    public CurrencyBatch setToUserVS(UserVS toUserVS) {
        this.toUserVS = toUserVS;
        return this;
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

    public BigDecimal getLeftOver() {
        return leftOver;
    }

    public void setLeftOver(BigDecimal leftOver) {
        this.leftOver = leftOver;
    }

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public CurrencyBatch setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
        return this;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public void setLeftOverCurrency(CurrencyDto leftOverCurrency) {
        this.leftOverCurrency = leftOverCurrency;
    }

    public Boolean getTimeLimited() {
        return timeLimited;
    }

    public void setTimeLimited(Boolean timeLimited) {
        this.timeLimited = timeLimited;
    }
}
