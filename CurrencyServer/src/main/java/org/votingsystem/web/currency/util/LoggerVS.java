package org.votingsystem.web.currency.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.currency.TransactionVSDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.*;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class LoggerVS {

    private static Logger reportslog = Logger.getLogger("reportsLog");
    private static Logger transactionslog = Logger.getLogger("transactionsLog");
    private static Logger currencyIssuedlog = Logger.getLogger("currencyIssuedLog");
    private static Logger weekPeriodLog = Logger.getLogger("weekPeriodLog");


    public static String reporstLogPath;
    public static String transactionsLogPath;
    public static String currencyIssuedLogPath;
    public static String weekPeriodLogPath;

    static class TransactionVSFormatter extends Formatter {
        @Override public String format(LogRecord record) {
            return record.getMessage();
        }
    }

    public static void init(String logsDir) {
        try {
            new File(logsDir).mkdirs();
            reporstLogPath = logsDir + "/reports.log";
            transactionsLogPath = logsDir + "/transactionsVS.log";
            currencyIssuedLogPath = logsDir + "/currencyIssued.log";
            weekPeriodLogPath = logsDir + "/weekPeriodLog.log";

            FileHandler transactionsHandler = new FileHandler(transactionsLogPath);
            transactionsHandler.setFormatter(new TransactionVSFormatter());
            transactionslog.addHandler(transactionsHandler);
            //handler.setLevel (java.util.logging.Level.FINE);


            FileHandler currencyIssuedHandler = new FileHandler(currencyIssuedLogPath);
            currencyIssuedHandler.setFormatter(new TransactionVSFormatter());
            currencyIssuedlog.addHandler(currencyIssuedHandler);

            FileHandler reportsHandler = new FileHandler(reporstLogPath);
            reportsHandler.setFormatter(new TransactionVSFormatter());
            reportslog.addHandler(reportsHandler);

            FileHandler weekPeriodHandler = new FileHandler(weekPeriodLogPath);
            weekPeriodHandler.setFormatter(new TransactionVSFormatter());
            weekPeriodLog.addHandler(reportsHandler);

        } catch (Exception ex) { ex.printStackTrace();}
    }


    public static void logReportVS(Map dataMap) throws JsonProcessingException {
        dataMap.put("date", Calendar.getInstance().getTime());
        reportslog.info(JSON.getMapper().writeValueAsString(dataMap) + ",");
    }

    public static void logReportVS(int status, String type, String message, String url) throws JsonProcessingException {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("date", Calendar.getInstance().getTime());
        dataMap.put("status", status);
        dataMap.put("type", type);
        dataMap.put("message", message);
        dataMap.put("url", url);
        reportslog.info(JSON.getMapper().writeValueAsString(dataMap) + ",");
    }

    public static void logTransactionVS(TransactionVSDto dto) throws JsonProcessingException {
        dto.setDateCreated(new Date());
        transactionslog.info(JSON.getMapper().writeValueAsString(dto) + ",");
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
        currencyIssuedlog.info(JSON.getMapper().writeValueAsString(dataMap) + ",");
    }

    public static void weekLog(Level level, String msg, Throwable thrown) {
        weekPeriodLog.log(level, msg, thrown);
    }

}
