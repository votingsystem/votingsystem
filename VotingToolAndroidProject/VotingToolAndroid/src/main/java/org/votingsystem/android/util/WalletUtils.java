package org.votingsystem.android.util;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Vicket;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.ObjectUtils;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import static org.votingsystem.android.util.LogUtils.LOGD;
import static org.votingsystem.android.util.LogUtils.makeLogTag;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class WalletUtils {

    private static final String TAG = makeLogTag(WalletUtils.class.getSimpleName());


    public static List<Vicket> getVicketList(String password, Context context) throws Exception {
            JSONArray vicketArray = getWallet(password, context);
        List<Vicket> vicketList = new ArrayList<Vicket>();
        for(int i = 0; i < vicketArray.length(); i++) {
            byte[] serializedVicket = vicketArray.getJSONObject(i).getString("object").getBytes();
            vicketList.add((Vicket) ObjectUtils.deSerializeObject(serializedVicket));
        }
        return vicketList;
    }

    public static void saveVicketList(Collection<Vicket> vicketList, String password,
            Context context) throws Exception {
        Object wallet = getWallet(password, context);
        JSONArray storedWalletJSON = null;
        if(wallet == null) storedWalletJSON = new JSONArray();
        else storedWalletJSON = (JSONArray) wallet;
        List<Map> serializedVicketList = getSerializedVicketList(vicketList);
        for(Map vicket : serializedVicketList) {
            storedWalletJSON.put(vicket);
        }
        WalletUtils.saveWallet(storedWalletJSON, password, context);
    }

    public static List<Map> getSerializedVicketList(Collection<Vicket> vicketCollection)
            throws UnsupportedEncodingException {
        List<Map> result = new ArrayList<Map>();
        for(Vicket vicket : vicketCollection) {
            Map vicketDataMap = vicket.getCertSubject().getDataMap();
            vicketDataMap.put("isTimeLimited", vicket.getIsTimeLimited());
            byte[] vicketSerialized =  ObjectUtils.serializeObject(vicket);
            vicketDataMap.put("object", new String(vicketSerialized, "UTF-8"));
            result.add(vicketDataMap);
        }
        return result;
    }

    public static byte[] getWalletEncrypted(Context context) throws ExceptionVS, IOException {
        try {
            FileInputStream fis = context.openFileInput(ContextVS.WALLET_FILE_NAME);
            return FileUtils.getBytesFromInputStream(fis);
        } catch (Exception ex) {  return null; }
    }

    public static JSONArray getWallet(String password, Context context) throws Exception {
        byte[] walletBytes = getWalletBytes(password, context);
        if(walletBytes == null) return null;
        else return new JSONArray(new String(walletBytes, "UTF-8"));
    }

    private static byte[] getWalletBytes(String password, Context context) throws Exception {
        String storedPasswordHash = PrefUtils.getStoredPasswordHash(context);
        String passwordHash = CMSUtils.getHashBase64(password, ContextVS.VOTING_DATA_DIGEST);
        if(!passwordHash.equals(storedPasswordHash)) {
            throw new ExceptionVS(context.getString(R.string.pin_error_msg));
        }
        byte[] encryptedWalletBytes = getWalletEncrypted(context);
        if(encryptedWalletBytes == null) return null;
        else return ((AppContextVS)context.getApplicationContext()).decryptMessage(encryptedWalletBytes);
    }

    private static byte[] getWalletBytes(byte[] encryptedWalletBytes, String password,
                 Context context) throws Exception {
        String storedPasswordHash = PrefUtils.getStoredPasswordHash(context);
        String passwordHash = CMSUtils.getHashBase64(password, ContextVS.VOTING_DATA_DIGEST);
        if(!passwordHash.equals(storedPasswordHash)) {
            throw new ExceptionVS(context.getString(R.string.pin_error_msg));
        }
        return ((AppContextVS)context.getApplicationContext()).decryptMessage(encryptedWalletBytes);
    }

    public static void saveWallet(Object walletJSON, String password, Context context)
            throws Exception {
        String storedPasswordHash = PrefUtils.getStoredPasswordHash(context);
        String passwordHash = CMSUtils.getHashBase64(password, ContextVS.VOTING_DATA_DIGEST);
        if(!passwordHash.equals(storedPasswordHash)) {
            throw new ExceptionVS(context.getString(R.string.pin_error_msg));
        }
        FileOutputStream fos = context.openFileOutput(ContextVS.WALLET_FILE_NAME, Context.MODE_PRIVATE);
        byte[] encryptedWalletBytes = Encryptor.encryptToCMS(walletJSON.toString().getBytes(),
                ((AppContextVS)context.getApplicationContext()).getX509UserCert());
        fos.write(encryptedWalletBytes);
        fos.close();
    }

}
