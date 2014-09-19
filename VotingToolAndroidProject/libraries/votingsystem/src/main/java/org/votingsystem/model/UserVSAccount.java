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


public class UserVSAccount implements Serializable {

    public static final String TAG = UserVSAccount.class.getSimpleName();

    public static final long serialVersionUID = 1L;

    public enum State {ACTIVE, SUSPENDED, CANCELLED}

    public enum Type {SYSTEM, EXTERNAL}

    private Long id;
    private State state = State.ACTIVE;
    private Type type = Type.SYSTEM;
    private UserVS userVS;
    private BigDecimal balance = null;
    private String currencyCode;
    private String IBAN;
    private TagVS tagVS;
    private Map<String, TagVSData> currencyMap;
    private Date lastRequestDate;
    private Date weekLapse;


    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getIBAN() {
        return IBAN;
    }

    public void setIBAN(String IBAN) {
        this.IBAN = IBAN;
    }

    public TagVS getTagVS() {
        return tagVS;
    }

    public void setTagVS(TagVS tagVS) {
        this.tagVS = tagVS;
    }

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


    public static UserVSAccount parse(JSONObject jsonData) throws ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        BigDecimal totalInputs = null;
        BigDecimal totalOutputs = null;
        UserVSAccount userVSAccount = new UserVSAccount();
        Iterator currencyIterator = jsonData.keys();
        Map<String, TagVSData> currencyMap = new HashMap<String, TagVSData>();
        while(currencyIterator.hasNext()) {
            String keyStr = (String) currencyIterator.next();
            TagVSData currencyData = TagVSData.parse(jsonData.getJSONObject(keyStr));
            currencyData.setCurrencyCode(keyStr);
            currencyMap.put(keyStr, currencyData);
            userVSAccount.setCurrencyMap(currencyMap);
        }
        return userVSAccount;
    }

    public Map<String, TagVSData> getCurrencyMap() {
        return currencyMap;
    }

    public void setCurrencyMap(Map<String, TagVSData> currencyMap) {
        this.currencyMap = currencyMap;
    }

    public Date getWeekLapse() {
        return weekLapse;
    }

    public void setWeekLapse(Date weekLapse) {
        this.weekLapse = weekLapse;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
