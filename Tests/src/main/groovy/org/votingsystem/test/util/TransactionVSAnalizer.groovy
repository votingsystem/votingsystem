package org.votingsystem.test.util

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import net.sf.json.JSONSerializer
import org.apache.log4j.Logger
import org.votingsystem.model.*
import org.votingsystem.signature.smime.SMIMEMessage
import org.votingsystem.util.DateUtils
import org.votingsystem.util.ExceptionVS
import org.votingsystem.util.HttpHelper
import org.votingsystem.vicket.model.TransactionVS

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSAnalizer {

    private static Logger log = Logger.getLogger(TransactionVSAnalizer.class);

    private DateUtils.TimePeriod timePeriod;
    private VicketServer vicketServer;
    private UserVSTransactionBatch systemVSBatch;
    private List<UserVSTransactionBatch> groupVSListBatch;
    private List<UserVSTransactionBatch> userVSListBatch;


    public TransactionVSAnalizer() { }

    public DateUtils.TimePeriod getTimePeriod() {
        return timePeriod;
    }


    public Map getReport() {
        List<TransactionVS> transactionsVSList = getTransacionList();
        Map<String, TransactionType> resultMap = new HashMap<>()
        for(TransactionVS transactionVS : transactionsVSList) {
            if(resultMap.containsKey(transactionVS.getType().toString()))
                resultMap.get(transactionVS.getType().toString()).addTransaction(transactionVS.amount)
            else resultMap.put(transactionVS.getType().toString(), new TransactionType(transactionVS));
        }
        return resultMap
    }

    public BigDecimal getTotalAmount() {
        BigDecimal result = BigDecimal.ZERO;
        for(TransactionVS transactionVS : bankVSTransacionList) {
            result.add(transactionVS.getAmount());
        }
        return result;
    }

    public class TransactionType {
        BigDecimal amount = BigDecimal.ZERO;
        Integer numTransactions = 0;
        TransactionVS.Type type;

        public TransactionType(TransactionVS transactionVS) {
            this.type = transactionVS.type;
            this.amount = transactionVS.amount;
            this.numTransactions = 1
        }

        public void addTransaction(BigDecimal amount) {
            numTransactions++;
            this.amount = this.amount.add(amount)
        }

        @Override public String toString() {
            return type.toString() + " - '" + numTransactions + "' transactions - total: " + amount.toString()
        }
    }

    private void validateUserVSTransactions(JSONObject userData) { }

    public static TransactionVSAnalizer parse(JSONObject requestJSON) {
        TransactionVSAnalizer transactionVSAnalizer = new TransactionVSAnalizer()
        transactionVSAnalizer.timePeriod = DateUtils.TimePeriod.parse(requestJSON.getJSONObject("timePeriod"))
        UserVSTransactionBatch systemBatch = UserVSTransactionBatch.parse(UserVS.Type.SYSTEM,
                requestJSON.getJSONObject("userBalances").getJSONObject("systemBalance"))
        transactionVSAnalizer.systemVSBatch = systemBatch

        JSONArray userVSArray = requestJSON.getJSONObject("userBalances").getJSONArray("groupVSBalanceList")
        List<UserVSTransactionBatch> groupListBatch = new ArrayList<>()
        for(int i = 0; i < userVSArray.size(); i++) {
            UserVSTransactionBatch groupBatch = UserVSTransactionBatch.parse(UserVS.Type.GROUP,userVSArray.get(i))
            groupListBatch.add(groupBatch)
        }
        transactionVSAnalizer.groupVSListBatch = groupListBatch

        userVSArray = requestJSON.getJSONObject("userBalances").getJSONArray("userVSBalanceList")
        List<UserVSTransactionBatch> userListBatch = new ArrayList<>()
        for(int i = 0; i < userVSArray.size(); i++) {
            UserVSTransactionBatch userBatch = UserVSTransactionBatch.parse(UserVS.Type.USER,userVSArray.get(i))
            userListBatch.add(userBatch)
        }
        transactionVSAnalizer.groupVSListBatch = userListBatch

        return transactionVSAnalizer
    }

}