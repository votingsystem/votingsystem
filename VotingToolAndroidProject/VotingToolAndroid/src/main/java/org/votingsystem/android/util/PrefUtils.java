package org.votingsystem.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONObject;
import org.votingsystem.android.R;
import org.votingsystem.model.AnonymousDelegation;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Representation;
import org.votingsystem.model.UserVS;
import org.votingsystem.model.CooinAccountsInfo;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.util.CertificationRequestVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.ObjectUtils;

import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;

import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.android.util.LogUtils.makeLogTag;
import static org.votingsystem.model.ContextVS.APPLICATION_ID_KEY;
import static org.votingsystem.model.ContextVS.NIF_KEY;
import static org.votingsystem.model.ContextVS.STATE_KEY;
import static org.votingsystem.model.ContextVS.State;
import static org.votingsystem.model.ContextVS.VOTING_SYSTEM_PRIVATE_PREFS;

public class PrefUtils {

    private static final String TAG = makeLogTag(PrefUtils.class.getSimpleName());

    public static TimeZone getDisplayTimeZone(Context context) {
        return TimeZone.getDefault();
    }

    private static Representation representation;
    private static AnonymousDelegation anonymousDelegation;

    public static void init(final Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        //initialize listened keys
        sp.edit().putBoolean(ContextVS.BOOTSTRAP_DONE, false).commit();
        sp.edit().putString(ContextVS.ACCESS_CONTROL_URL_KEY, null).commit();
        new Thread(new Runnable() {
            @Override public void run() { getRepresentationState(context); }
        }).start();
    }

    public static void markDataBootstrapDone(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putBoolean(ContextVS.BOOTSTRAP_DONE, true).commit();
    }

    public static void markAccessControlLoaded(String accessControlURL, Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(ContextVS.ACCESS_CONTROL_URL_KEY, accessControlURL).commit();
    }

    public static boolean isDataBootstrapDone(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return sp.getBoolean(ContextVS.BOOTSTRAP_DONE, false);
    }

    public static String getApplicationId(Context context)  {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String applicationId = settings.getString(ContextVS.APPLICATION_ID_KEY, null);
        if(applicationId == null) {
            applicationId = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(APPLICATION_ID_KEY, applicationId);
            editor.commit();
            LOGD(TAG, ".getApplicationId - new applicationId: " + applicationId);
        }
        return applicationId;
    }

    public static Calendar getLastPendingOperationCheckedTime(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        Calendar lastCheckedTime = Calendar.getInstance();
        lastCheckedTime.setTimeInMillis(sp.getLong(ContextVS.PENDING_OPERATIONS_LAST_CHECKED_KEY, 0L));
        return lastCheckedTime;
    }

    public static void markPendingOperationCheckedNow(Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.edit().putLong(ContextVS.PENDING_OPERATIONS_LAST_CHECKED_KEY,
                Calendar.getInstance().getTimeInMillis()).commit();
    }

    public static void registerPreferenceChangeListener(Context context,
        SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterPreferenceChangeListener(
            SharedPreferences.OnSharedPreferenceChangeListener listener, Context context) {
        SharedPreferences sp = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static Date getCooinAccountsLastCheckDate(Context context) {
        SharedPreferences pref = context.getSharedPreferences(ContextVS.VOTING_SYSTEM_PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        GregorianCalendar lastCheckedTime = new GregorianCalendar();
        lastCheckedTime.setTimeInMillis(pref.getLong(ContextVS.USERVS_ACCOUNT_LAST_CHECKED_KEY, 0));
        Calendar currentLapseCalendar = DateUtils.getMonday(Calendar.getInstance());
        if(lastCheckedTime.getTime().after(currentLapseCalendar.getTime())) {
            return lastCheckedTime.getTime();
        } else return null;
    }

    public static CooinAccountsInfo getCooinAccountsInfo(Context context) throws Exception {
        Calendar currentMonday = DateUtils.getMonday(Calendar.getInstance());
        String editorKey = ContextVS.PERIOD_KEY + "_" + DateUtils.getPath(currentMonday.getTime());
        SharedPreferences pref = context.getSharedPreferences(ContextVS.VOTING_SYSTEM_PRIVATE_PREFS,
                Context.MODE_PRIVATE);
        String userInfoStr = pref.getString(editorKey, null);
        if(userInfoStr != null) return CooinAccountsInfo.parse(new JSONObject(userInfoStr));
        else return null;
    }

    public static void putCooinAccountsInfo(CooinAccountsInfo userInfo,
                DateUtils.TimePeriod timePeriod, Context context) throws Exception {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(ContextVS.USERVS_ACCOUNT_LAST_CHECKED_KEY,
                Calendar.getInstance().getTimeInMillis());
        String editorKey = ContextVS.PERIOD_KEY + "_" + DateUtils.getPath(timePeriod.getDateFrom());
        editor.putString(editorKey, userInfo.toJSON().toString());
        editor.commit();
    }

    public static void putPin(Integer pin, Context context) {
        try {
            SharedPreferences settings = context.getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            String hashPin = CMSUtils.getHashBase64(pin.toString(), ContextVS.VOTING_DATA_DIGEST);
            editor.putString(ContextVS.PIN_KEY, hashPin);
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static String getPinHash(Context context) throws NoSuchAlgorithmException, ExceptionVS {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(ContextVS.PIN_KEY, null);
    }

    public static void putWallet(byte[] encryptedWalletBytes, Context context) {
        try {
            SharedPreferences settings = context.getSharedPreferences(
                    VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            if(encryptedWalletBytes == null) editor.putString(ContextVS.WALLET_FILE_NAME, null);
            else editor.putString(ContextVS.WALLET_FILE_NAME,
                    Base64.encodeToString(encryptedWalletBytes, Base64.DEFAULT));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static String getWallet(Context context) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        return settings.getString(ContextVS.WALLET_FILE_NAME, null);
    }

    public static String getCsrRequest(Context context) {
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

    public static void putAppCertState(String accessControlURL, State state, String nif,
           Context context) {
        LOGD(TAG + ".putAppCertState", STATE_KEY + "_" + accessControlURL +
                " - state: " + state.toString());
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(STATE_KEY + "_" + accessControlURL , state.toString());
        if(nif != null) editor.putString(NIF_KEY, nif);
        editor.commit();
    }

    public static void putCsrRequest(Long requestId, CertificationRequestVS certificationRequest,
             Context context) {
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

    public static void putSessionUserVS(UserVS userVS, Context context) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedUserVS = ObjectUtils.serializeObject(userVS);
        try {
            editor.putString(ContextVS.USER_KEY, new String(serializedUserVS, "UTF-8"));
            editor.commit();
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static UserVS getSessionUserVS(Context context) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedUserVS = settings.getString(ContextVS.USER_KEY, null);
        if(serializedUserVS != null) {
            UserVS userVS = (UserVS) ObjectUtils.deSerializeObject(serializedUserVS.getBytes());
            return userVS;
        }
        return null;
    }

    public static void putRepresentationState(Representation representationState, Context context) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        byte[] serializedState = ObjectUtils.serializeObject(representationState);
        try {
            editor.putString(Representation.class.getSimpleName(),
                    new String(serializedState, "UTF-8"));
            editor.commit();
            representation = representationState;
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static Representation getRepresentationState(Context context) {
        if(representation != null) return representation;
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        String serializedRepresentation = settings.getString(
                Representation.class.getSimpleName(), null);
        editor.putString(ContextVS.REPRESENTATIVE_DATA_FILE_NAME, null);
        if(serializedRepresentation != null) {
            representation = (Representation) ObjectUtils.deSerializeObject(
                    serializedRepresentation.getBytes());
        }
        return representation;
    }

    public static void putAnonymousDelegation(AnonymousDelegation delegation, Context context) {
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        Representation representation = null;
        String serializedDelegation = null;
        try {
            if(delegation != null) {
                serializedDelegation = new String(ObjectUtils.serializeObject(delegation), "UTF-8");
                representation = new Representation(Calendar.getInstance().getTime(),
                        Representation.State.WITH_ANONYMOUS_REPRESENTATION, delegation.getRepresentative(),
                        delegation.getDateTo());
            } else {
                representation = new Representation(Calendar.getInstance().getTime(),
                        Representation.State.WITHOUT_REPRESENTATION, null, null);
            }
            editor.putString(ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY, serializedDelegation);
            editor.commit();
            putRepresentationState(representation, context);
            anonymousDelegation = delegation;
        } catch(Exception ex) {ex.printStackTrace();}
    }

    public static AnonymousDelegation getAnonymousDelegation(Context context) {
        if(anonymousDelegation != null) return anonymousDelegation;
        SharedPreferences settings = context.getSharedPreferences(
                VOTING_SYSTEM_PRIVATE_PREFS, Context.MODE_PRIVATE);
        String serializedObject = settings.getString(
                ContextVS.ANONYMOUS_REPRESENTATIVE_DELEGATION_KEY, null);
        if(serializedObject != null) {
            anonymousDelegation = (AnonymousDelegation) ObjectUtils.
                    deSerializeObject(serializedObject.getBytes());
        }
        return anonymousDelegation;
    }

    public static void changePin(String newPin, String oldPin, Context context)
            throws ExceptionVS, NoSuchAlgorithmException {
        String storedPinHash = PrefUtils.getPinHash(context);
        String pinHash = CMSUtils.getHashBase64(oldPin, ContextVS.VOTING_DATA_DIGEST);
        if(!storedPinHash.equals(pinHash)) {
            throw new ExceptionVS(context.getString(R.string.pin_error_msg));
        }
        PrefUtils.putPin(Integer.valueOf(newPin), context);
    }
}
