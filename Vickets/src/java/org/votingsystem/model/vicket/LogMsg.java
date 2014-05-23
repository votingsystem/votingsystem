package org.votingsystem.model.vicket;

import grails.converters.JSON;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class LogMsg {

    ///(LogMsgVS) ([a-zA-Z_]+) (\{(.*?)\})/
    public static final Pattern regExPattern = Pattern.compile("(LogMsgVS) ([a-zA-Z_]+) (\\{(.*?)\\})");

    public static final String PREFIX = "LogMsgVS";
    public static final String REPORT_MSG = "REPORT_MSG_";
    public static final String TRSANSACTIONVS_MSG = "TRSANSACTIONVS_MSG";

    public static String getReportVSMsg(Map dataMap) {
        dataMap.put("date", Calendar.getInstance().getTime());
        return PREFIX + " " + REPORT_MSG + " " + new JSON(dataMap);
    }

    public static String getTransactionVSMsg(Map dataMap) {
        dataMap.put("date", Calendar.getInstance().getTime());
        return PREFIX + " " + TRSANSACTIONVS_MSG + " " + new JSON(dataMap);
    }

    public static String getTransactionVSMsg(int status, String type, String fromUser, String toUser,
             String currency, BigDecimal amount, String msg) {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("status", status);
        dataMap.put("date", Calendar.getInstance().getTime());
        dataMap.put("status", status);
        dataMap.put("fromUser", fromUser);
        dataMap.put("toUser", toUser);
        dataMap.put("type", type);
        dataMap.put("currency", currency);
        dataMap.put("amount", amount.setScale(2));
        dataMap.put("message", msg);
        return PREFIX + " " + TRSANSACTIONVS_MSG + " " + new JSON(dataMap);
    }

}
