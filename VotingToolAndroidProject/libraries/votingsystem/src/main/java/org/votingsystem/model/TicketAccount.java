package org.votingsystem.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.util.DateUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class TicketAccount implements Serializable {

    public static final String TAG = "TicketAccount";

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


    public static TicketAccount parse(JSONObject jsonData) throws ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        BigDecimal totalInputs = null;
        BigDecimal totalOutputs = null;
        TicketAccount ticketAccount = new TicketAccount();
        Iterator currencyIterator = jsonData.keys();
        Map<CurrencyVS, CurrencyData> currencyMap = new HashMap<CurrencyVS, CurrencyData>();
        while(currencyIterator.hasNext()) {
            String keyStr = (String) currencyIterator.next();
            CurrencyVS currencyVS = CurrencyVS.valueOf(keyStr);
            CurrencyData currencyData = CurrencyData.parse(jsonData.getJSONObject(keyStr));
            currencyData.setCurrencyVS(currencyVS);
            currencyMap.put(currencyVS, currencyData);
            ticketAccount.setCurrencyMap(currencyMap);
        }
        return ticketAccount;
    }

    public Map<CurrencyVS, CurrencyData> getCurrencyMap() {
        return currencyMap;
    }

    public void setCurrencyMap(Map<CurrencyVS, CurrencyData> currencyMap) {
        this.currencyMap = currencyMap;
    }
}
