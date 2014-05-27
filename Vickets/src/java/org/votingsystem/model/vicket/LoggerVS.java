package org.votingsystem.model.vicket;

import grails.converters.JSON;
import org.apache.log4j.Logger;

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
public class LoggerVS {

    private static Logger reportslog = Logger.getLogger("reportsLog");
    private static Logger transactionslog = Logger.getLogger("transactionsLog");

    public static void logReportVS(Map dataMap) {
        dataMap.put("date", Calendar.getInstance().getTime());
        reportslog.info(new JSON(dataMap) + ",");
    }

    public static void logReportVS(int status, String type, String message, String url) {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("date", Calendar.getInstance().getTime());
        dataMap.put("status", status);
        dataMap.put("type", type);
        dataMap.put("message", message);
        dataMap.put("url", url);
        reportslog.info(new JSON(dataMap) + ",");
    }

    public static void logTransactionVS(Map dataMap) {
        dataMap.put("date", Calendar.getInstance().getTime());
        transactionslog.info(new JSON(dataMap) + ",");
    }

    public static void logTransactionVS(long id, int status, String type, String fromUser, String toUser,
         String currency, BigDecimal amount, String msg, Date dateCreated, String subject) {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("id", id);
        dataMap.put("status", status);
        dataMap.put("date", Calendar.getInstance().getTime());
        dataMap.put("fromUser", fromUser);
        dataMap.put("toUser", toUser);
        dataMap.put("type", type);
        dataMap.put("subject", subject);
        dataMap.put("currency", currency);
        dataMap.put("amount", amount.setScale(2));
        dataMap.put("message", msg);
        dataMap.put("dateCreated", dateCreated);
        transactionslog.info(new JSON(dataMap) + ",");
    }

}
