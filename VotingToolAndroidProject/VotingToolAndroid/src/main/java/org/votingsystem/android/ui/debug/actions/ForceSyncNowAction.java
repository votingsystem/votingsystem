package org.votingsystem.android.ui.debug.actions;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import org.votingsystem.android.ui.debug.DebugAction;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.android.util.WalletUtils;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.android.util.LogUtils.makeLogTag;

public class ForceSyncNowAction implements DebugAction {
    private static final String TAG = makeLogTag(ForceSyncNowAction.class);

    @Override
    public void run(final Context context, final Callback callback) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        new AsyncTask<Context, Void, Void>() {
            @Override protected Void doInBackground(Context... contexts) {
                LOGD(TAG, "doInBackground");
                try {
                    LOGD(TAG, "resetting wallet");
                    PrefUtils.putWalletPin("1234", context);
                    WalletUtils.saveWallet(null, "1234", context);
                } catch(Exception ex) {ex.printStackTrace();}
                return null;
            }
        }.execute(context);
    }

    @Override public String getLabel() {
        return "Force data sync now";
    }

}
