package org.votingsystem.model;

import android.content.Context;

import org.votingsystem.android.lib.R;
import java.util.Arrays;
import java.util.List;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum Payment {

    SIGNED_TRANSACTION, ANONYMOUS_SIGNED_TRANSACTION, COOIN_SEND;

    public static List<String> getPaymentMethods(Context context) {
        //preserve the same order
        List<String> result = Arrays.asList(
                context.getString(R.string.signed_transaction_lbl),
                context.getString(R.string.anonymous_signed_transaction_lbl),
                context.getString(R.string.cooin_send_lbl));
        return result;
    }

    public static Payment getByPosition(int position) {
        if(position == 0) return SIGNED_TRANSACTION;
        if(position == 1) return ANONYMOUS_SIGNED_TRANSACTION;
        if(position == 2) return COOIN_SEND;
        return null;
    }

    public String getDescription(Context context) {
        switch(this) {
            case SIGNED_TRANSACTION: return context.getString(R.string.signed_transaction_lbl);
            case ANONYMOUS_SIGNED_TRANSACTION: return context.getString(R.string.anonymous_signed_transaction_lbl);
            case COOIN_SEND: return context.getString(R.string.cooin_send_lbl);
        }
        return null;
    }
}
