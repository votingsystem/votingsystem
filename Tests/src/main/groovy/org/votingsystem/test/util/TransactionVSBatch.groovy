package org.votingsystem.test.util

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.apache.log4j.Logger
import org.votingsystem.model.UserVS
import org.votingsystem.util.ExceptionVS
import org.votingsystem.vicket.model.TransactionVS

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class TransactionVSBatch {

    private static Logger log = Logger.getLogger(TransactionVSBatch.class);

    List<TransactionVS> transacionVSList;

    private UserVS.Type type;
    private TransactionVS.Source source;

    public UserVS.Type getType() {
        return type;
    }

    public TransactionVS.Source getSource() {
        return source;
    }

    public TransactionVSBatch(UserVS.Type type, TransactionVS.Source source) {
        this.type = type;
        this.source = source;
        transacionVSList = new ArrayList<>()
    }

    public void addTransaction(TransactionVS transacionVS) {
        transacionVSList.add(transacionVS)
    }

    public void checkBalances(Map serverBalanceMap) throws ExceptionVS {
        Map transactionVSListCurrencyMap = getCurrencyMap(transacionVSList)
        for(String currency : transactionVSListCurrencyMap.keySet()) {
            if(!serverBalanceMap.containsKey(currency)) throw new ExceptionVS("Currency '" + currency + "' not found" +
                " in serverBalanceMap")
            Map tagDataMap = transactionVSListCurrencyMap[currency]
            for(String tag : tagDataMap.keySet()) {
                BigDecimal calculatedAmount = tagDataMap[tag].amount
                BigDecimal serverAmount
                if(serverBalanceMap[currency][tag] instanceof Map) serverAmount =
                        new BigDecimal(serverBalanceMap[currency][tag].total).setScale(2, BigDecimal.ROUND_DOWN)
                else serverAmount = new BigDecimal(serverBalanceMap[currency][tag]).setScale(2, BigDecimal.ROUND_DOWN)
                if(calculatedAmount.compareTo(serverAmount) != 0) throw new ExceptionVS("User type '${type.toString()}' " +
                        " - transactions '${source.toString()}' - ERROR currency: '" + currency + "' tag: '" + tag +
                        "' calculated amount: '" + calculatedAmount + "' server amount: ''" + serverAmount + "''")
            }
        }
        log.debug("Check OK - transactionVSListCurrencyMap: ${transactionVSListCurrencyMap} - serverBalanceMap: ${serverBalanceMap}")
    }

    public static Map getCurrencyMap(List<TransactionVS> transactionVSList) {
        Map currencyInfoMap = [:]
        for(TransactionVS transactionvs:transactionVSList) {
            transactionvs.getTag().getName()
            if(currencyInfoMap[transactionvs.currencyCode]) {
                if(currencyInfoMap[transactionvs.getCurrencyCode()][transactionvs.getTag().getName()] != null){
                    Map tagVSInfoMap = currencyInfoMap[transactionvs.currencyCode][transactionvs.tag.name]
                    tagVSInfoMap.numTransactionVS++
                    tagVSInfoMap.amount = ((BigDecimal)tagVSInfoMap.amount).add(transactionvs.amount)
                } else currencyInfoMap[transactionvs.currencyCode][(transactionvs.tag.name)] =
                        [numTransactionVS:1, amount:transactionvs.amount]
            } else {
                currencyInfoMap[(transactionvs.currencyCode)] = [(transactionvs.tag.name):[numTransactionVS:1,
                    amount:transactionvs.amount]]
            }
        }
        return currencyInfoMap
    }

    private void checkBalanceMap() {

    }

}
