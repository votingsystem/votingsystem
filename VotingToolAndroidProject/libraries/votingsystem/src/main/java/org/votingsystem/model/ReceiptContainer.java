package org.votingsystem.model;

import android.content.Context;

import org.votingsystem.android.R;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class ReceiptContainer implements Serializable {

    private Long localId = -1L;
    private transient SMIMEMessageWrapper receipt;
    private byte[] receiptBytes;
    private TypeVS typeVS;
    private String subject;
    private String url;

    public ReceiptContainer() {}

    public ReceiptContainer(TypeVS typeVS, String url) {
        this.typeVS = typeVS;
        this.url = url;
    }

    private static final long serialVersionUID = 1L;

    public enum State {ACTIVE, CANCELLED}


    public String getTypeDescription(Context context) {
        switch(getType()) {
            case VOTEVS:
                return context.getString(R.string.receipt_vote_subtitle);
            case CANCEL_VOTE:
            case VOTEVS_CANCELLED:
                return context.getString(R.string.receipt_cancel_vote_subtitle);
            case REPRESENTATIVE_SELECTION:
            case ANONYMOUS_REPRESENTATIVE_REQUEST:
                return context.getString(R.string.receipt_anonimous_representative_request_subtitle);
            default:
                return context.getString(R.string.receipt_lbl);
        }
    }

    public int getLogoId() {
        switch(getType()) {
            case VOTEVS:
            case CANCEL_VOTE:
            case VOTEVS_CANCELLED:
                return R.drawable.poll_16x16;
            case REPRESENTATIVE_SELECTION:
            case ANONYMOUS_REPRESENTATIVE_REQUEST:
                return R.drawable.system_users_16;
            default:
                return R.drawable.receipt_32;
        }
    }

    public String getURL() {
        return url;
    }

    public void setReceiptBytes(byte[] receiptBytes) throws Exception {
        this.receiptBytes = receiptBytes;
        receipt = new SMIMEMessageWrapper(null, new ByteArrayInputStream(receiptBytes), null);
        subject = receipt.getSubject();
    }

    public String getMessageId() {
        String result = null;
        if(receipt != null) {
            try {
                String[] headers = receipt.getHeader("Message-ID");
                if(headers != null && headers.length >0) return headers[0];
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }

    public SMIMEMessageWrapper getReceipt() throws Exception {
        if(receipt == null && receiptBytes != null) {
            try {
                receipt = new SMIMEMessageWrapper(
                        null, new ByteArrayInputStream(receiptBytes), null);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return receipt;
    }

    public String getSubject() {
        return subject;
    }

    public TypeVS getType() {
        return typeVS;
    }

    public Date getValidFrom() {
        Date result = null;
        try {
            result = getReceipt().getSigner().getCertificate().getNotBefore();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public Date getValidTo() {
        Date result = null;
        try {
            result = getReceipt().getSigner().getCertificate().getNotAfter();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

}