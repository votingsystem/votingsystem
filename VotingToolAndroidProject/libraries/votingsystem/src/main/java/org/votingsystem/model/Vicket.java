package org.votingsystem.model;

import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.signature.util.CertificationRequestVS;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Date;
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
    private transient SMIMEMessageWrapper receipt;
    private transient SMIMEMessageWrapper cancellationReceipt;
    private CertificationRequestVS certificationRequest;
    private byte[] receiptBytes;
    private byte[] cancellationReceiptBytes;
    private String originHashCertVS;
    private String hashCertVSBase64;
    private BigDecimal amount;
    private String subject;
    private State state;
    private Date cancellationDate;
    private CurrencyVS currency;
    private String url;
    private String vicketServerURL;

    public Vicket(String vicketServerURL, BigDecimal amount, CurrencyVS currency, TypeVS typeVS) {
        this.amount = amount;
        setTypeVS(typeVS);
        this.vicketServerURL = vicketServerURL;
        this.currency = currency;
        try {
            setOriginHashCertVS(UUID.randomUUID().toString());
            setHashCertVSBase64(CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST));
            certificationRequest = CertificationRequestVS.getVicketRequest(
                    ContextVS.KEY_SIZE, ContextVS.SIG_NAME, ContextVS.VOTE_SIGN_MECHANISM,
                    ContextVS.PROVIDER, vicketServerURL, hashCertVSBase64, amount.toString(),
                    currency.toString());


            //public static CertificationRequestVS getVicketRequest(int keySize, String keyName, String signatureMechanism, String provider, String vicketProviderURL, String hashCertVS, String amount)

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public CertificationRequestVS getCertificationRequest() {
        return certificationRequest;
    }

    public CurrencyVS getCurrency() {
        return currency;
    }

    public void setCurrency(CurrencyVS currency) {
        this.currency = currency;
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

    @Override public SMIMEMessageWrapper getReceipt() throws Exception {
        if(receipt == null && receiptBytes != null) receipt =
                new SMIMEMessageWrapper(null, new ByteArrayInputStream(receiptBytes), null);
        return receipt;
    }

    public SMIMEMessageWrapper getCancellationReceipt() throws Exception {
        if(cancellationReceipt == null && cancellationReceiptBytes != null) cancellationReceipt =
                new SMIMEMessageWrapper(null, new ByteArrayInputStream(cancellationReceiptBytes), null);
        return cancellationReceipt;
    }

    public void setReceiptBytes(byte[] receiptBytes) {
        this.setState(State.EXPENDED);
        this.receiptBytes = receiptBytes;
    }

    public void setCancellationReceiptBytes(byte[] receiptBytes) {
        this.cancellationReceiptBytes = receiptBytes;
    }

    public void setCancellationReceipt(SMIMEMessageWrapper receipt) {
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
            SMIMEMessageWrapper receipt = getReceipt();
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

    public String getHashCertVSBase64() {
        return hashCertVSBase64;
    }

    public void setHashCertVSBase64(String hashCertVSBase64) {
        this.hashCertVSBase64 = hashCertVSBase64;
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
}