package org.votingsystem.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class TagVSData implements Serializable {

    public static final String TAG = TagVSData.class.getSimpleName();

    public static final long serialVersionUID = 1L;

    private List<TransactionVS> transactionList;
    private List<Vicket> vicketList;
    private BigDecimal totalInputs = new BigDecimal(0);
    private BigDecimal totalOutputs = new BigDecimal(0);
    private Date lastRequestDate;
    private String currencyCode;

    public TagVSData() {}

    public TagVSData(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public TagVSData(List<TransactionVS> transactionList) throws Exception {
        this.transactionList = transactionList;
        for (TransactionVS transaction : transactionList) {
            if(currencyCode == null) currencyCode = transaction.getCurrencyCode();
            else if(currencyCode != transaction.getCurrencyCode()) throw new Exception(
                    "currencyCode ERROR expected: '" + currencyCode + "' - found: '" +
                            transaction.getCurrencyCode() + "'");
            switch(transaction.getType()) {
                case VICKET_CANCELLATION:
                    totalInputs = totalInputs.add(transaction.getAmount());
                    break;
                case VICKET_REQUEST:
                    totalOutputs = totalOutputs.add(transaction.getAmount());
                    break;
                default:
                    Log.d(TAG + ".parse(..) ", "Unknown transaction type: " +
                            transaction.getType().toString());
            }
        }
    }

    public List<TransactionVS> getTransactionList() {
        return transactionList;
    }

    public void setTransactionList(List<TransactionVS> transactionList) {
        this.transactionList = transactionList;
    }

    public BigDecimal getTotalInputs() {
        return totalInputs;
    }

    public void setTotalInputs(BigDecimal totalInputs) {
        this.totalInputs = totalInputs;
    }

    public BigDecimal getTotalOutputs() {
        return totalOutputs;
    }

    public void setTotalOutputs(BigDecimal totalOutputs) {
        this.totalOutputs = totalOutputs;
    }

    public BigDecimal getAccountBalance() {
        return totalInputs.add(totalOutputs.negate());
    }

    public Date getLastRequestDate() {
        return lastRequestDate;
    }

    public void setLastRequestDate(Date lastRequestDate) {
        this.lastRequestDate = lastRequestDate;
    }

    public BigDecimal getCashBalance() {
        BigDecimal result = new BigDecimal(0);
        if(vicketList != null && !vicketList.isEmpty()) {
            for(Vicket vicket : vicketList) {
                result = result.add(vicket.getAmount());
            }
        }
        return result;
    }


    public static TagVSData parse(JSONObject jsonData) throws
            ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        BigDecimal totalInputs = null;
        BigDecimal totalOutputs = null;
        TagVSData currencyData = new TagVSData();
        if(jsonData.has("totalInputs")) {
            totalInputs = new BigDecimal(jsonData.getString("totalInputs"));
            currencyData.setTotalInputs(totalInputs);
        }
        if(jsonData.has("totalOutputs")) {
            totalOutputs = new BigDecimal(jsonData.getString("totalOutputs"));
            currencyData.setTotalOutputs(totalOutputs);
        }
        if(jsonData.has("transactionList")) {
            jsonArray = jsonData.getJSONArray("transactionList");
            List<TransactionVS> transactionList = new ArrayList<TransactionVS>();
            BigDecimal totalInputsTransactions = new BigDecimal(0);
            BigDecimal totalOutputsTransactions = new BigDecimal(0);
            for (int i = 0; i< jsonArray.length(); i++) {
                TransactionVS transaction = TransactionVS.parse(jsonArray.getJSONObject(i));
                switch(transaction.getType()) {
                    case VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER_GROUP:
                    case VICKET_DEPOSIT_FROM_GROUP_TO_MEMBER:
                    case VICKET_DEPOSIT_FROM_GROUP_TO_ALL_MEMBERS:
                    case VICKET_CANCELLATION:
                    case BANKVS_INPUT:
                        totalInputsTransactions = totalInputsTransactions.add(transaction.getAmount());
                        break;
                    case VICKET_REQUEST:
                        totalOutputsTransactions = totalOutputsTransactions.add(transaction.getAmount());
                        break;
                    default:
                        Log.d(TAG + ".parse(..) ", "Unknown transaction type: " +
                                transaction.getType().toString());
                }
                transactionList.add(transaction);
            }
            if(!totalInputs.equals(totalInputsTransactions)) {
                Log.d(TAG + ".parse(...)", "ERROR - totalInputs: " + totalInputs +
                        " - totalInputsTransactions: " + totalInputsTransactions);
            }
            if(!totalOutputs.equals(totalOutputsTransactions)) {
                Log.d(TAG + ".parse(...)", "ERROR - totalOutputs: " + totalOutputs +
                        " - totalOutputsTransactions: " + totalOutputsTransactions);
            }
            currencyData.setTransactionList(transactionList);
        }
        return currencyData;
    }

    public List<Vicket> getVicketList() {
        return vicketList;
    }

    public void setVicketList(List<Vicket> vicketList) {
        this.vicketList = vicketList;
    }

    public void addVicket(Vicket vicket) {
        if (vicketList == null) vicketList = new ArrayList<Vicket>();
        vicketList.add(vicket);
    }


    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

}
