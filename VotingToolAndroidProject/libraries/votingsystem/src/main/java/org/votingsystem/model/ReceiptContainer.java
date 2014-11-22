package org.votingsystem.model;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;
import org.votingsystem.android.lib.R;
import org.votingsystem.signature.smime.SMIMEMessage;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ReceiptContainer implements Serializable {

    public static final String TAG = ReceiptContainer.class.getSimpleName();

    private Long localId = -1L;
    private transient SMIMEMessage receipt;
    private byte[] receiptBytes;
    private TypeVS typeVS;
    private String subject;
    private String url;

    public ReceiptContainer() {}

    public ReceiptContainer(TypeVS typeVS, String url) {
        this.typeVS = typeVS;
        this.url = url;
    }

    public ReceiptContainer(TransactionVS transactionVS) {
        this.typeVS = transactionVS.getTypeVS();
        this.url = transactionVS.getMessageSMIMEURL();
        receiptBytes = transactionVS.getMessageSMIMEBytes();
    }

    private static final long serialVersionUID = 1L;

    public enum State {ACTIVE, CANCELLED}


    public String getTypeDescription(Context context) {
        switch(getTypeVS()) {
            case VOTEVS:
                return context.getString(R.string.receipt_vote_subtitle);
            case CANCEL_VOTE:
            case VOTEVS_CANCELLED:
                return context.getString(R.string.receipt_cancel_vote_subtitle);
            case REPRESENTATIVE_SELECTION:
            case ANONYMOUS_REPRESENTATIVE_REQUEST:
                return context.getString(R.string.receipt_anonimous_representative_request_subtitle);
            case VICKET_REQUEST:
                return context.getString(R.string.vicket_request_subtitle);
            case ANONYMOUS_REPRESENTATIVE_SELECTION:
                return context.getString(R.string.anonymous_representative_selection_lbl);
            default:
                return context.getString(R.string.receipt_lbl) + ": " + getTypeVS().toString();
        }
    }

    public int getLogoId() {
        switch(getTypeVS()) {
            case VOTEVS:
            case CANCEL_VOTE:
            case VOTEVS_CANCELLED:
                return R.drawable.poll_32;
            case REPRESENTATIVE_SELECTION:
            case ANONYMOUS_REPRESENTATIVE_REQUEST:
                return R.drawable.system_users_32;
            default:
                return R.drawable.receipt_32;
        }
    }

    public String getURL() {
        return url;
    }

    public void setReceiptBytes(byte[] receiptBytes) throws Exception {
        this.receiptBytes = receiptBytes;
        if(receiptBytes != null) {
            receipt = new SMIMEMessage(new ByteArrayInputStream(receiptBytes));
            subject = receipt.getSubject();
            receipt.isValidSignature();
            JSONObject signedJSON = new JSONObject(receipt.getSignedContent());
            if(signedJSON.has("operation"))
                this.typeVS = TypeVS.valueOf(signedJSON.getString("operation"));
        }
    }

    public String getMessageId() {
        String result = null;
        if(receipt != null) {
            try {
                String[] headers = receipt.getHeader("Message-ID");
                if(headers != null && headers.length >0) return headers[0];
            } catch(Exception ex) { ex.printStackTrace(); }
        }
        return result;
    }

    public SMIMEMessage getReceipt() throws Exception {
        if(receipt == null && receiptBytes != null) {
            receipt = new SMIMEMessage(new ByteArrayInputStream(receiptBytes));
        }
        receipt.isValidSignature();
        return receipt;
    }

    public boolean hashReceipt() {
        return (receipt != null || receiptBytes != null);
    }

    public String getSubject() {
        return subject;
    }

    public TypeVS getTypeVS() {
        if(typeVS == null && (receipt != null || receiptBytes != null)) {
            try {
                JSONObject signedContent = new JSONObject(getReceipt().getSignedContent());
                if(signedContent.has("operation")) {
                    typeVS = TypeVS.valueOf(signedContent.getString("operation"));
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        }
        return typeVS;
    }

    public void setTypeVS(TypeVS typeVS) {
        this.typeVS = typeVS;
    }

    public Date getDateFrom() {
        Date result = null;
        try {
            result = getReceipt().getSigner().getCertificate().getNotBefore();
        } catch(Exception ex) { ex.printStackTrace(); }
        return result;
    }

    public Date getDateTo() {
        Date result = null;
        try {
            result = getReceipt().getSigner().getCertificate().getNotAfter();
        } catch(Exception ex) { ex.printStackTrace(); }
        return result;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }

}