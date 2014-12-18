package org.votingsystem.android.ui.debug.actions;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.activity.BrowserVSActivity;
import org.votingsystem.android.ui.debug.DebugAction;
import org.votingsystem.model.ContextVS;

import static org.votingsystem.android.util.LogUtils.LOGD;

public class BrowserVSAction implements DebugAction {
    private static final String TAG = BrowserVSAction.class.getSimpleName();

    private AppContextVS appContext;

    public BrowserVSAction(AppContextVS context) {
        this.appContext = context;
    }

    @Override public void run(final Context context, final Callback callback) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        new AsyncTask<Context, Void, Void>() {
            @Override protected Void doInBackground(Context... contexts) {
                LOGD(TAG, "doInBackground");
                Intent intent = new Intent(appContext, BrowserVSActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(ContextVS.URL_KEY, appContext.getCooinServer().getServerURL());
                appContext.startActivity(intent);
                return null;
            }
        }.execute(context);
    }

    @Override public String getLabel() {
        return "BrowserVS";
    }

}
