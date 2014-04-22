package org.votingsystem.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class VicketAccount implements Serializable {

    public static final String TAG = "VicketAccount";

    public static final long serialVersionUID = 1L;

    private UserVS userVS;
    private Map<CurrencyVS, CurrencyData> currencyMap;
    private Date lastRequestDate;
    private Date weekLapse;


    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
    }

    public Date getLastRequestDate() {
        return lastRequestDate;
    }

    public void setLastRequestDate(Date lastRequestDate) {
        this.lastRequestDate = lastRequestDate;
    }


    public static VicketAccount parse(JSONObject jsonData) throws ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        BigDecimal totalInputs = null;
        BigDecimal totalOutputs = null;
        VicketAccount vicketAccount = new VicketAccount();
        Iterator currencyIterator = jsonData.keys();
        Map<CurrencyVS, CurrencyData> currencyMap = new HashMap<CurrencyVS, CurrencyData>();
        while(currencyIterator.hasNext()) {
            String keyStr = (String) currencyIterator.next();
            CurrencyVS currencyVS = CurrencyVS.valueOf(keyStr);
            CurrencyData currencyData = CurrencyData.parse(jsonData.getJSONObject(keyStr));
            currencyData.setCurrencyVS(currencyVS);
            currencyMap.put(currencyVS, currencyData);
            vicketAccount.setCurrencyMap(currencyMap);
        }
        return vicketAccount;
    }

    public Map<CurrencyVS, CurrencyData> getCurrencyMap() {
        return currencyMap;
    }

    public void setCurrencyMap(Map<CurrencyVS, CurrencyData> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public Date getWeekLapse() {
        return weekLapse;
    }

    public void setWeekLapse(Date weekLapse) {
        this.weekLapse = weekLapse;
    }
}
