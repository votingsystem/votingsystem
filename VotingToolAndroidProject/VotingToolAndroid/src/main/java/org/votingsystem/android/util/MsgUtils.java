package org.votingsystem.android.util;

import android.content.Context;
import android.text.TextUtils;
import android.text.format.DateUtils;

import org.votingsystem.android.R;
import org.votingsystem.model.TagVS;
import org.votingsystem.model.TransactionVS;
import org.votingsystem.model.Vicket;

import java.security.cert.X509Certificate;
import java.util.Formatter;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    /**
     * Flags used with {@link android.text.format.DateUtils#formatDateRange}.
     */
    private static final int TIME_FLAGS = DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY;

    public static String formatEventVSSubtitle(Context context,String params) {
        return null;
    }

    public static String formatIntervalTimeString(long intervalStart, long intervalEnd,
              StringBuilder recycle, Context context) {
        if (recycle == null) {
            recycle = new StringBuilder();
        } else {
            recycle.setLength(0);
        }
        Formatter formatter = new Formatter(recycle);
        return DateUtils.formatDateRange(context, formatter, intervalStart, intervalEnd, TIME_FLAGS,
                PrefUtils.getDisplayTimeZone(context).getID()).toString();
    }

    public static String getHashtagsString(String hashtags) {
        if (!TextUtils.isEmpty(hashtags)) {
            if (!hashtags.startsWith("#")) {
                hashtags = "#" + hashtags;
            }
        }
        return null;
    }

    public static String getTagVSMessage(String tag, Context context) {
        if(TagVS.WILDTAG.equals(tag)) return context.getString(R.string.wildtag_lbl);
        else return tag;
    }

    public static String getVicketDescriptionMessage(Vicket vicket, Context context) {
        return vicket.getAmount().toPlainString() + " " + vicket.getCurrencyCode() +
                " " + context.getString(R.string.for_lbl ) + " '" +
                getTagVSMessage(vicket.getSignedTagVS(), context) + "'";
    }

    public static String getCertInfoMessage(X509Certificate certificate, Context context) {
        return context.getString(R.string.cert_info_formated_msg,
                certificate.getSubjectDN().toString(),
                certificate.getIssuerDN().toString(),
                certificate.getSerialNumber().toString(),
                org.votingsystem.util.DateUtils.getDayWeekDateStr(certificate.getNotBefore()),
                org.votingsystem.util.DateUtils.getDayWeekDateStr(certificate.getNotAfter()));
    }

    public static String getVicketStateMessage(Vicket vicket, Context context) {
        switch(vicket.getState()) {
            case CANCELLED: return context.getString(R.string.cancelled_lbl);
            case EXPENDED: return context.getString(R.string.expended_lbl);
            case LAPSED: return context.getString(R.string.lapsed_lbl);
            case REJECTED: return context.getString(R.string.rejected_lbl);
            default:return null;
        }
    }

    public static String getVicketRequestMessage(TransactionVS transactionVS, Context context) {
        String tagMessage = getTagVSMessage(transactionVS.getTagVS().getName(), context);
        return context.getString(R.string.vicket_request_msg, transactionVS.getAmount().toPlainString(),
                transactionVS.getCurrencyCode(), tagMessage);
    }

}
