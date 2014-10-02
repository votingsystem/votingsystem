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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.json.JSONObject;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.UserVSTransactionVSListInfo;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.signature.util.VotingSystemKeyStoreException;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ObjectUtils;

import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.android.util.LogUtils.makeLogTag;
import static org.votingsystem.model.ContextVS.NIF_KEY;
import static org.votingsystem.model.ContextVS.STATE_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

/**
 * Utilities and constants related to app preferences.
 */
public class PrefUtils {

    private static final String TAG = makeLogTag(PrefUtils.class.getSimpleName());

    /**
     * Boolean preference that indicates whether we installed the boostrap data or not.
     */
    public static final String PREF_DATA_BOOTSTRAP_DONE = "pref_data_bootstrap_done";

    /**
     * Boolean indicating whether we should attempt to sign in on startup (default true).
     */
    public static final String PREF_USER_REFUSED_SIGN_IN = "pref_user_refused_sign_in";


    /** Long indicating when a sync was last ATTEMPTED (not necessarily succeeded) */
    public static final String PREF_LAST_SYNC_ATTEMPTED = "pref_last_sync_attempted";

    /** Long indicating when a sync last SUCCEEDED */
    public static final String PREF_LAST_SYNC_SUCCEEDED = "pref_last_sync_succeeded";


    public static TimeZone getDisplayTimeZone(Context context) {
        return TimeZone.getDefault();
    }

    public static void markDataBootstrapDone(final Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(PREF_DATA_BOOTSTRAP_DONE, true).commit();
    }

    public static boolean isDataBootstrapDone(final Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_DATA_BOOTSTRAP_DONE, false);
    }

    public static void markUserRefusedSignIn(final Context context) {
        markUserRefusedSignIn(context, true);
    }

    public static void markUserRefusedSignIn(final Context context, final boolean refused) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(PREF_USER_REFUSED_SIGN_IN, refused).apply();
    }

    public static boolean hasUserRefusedSignIn(final Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(PREF_USER_REFUSED_SIGN_IN, false);
    }

    public static long getLastSyncAttemptedTime(final Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getLong(PREF_LAST_SYNC_ATTEMPTED, 0L);
    }

    public static void markSyncAttemptedNow(final Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putLong(PREF_LAST_SYNC_ATTEMPTED, Calendar.getInstance().getTimeInMillis()).commit();
    }

    public static long getLastSyncSucceededTime(final Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getLong(PREF_LAST_SYNC_SUCCEEDED, 0L);
    }

    public static void markSyncSucceededNow(final Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putLong(PREF_LAST_SYNC_SUCCEEDED, Calendar.getInstance().getTimeInMillis()).commit();
    }

    public static void registerOnSharedPreferenceChangeListener(final Context context,
        SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterOnSharedPreferenceChangeListener(final Context context,
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static UserVSTransactionVSListInfo getUserVSTransactionVSListInfo(
            final Context context) throws Exception {
        Calendar currentMonday = DateUtils.getMonday(Calendar.getInstance());
        String editorKey = ContextVS.PERIOD_KEY + "_" + DateUtils.getDirPath(currentMonday.getTime());
        SharedPreferences pref = context.getSharedPreferences(ContextVS.VOTING_SYSTEM_PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        String userInfoStr = pref.getString(editorKey, null);
        if(userInfoStr != null) return UserVSTransactionVSListInfo.parse(new JSONObject(userInfoStr));
        else return null;
    }

    public static void putUserVSTransactionVSListInfo(final Context context,
              UserVSTransactionVSListInfo userInfo, DateUtils.TimePeriod timePeriod) throws Exception {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String editorKey = ContextVS.PERIOD_KEY + "_" + DateUtils.getDirPath(timePeriod.getDateFrom());
        editor.putString(editorKey, userInfo.toJSON().toString());
        editor.commit();
    }

    public static void setPin(final Context context, String pin) {
        try {
            SharedPreferences settings = context.getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            String hashPin = CMSUtils.getHashBase64(pin, ContextVS.VOTING_DATA_DIGEST);
            editor.putString(ContextVS.PIN_KEY, hashPin);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static String getStoredPasswordHash(final Context context) throws NoSuchAlgorithmException,
            VotingSystemKeyStoreException {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(ContextVS.PIN_KEY, null);
    }

    public static String getCsrRequest(final Context context) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(ContextVS.CSR_KEY, null);
    }

    public static State getAppCertState(final Context context, String accessControlURL) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String stateStr = settings.getString(
                STATE_KEY + "_" + accessControlURL, State.WITHOUT_CSR.toString());
        return State.valueOf(stateStr);
    }

    public static void putAppCertState(final Context context, String accessControlURL, State state, String nif) {
        LOGD(TAG + ".putAppCertState(...)", STATE_KEY + "_" + accessControlURL +  " - state: " + state.toString());
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(STATE_KEY + "_" + accessControlURL , state.toString());
        if(nif != null) editor.putString(NIF_KEY, nif);
        editor.commit();
    }

    public static void putLastVicketAccountCheckTime(final Context context) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(ContextVS.VICKET_ACCOUNT_LAST_CHECKED_KEY,
                Calendar.getInstance().getTimeInMillis());
        editor.commit();
    }

    public static Date getLastVicketAccountCheckTime(final Context context) {
        SharedPreferences pref = context.getSharedPreferences(ContextVS.VOTING_SYSTEM_PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        GregorianCalendar lastCheckedTime = new GregorianCalendar();
        lastCheckedTime.setTimeInMillis(pref.getLong(ContextVS.VICKET_ACCOUNT_LAST_CHECKED_KEY, 0));
        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());
        if(lastCheckedTime.getTime().after(currentLapseCalendar.getTime())) {
            return lastCheckedTime.getTime();
        } else return null;
    }

    public static void putCsrRequest(final Context context, Long requestId,
             CertificationRequestVS certificationRequest) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedCertificationRequest = ObjectUtils.serializeObject(certificationRequest);
        editor.putLong(ContextVS.CSR_REQUEST_ID_KEY, requestId);
        try {
            editor.putString(ContextVS.CSR_KEY, new String(serializedCertificationRequest, "UTF-8"));
        } catch(Exception ex) {ex.printStackTrace();}
        editor.commit();
    }

    public static void putSessionUserVS(final Context context, UserVS userVS) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedUserVS = ObjectUtils.serializeObject(userVS);
        try {
            editor.putString(ContextVS.USER_KEY, new String(serializedUserVS, "UTF-8"));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static UserVS getSessionUserVS(final Context context) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedUserVS = settings.getString(ContextVS.USER_KEY, null);
        if(serializedUserVS != null) {
            UserVS userVS = (UserVS) ObjectUtils.deSerializeObject(serializedUserVS.getBytes());
            return userVS;
        }
        return null;
    }

}
