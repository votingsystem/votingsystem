package org.votingsystem.model.currency;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.model.BatchRequest;
import org.votingsystem.model.MessageSMIME;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
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
@DiscriminatorValue("CurrencyTransactionBatch")
public class CurrencyTransactionBatch extends BatchRequest implements Serializable {

    private static java.util.logging.Logger log = java.util.logging.Logger.getLogger(CurrencyTransactionBatch.class.getSimpleName());


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

    public CurrencyTransactionBatch(Map dataMap) throws Exception {
        super(new ObjectMapper().writeValueAsString(dataMap).getBytes("UTF-8"));
        currencyAmount = BigDecimal.ZERO;
        currencyList = new ArrayList<Currency>();
        if(dataMap.get("csrCurrency") != null) {
            PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(
                    ((String)dataMap.get("csrCurrency")).getBytes());
            leftOverCurrency = new Currency(csr);
        }
        Iterator<JsonNode> ite = ((ArrayNode)dataMap.get("currency")).elements();
        boolean initialized = false;
        while (ite.hasNext()) {
            JsonNode node = ite.next();
            SMIMEMessage smimeMessage = new SMIMEMessage(new ByteArrayInputStream(
                    Base64.getDecoder().decode(node.asText().getBytes())));
            smimeMessage.isValidSignature();
            try {
                Currency currency = new Currency(smimeMessage);
                currencyAmount = currencyAmount.add(currency.getAmount());
                currencyList.add(currency);
                if(!initialized) {
                    initialized = true;
                    this.operation = currency.getOperation();
                    this.paymentMethod = currency.getPaymentMethod();
                    this.setSubject(currency.getSubject());
                    this.toUserIBAN = currency.getToUserIBAN();
                    this.batchAmount = currency.getBatchAmount();
                    this.setCurrencyCode(currency.getCurrencyCode());
                    this.tag = currency.getTag().getName();
                    this.isTimeLimited = currency.getIsTimeLimited();
                    this.batchUUID = currency.getBatchUUID();
                } else checkCurrencyData(currency);
            } catch(Exception ex) {
                throw new ExceptionVS("Error with currency : " + ex.getMessage(), ex);
            }
        }
        leftOver = currencyAmount.subtract(batchAmount);
        if(leftOver.compareTo(BigDecimal.ZERO) < 0) new ValidationExceptionVS(
                "CurrencyTransactionBatch insufficientCash - required '" + batchAmount.toString() + "' " + "found '" +
                currencyAmount.toString() + "'");
        if(leftOverCurrency != null && leftOver.compareTo(leftOverCurrency.getAmount()) != 0) new ValidationExceptionVS(
                "CurrencyTransactionBatch leftOverMissMatch, expected '" + leftOver.toString() +
                "found '" + leftOverCurrency.getAmount().toString() + "'");
    }

    public Map getDataMap() {
        Map result = new HashMap<>();
        result.put("operation", this.operation.toString());
        result.put("paymentMethod", this.paymentMethod.toString());
        if(getSubject() != null) result.put("subject", getSubject());
        if(toUserIBAN != null) result.put("toUserIBAN", toUserIBAN);
        if(batchAmount != null) result.put("batchAmount", batchAmount.toString());
        if(currencyAmount != null) result.put("currencyAmount", currencyAmount.toString());
        if(getCurrencyCode() != null) result.put("currencyCode", getCurrencyCode());
        if(tag != null) result.put("tag", tag);
        List<String> hashCertVSCurrency = new ArrayList<>();
        for(Currency currency : currencyList) {
            hashCertVSCurrency.add(currency.getHashCertVS());
        }
        result.put("hashCertVSCurrency", hashCertVSCurrency);
        result.put("isTimeLimited", isTimeLimited);
        if(batchUUID != null) result.put("batchUUID", batchUUID);
        return result;
    }

    public CurrencyTransactionBatch() {}

    public CurrencyTransactionBatch(List<Currency> currencyList) {
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

    public void checkCurrencyData(Currency currency) throws ExceptionVS {
        String currencyData = "Currency with hash '" + currency.getHashCertVS() + "' ";
        if(operation != currency.getOperation()) throw new ValidationExceptionVS(
                currencyData + "expected operation " + operation + " found " + currency.getOperation());
        if(paymentMethod != currency.getPaymentMethod()) throw new ValidationExceptionVS(
                currencyData + "expected paymentOption " + paymentMethod + " found " + currency.getPaymentMethod());
        if(!getSubject().equals(currency.getSubject())) throw new ValidationExceptionVS(
                currencyData + "expected subject " + getSubject() + " found " + currency.getSubject());
        if(!toUserIBAN.equals(currency.getToUserIBAN())) throw new ValidationExceptionVS(
                currencyData + "expected subject " + toUserIBAN + " found " + currency.getToUserIBAN());
        if(batchAmount.compareTo(currency.getBatchAmount()) != 0) throw new ValidationExceptionVS(
                currencyData + "expected batchAmount " + batchAmount.toString() + " found " + currency.getBatchAmount().toString());
        if(!getCurrencyCode().equals(currency.getCurrencyCode())) throw new ValidationExceptionVS(
                currencyData + "expected currencyCode " + getCurrencyCode() + " found " + currency.getCurrencyCode());
        if(!tag.equals(currency.getTag().getName())) throw new ValidationExceptionVS(
                currencyData + "expected tag " + tag + " found " + currency.getTag().getName());
        if(!batchUUID.equals(currency.getBatchUUID())) throw new ValidationExceptionVS(
                currencyData + "expected batchUUID " + batchUUID + " found " + currency.getBatchUUID());
    }

    public void initTransactionVSRequest(String toUserName, String toUserIBAN, String subject,
                 Boolean isTimeLimited, String timeStampServiceURL) throws Exception {
        for(Currency currency : currencyList) {
            Map transactionRequest = currency.getTransaction(toUserName, toUserIBAN, subject, isTimeLimited);
            SMIMEMessage smimeMessage = currency.getCertificationRequest().getSMIME(currency.getHashCertVS(),
                    StringUtils.getNormalized(currency.getToUserName()),
                    new ObjectMapper().writeValueAsString(transactionRequest), currency.getSubject(), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampServiceURL);
            currency.setSMIME(timeStamper.call());
        }
    }
    public Map getTransactionVSRequest(TypeVS operation, Payment paymentMethod, String subject, String toUserIBAN,
            BigDecimal batchAmount, String currencyCode, String tag, Boolean isTimeLimited, String timeStampServiceURL)
            throws Exception {
        this.operation = operation;
        this.paymentMethod = paymentMethod;
        this.subject = subject;
        this.toUserIBAN = toUserIBAN;
        this.batchAmount = batchAmount;
        this.currencyCode = currencyCode;
        this.tag = tag;
        this.batchUUID = UUID.randomUUID().toString();
        Map transactionRequest = getDataMap();
        Map result = new HashMap<>();
        List<String> currencyTransactionBatch = new ArrayList<String>();
        for (Currency currency : currencyList) {
            SMIMEMessage smimeMessage = currency.getCertificationRequest().getSMIME(currency.getHashCertVS(),
                    StringUtils.getNormalized(currency.getToUserName()),
                    new ObjectMapper().writeValueAsString(transactionRequest), subject, null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampServiceURL);
            currency.setSMIME(timeStamper.call());
            currencyTransactionBatch.add(Base64.getEncoder().encodeToString(currency.getSMIME().getBytes()));
        }
        result.put("currency", currencyTransactionBatch);
        return result;
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

    public CurrencyTransactionBatch setToUserVS(UserVS toUserVS) {
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

    public CurrencyTransactionBatch setMessageSMIME(MessageSMIME messageSMIME) {
        this.messageSMIME = messageSMIME;
        return this;
    }
}
