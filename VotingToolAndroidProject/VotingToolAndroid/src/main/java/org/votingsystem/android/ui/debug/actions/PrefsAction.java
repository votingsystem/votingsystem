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

import android.content.ContentResolver;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;

import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.ui.debug.DebugAction;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.model.ContextVS;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.android.util.LogUtils.makeLogTag;

/**
 * A DebugAction that runs an immediate full sync.
 */
public class PrefsAction implements DebugAction {
    private static final String TAG = makeLogTag(PrefsAction.class);

    @Override
    public void run(final Context context, final Callback callback) {
        PrefUtils.putCsrRequest(context, 1L, null);
        PrefUtils.setPin(context, "pins");
        PrefUtils.putAppCertState(context, ((AppContextVS)context.getApplicationContext()).
                getAccessControl().getServerURL(), ContextVS.State.WITHOUT_CSR, null);
    }

    @Override
    public String getLabel() {
        return "Change PrefsUtil";
    }

}
