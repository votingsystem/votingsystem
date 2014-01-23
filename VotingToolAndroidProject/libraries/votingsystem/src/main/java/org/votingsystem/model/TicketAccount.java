package org.votingsystem.model;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.DateUtils;

import android.util.Log;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class TicketAccount implements Serializable {

    public static final String TAG = "TicketAccount";

    public static final long serialVersionUID = 1L;

    private UserVS userVS;
    private List<TransactionVS> transactionList;
    private BigDecimal totalInputs = new BigDecimal(0);
    private BigDecimal totalOutputs = new BigDecimal(0);
    private BigDecimal cashBalance = new BigDecimal(0);
    private Date lastRequestDate;


    public UserVS getUserVS() {
        return userVS;
    }

    public void setUserVS(UserVS userVS) {
        this.userVS = userVS;
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
        return cashBalance;
    }

    public void setCashBalance(BigDecimal cashBalance) {
        this.cashBalance = cashBalance;
    }

    public static TicketAccount parse(JSONObject jsonData) throws ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        BigDecimal totalInputs = null;
        BigDecimal totalOutputs = null;
        TicketAccount ticketAccount = new TicketAccount();
        if(jsonData.has("totalInputs")) {
            totalInputs = new BigDecimal(jsonData.getString("totalInputs"));
            ticketAccount.setTotalInputs(totalInputs);
        }
        if(jsonData.has("totalOutputs")) {
            totalOutputs = new BigDecimal(jsonData.getString("totalOutputs"));
            ticketAccount.setTotalOutputs(totalOutputs);
        }
        if(jsonData.has("date")){
            ticketAccount.setLastRequestDate(DateUtils.getDateFromString(
                    jsonData.getString("date")));
        }
        if(jsonData.has("transactions")) {
            jsonArray = jsonData.getJSONArray("transactions");
            List<TransactionVS> transactionList = new ArrayList<TransactionVS>();
            BigDecimal totalInputsTransactions = new BigDecimal(0);
            BigDecimal totalOutputsTransactions = new BigDecimal(0);
            for (int i = 0; i< jsonArray.length(); i++) {
                TransactionVS transaction = TransactionVS.parse(jsonArray.getJSONObject(i));
                if(transaction.getType() == TransactionVS.Type.USER_INPUT) {
                    totalInputsTransactions = totalInputsTransactions.add(transaction.getAmount());
                } else if(transaction.getType() == TransactionVS.Type.USER_OUTPUT) {
                    totalOutputsTransactions = totalOutputsTransactions.add(transaction.getAmount());
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
            ticketAccount.setTransactionList(transactionList);
        }
        return ticketAccount;
    }

}
