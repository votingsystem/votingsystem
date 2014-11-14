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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;

import org.votingsystem.android.R;
import org.votingsystem.android.fragment.EditorFragment;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.OperationVS;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class FragmentContainerActivity extends FragmentActivity {

	public static final String TAG = FragmentContainerActivity.class.getSimpleName();


    @Override public void onCreate(Bundle savedInstanceState) {
        LOGD(TAG + ".onCreate(...)", "savedInstanceState: " + savedInstanceState +
                " - intent extras: " + getIntent().getExtras());
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.generic_fragment_container_activity);
        // if we're being restored from a previous state should return or else
        // we could end up with overlapping fragments.
        if (savedInstanceState != null) {
            return;
        }
        String fragmentClass = getIntent().getStringExtra(ContextVS.FRAGMENT_KEY);
        try {
            Class clazz = Class.forName(fragmentClass);
            Fragment fragment = (Fragment)clazz.newInstance();
            fragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, fragment,
                            ((Object)fragment).getClass().getSimpleName()).commit();
        } catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    public void setTitle(String title, String subTitle, Integer iconId) {
        getActionBar().setTitle(title);
        if(subTitle != null) getActionBar().setSubtitle(subTitle);
        if(iconId != null) getActionBar().setLogo(iconId);
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


    @Override public void onActivityResult(int requestCode, int resultCode, Intent data) {
        LOGD(TAG + ".onActivityResult(...)", "requestCode: " + requestCode + " - resultCode: " +
                resultCode);
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void processOperation(OperationVS operationVS) {
        EditorFragment fragment = (EditorFragment)getSupportFragmentManager().
                findFragmentByTag(EditorFragment.class.getSimpleName());
        if (fragment != null) fragment.processOperation(operationVS);

    }



    /*@Override public void onBackPressed() {
        EditorFragment fragment = (EditorFragment)getSupportFragmentManager().
                findFragmentByTag(EditorFragment.TAG);
        if (fragment != null) fragment.onBackPressed();
        else  super.onBackPressed();
    }*/

}