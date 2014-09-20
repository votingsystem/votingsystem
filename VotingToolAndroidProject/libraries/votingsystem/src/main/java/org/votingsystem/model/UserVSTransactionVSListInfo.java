package org.votingsystem.model;

import org.json.JSONObject;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.ExceptionVS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
    private Map<String, Map<String, BigDecimal>> balancesToMap;
    private Map<String, Map<String, BigDecimal>> balancesFromMap;
    private Map<String, Map<String, BigDecimal>> balancesResultMap;

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

    public Map<String, Map<String, BigDecimal>> getBalancesToMap() {
        return balancesToMap;
    }

    public void setBalancesToMap(Map<String, Map<String, BigDecimal>> balancesToMap) {
        this.balancesToMap = balancesToMap;
    }

    public Map<String, Map<String, BigDecimal>> getBalancesFromMap() {
        return balancesFromMap;
    }

    public void setBalancesFromMap(Map<String, Map<String, BigDecimal>> balancesFromMap) {
        this.balancesFromMap = balancesFromMap;
    }

    public Map<String, Map<String, BigDecimal>> getBalancesResultMap() {
        return balancesResultMap;
    }

    public void setBalancesResultMap(Map<String, Map<String, BigDecimal>> balancesResultMap) {
        this.balancesResultMap = balancesResultMap;
    }

    public BigDecimal getAvailableForTagVS(String currencyCode, String tagStr) throws ExceptionVS {
        if(balancesResultMap.containsKey(currencyCode)) {
            Map<String, BigDecimal> currencyMap = balancesResultMap.get(currencyCode);
            if(currencyMap.containsKey(tagStr)) return currencyMap.get(tagStr);
        }
        throw new ExceptionVS("User has not account for tag '" + tagStr + "' with currency '" + currencyCode +"'");
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
        if(jsonData.has("balanceResult")) result.setBalancesResultMap(
                parseBalanceMap(jsonData.getJSONObject("balanceResult")));
        return result;
    }

    public static Map<String, Map<String, BigDecimal>> parseBalanceMap(JSONObject jsonData) throws Exception {
        Iterator currencyIterator = jsonData.keys();
        Map<String, Map<String, BigDecimal>> result = new HashMap<String, Map<String, BigDecimal>>();
        while(currencyIterator.hasNext()) {
            String currencyStr = (String) currencyIterator.next();
            Map<String, BigDecimal> tagVSBalanceMap = TagVS.parseTagVSBalanceMap(jsonData.getJSONObject(currencyStr));
            result.put(currencyStr, tagVSBalanceMap);
        }
        return result;
    }

    public DateUtils.TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(DateUtils.TimePeriod timePeriod) {
        this.timePeriod = timePeriod;
    }

}
