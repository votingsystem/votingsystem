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

package org.votingsystem.android.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.votingsystem.android.R;
import org.votingsystem.android.util.PrefUtils;
import org.votingsystem.model.ContextVS;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.android.util.LogUtils.makeLogTag;


public class SettingsActivity extends PreferenceActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = makeLogTag(SettingsActivity.class);

    @Override protected void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", " - savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        setContentView(R.layout.settings_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                SettingsActivity.super.onBackPressed();
            }
        });
        toolbar.setTitle(R.string.navdrawer_item_settings);
        PrefUtils.registerPreferenceChangeListener(this, this);
        Preference button = (Preference)findPreference("requestCertButton");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override public boolean onPreferenceClick(Preference arg0) {
                Intent intent = new Intent(SettingsActivity.this, CertRequestActivity.class);
                intent.putExtra(ContextVS.OPERATIONVS_KEY, "");
                startActivity(intent);
                return true;
            }
        });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        PrefUtils.unregisterPreferenceChangeListener(this, this);
    }


    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        LOGD(TAG, ".onSharedPreferenceChanged- key: " + key);
    }
}
