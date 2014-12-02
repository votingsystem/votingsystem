package org.votingsystem.android.ui.debug.actions;

import android.app.Activity;
import android.content.Context;
import android.support.v4.app.Fragment;

import com.google.zxing.integration.android.IntentIntegrator;

import org.votingsystem.android.ui.debug.DebugAction;

public class QRScannerAction implements DebugAction {

    private Activity activity;
    private Fragment fragment;

    public QRScannerAction(Activity activity) {
        this.activity = activity;
    }

    public QRScannerAction(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void run(final Context context, final Callback callback) {
        IntentIntegrator integrator = null;
        if(activity != null) integrator = new IntentIntegrator(activity);
        else if(fragment != null) integrator = new IntentIntegrator(fragment);
        integrator.addExtra("SCAN_WIDTH", 1000);
        integrator.addExtra("SCAN_HEIGHT", 1000);
        integrator.addExtra("RESULT_DISPLAY_DURATION_MS", 3000L);
        integrator.addExtra("PROMPT_MESSAGE", "Enfoque el código QR");
        integrator.initiateScan(IntentIntegrator.QR_CODE_TYPES, context);
    }

    @Override
    public String getLabel() {
        return "launch QR scanner";
    }
}
