package org.votingsystem.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserVSTransactionVSListInfo {

    public static final String TAG = UserVSTransactionVSListInfo.class.getSimpleName();

    private UserVS userVS;
    private DateUtils.TimePeriod timePeriod;
    private List<TransactionVS> transactionVSFromList;
    private List<TransactionVS> transactionVSToList;
    private Map<String, Map<String, TagVS>> balancesToMap;
    private Map<String, Map<String, TagVS>> balancesFromMap;
    private Map<String, Map<String, TagVS>> balancesCashMap;

    public List<TransactionVS> getTransactionList() {
        List<TransactionVS> result = new ArrayList<TransactionVS>();
        if(transactionVSFromList != null) result.addAll(transactionVSFromList);
        if(transactionVSToList != null) result.addAll(transactionVSToList);
        return result;
    }

    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public List<TransactionVS> getTransactionVSFromList() {
        return transactionVSFromList;
    }

    public void setTransactionVSFromList(List<TransactionVS> transactionVSFromList) {
        this.transactionVSFromList = transactionVSFromList;
    }

    public List<TransactionVS> getTransactionVSToList() {
        return transactionVSToList;
    }

    public void setTransactionVSToList(List<TransactionVS> transactionVSToList) {
        this.transactionVSToList = transactionVSToList;
    }

    public Map<String, Map<String, TagVS>> getBalancesToMap() {
        return balancesToMap;
    }

    public void setBalancesToMap(Map<String, Map<String, TagVS>> balancesToMap) {
        this.balancesToMap = balancesToMap;
    }

    public Map<String, Map<String, TagVS>> getBalancesFromMap() {
        return balancesFromMap;
    }

    public void setBalancesFromMap(Map<String, Map<String, TagVS>> balancesFromMap) {
        this.balancesFromMap = balancesFromMap;
    }

    public BigDecimal getAvailableForTagVS(String currencyCode, String tagStr) throws ExceptionVS {
        if(balancesCashMap.containsKey(currencyCode)) {
            Map<String, TagVS> currencyMap = balancesCashMap.get(currencyCode);
            if(currencyMap.containsKey(tagStr)) return currencyMap.get(tagStr).getTotal();
        }
        throw new ExceptionVS("User has not account for tag '" + tagStr + "' with currency '" + currencyCode +"'");
    }

    public Map<String, TagVS> getTagVSBalancesMap(String currencyCode) throws ExceptionVS {
        if(balancesCashMap.containsKey(currencyCode)) {
            return balancesCashMap.get(currencyCode);
        } else {
            Log.d(TAG + ".getTagVSBalancesMap(...)", "User has not accounts for currency '" + currencyCode +"'");
            return null;
        }
    }

    public static UserVSTransactionVSListInfo parse(JSONObject jsonData) throws Exception {
        UserVSTransactionVSListInfo result =  new UserVSTransactionVSListInfo();
        DateUtils.TimePeriod timePeriod = DateUtils.TimePeriod.parse(jsonData.getJSONObject("timePeriod"));
        result.setTimePeriod(timePeriod);
        result.setUserVS(UserVS.parse(jsonData.getJSONObject("userVS")));
        if(jsonData.has("transactionFromList"))result.setTransactionVSFromList(
                TransactionVS.parseList(jsonData.getJSONArray("transactionFromList")));
        if(jsonData.has("transactionToList"))result.setTransactionVSToList(
                TransactionVS.parseList(jsonData.getJSONArray("transactionToList")));
        if(jsonData.has("balancesFrom")) result.setBalancesFromMap(
                parseBalanceMap(jsonData.getJSONObject("balancesFrom")));
        if(jsonData.has("balancesTo")) result.setBalancesToMap(
                parseBalanceMap(jsonData.getJSONObject("balancesTo")));
        if(jsonData.has("balancesCash")) result.setBalancesCashMap(
                parseBalanceMap(jsonData.getJSONObject("balancesCash")));
        return result;
    }

    public JSONObject toJSON() throws Exception {
        JSONObject jsonData = new JSONObject();
        if(timePeriod != null) jsonData.put("timePeriod", timePeriod.toJSON());
        if(userVS != null) jsonData.put("userVS", userVS.toJSON());
        if(transactionVSFromList != null && !transactionVSFromList.isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            for(TransactionVS transactionVS: transactionVSFromList) {
                jsonArray.put(transactionVS.toJSON());
            }
            jsonData.put("transactionFromList", jsonArray);
        }
        if(transactionVSToList != null && !transactionVSToList.isEmpty()) {
            JSONArray jsonArray = new JSONArray();
            for(TransactionVS transactionVS: transactionVSToList) {
                jsonArray.put(transactionVS.toJSON());
            }
            jsonData.put("transactionToList", jsonArray);
        }
        if(balancesFromMap != null) jsonData.put("balancesFrom", toJSON(balancesFromMap, false));
        if(balancesToMap != null) jsonData.put("balancesTo", toJSON(balancesToMap, true));
        if(balancesCashMap != null) jsonData.put("balancesCash", toJSON(balancesCashMap, false));
        return jsonData;
    }


    public static Map<String, Map<String, TagVS>> parseBalanceMap(JSONObject jsonData) throws Exception {
        Iterator currencyIterator = jsonData.keys();
        Map<String, Map<String, TagVS>> result = new HashMap<String, Map<String, TagVS>>();
        while(currencyIterator.hasNext()) {
            String currencyStr = (String) currencyIterator.next();
            Map<String, TagVS> tagVSBalanceMap = TagVS.parseTagVSBalanceMap(jsonData.getJSONObject(currencyStr));
            result.put(currencyStr, tagVSBalanceMap);
        }
        return result;
    }

    public static JSONObject toJSON(Map<String, Map<String, TagVS>> balancesMap,
                   boolean isWithTimeLimitedData) throws Exception {
        JSONObject jsonData = new JSONObject();
        Set<String> currencySet = balancesMap.keySet();
        for(String currency: currencySet) {
            Map<String, TagVS> tagVSMap = balancesMap.get(currency);
            JSONObject jsonTagVSData = new JSONObject();
            Set<String> tagSet = tagVSMap.keySet();
            for(String tag: tagSet) {
                if(isWithTimeLimitedData) {
                    JSONObject data = new JSONObject();
                    data.put("total", tagVSMap.get(tag).getTotal());
                    data.put("timeLimited", tagVSMap.get(tag).getTimeLimited());
                    jsonTagVSData.put(tag, data);
                } else {
                    jsonTagVSData.put(tag, tagVSMap.get(tag).getTotal());
                }
            }
            jsonData.put(currency, jsonTagVSData);
        }
        return jsonData;
    }

    public DateUtils.TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(DateUtils.TimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

    public Map<String, Map<String, TagVS>> getBalancesCashMap() {
        return balancesCashMap;
    }

    public void setBalancesCashMap(Map<String, Map<String, TagVS>> balancesCashMap) {
        this.balancesCashMap = balancesCashMap;
    }
}
