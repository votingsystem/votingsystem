package org.votingsystem.test.util;

import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
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


    public static UserVSTransactionBatch parse(UserVS.Type type, Map dataMap) throws Exception {
        TransactionVSBatch transactionVSFromBatch = new TransactionVSBatch(type, TransactionVS.Source.FROM);
        List transactionsFromArray = (List) dataMap.get("transactionFromList");
        for(int i = 0; i < transactionsFromArray.size(); i++) {
            transactionVSFromBatch.addTransaction(TransactionVS.parse((Map) transactionsFromArray.get(i)));
        }
        transactionVSFromBatch.checkBalances((Map<String, Map>) dataMap.get("balancesFrom"));
        TransactionVSBatch transactionVSToBatch = new TransactionVSBatch(type, TransactionVS.Source.TO);
        List transactionsToArray = (List) dataMap.get("transactionToList");
        for(int i = 0; i < transactionsToArray.size(); i++) {
            transactionVSToBatch.addTransaction(TransactionVS.parse((Map) transactionsToArray.get(i)));
        }
        transactionVSToBatch.checkBalances((Map<String, Map>) dataMap.get("balancesTo"));
        UserVSTransactionBatch userBatch = new UserVSTransactionBatch(type, transactionVSFromBatch,transactionVSToBatch);
        return userBatch;
    }

    public Map<String, Report> getReport() {
        Map result = new HashMap<>();
        result.put("transactionFromList", transactionVSFromBatch.getReport());
        result.put("transactionToList", transactionVSToBatch.getReport());
        return result;
    }

 }