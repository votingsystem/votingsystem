package org.votingsystem.android.util;

import android.content.Context;

import org.votingsystem.android.R;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.model.TypeVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    public static ResponseVS getBroadcastResponse(TypeVS operation, String serviceCaller,
              ResponseVS responseVS, Context context) {
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            responseVS.setIconId(R.drawable.fa_check_32);
            responseVS.setCaption(context.getString(R.string.ok_lbl));

        } else {
            responseVS.setIconId(R.drawable.fa_times_32);
            responseVS.setCaption(context.getString(R.string.error_lbl));
        }
        responseVS.setTypeVS(operation);
        responseVS.setServiceCaller(serviceCaller);
        return responseVS;
    }

}
