package org.votingsystem.android.ui.debug.actions;

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import org.votingsystem.android.contentprovider.TransactionVSContentProvider;
import org.votingsystem.android.ui.debug.DebugAction;

import static org.votingsystem.util.LogUtils.LOGD;

public class DeleteDBAction implements DebugAction {

    private static final String TAG = DeleteDBAction.class.getSimpleName();

    @Override public void run(final Context context, final Callback callback) {
        final Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        new AsyncTask<Context, Void, Void>() {
            @Override protected Void doInBackground(Context... contexts) {
                LOGD(TAG, "doInBackground");
                String selection = TransactionVSContentProvider.ID_COL + " > ?";
                String[] selectionArgs = { "0" };
                context.getContentResolver().delete(
                        TransactionVSContentProvider.CONTENT_URI, selection, selectionArgs);
                return null;
            }
        }.execute(context);
    }

    @Override public String getLabel() {
        return "Delete DB";
    }

}
