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
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.google.zxing.integration.android.IntentIntegrator;

import org.votingsystem.android.activity.FragmentContainerActivity;
import org.votingsystem.android.fragment.QREncoderFragment;
import org.votingsystem.android.ui.debug.DebugAction;
import org.votingsystem.model.ContextVS;

public class QREncoderAction implements DebugAction {

    private Activity activity;
    private static final String qrMessage = "operation=TRANSACTION;id=1;URL=https://cooins:8086/Cooins;amount=100_eur_WILDTAG;";

    public QREncoderAction(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void run(final Context context, final Callback callback) {
        Intent intent = new Intent(activity, FragmentContainerActivity.class);
        intent.putExtra(ContextVS.MESSAGE_KEY, qrMessage);
        intent.putExtra(ContextVS.FRAGMENT_KEY, QREncoderFragment.class.getName());
        activity.startActivity(intent);
    }

    @Override
    public String getLabel() {
        return "launch QR encoder";
    }
}
