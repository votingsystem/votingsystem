package org.votingsystem.cooin.model;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.model.*;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.StringUtils;

import javax.persistence.*;
import java.io.*;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Entity
@DiscriminatorValue("CooinTransactionBatch")
public class CooinTransactionBatch extends BatchRequest implements Serializable {

    private static Logger log = Logger.getLogger(CooinTransactionBatch.class);

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="toUserVS") private UserVS toUserVS;
    @Column(name="batchAmount") private BigDecimal batchAmount = null;
    @Column(name="cooinAmount") private BigDecimal cooinAmount = null;
    @ManyToOne(fetch= FetchType.LAZY)
    @JoinColumn(name="tagVS", nullable=false) private TagVS tagVS;
    @Column(name="isTimeLimited") private Boolean isTimeLimited = Boolean.FALSE;
    @Column(name="paymentMethod", nullable=false) @Enumerated(EnumType.STRING) private Payment paymentMethod;
    @Column(name="batchUUID") private String batchUUID;
    @Column(name="subject") private String subject;

    @Transient private List<Cooin> cooinList;
    @Transient private Cooin leftOverCooin;
    @Transient private BigDecimal leftOver;
    @Transient private TypeVS operation;
    @Transient private String currencyCode;
    @Transient private String toUserIBAN;
    @Transient private String tag;

    public CooinTransactionBatch(String contentStr) throws Exception {
        super(contentStr.getBytes("UTF-8"));
        cooinAmount = BigDecimal.ZERO;
        JSONObject jsonObject = (JSONObject) JSONSerializer.toJSON(contentStr);
        cooinList = new ArrayList<Cooin>();
        JSONArray jsonArray = jsonObject.getJSONArray("cooins");
        if(jsonObject.containsKey("csrCooin")) {
            PKCS10CertificationRequest csr = CertUtils.fromPEMToPKCS10CertificationRequest(
                    jsonObject.getString("csrCooin").getBytes());
            leftOverCooin = new Cooin(csr);
        }
        for(int i = 0; i < jsonArray.size(); i++) {
            SMIMEMessage smimeMessage = new SMIMEMessage(new ByteArrayInputStream(
                    Base64.getDecoder().decode(jsonArray.getString(i).getBytes())));
            smimeMessage.isValidSignature();
            try {
                Cooin cooin = new Cooin(smimeMessage);
                cooinAmount = cooinAmount.add(cooin.getAmount());
                cooinList.add(cooin);
                if(i == 0) {
                    this.operation = cooin.getOperation();
                    this.paymentMethod = cooin.getPaymentMethod();
                    this.setSubject(cooin.getSubject());
                    this.toUserIBAN = cooin.getToUserIBAN();
                    this.batchAmount = cooin.getBatchAmount();
                    this.setCurrencyCode(cooin.getCurrencyCode());
                    this.tag = cooin.getTag().getName();
                    this.isTimeLimited = cooin.getIsTimeLimited();
                    this.batchUUID = cooin.getBatchUUID();
                } else checkCooinData(cooin);
            } catch(Exception ex) {
                throw new ExceptionVS("Error with cooin : '" + i + "' - " + ex.getMessage(), ex);
            }
        }
        leftOver = cooinAmount.subtract(batchAmount);
        if(leftOver.compareTo(BigDecimal.ZERO) < 0) new ValidationExceptionVS(CooinTransactionBatch.class,
                "CooinTransactionBatch insufficientCash - required '" + batchAmount.toString() + "' " + "found '" +
                cooinAmount.toString() + "'");
        if(leftOverCooin != null && leftOver.compareTo(leftOverCooin.getAmount()) != 0) new ValidationExceptionVS(
                CooinTransactionBatch.class, "CooinTransactionBatch leftOverMissMatch, expected '" + leftOver.toString() +
                "found '" + leftOverCooin.getAmount().toString() + "'");
    }

    public JSONObject getDataJSON() {
        JSONObject result = new JSONObject();
        result.put("operation", this.operation.toString());
        result.put("paymentMethod", this.paymentMethod.toString());
        if(getSubject() != null) result.put("subject", getSubject());
        if(toUserIBAN != null) result.put("toUserIBAN", toUserIBAN);
        if(batchAmount != null) result.put("batchAmount", batchAmount.toString());
        if(cooinAmount != null) result.put("cooinAmount", cooinAmount.toString());
        if(getCurrencyCode() != null) result.put("currencyCode", getCurrencyCode());
        if(tag != null) result.put("tag", tag);
        List<String> hashCertVSCooins = new ArrayList<>();
        for(Cooin cooin: cooinList) {
            hashCertVSCooins.add(cooin.getHashCertVS());
        }
        result.put("hashCertVSCooins", hashCertVSCooins);
        result.put("isTimeLimited", isTimeLimited);
        if(batchUUID != null) result.put("batchUUID", batchUUID);
        return result;
    }

    public CooinTransactionBatch() {}

    public void addCooin(Cooin cooin) {
        if(cooinList == null) cooinList = new ArrayList<Cooin>();
        cooinList.add(cooin);
    }

    public void addCooin(File cooinFile) throws IOException {
        log.debug("addCooin - file name: " + cooinFile.getName());
        Cooin cooin = (Cooin) ObjectUtils.deSerializeObject(FileUtils.getBytesFromFile(cooinFile));
        cooin.setFile(cooinFile);
        addCooin(cooin);
    }

    public void checkCooinData(Cooin cooin) throws ExceptionVS {
        String cooinData = "Cooin with hash '" + cooin.getHashCertVS() + "' ";
        if(operation != cooin.getOperation()) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected operation " + operation + " found " + cooin.getOperation());
        if(paymentMethod != cooin.getPaymentMethod()) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected paymentOption " + paymentMethod + " found " + cooin.getPaymentMethod());
        if(!getSubject().equals(cooin.getSubject())) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected subject " + getSubject() + " found " + cooin.getSubject());
        if(!toUserIBAN.equals(cooin.getToUserIBAN())) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected subject " + toUserIBAN + " found " + cooin.getToUserIBAN());
        if(batchAmount.compareTo(cooin.getBatchAmount()) != 0) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected batchAmount " + batchAmount.toString() + " found " + cooin.getBatchAmount().toString());
        if(!getCurrencyCode().equals(cooin.getCurrencyCode())) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected currencyCode " + getCurrencyCode() + " found " + cooin.getCurrencyCode());
        if(!tag.equals(cooin.getTag().getName())) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected tag " + tag + " found " + cooin.getTag().getName());
        if(!batchUUID.equals(cooin.getBatchUUID())) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected batchUUID " + batchUUID + " found " + cooin.getBatchUUID());
    }

    public void initTransactionVSRequest(String toUserName, String toUserIBAN, String subject,
                 Boolean isTimeLimited, String timeStampServiceURL) throws Exception {
        for(Cooin cooin : cooinList) {
            JSONObject transactionRequest = cooin.getTransaction(toUserName, toUserIBAN, subject, isTimeLimited);
            SMIMEMessage smimeMessage = cooin.getCertificationRequest().getSMIME(cooin.getHashCertVS(),
                    StringUtils.getNormalized(cooin.getToUserName()),
                    transactionRequest.toString(), cooin.getSubject(), null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampServiceURL);
            ResponseVS responseVS = timeStamper.call();
            if(ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            cooin.setSMIME(timeStamper.getSMIME());
        }
    }
    public JSONObject getTransactionVSRequest(TypeVS operation, Payment paymentMethod, String subject, String toUserIBAN,
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
        JSONObject transactionRequest = getDataJSON();
        JSONObject requestJSON = new JSONObject();
        List<String> cooinTransactionBatch = new ArrayList<String>();
        for (Cooin cooin : cooinList) {
            SMIMEMessage smimeMessage = cooin.getCertificationRequest().getSMIME(cooin.getHashCertVS(),
                    StringUtils.getNormalized(cooin.getToUserName()), transactionRequest.toString(), subject, null);
            MessageTimeStamper timeStamper = new MessageTimeStamper(smimeMessage, timeStampServiceURL);
            ResponseVS responseVS = timeStamper.call();
            if (ResponseVS.SC_OK != responseVS.getStatusCode()) throw new ExceptionVS(responseVS.getMessage());
            cooin.setSMIME(timeStamper.getSMIME());
            cooinTransactionBatch.add(Base64.getEncoder().encodeToString(cooin.getSMIME().getBytes()));
        }
        requestJSON.put("cooins", cooinTransactionBatch);
        return requestJSON;
    }

    public void validateTransactionVSResponse(JSONObject responseJSON, Set<TrustAnchor> trustAnchor) throws Exception {
        SMIMEMessage receipt = new SMIMEMessage(new ByteArrayInputStream(
                Base64.getDecoder().decode(responseJSON.getString("receipt").getBytes())));
        if(responseJSON.containsKey("leftOverCoin")) {

        }


        Map<String, Cooin> cooinMap = getCooinMap();
        if(cooinMap.size() != responseJSON.size()) throw new ExceptionVS("Num. cooins: '" +
                cooinMap.size() + "' - num. receipts: " + responseJSON.size());
        for(int i = 0; i < responseJSON.size(); i++) {
            JSONObject receiptData = (JSONObject) responseJSON.get(i);
            String hashCertVS = (String) receiptData.keySet().iterator().next();
            SMIMEMessage smimeReceipt = new SMIMEMessage(new ByteArrayInputStream(
                    Base64.getDecoder().decode(receiptData.getString(hashCertVS).getBytes())));
            String signatureHashCertVS = CertUtils.getHashCertVS(smimeReceipt.getCooinCert(), ContextVS.COOIN_OID);
            Cooin cooin = cooinMap.remove(signatureHashCertVS);
            cooin.validateReceipt(smimeReceipt, trustAnchor);
        }
        if(cooinMap.size() != 0) throw new ExceptionVS(cooinMap.size() + " Cooin transactions without receipt");
    }

    public Map<String, Cooin> getCooinMap() throws ExceptionVS {
        if(cooinList == null) throw new ExceptionVS("Empty cooinList");
        Map<String, Cooin> result = new HashMap<String, Cooin>();
        for(Cooin cooin : cooinList) {
            result.put(cooin.getHashCertVS(), cooin);
        }
        return result;
    }

    public Cooin getLeftOverCooin() {
        return leftOverCooin;
    }

    public List<Cooin> getCooinList() {
        return cooinList;
    }


    public void setCooinList(List<Cooin> cooinList) {
        this.cooinList = cooinList;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public BigDecimal getCooinAmount() {
        return cooinAmount;
    }

    public void setCooinAmount(BigDecimal cooinAmount) {
        this.cooinAmount = cooinAmount;
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

    public CooinTransactionBatch setToUserVS(UserVS toUserVS) {
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
}
