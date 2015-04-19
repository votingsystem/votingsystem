package org.votingsystem.model.currency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.dto.currency.CurrencyBatchDto;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.BatchRequest;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.*;

import javax.persistence.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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
    @Column(name="isTimeLimited") private Boolean isTimeLimited = Boolean.FALSE;
    @Column(name="paymentMethod", nullable=false) @Enumerated(EnumType.STRING) private Payment paymentMethod;
    @OneToOne private MessageSMIME messageSMIME;
    @Column(name="batchUUID") private String batchUUID;
    @Column(name="subject") private String subject;

    @Transient private List<Currency> currencyList;
    @Transient private Currency leftOverCurrency;
    @Transient private BigDecimal leftOver;
    @Transient private TypeVS operation;
    @Transient private String currencyCode;
    @Transient private String toUserIBAN;
    @Transient private String tag;


    public CurrencyBatch() {}

    public CurrencyBatch(List<Currency> currencyList) {
        this.currencyList = currencyList;
    }

    public void addCurrency(Currency currency) {
        if(currencyList == null) currencyList = new ArrayList<Currency>();
        currencyList.add(currency);
    }

    public void addCurrency(File currencyFile) throws IOException {
        log.info("addCurrency - file name: " + currencyFile.getName());
        Currency currency = (Currency) ObjectUtils.deSerializeObject(FileUtils.getBytesFromFile(currencyFile));
        currency.setFile(currencyFile);
        addCurrency(currency);
    }

    public void initTransactionVSRequest(String toUserName, String toUserIBAN, String subject,
                 Boolean isTimeLimited, String timeStampServiceURL) throws Exception {
        for(Currency currency : currencyList) {
            TransactionVSDto requestDto = currency.getSendRequest(toUserName, toUserIBAN, subject, isTimeLimited);
            SMIMEMessage smimeMessage = currency.getCertificationRequest().getSMIME(currency.getHashCertVS(),
                    StringUtils.getNormalized(currency.getToUserName()),
                    new ObjectMapper().writeValueAsString(requestDto), currency.getSubject(), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampServiceURL);
            currency.setSMIME(timeStamper.call());
        }
    }
    public CurrencyBatchDto getTransactionVSRequest(TypeVS operation, Payment paymentMethod, String subject, String toUserIBAN,
            BigDecimal batchAmount, String currencyCode, String tag, Boolean isTimeLimited, String timeStampServiceURL)
            throws Exception {
        this.setOperation(operation);
        this.paymentMethod = paymentMethod;
        this.subject = subject;
        this.toUserIBAN = toUserIBAN;
        this.setBatchAmount(batchAmount);
        this.currencyCode = currencyCode;
        this.tag = tag;
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

    public void validateTransactionVSResponse(Map dataMap, Set<TrustAnchor> trustAnchor) throws Exception {
        SMIMEMessage receipt = new SMIMEMessage(new ByteArrayInputStream(
                Base64.getDecoder().decode(((String)dataMap.get("receipt")).getBytes())));
        if(dataMap.containsKey("leftOverCoin")) {

        }


        Map<String, Currency> currencyMap = getCurrencyMap();
        if(currencyMap.size() != dataMap.size()) throw new ExceptionVS("Num. currency: '" +
                currencyMap.size() + "' - num. receipts: " + dataMap.size());
        for(int i = 0; i < dataMap.size(); i++) {
            Map receiptData = (Map) dataMap.get(i);
            String hashCertVS = (String) receiptData.keySet().iterator().next();
            SMIMEMessage smimeReceipt = new SMIMEMessage(new ByteArrayInputStream(
                    Base64.getDecoder().decode(((String)receiptData.get(hashCertVS)).getBytes())));
            String signatureHashCertVS = CertUtils.getHashCertVS(smimeReceipt.getCurrencyCert(), ContextVS.CURRENCY_OID);
            Currency currency = currencyMap.remove(signatureHashCertVS);
            currency.validateReceipt(smimeReceipt, trustAnchor);
        }
        if(currencyMap.size() != 0) throw new ExceptionVS(currencyMap.size() + " Currency transactions without receipt");
    }

    public Map<String, Currency> getCurrencyMap() throws ExceptionVS {
        if(currencyList == null) throw new ExceptionVS("Empty currencyList");
        Map<String, Currency> result = new HashMap<String, Currency>();
        for(Currency currency : currencyList) {
            result.put(currency.getHashCertVS(), currency);
        }
        return result;
    }

    public Currency getLeftOverCurrency() {
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

    public String getTag() {
        return tag;
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

    public Payment getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(Payment paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public Boolean getIsTimeLimited() {
        return isTimeLimited;
    }

    public void setIsTimeLimited(Boolean isTimeLimited) {
        this.isTimeLimited = isTimeLimited;
    }

    public MessageSMIME getMessageSMIME() {
        return messageSMIME;
    }

    public CurrencyBatch setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
        return this;
    }

    public TypeVS getOperation() {
        return operation;
    }

    public void setOperation(TypeVS operation) {
        this.operation = operation;
    }

    public void setBatchAmount(BigDecimal batchAmount) {
        this.batchAmount = batchAmount;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}