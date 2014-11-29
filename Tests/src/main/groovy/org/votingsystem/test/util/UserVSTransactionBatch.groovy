package org.votingsystem.test.util

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.votingsystem.model.UserVS
import org.votingsystem.util.ExceptionVS
import org.votingsystem.cooin.model.TransactionVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class UserVSTransactionBatch {


    public UserVS.Type type;
    public TransactionVSBatch transactionVSFromBatch, transactionVSToBatch;

    public UserVSTransactionBatch(UserVS.Type type, TransactionVSBatch transactionVSFromBatch,
            TransactionVSBatch transactionVSToBatch) {
        this.transactionVSFromBatch = transactionVSFromBatch;
        this.transactionVSToBatch = transactionVSToBatch;
        this.type = type;
    }

    public TransactionVSBatch getTransactionVSFromBatch() {
        return transactionVSFromBatch;
    }

    public TransactionVSBatch getTransactionVSToBatch() {
        return transactionVSToBatch;
    }

    public UserVS.Type getType() { return type;}


    public static UserVSTransactionBatch parse(UserVS.Type type, JSONObject userJSON) {
        TransactionVSBatch transactionVSFromBatch = new TransactionVSBatch(type, TransactionVS.Source.FROM);
        JSONArray transactionsFromArray = userJSON.get("transactionFromList")
        for(int i = 0; i < transactionsFromArray.size(); i++) {
            transactionVSFromBatch.addTransaction(TransactionVS.parse(transactionsFromArray.get(i)))
        }
        transactionVSFromBatch.checkBalances(userJSON.getJSONObject("balancesFrom"))
        TransactionVSBatch transactionVSToBatch = new TransactionVSBatch(type, TransactionVS.Source.TO);
        JSONArray transactionsToArray = userJSON.get("transactionToList")
        for(int i = 0; i < transactionsToArray.size(); i++) {
            transactionVSToBatch.addTransaction(TransactionVS.parse(transactionsToArray.get(i)))
        }
        transactionVSToBatch.checkBalances(userJSON.getJSONObject("balancesTo"))
        UserVSTransactionBatch userBatch = new UserVSTransactionBatch(type, transactionVSFromBatch,transactionVSToBatch)
        return userBatch
    }

    public Map getReport() {
        return [transactionFromList:transactionVSFromBatch.getReport(), transactionToList:transactionVSToBatch.getReport()]
    }

    public static Map sumReport(Map reportMap, Map newReport) throws ExceptionVS {
        return[transactionFromList:TransactionVSBatch.sumReport(reportMap.transactionFromList, newReport.transactionFromList),
               transactionToList:TransactionVSBatch.sumReport(reportMap.transactionToList, newReport.transactionToList)]
    }

 }