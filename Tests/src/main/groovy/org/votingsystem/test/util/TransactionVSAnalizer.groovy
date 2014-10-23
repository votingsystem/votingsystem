package org.votingsystem.test.util

import net.sf.json.JSONArray
import net.sf.json.JSONObject
import org.apache.log4j.Logger
import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSAnalizer {

    private static Logger log = Logger.getLogger(TransactionVSAnalizer.class);

    private DateUtils.TimePeriod timePeriod;
    private UserVSTransactionBatch systemVSBatch;
    private List<UserVSTransactionBatch> groupVSListBatch;
    private List<UserVSTransactionBatch> userVSListBatch;

    public TransactionVSAnalizer() { }

    public DateUtils.TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public static TransactionVSAnalizer parse(JSONObject requestJSON) {
        TransactionVSAnalizer transactionVSAnalizer = new TransactionVSAnalizer()
        transactionVSAnalizer.timePeriod = DateUtils.TimePeriod.parse(requestJSON.getJSONObject("timePeriod"))
        UserVSTransactionBatch systemBatch = UserVSTransactionBatch.parse(UserVS.Type.SYSTEM,
                requestJSON.getJSONObject("userBalances").getJSONObject("systemBalance"))
        transactionVSAnalizer.systemVSBatch = systemBatch

        JSONArray userVSArray = requestJSON.getJSONObject("userBalances").getJSONArray("groupVSBalanceList")
        List<UserVSTransactionBatch> groupListBatch = new ArrayList<>()
        for(int i = 0; i < userVSArray.size(); i++) {
            UserVSTransactionBatch groupBatch = UserVSTransactionBatch.parse(UserVS.Type.GROUP, userVSArray.get(i))
            groupListBatch.add(groupBatch)
        }
        transactionVSAnalizer.groupVSListBatch = groupListBatch

        userVSArray = requestJSON.getJSONObject("userBalances").getJSONArray("userVSBalanceList")
        List<UserVSTransactionBatch> userListBatch = new ArrayList<>()
        for(int i = 0; i < userVSArray.size(); i++) {
            UserVSTransactionBatch userBatch = UserVSTransactionBatch.parse(UserVS.Type.USER,userVSArray.get(i))
            userListBatch.add(userBatch)
        }
        transactionVSAnalizer.userVSListBatch = userListBatch

        return transactionVSAnalizer
    }

    public Map getReport(UserVS.Type userType) {
        Map resultReport = null
        List<UserVSTransactionBatch> batchList
        switch(userType) {
            case UserVS.Type.USER:
                batchList = userVSListBatch;
                break;
            case UserVS.Type.GROUP:
                batchList = groupVSListBatch;
                break;
        }
        for(UserVSTransactionBatch transactionBatch : batchList) {
            if(!resultReport) resultReport = transactionBatch.getReport()
            else resultReport = UserVSTransactionBatch.sumReport(resultReport, transactionBatch.getReport())
        }
        return resultReport
    }

}