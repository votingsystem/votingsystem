package org.votingsystem.test.util

import org.votingsystem.model.UserVS
import org.votingsystem.util.DateUtils
import org.votingsystem.vicket.model.TransactionVS

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
class TransactionVSUtils {

    public static Map getGroupVSTransactionVS(TransactionVS transactionVS, UserVS groupVS) {
        Map result = new HashMap();
        result.put("operation", transactionVS.type.toString());
        result.put("fromUser", groupVS.getName());
        result.put("fromUserIBAN", groupVS.getIBAN());
        result.put("amount", transactionVS.amount.toString());
        result.put("currencyCode", transactionVS.currencyCode);
        result.put("subject", transactionVS.subject + " - " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()));
        if(TransactionVS.Type.FROM_GROUP_TO_ALL_MEMBERS != transactionVS.type) {
            result.put("toUserName", transactionVS.toUserVS.getName());
            result.put("toUserIBAN", Arrays.asList(transactionVS.toUserVS.getIBAN()));
        } else result.put("toUserIBAN", transactionVS.getToUserVSList());
        result.put("isTimeLimited", transactionVS.isTimeLimited);
        result.put("tags", Arrays.asList(transactionVS.tag.getName()));
        result.put("UUID", UUID.randomUUID().toString());
        return result;
    }

    public static Map getBankVSTransactionVS(TransactionVS transactionVS) {
        Map result = new HashMap();
        result.put("operation", transactionVS.type.toString());
        result.put("bankIBAN", transactionVS.fromUserVS.getIBAN());
        result.put("fromUser", transactionVS.fromUser);
        result.put("fromUserIBAN", transactionVS.fromUserIBAN);
        result.put("amount", transactionVS.amount.toString());
        result.put("currencyCode", transactionVS.currencyCode);
        result.put("subject", transactionVS.subject + " - " + DateUtils.getDayWeekDateStr(Calendar.getInstance().getTime()));
        result.put("toUserName", transactionVS.toUserVS.getName());
        result.put("toUserIBAN", Arrays.asList(transactionVS.toUserVS.getIBAN()));
        result.put("isTimeLimited", transactionVS.isTimeLimited);
        result.put("tags", Arrays.asList(transactionVS.tag.getName()));
        result.put("UUID", UUID.randomUUID().toString());
        return result;
    }
}
