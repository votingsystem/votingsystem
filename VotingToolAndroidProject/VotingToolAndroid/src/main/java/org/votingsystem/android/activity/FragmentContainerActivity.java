/*
 * Copyright 2011 - Jose. J. García Zornoza
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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.android.util.Utils;
import org.votingsystem.model.ContextVS;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class FragmentContainerActivity extends ActionBarActivity {

	public static final String TAG = FragmentContainerActivity.class.getSimpleName();


    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate", "savedInstanceState: " + savedInstanceState);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_container_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_vs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // if we're being restored from a previous state should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
        String fragmentClass = getIntent().getStringExtra(ContextVS.FRAGMENT_KEY);
        try {
            Class clazz = Class.forName(fragmentClass);
            Fragment fragment = (Fragment)clazz.newInstance();
            fragment.setArguments(Utils.intentToFragmentArguments(getIntent()));
            getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, fragment,
                            ((Object)fragment).getClass().getSimpleName()).commit();
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    public void setTitle(String title, String subTitle, Integer iconId) {
        getSupportActionBar().setTitle(title);
        if(subTitle != null) getSupportActionBar().setSubtitle(subTitle);
        if(iconId != null) getSupportActionBar().setLogo(iconId);
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        LOGD(TAG + ".onOptionsItemSelected", " - item: " + item.getTitle());
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}