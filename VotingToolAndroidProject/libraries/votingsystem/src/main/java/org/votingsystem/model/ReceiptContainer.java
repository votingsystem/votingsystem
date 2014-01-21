package org.votingsystem.model;

import android.content.Context;

import org.votingsystem.android.R;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;

import java.io.Serializable;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public abstract class ReceiptContainer implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum State {ACTIVE, CANCELLED}

    public abstract String getSubject();

    public abstract TypeVS getType() ;

    public abstract Date getValidFrom();

    public abstract Date getValidTo();

    public abstract Long getLocalId();

    public abstract void setLocalId(Long localId);

    public abstract SMIMEMessageWrapper getReceipt() throws Exception;

    public abstract String getMessageId();

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
                return R.drawable.receipt_22;
        }
    }


}