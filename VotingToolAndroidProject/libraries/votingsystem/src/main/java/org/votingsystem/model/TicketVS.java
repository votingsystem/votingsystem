package org.votingsystem.model;

import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class TicketVS extends ReceiptContainer {

    private static final long serialVersionUID = 1L;

    public static final String TAG = "TicketVS";

    private Long localId = -1L;
    private transient SMIMEMessageWrapper receipt;
    private byte[] receiptBytes;
    private String originHashCertVS;
    private String hashCertVSBase64;
    private BigDecimal amount;
    private TypeVS typeVS;
    private String subject;
    private String url;

    public TicketVS(BigDecimal amount, TypeVS typeVS) {
        this.setAmount(amount);
        this.setTypeVS(typeVS);
        try {
            setOriginHashCertVS(UUID.randomUUID().toString());
            setHashCertVSBase64(CMSUtils.getHashBase64(getOriginHashCertVS(), ContextVS.VOTING_DATA_DIGEST));
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override public String getSubject() {
        return subject;
    }

    @Override public TypeVS getType() {
        return typeVS;
    }

    @Override public Date getValidFrom() {
        return receipt.getSigner().getCertificate().getNotBefore();
    }

    @Override public Date getValidTo() {
        return receipt.getSigner().getCertificate().getNotAfter();
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

    public void setReceiptBytes(byte[] receiptBytes) {
        this.receiptBytes = receiptBytes;
    }

    @Override public String getMessageId() {
        String result = null;
        try {
            SMIMEMessageWrapper receipt = getReceipt();
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

    public TypeVS getTypeVS() {
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

}