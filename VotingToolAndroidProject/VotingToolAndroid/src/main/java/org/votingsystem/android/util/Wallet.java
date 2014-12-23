package org.votingsystem.android.util;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.Cooin;
import org.votingsystem.signature.smime.CMSUtils;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;
import org.votingsystem.util.ObjectUtils;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.votingsystem.android.util.LogUtils.LOGD;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Wallet {

    private static final String TAG = Wallet.class.getSimpleName();

    private static List<Cooin> cooinList = null;

    public static List<Cooin> getCooinList() {
        if(cooinList == null) return null;
        else return new ArrayList<Cooin>(cooinList);
    }

    public static List<Cooin> getCooinList(String password, AppContextVS context) throws Exception {
        JSONArray storedWalletJSON = getWallet(password, context);
        if(storedWalletJSON == null) cooinList = new ArrayList<Cooin>();
        else cooinList = getCooinListFromJSONArray(storedWalletJSON);
        return new ArrayList<Cooin>(cooinList);
    }

    public static List<Cooin> getCooinListFromJSONArray(JSONArray jsonArray) throws Exception {
        List<Cooin> cooinList = new ArrayList<Cooin>();
        for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject cooinJSON = jsonArray.getJSONObject(i);
            byte[] serializedCooin = ((JSONObject)cooinJSON).getString("object").getBytes();
            cooinList.add((Cooin) ObjectUtils.deSerializeObject(serializedCooin));
        }
        return cooinList;
    }

    public static void saveCooinList(Collection<Cooin> newCooinList, String password,
             AppContextVS context) throws Exception {
        Object wallet = getWallet(password, context);
        JSONArray storedWalletJSON = null;
        if(wallet == null) storedWalletJSON = new JSONArray();
        else storedWalletJSON = (JSONArray) wallet;
        List<Map> serializedCooinList = getSerializedCooinList(newCooinList);
        for(Map cooin : serializedCooinList) {
            storedWalletJSON.put(new JSONObject(cooin));
        }
        Wallet.saveWallet(storedWalletJSON, password, context);
        cooinList = getCooinListFromJSONArray(storedWalletJSON);
    }

    public static void removeCooinList(
            Collection<Cooin> cooinListToRemove, AppContextVS context) throws Exception {
        Map<String, Cooin> cooinMap = new HashMap<String, Cooin>();
        for(Cooin cooin:cooinList) {
            cooinMap.put(cooin.getHashCertVS(), cooin);
        }
        for(Cooin cooin : cooinListToRemove) {
            if(cooinMap.remove(cooin.getHashCertVS()) != null)  LOGD(TAG +  ".removeCooinList",
                    "removed cooin: " + cooin.getHashCertVS());
        }
        cooinList = new ArrayList<>(cooinMap.values());
        Wallet.saveWallet(new JSONArray(getSerializedCooinList(cooinList)), null, context);
    }

    public static Map<String, Map<String, Map>> getCurrencyMap() {
        Map<String, Map<String, Map>> result = new HashMap<String, Map<String, Map>>();
        DateUtils.TimePeriod timePeriod = DateUtils.getCurrentWeekPeriod();
        for(Cooin cooin:cooinList) {
            if(result.containsKey(cooin.getCurrencyCode())) {
                Map<String, Map> tagMap = result.get(cooin.getCurrencyCode());
                if(tagMap.containsKey(cooin.getSignedTagVS())) {
                    Map<String, BigDecimal> tagInfoMap = tagMap.get(cooin.getSignedTagVS());
                    tagInfoMap.put("total", tagInfoMap.get("total").add(cooin.getAmount()));
                    if(timePeriod.inRange(cooin.getDateTo())) tagInfoMap.put("timeLimited",
                            tagInfoMap.get("timeLimited").add(cooin.getAmount()));
                    tagMap.put(cooin.getSignedTagVS(), tagInfoMap);
                } else {
                    Map<String, BigDecimal> tagInfoMap = new HashMap<String, BigDecimal>();
                    tagInfoMap.put("total", cooin.getAmount());
                    if(timePeriod.inRange(cooin.getDateTo())) tagInfoMap.put("timeLimited", cooin.getAmount());
                    else tagInfoMap.put("timeLimited", BigDecimal.ZERO);
                    tagMap.put(cooin.getSignedTagVS(), tagInfoMap);
                }
            } else {
                Map<String, Map> tagMap = new HashMap<String, Map>();
                Map<String, BigDecimal> tagInfoMap = new HashMap<String, BigDecimal>();
                tagInfoMap.put("total", cooin.getAmount());
                if(timePeriod.inRange(cooin.getDateTo())) tagInfoMap.put("timeLimited", cooin.getAmount());
                else tagInfoMap.put("timeLimited", BigDecimal.ZERO);
                tagMap.put(cooin.getSignedTagVS(), tagInfoMap);
                result.put(cooin.getCurrencyCode(), tagMap);
            }
        }
        return result;
    }

    public static List<Map> getSerializedCooinList(Collection<Cooin> cooinCollection)
            throws UnsupportedEncodingException {
        List<Map> result = new ArrayList<Map>();
        for(Cooin cooin : cooinCollection) {
            Map cooinDataMap = cooin.getCertSubject().getDataMap();
            cooinDataMap.put("isTimeLimited", cooin.getIsTimeLimited());
            byte[] cooinSerialized =  ObjectUtils.serializeObject(cooin);
            cooinDataMap.put("object", new String(cooinSerialized, "UTF-8"));
            result.add(cooinDataMap);
        }
        return result;
    }

    public static List<Map> getSerializedCertificationRequestList(Collection<Cooin> cooinCollection)
            throws UnsupportedEncodingException {
        List<Map> result = new ArrayList<Map>();
        for(Cooin cooin : cooinCollection) {
            Map cooinDataMap = cooin.getCertSubject().getDataMap();
            byte[] serializedCertificationRequest =  ObjectUtils.serializeObject(
                    cooin.getCertificationRequest());
            cooinDataMap.put("certificationRequest",
                    new String(serializedCertificationRequest, "UTF-8"));
            result.add(cooinDataMap);
        }
        return result;
    }

    public static JSONArray getSerializedCooinArray(Collection<Cooin> cooinCollection)
            throws UnsupportedEncodingException {
        JSONArray jsonArray = new JSONArray();
        for(Cooin cooin : cooinCollection) {
            Map cooinDataMap = cooin.getCertSubject().getDataMap();
            cooinDataMap.put("isTimeLimited", cooin.getIsTimeLimited());
            byte[] cooinSerialized =  ObjectUtils.serializeObject(cooin);
            cooinDataMap.put("object", new String(cooinSerialized, "UTF-8"));
            jsonArray.put(new JSONObject(cooinDataMap));
        }
        return jsonArray;
    }

    public static JSONArray getWallet(String password, AppContextVS context) throws Exception {
        byte[] walletBytes = getWalletBytes(password, context);
        if(walletBytes == null) return null;
        else return new JSONArray(new String(walletBytes, "UTF-8"));
    }

    private static byte[] getWalletBytes(String password, AppContextVS context) throws Exception {
        if(password != null) {
            String storedPasswordHash = PrefUtils.getPinHash(context);
            String passwordHash = CMSUtils.getHashBase64(password, ContextVS.VOTING_DATA_DIGEST);
            if(!passwordHash.equals(storedPasswordHash)) {
                throw new ExceptionVS(context.getString(R.string.pin_error_msg));
            }
        }
        try {
            String walletBase64 = PrefUtils.getWallet(context);
            if(walletBase64 == null) return null;
            else return context.decryptMessage(walletBase64.getBytes());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null; }
    }

    public static void saveWallet(Object walletJSON, String password, AppContextVS context)
            throws Exception {
        if(password != null) {
            String storedPasswordHash = PrefUtils.getPinHash(context);
            String passwordHash = CMSUtils.getHashBase64(password, ContextVS.VOTING_DATA_DIGEST);
            if(!passwordHash.equals(storedPasswordHash)) {
                throw new ExceptionVS(context.getString(R.string.pin_error_msg));
            }
        }
        if(walletJSON != null) {
            byte[] encryptedWalletBytes = Encryptor.encryptToCMS(
                    walletJSON.toString().getBytes(), context.getX509UserCert());
            PrefUtils.putWallet(encryptedWalletBytes, context);
        } else PrefUtils.putWallet(null, context);

    }

}