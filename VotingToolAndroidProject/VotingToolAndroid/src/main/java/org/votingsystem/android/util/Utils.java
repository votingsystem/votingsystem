package org.votingsystem.android.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import org.votingsystem.android.R;
import org.votingsystem.model.TypeVS;
import org.votingsystem.util.ResponseVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Utils {

    public static ResponseVS getBroadcastResponse(TypeVS operation, String serviceCaller,
              ResponseVS responseVS, Context context) {
        if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
            if(responseVS.getIconId() == null) responseVS.setIconId(R.drawable.fa_check_32);
            if(responseVS.getCaption() == null) responseVS.setCaption(context.getString(R.string.ok_lbl));

        } else {
            if(responseVS.getIconId() == null) responseVS.setIconId(R.drawable.fa_times_32);
            if(responseVS.getCaption() == null) responseVS.setCaption(context.getString(R.string.error_lbl));
        }
        responseVS.setTypeVS(operation).setServiceCaller(serviceCaller);
        return responseVS;
    }

    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }
        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }
        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }
        return arguments;
    }

}
