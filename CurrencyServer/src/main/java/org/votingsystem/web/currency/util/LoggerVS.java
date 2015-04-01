package org.votingsystem.web.currency.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.votingsystem.model.TagVS;
import org.votingsystem.util.DateUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class LoggerVS {

    private static Logger reportslog = Logger.getLogger("reportsLog");
    private static Logger transactionslog = Logger.getLogger("transactionsLog");
    private static Logger currencyIssuedlog = Logger.getLogger("currencyIssuedLog");



    public static void logReportVS(Map dataMap) throws JsonProcessingException {
        dataMap.put("date", Calendar.getInstance().getTime());
        reportslog.info(new ObjectMapper().writeValueAsString(dataMap) + ",");
    }

    public static void logReportVS(int status, String type, String message, String url) throws JsonProcessingException {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("date", Calendar.getInstance().getTime());
        dataMap.put("status", status);
        dataMap.put("type", type);
        dataMap.put("message", message);
        dataMap.put("url", url);
        reportslog.info(new ObjectMapper().writeValueAsString(dataMap) + ",");
    }

    public static void logTransactionVS(Map dataMap) throws JsonProcessingException {
        dataMap.put("date", Calendar.getInstance().getTime());
        transactionslog.info(new ObjectMapper().writeValueAsString(dataMap) + ",");
    }

    public static void logTransactionVS(long id, String state, String type, String fromUser, String toUser,
         String currency, BigDecimal amount, TagVS tag, Date dateCreated, Date validTo, String subject, boolean isParent) throws JsonProcessingException {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("id", id);
        dataMap.put("state", state);
        dataMap.put("fromUser", fromUser);
        dataMap.put("toUser", toUser);
        dataMap.put("type", type);
        dataMap.put("subject", subject);
        dataMap.put("currency", currency);
        dataMap.put("amount", amount.setScale(2, RoundingMode.FLOOR).toString());
        if(tag != null) dataMap.put("tag", tag.getName());
        dataMap.put("dateCreated", DateUtils.getDayWeekDateStr(dateCreated));
        dataMap.put("isTimeLimited", (validTo == null)?false:true);
        dataMap.put("isParent", isParent);
        transactionslog.info(new ObjectMapper().writeValueAsString(dataMap) + ",");
    }

    public static void logCurrencyIssued(long id, String currency, BigDecimal amount,
                TagVS tag, boolean isTimeLimited, Date dateCreated, Date validTo) throws JsonProcessingException {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("id", id);
        dataMap.put("currency", currency);
        dataMap.put("amount", amount.setScale(2, RoundingMode.FLOOR).toString());
        dataMap.put("tag", tag.getName());
        dataMap.put("isTimeLimited", isTimeLimited);
        dataMap.put("dateCreated", DateUtils.getDayWeekDateStr(dateCreated));
        dataMap.put("validTo", DateUtils.getDayWeekDateStr(validTo));
        currencyIssuedlog.info(new ObjectMapper().writeValueAsString(dataMap) + ",");
    }

    public static Logger getLogger(String logFilePath) throws IOException {
        Logger logger = Logger.getLogger(LoggerVS.class.getSimpleName());
        FileHandler handler = new FileHandler(logFilePath, true);
        handler.setLevel (java.util.logging.Level.FINE);
        logger.addHandler(handler);
        return logger;
    }

    //Setting the Logging Configuration Programmatically -> http://docs.oracle.com/cd/E19340-01/820-6767/gcbkk/index.html
    public static java.util.logging.Logger getLogger(String logName, String logFilePath) throws IOException {
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(logName);
        FileHandler handler = new FileHandler(logFilePath, true);
        //FileHandler handler = new FileHandler("%t/test.log");
        handler.setLevel (java.util.logging.Level.FINE);
        logger.addHandler(handler);
        return logger;
    }

}
