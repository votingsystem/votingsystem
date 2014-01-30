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


public class CurrencyData implements Serializable {

    public static final String TAG = "CurrencyData";

    public static final long serialVersionUID = 1L;

    private List<TransactionVS> transactionList;
    private List<TicketVS> ticketList;
    private BigDecimal totalInputs = new BigDecimal(0);
    private BigDecimal totalOutputs = new BigDecimal(0);
    private Date lastRequestDate;
    private CurrencyVS currencyVS;


    public CurrencyData() {}

    public CurrencyData(List<TransactionVS> transactionList) throws Exception {
        this.transactionList = transactionList;
        for (TransactionVS transaction : transactionList) {
            if(currencyVS == null) currencyVS = transaction.getCurrencyVS();
            else if(currencyVS != transaction.getCurrencyVS()) throw new Exception(
                    "Transaction List with mixed currencies " + currencyVS + ", " +
                    transaction.getCurrencyVS());
            switch(transaction.getType()) {
                case USER_ALLOCATION_INPUT:
                case USER_INPUT:
                case TICKET_CANCELLATION:
                    totalInputs = totalInputs.add(transaction.getAmount());
                    break;
                case USER_OUTPUT:
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
        if(ticketList != null && !ticketList.isEmpty()) {
            for(TicketVS ticketVS : ticketList) {
                result = result.add(ticketVS.getAmount());
            }
        }
        return result;
    }


    public static CurrencyData parse(JSONObject jsonData) throws
            ParseException, JSONException {
        JSONArray jsonArray;
        JSONObject jsonObject;
        BigDecimal totalInputs = null;
        BigDecimal totalOutputs = null;
        CurrencyData currencyData = new CurrencyData();
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
                    case USER_ALLOCATION_INPUT:
                    case USER_INPUT:
                    case TICKET_CANCELLATION:
                        totalInputsTransactions = totalInputsTransactions.add(transaction.getAmount());
                        break;
                    case USER_OUTPUT:
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

    public List<TicketVS> getTicketList() {
        return ticketList;
    }

    public void setTicketList(List<TicketVS> ticketList) {
        this.ticketList = ticketList;
    }

    public void addTicket(TicketVS ticket) {
        if (ticketList == null) ticketList = new ArrayList<TicketVS>();
        ticketList.add(ticket);
    }


    public CurrencyVS getCurrencyVS() {
        return currencyVS;
    }

    public void setCurrencyVS(CurrencyVS currencyVS) {
        this.currencyVS = currencyVS;
    }

}
