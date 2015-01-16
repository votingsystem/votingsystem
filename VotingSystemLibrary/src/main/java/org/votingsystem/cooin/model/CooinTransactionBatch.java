package org.votingsystem.cooin.model;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.callable.MessageTimeStamper;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;
import org.votingsystem.util.StringUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.cert.TrustAnchor;
import java.util.*;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinTransactionBatch {

    private static Logger log = Logger.getLogger(CooinTransactionBatch.class);

    private List<Cooin> cooinList;
    private String csrCooin;
    private TypeVS operation;
    private String subject;
    private String currencyCode;
    private String toUserIBAN;
    private String tag;
    private String cooinBatchUUID;
    private BigDecimal batchAmount;

    public CooinTransactionBatch() {}

    public CooinTransactionBatch(List<Cooin> cooinList) {
        this.cooinList = cooinList;
    }

    public CooinTransactionBatch(List<Cooin> cooinList, String csrCooin) {
        this.cooinList = cooinList;
        this.csrCooin = csrCooin;
    }

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

    public CooinTransactionBatch(String cooinsArrayStr) throws Exception {
        JSONArray cooinsArray = (JSONArray) JSONSerializer.toJSON(cooinsArrayStr);
        cooinList = new ArrayList<Cooin>();
        for (int i = 0; i < cooinsArray.size(); i++) {
            SMIMEMessage smimeMessage = new SMIMEMessage(new ByteArrayInputStream(
                    Base64.getDecoder().decode(cooinsArray.getString(i).getBytes())));
            try {
                cooinList.add(new Cooin(smimeMessage));
            } catch (Exception ex) {
                throw new ExceptionVS("Error on cooin number: '" + i + "' - " + ex.getMessage(), ex);
            }
        }
    }

    public void loadTransactionData(Cooin cooin) {
        this.operation = cooin.getOperation();
        this.subject = cooin.getSubject();
        this.toUserIBAN = cooin.getToUserIBAN();
        this.batchAmount = cooin.getBatchAmount();
        this.currencyCode = cooin.getCurrencyCode();
        this.tag = cooin.getTag().getName();
        this.cooinBatchUUID = cooin.getCooinBatchUUID();
    }

    public void checkCooinData(Cooin cooin) throws ExceptionVS {
        String cooinData = "Cooin with hash '" + cooin.getHashCertVS() + "' ";
        if(operation != cooin.getOperation()) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected operation " + operation.toString() + " found " + cooin.getOperation().toString());
        if(!subject.equals(cooin.getSubject())) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected subject " + subject + " found " + cooin.getSubject());
        if(!toUserIBAN.equals(cooin.getToUserIBAN())) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected subject " + toUserIBAN + " found " + cooin.getToUserIBAN());
        if(batchAmount.compareTo(cooin.getBatchAmount()) != 0) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected batchAmount " + batchAmount.toString() + " found " + cooin.getBatchAmount().toString());
        if(!currencyCode.equals(cooin.getCurrencyCode())) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected currencyCode " + currencyCode + " found " + cooin.getCurrencyCode());
        if(!tag.equals(cooin.getTag().getName())) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected tag " + tag + " found " + cooin.getTag().getName());
        if(!cooinBatchUUID.equals(cooin.getCooinBatchUUID())) throw new ValidationExceptionVS(CooinTransactionBatch.class,
                cooinData + "expected cooinBatchUUID " + cooinBatchUUID + " found " + cooin.getCooinBatchUUID());
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

    public JSONArray getTransactionVSRequest() throws Exception {
        List<String> cooinTransactionBatch = new ArrayList<String>();
        for(Cooin cooin : cooinList) {
            cooinTransactionBatch.add(Base64.getEncoder().encodeToString(cooin.getSMIME().getBytes()));
        }
        return (JSONArray) JSONSerializer.toJSON(cooinTransactionBatch);
    }

    public void validateTransactionVSResponse(JSONArray responseJSON, Set<TrustAnchor> trustAnchor) throws Exception {
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

    public List<Cooin> getCooinList() {
        return cooinList;
    }


    public void setCooinList(List<Cooin> cooinList) {
        this.cooinList = cooinList;
    }

    public BigDecimal getBatchAmount() {
        return batchAmount;
    }

    public String getCsrCooin() {
        return csrCooin;
    }

    public void setCsrCooin(String csrCooin) {
        this.csrCooin = csrCooin;
    }
}
