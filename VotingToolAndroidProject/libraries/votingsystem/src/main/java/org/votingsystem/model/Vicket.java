package org.votingsystem.model;

import org.json.JSONObject;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertificationRequestVS;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Vicket extends ReceiptContainer {

    private static final long serialVersionUID = 1L;

    public static final String TAG = Vicket.class.getSimpleName();

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public enum State { OK, REJECTED, CANCELLED, EXPENDED, LAPSED;}

    private Long localId = -1L;
    private TransactionVS transaction;
    private transient SMIMEMessage receipt;
    private transient SMIMEMessage cancellationReceipt;
    private CertificationRequestVS certificationRequest;
    private byte[] receiptBytes;
    private byte[] cancellationReceiptBytes;
    private String originHashCertVS;
    private String hashCertVS;
    private BigDecimal amount;
    private String subject;
    private State state;
    private Date cancellationDate;
    private String currencyCode;
    private String url;
    private String vicketServerURL;

    public Vicket(String vicketServerURL, BigDecimal amount, String currencyCode, String tagVS,
              TypeVS typeVS) {
        this.amount = amount;
        setTypeVS(typeVS);
        this.vicketServerURL = vicketServerURL;
        this.currencyCode = currencyCode;
        try {
            setOriginHashCertVS(UUID.randomUUID().toString());
            setHashCertVS(CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST));
            certificationRequest = CertificationRequestVS.getVicketRequest(
                    ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                    ContextVS.PROVIDER, vicketServerURL, hashCertVS, amount.toString(),
                    this.currencyCode, tagVS);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public CertificationRequestVS getCertificationRequest() {
        return certificationRequest;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Override public String getSubject() {
        return subject;
    }

    @Override public Date getValidFrom() {
        return certificationRequest.getCertificate().getNotBefore();
    }

    @Override public Date getValidTo() {
        return certificationRequest.getCertificate().getNotAfter();
    }

    @Override public Long getLocalId() {
        return localId;
    }

    @Override public void setLocalId(Long localId) {
        this.localId = localId;
    }

    @Override public SMIMEMessage getReceipt() throws Exception {
        if(receipt == null && receiptBytes != null) receipt =
                new SMIMEMessage(null, new ByteArrayInputStream(receiptBytes), null);
        return receipt;
    }

    public SMIMEMessage getCancellationReceipt() throws Exception {
        if(cancellationReceipt == null && cancellationReceiptBytes != null) cancellationReceipt =
                new SMIMEMessage(null, new ByteArrayInputStream(cancellationReceiptBytes), null);
        return cancellationReceipt;
    }

    public void setReceiptBytes(byte[] receiptBytes) {
        this.setState(State.EXPENDED);
        this.receiptBytes = receiptBytes;
    }

    public void setCancellationReceiptBytes(byte[] receiptBytes) {
        this.cancellationReceiptBytes = receiptBytes;
    }

    public void setCancellationReceipt(SMIMEMessage receipt) {
        try {
            this.cancellationReceiptBytes = receipt.getBytes();
            this.cancellationReceipt = receipt;
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public Date getCancellationDate() {
        try {
            if(cancellationDate == null && getCancellationReceipt() != null)
                cancellationDate = getCancellationReceipt().getSigner().getTimeStampToken().
                        getTimeStampInfo().getGenTime();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return cancellationDate;
    }

    @Override public String getMessageId() {
        String result = null;
        try {
            SMIMEMessage receipt = getReceipt();
            if(receipt == null) return null;
            String[] headers = receipt.getHeader("Message-ID");
            if(headers != null && headers.length >0) return headers[0];
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public String getOriginHashCertVS() {
        return originHashCertVS;
    }

    public void setOriginHashCertVS(String originHashCertVS) {
        this.originHashCertVS = originHashCertVS;
    }

    public String getHashCertVS() {
        return hashCertVS;
    }

    public void setHashCertVS(String hashCertVS) {
        this.hashCertVS = hashCertVS;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }


    public TransactionVS getTransaction() {
        return transaction;
    }

    public void setTransaction(TransactionVS transaction) {
        this.transaction = transaction;
    }

    public static JSONObject getUserVSAccountInfoRequest(String nif) {
        Map mapToSend = new HashMap();
        mapToSend.put("NIF", nif);
        mapToSend.put("operation", TypeVS.VICKET_USER_INFO.toString());
        mapToSend.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(mapToSend);
    }

    public JSONObject getCancellationRequest() {
        Map dataMap = new HashMap();
        dataMap.put("UUID", UUID.randomUUID().toString());
        dataMap.put("operation", TypeVS.VICKET_CANCEL.toString());
        dataMap.put("hashCertVS", getHashCertVS());
        dataMap.put("originHashCertVS", getOriginHashCertVS());
        dataMap.put("vicketCertSerialNumber", getCertificationRequest().
                getCertificate().getSerialNumber().longValue());
        return new JSONObject(dataMap);
    }

    public JSONObject getTransactionRequest(String toUserName,
            String toUserIBAN, String tag, Boolean isTimeLimited) {
        Map dataMap = new HashMap();
        dataMap.put("operation", TypeVS.VICKET.toString());
        dataMap.put("subject", subject);
        dataMap.put("toUser", toUserName);
        dataMap.put("toUserIBAN", toUserIBAN);
        dataMap.put("tagVS", tag);
        dataMap.put("amount", amount.toString());
        dataMap.put("currencyCode", currencyCode);
        if(isTimeLimited != null) dataMap.put("isTimeLimited", isTimeLimited.booleanValue());
        dataMap.put("UUID", UUID.randomUUID().toString());
        return new JSONObject(dataMap);
    }

}