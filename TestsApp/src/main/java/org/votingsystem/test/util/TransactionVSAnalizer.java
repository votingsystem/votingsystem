package org.votingsystem.test.util;

import org.votingsystem.model.UserVS;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.DateUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TransactionVSAnalizer {

    private static Logger log = Logger.getLogger(TransactionVSAnalizer.class.getSimpleName());

    private DateUtils.TimePeriod timePeriod;
    private UserVSTransactionBatch systemVSBatch;
    private List<UserVSTransactionBatch> groupVSListBatch;
    private List<UserVSTransactionBatch> userVSListBatch;

    public TransactionVSAnalizer() { }

    public DateUtils.TimePeriod getTimePeriod() {
        return timePeriod;
    }

    public static TransactionVSAnalizer parse(Map dataMap) throws Exception {
        TransactionVSAnalizer transactionVSAnalizer = new TransactionVSAnalizer();
        transactionVSAnalizer.timePeriod = DateUtils.TimePeriod.parse((Map) dataMap.get("timePeriod"));
        UserVSTransactionBatch systemBatch = UserVSTransactionBatch.parse(UserVS.Type.SYSTEM,
                (Map) ((Map)dataMap.get("userBalances")).get("systemBalance"));
        transactionVSAnalizer.systemVSBatch = systemBatch;
        List userVSArray = (List) ((Map)dataMap.get("userBalances")).get("groupVSBalanceList");
        List<UserVSTransactionBatch> groupListBatch = new ArrayList<>();
        for(int i = 0; i < userVSArray.size(); i++) {
            UserVSTransactionBatch groupBatch = UserVSTransactionBatch.parse(UserVS.Type.GROUP, (Map) userVSArray.get(i));
            groupListBatch.add(groupBatch);
        }
        transactionVSAnalizer.groupVSListBatch = groupListBatch;
        userVSArray = (List) ((Map)dataMap.get("userBalances")).get("userVSBalanceList");
        List<UserVSTransactionBatch> userListBatch = new ArrayList<>();
        for(int i = 0; i < userVSArray.size(); i++) {
            UserVSTransactionBatch userBatch = UserVSTransactionBatch.parse(UserVS.Type.USER,
                    (Map) userVSArray.get(i));
            userListBatch.add(userBatch);
        }
        transactionVSAnalizer.userVSListBatch = userListBatch;
        return transactionVSAnalizer;
    }

    public Map<String, Report> getReport(UserVS.Type userType) throws ExceptionVS {
        Map<String, Report> result = null;
        List<UserVSTransactionBatch> batchList = null;
        switch(userType) {
            case USER:
                batchList = userVSListBatch;
                break;
            case GROUP:
                batchList = groupVSListBatch;
                break;
        }
        for(UserVSTransactionBatch transactionBatch : batchList) {
            if(result != null) result = transactionBatch.getReport();
            else result = TransactionVSUtils.sumReport(result, transactionBatch.getReport());
        }
        return result;
    }

}