/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
