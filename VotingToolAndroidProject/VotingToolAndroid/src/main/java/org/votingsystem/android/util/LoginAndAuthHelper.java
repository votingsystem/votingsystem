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

package org.votingsystem.android.util;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import java.lang.ref.WeakReference;

import static org.votingsystem.util.LogUtils.LOGD;
import static org.votingsystem.util.LogUtils.LOGE;
import static org.votingsystem.util.LogUtils.LOGW;
import static org.votingsystem.util.LogUtils.makeLogTag;


/**
 * This helper handles the UI flow for signing in and authenticating an account. It handles
 * connecting to the Google+ API to fetch profile data (name, cover photo, etc) and
 * also getting the auth token for the necessary scopes. The life of this object is
 * tied to an Activity. Do not attempt to share it across Activities, as unhappiness will
 * result.
 */
public class LoginAndAuthHelper {

    private static final String TAG = makeLogTag(LoginAndAuthHelper.class);

    Context mAppContext;

    // Controls whether or not we can show sign-in UI. Starts as true;
    // when sign-in *fails*, we will show the UI only once and set this flag to false.
    // After that, we don't attempt again in order not to annoy the user.
    private static boolean sCanShowSignInUi = true;
    private static boolean sCanShowAuthUi = true;

    // The Activity this object is bound to (we use a weak ref to avoid context leaks)
    WeakReference<Activity> mActivityRef;

    // Callbacks interface we invoke to notify the user of this class of useful events
    WeakReference<Callbacks> mCallbacksRef;

    // Name of the account to log in as (e.g. "foo@example.com")
    String mAccountName;


    // Async task that fetches the token
    GetTokenTask mTokenTask = null;

    // Are we in the started state? Started state is between onStart and onStop.
    boolean mStarted = false;

    // True if we are currently showing UIs to resolve a connection error.
    boolean mResolving = false;


    public interface Callbacks {
        void onPlusInfoLoaded(String accountName);
        void onAuthSuccess(String accountName, boolean newlyAuthenticated);
        void onAuthFailure(String accountName);
    }

    public LoginAndAuthHelper(Activity activity, Callbacks callbacks, String accountName) {
        LOGD(TAG, "Helper created. Account: " + mAccountName);
        mActivityRef = new WeakReference<Activity>(activity);
        mCallbacksRef = new WeakReference<Callbacks>(callbacks);
        mAppContext = activity.getApplicationContext();
        mAccountName = accountName;
        /*if (PrefUtils.hasUserRefusedSignIn(activity)) {
            // If we know the user refused sign-in, let's not annoy them.
            sCanShowSignInUi = sCanShowAuthUi = false;
        }*/
    }

    public boolean isStarted() {
        return mStarted;
    }

    public String getAccountName() {
        return mAccountName;
    }

    private Activity getActivity(String methodName) {
        Activity activity = mActivityRef.get();
        if (activity == null) {
            LOGD(TAG, "Helper lost Activity reference, ignoring (" + methodName + ")");
        }
        return activity;
    }

    public void retryAuthByUserRequest() {
        LOGD(TAG, "Retrying sign-in/auth (user-initiated).");
    }

    /** Starts the helper. Call this from your Activity's onStart(). */
    public void start() {
        Activity activity = getActivity("start()");
        if (activity == null) {
            return;
        }

        if (mStarted) {
            LOGW(TAG, "Helper already started. Ignoring redundant call.");
            return;
        }

        mStarted = true;
        if (mResolving) {
            // if resolving, don't reconnect the plus client
            LOGD(TAG, "Helper ignoring signal to start because we're resolving a failure.");
            return;
        }
        LOGD(TAG, "Helper starting. Connecting " + mAccountName);
    }


    private void reportAuthSuccess(boolean newlyAuthenticated) {
        LOGD(TAG, "Auth success for account " + mAccountName + ", newlyAuthenticated=" + newlyAuthenticated);
        Callbacks callbacks;
        if (null != (callbacks = mCallbacksRef.get())) {
            callbacks.onAuthSuccess(mAccountName, newlyAuthenticated);
        }
    }

    private void reportAuthFailure() {
        LOGD(TAG, "Auth FAILURE for account " + mAccountName);
        Callbacks callbacks;
        if (null != (callbacks = mCallbacksRef.get())) {
            callbacks.onAuthFailure(mAccountName);
        }
    }

    /** Async task that obtains the auth token. */
    private class GetTokenTask extends AsyncTask<Void, Void, String> {
        public GetTokenTask() {}

        @Override
        protected String doInBackground(Void... params) {
            try {
                if (isCancelled()) {
                    LOGD(TAG, "doInBackground: task cancelled, so giving up on auth.");
                    return null;
                }
                LOGD(TAG, "Starting background auth for " + mAccountName);
                final String token = null;
                return token;
            }catch (Exception e) {
                LOGE(TAG, "RuntimeException encountered: " + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(String token) {
            super.onPostExecute(token);

            if (isCancelled()) {
                LOGD(TAG, "Task cancelled, so not reporting auth success.");
            } else if (!mStarted) {
                LOGD(TAG, "Activity not started, so not reporting auth success.");
            } else {
                LOGD(TAG, "GetTokenTask reporting auth success.");
                reportAuthSuccess(true);
            }
        }

    }

}
