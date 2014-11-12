package org.votingsystem.model;

import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.signature.util.CertUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Vicket extends ReceiptContainer {

    private static final long serialVersionUID = 1L;

    public static final String TAG = Vicket.class.getSimpleName();

    public enum State { OK, REJECTED, CANCELLED, EXPENDED, LAPSED;}

    private Long localId = -1L;
    private TransactionVS transaction;
    private transient SMIMEMessage receipt;
    private transient SMIMEMessage cancellationReceipt;
    private transient SMIMEMessage smimeMessage;
    private transient X509Certificate x509AnonymousCert;
    private transient CertSubject certSubject;
    private CertificationRequestVS certificationRequest;
    private byte[] receiptBytes;
    private byte[] cancellationReceiptBytes;
    private String originHashCertVS;
    private String hashCertVS;
    private BigDecimal amount;
    private String signedTagVS;
    private String subject;
    private State state;
    private Date cancellationDate;
    private String currencyCode;
    private String url;
    private String vicketServerURL;
    private Boolean isTimeLimited = Boolean.FALSE;
    private Date validFrom;
    private Date validTo;
    private String toUserIBAN;
    private String toUserName;

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

    public Vicket(SMIMEMessage smimeMessage) throws Exception {
        this.smimeMessage = smimeMessage;
        x509AnonymousCert = smimeMessage.getVicketCert();
        JSONObject certExtensionData = CertUtils.getCertExtensionData(x509AnonymousCert, ContextVS.VICKET_OID);
        initCertData(certExtensionData, smimeMessage.getVicketCert().getSubjectDN().toString());
        validateSignedData();
    }

    public void initSigner(byte[] csrBytes) throws Exception {
        certificationRequest.initSigner(csrBytes);
        x509AnonymousCert = certificationRequest.getCertificate();
        JSONObject certExtensionData = CertUtils.getCertExtensionData(x509AnonymousCert, ContextVS.VICKET_OID);
        initCertData(certExtensionData, x509AnonymousCert.getSubjectDN().toString());
        certSubject.addDateInfo(x509AnonymousCert);
    }

    public void validateSignedData() throws Exception {
        JSONObject messageJSON = new  JSONObject(smimeMessage.getSignedContent());
        BigDecimal toUserVSAmount = new BigDecimal(messageJSON.getString("amount"));
        TypeVS operation = TypeVS.valueOf(messageJSON.getString("operation"));
        if(TypeVS.VICKET_SEND != operation)
            throw new ExceptionVS("Error - Vicket with invalid operation '" + operation.toString() + "'");
        if(amount.compareTo(toUserVSAmount) != 0) throw new ExceptionVS(getErrorPrefix() + "and value '" + amount +
                "' has signed amount  '" + toUserVSAmount + "'");
        if(!currencyCode.equals(messageJSON.getString("currencyCode"))) throw new ExceptionVS(getErrorPrefix() +
                "currencyCode '" + currencyCode + "' has signed currencyCode  '" + messageJSON.getString("currencyCode"));
        if(!signedTagVS.equals(messageJSON.getString("tag"))) throw new ExceptionVS(getErrorPrefix() + "tag '" +
                signedTagVS + "' has signed tag  '" + messageJSON.getString("tag"));
        Date signatureTime = smimeMessage.getTimeStampToken(x509AnonymousCert).getTimeStampInfo().getGenTime();
        if(signatureTime.after(x509AnonymousCert.getNotAfter())) throw new ExceptionVS(getErrorPrefix() + "valid to '" +
                x509AnonymousCert.getNotAfter().toString() + "' has signature date '" + signatureTime.toString() + "'");
        if(messageJSON.getBoolean("isTimeLimited") == false) {
            boolean isTimeLimited = checkIfTimeLimited(x509AnonymousCert.getNotBefore(), x509AnonymousCert.getNotAfter());
            if(isTimeLimited) throw new ExceptionVS(getErrorPrefix() +
                    "Time limited Vicket with 'isTimeLimited' signature param set to false");
        }
        subject = messageJSON.getString("subject");
        toUserIBAN = messageJSON.getString("toUserIBAN");
        toUserName = messageJSON.getString("toUserName");
        if(messageJSON.has("isTimeLimited")) isTimeLimited = messageJSON.getBoolean("isTimeLimited");
    }

    public static boolean checkIfTimeLimited(Date notBefore, Date notAfter) {
        long diff = notAfter.getTime() - notBefore.getTime();
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) <= 7;//one week
    }

    private String getErrorPrefix() {
        return "ERROR - Vicket with hash: " + hashCertVS + " - ";
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
                new SMIMEMessage(new ByteArrayInputStream(receiptBytes));
        return receipt;
    }

    public SMIMEMessage getCancellationReceipt() throws Exception {
        if(cancellationReceipt == null && cancellationReceiptBytes != null) cancellationReceipt =
                new SMIMEMessage(new ByteArrayInputStream(cancellationReceiptBytes));
        return cancellationReceipt;
    }

    public State getState() {
        return state;
    }

    public Vicket setState(State state) {
        this.state = state;
        return this;
    }

    public Boolean getIsTimeLimited() {
        return isTimeLimited;
    }


    public void setReceiptBytes(byte[] receiptBytes) {
        this.state = State.EXPENDED;
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

    public Vicket.CertSubject getCertSubject() {return certSubject;}

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

    public JSONObject getTransaction(String toUserName,
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

    public Vicket initCertData(JSONObject certExtensionData, String subjectDN)
            throws ExceptionVS, JSONException {
        if(certExtensionData == null) throw new ExceptionVS("Vicket without cert extension data");
        if(!certExtensionData.has("hashCertVS"))  throw new ExceptionVS("Vicket without cert hash");
        vicketServerURL = certExtensionData.getString("vicketServerURL");
        hashCertVS = certExtensionData.getString("hashCertVS");
        if(hashCertVS == null) throw new ExceptionVS("Vicket without hash");
        certSubject = new Vicket.CertSubject(subjectDN, hashCertVS);
        amount = new BigDecimal(certExtensionData.getString("vicketValue"));
        currencyCode = certExtensionData.getString("currencyCode");
        signedTagVS = certExtensionData.getString("tag");
        if(!certSubject.getVicketServerURL().equals(vicketServerURL) ||
                certSubject.getVicketValue().compareTo(amount) != 0 ||
                !certSubject.getCurrencyCode().equals(currencyCode) ||
                !certSubject.getTag().equals(signedTagVS))
                    throw new ExceptionVS("Vicket with errors. SubjectDN: '" +
                subjectDN + "' - cert extension data: '" + certExtensionData.toString() + "'");
        return this;
    }

    public static class CertSubject implements Serializable {

        public static final long serialVersionUID = 1L;

        private String currencyCode;
        private BigDecimal vicketValue;
        private String vicketServerURL;
        private String tag;
        private String hashCertVS;
        private Map<String, String> dataMap;
        private Date notBefore;
        private Date notAfter;

        public CertSubject(String subjectDN, String hashCertVS) {
            this.hashCertVS = hashCertVS;
            if (subjectDN.contains("CURRENCY_CODE:")) currencyCode =
                    subjectDN.split("CURRENCY_CODE:")[1].split(",")[0];
            if (subjectDN.contains("VICKET_VALUE:")) vicketValue =
                    new BigDecimal(subjectDN.split("VICKET_VALUE:")[1].split(",")[0]);
            if (subjectDN.contains("TAG:")) tag = subjectDN.split("TAG:")[1].split(",")[0];
            if (subjectDN.contains("vicketServerURL:")) vicketServerURL =
                    subjectDN.split("vicketServerURL:")[1].split(",")[0];
            dataMap = new HashMap<String, String>();
            dataMap.put("vicketServerURL", vicketServerURL);
            dataMap.put("currencyCode", currencyCode);
            dataMap.put("vicketValue", vicketValue.toString());
            dataMap.put("tag", tag);
            dataMap.put("hashCertVS", hashCertVS);
        }

        public void addDateInfo(X509Certificate x509AnonymousCert) {
            this.notBefore = x509AnonymousCert.getNotBefore();
            this.notAfter = x509AnonymousCert.getNotAfter();
            dataMap.put("notBefore", DateUtils.getDateStr(x509AnonymousCert.getNotBefore()));
            dataMap.put("notAfter", DateUtils.getDateStr(x509AnonymousCert.getNotAfter()));
        }

        public String getCurrencyCode() {return this.currencyCode;}
        public BigDecimal getVicketValue() {return this.vicketValue;}
        public String getVicketServerURL() {return this.vicketServerURL;}
        public String getTag() {return this.tag;}
        public Map<String, String> getDataMap() {return dataMap;}
        public Date getNotBefore() { return notBefore; }
        public Date getNotAfter() { return notAfter; }
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        try {
            if(smimeMessage != null) s.writeObject(smimeMessage.getBytes());
            else s.writeObject(null);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void readObject(ObjectInputStream s) throws Exception {
        s.defaultReadObject();
        byte[] smimeMessageBytes = (byte[]) s.readObject();
        if(smimeMessageBytes != null) {
            smimeMessage = new SMIMEMessage(new ByteArrayInputStream(smimeMessageBytes));
        }
        if(x509AnonymousCert != null) {
            validFrom = x509AnonymousCert.getNotBefore();
            validTo = x509AnonymousCert.getNotAfter();
        }

    }

}