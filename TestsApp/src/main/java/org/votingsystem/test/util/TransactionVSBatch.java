package org.votingsystem.test.util;

import org.votingsystem.model.UserVS;
import org.votingsystem.model.currency.TransactionVS;
import org.votingsystem.test.dto.ReportDto;
import org.votingsystem.throwable.ExceptionVS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSBatch {

    private static Logger log = Logger.getLogger(TransactionVSBatch.class.getName());

    private List<TransactionVS> transacionVSList;
    private UserVS.Type type;
    private TransactionVS.Source source;
    private String currencyCode;

    public UserVS.Type getType() {
        return type;
    }

    public TransactionVS.Source getSource() {
        return source;
    }

    public TransactionVSBatch(UserVS.Type type, TransactionVS.Source source) {
        this.type = type;
        this.source = source;
        transacionVSList = new ArrayList<>();
    }

    public BigDecimal getTotalAmount() {
        BigDecimal result = BigDecimal.ZERO;
        for(TransactionVS transaction : transacionVSList) result = result.add(transaction.getAmount());
        return result;
    }

    public void addTransaction(TransactionVS transacionVS) throws ExceptionVS {
        if(currencyCode == null) currencyCode = transacionVS.getCurrencyCode();
        else if(!currencyCode.equals(transacionVS.getCurrencyCode())) throw new ExceptionVS("TransactionVSBatch - type:" +
            type.toString() + "' - source '" + source.toString() + "' expected currency: " + currencyCode + " - found '" +
                transacionVS.getCurrencyCode() + "'");
        transacionVSList.add(transacionVS);
    }

    public void checkBalances(Map<String, Map> serverBalanceMap) throws ExceptionVS {
        Map<String, Map> transactionVSListCurrencyMap = getCurrencyMap(transacionVSList);
        for(String currency : transactionVSListCurrencyMap.keySet()) {
            if(!serverBalanceMap.containsKey(currency)) throw new ExceptionVS("Currency '" + currency + "' not found" +
                " in serverBalanceMap");
            Map<String, Object> tagDataMap = (Map) transactionVSListCurrencyMap.get(currency);
            for(String tag : tagDataMap.keySet()) {
                BigDecimal calculatedAmount = (BigDecimal) ((Map)tagDataMap.get(tag)).get("amount");
                BigDecimal serverAmount = null;
                Object data = null;
                if((data = serverBalanceMap.get(currency).get(tag)) instanceof Map) serverAmount =
                        new BigDecimal((String) ((Map)data).get("total")).setScale(2, BigDecimal.ROUND_DOWN);
                else serverAmount = new BigDecimal((String) serverBalanceMap.get(currency).get(tag)).setScale(
                        2, BigDecimal.ROUND_DOWN);
                if(calculatedAmount.compareTo(serverAmount) != 0) throw new ExceptionVS("User type '${type.toString()}' " +
                        " - transactions '${source.toString()}' - ERROR currency: '" + currency + "' tag: '" + tag +
                        "' calculated amount: '" + calculatedAmount + "' server amount: ''" + serverAmount + "''");
            }
        }
        log.info("Check OK - transactionVSListCurrencyMap: " + transactionVSListCurrencyMap +
                "  - serverBalanceMap: " + serverBalanceMap);
    }

    public static Map<String, Map> getCurrencyMap(List<TransactionVS> transactionVSList) {
        Map<String, Map> currencyInfoMap = new HashMap<>();
        Map<String, Map<String, Object>> tagMap = null;
        Map<String, Object> tagInfoMap = null;
        for(TransactionVS transactionvs:transactionVSList) {
            transactionvs.getTag().getName();
            if((tagMap = currencyInfoMap.get(transactionvs.getCurrencyCode())) != null) {
                if((tagInfoMap = tagMap.get(transactionvs.getTag().getName())) != null){
                    tagInfoMap.put("numTransactionVS", ((Integer)tagInfoMap.get("numTransactionVS")) + 1);
                    BigDecimal newAmount = ((BigDecimal)tagInfoMap.get("amount")).add(transactionvs.getAmount());
                    tagInfoMap.put("amount", newAmount);
                } else {
                    tagInfoMap = new HashMap<>();
                    tagInfoMap.put("numTransactionVS", 1);
                    tagInfoMap.put("amount", transactionvs.getAmount());
                    tagMap.put(transactionvs.getTag().getName(), tagInfoMap);
                }
            } else {
                tagMap = new HashMap<>();
                tagInfoMap = new HashMap<>();
                tagInfoMap.put("numTransactionVS", 1);
                tagInfoMap.put("amount", transactionvs.getAmount());
                tagMap.put(transactionvs.getTag().getName(), tagInfoMap);
                currencyInfoMap.put(transactionvs.getCurrencyCode(), tagMap);
            }
        }
        return currencyInfoMap;
    }

    public ReportDto getReport() {
        return new ReportDto.Builder(type, currencyCode).numTotal(transacionVSList.size()).totalAmount(
                getTotalAmount()).source(source).currencyMap(TransactionVSUtils.getCurrencyMap(transacionVSList)).build();
    }


}
