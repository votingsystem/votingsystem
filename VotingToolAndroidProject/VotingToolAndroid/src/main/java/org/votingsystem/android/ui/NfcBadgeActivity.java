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

package org.votingsystem.android.ui;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;

import java.util.Arrays;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.android.util.LogUtils.LOGI;
import static org.votingsystem.android.util.LogUtils.LOGW;
import static org.votingsystem.android.util.LogUtils.makeLogTag;


public class NfcBadgeActivity extends Activity {
    private static final String TAG = makeLogTag(NfcBadgeActivity.class);
    private static final String URL_PREFIX = "sistemavotacion.org";
    // For debug purposes
    public static final String ACTION_SIMULATE = "org.votingsystem.cooins.ACTION_SIMULATE";

    @Override
    public void onStart() {
        super.onStart();
        // Check for NFC data
        Intent i = getIntent();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(i.getAction())) {
            LOGI(TAG, "Badge detected");
            /* [ANALYTICS:EVENT]
             * TRIGGER:   Scan another attendee's badge.
             * CATEGORY:  'NFC'
             * ACTION:    'Read'
             * LABEL:     'Badge'. Badge info IS NOT collected.
             * [/ANALYTICS]
             */

            readTag((Tag) i.getParcelableExtra(NfcAdapter.EXTRA_TAG));
        } else if (ACTION_SIMULATE.equals(i.getAction())) {
            String simulatedUrl = i.getDataString();
            LOGD(TAG, "Simulating badge scanning with URL " + simulatedUrl);
            // replace https by Unicode character 4, as per normal badge encoding rules
            recordBadge(simulatedUrl.replace("https://", "\u0004"));
        } else {
            LOGW(TAG, "Invalid action in Intent to NfcBadgeActivity: " + i.getAction());
        }
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    private void readTag(Tag t) {
        byte[] id = t.getId();

        // get NDEF tag details
        Ndef ndefTag = Ndef.get(t);

        // get NDEF message details
        NdefMessage ndefMesg = ndefTag.getCachedNdefMessage();
        if (ndefMesg == null) {
            return;
        }
        NdefRecord[] ndefRecords = ndefMesg.getRecords();
        if (ndefRecords == null) {
            return;
        }
        for (NdefRecord record : ndefRecords) {
            short tnf = record.getTnf();
            String type = new String(record.getType());
            if (tnf == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(type.getBytes(), NdefRecord.RTD_URI)) {
                String url = new String(record.getPayload());
                recordBadge(url);
            }
        }
    }

    private void recordBadge(String url) {
        LOGD(TAG, "Recording badge, URL " + url);
    }



}
