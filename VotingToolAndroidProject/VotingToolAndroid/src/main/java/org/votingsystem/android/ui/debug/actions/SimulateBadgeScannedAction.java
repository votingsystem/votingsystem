package org.votingsystem.android.ui.debug.actions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import org.votingsystem.android.ui.NfcBadgeActivity;
import org.votingsystem.android.ui.debug.DebugAction;

public class SimulateBadgeScannedAction implements DebugAction {

    @Override public void run(final Context context, final Callback callback) {
        final String url = null;
        context.startActivity(new Intent(NfcBadgeActivity.ACTION_SIMULATE, Uri.parse(url),
        context, NfcBadgeActivity.class));
        Toast.makeText(context, "Simulating badge scan: " + url, Toast.LENGTH_LONG).show();
    }

    @Override public String getLabel() {
        return "simulate NFC badge scan";
    }

}