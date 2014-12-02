package org.votingsystem.android.ui.debug.actions;

import android.content.Context;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.ui.debug.DebugAction;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.model.ContextVS;

public class PrefsAction implements DebugAction {

    private static final String TAG = PrefsAction.class.getSimpleName();

    @Override public void run(final Context context, final Callback callback) {
        PrefUtils.putCsrRequest(1L, null,context);
        PrefUtils.putPin(1234, context);
        PrefUtils.putAppCertState(((AppContextVS)context.getApplicationContext()).
                getAccessControl().getServerURL(), ContextVS.State.WITHOUT_CSR, null, context);
    }

    @Override public String getLabel() {
        return "Change PrefsUtil";
    }

}
