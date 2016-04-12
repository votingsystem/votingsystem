package org.votingsystem.web.currency.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jboss.logmanager.handlers.PeriodicRotatingFileHandler;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.JSON;

import java.io.File;
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

    public static final int LIMIT = 2 * 1024 * 1024;
    public static final int COUNT = 20;

    private String reporstLogPath;
    private String transactionsLogPath;
    private String currencyIssuedLogPath;
    private String weekPeriodLogPath;

    private static LoggerVS INSTANCE;

    static class TransactionFormatter extends Formatter {
        @Override public String format(LogRecord record) {
            return record.getMessage();
        }
    }

    public static void init(String logsDir) {
        try {
            INSTANCE = new LoggerVS();
            new File(logsDir).mkdirs();
            INSTANCE.reporstLogPath = logsDir + "/reports.log";
            INSTANCE.transactionsLogPath = logsDir + "/transactions.log";
            INSTANCE.currencyIssuedLogPath = logsDir + "/currency_issued.log";
            INSTANCE.weekPeriodLogPath = logsDir + "/week_period.log";

            FileHandler transactionsHandler = new FileHandler(INSTANCE.transactionsLogPath);
            transactionslog.addHandler(transactionsHandler);

            FileHandler currencyIssuedHandler = new FileHandler(INSTANCE.currencyIssuedLogPath, LIMIT, COUNT);
            currencyIssuedHandler.setFormatter(new TransactionFormatter());
            currencyIssuedlog.addHandler(currencyIssuedHandler);

            FileHandler reportsHandler = new FileHandler(INSTANCE.reporstLogPath, LIMIT, COUNT);
            reportsHandler.setFormatter(new TransactionFormatter());
            reportslog.addHandler(reportsHandler);

            FileHandler weekPeriodHandler = new FileHandler(INSTANCE.weekPeriodLogPath, LIMIT, COUNT);
            weekPeriodHandler.setFormatter(new TransactionFormatter());
            weekPeriodLog.addHandler(weekPeriodHandler);

        } catch (Exception ex) { ex.printStackTrace();}
    }

    public static String getReporstLogPath() {
        return INSTANCE.reporstLogPath;
    }

    public static String getTransactionsLogPath() {
        return INSTANCE.transactionsLogPath;
    }

    public static String getCurrencyIssuedLogPath() {
        return INSTANCE.currencyIssuedLogPath;
    }

    public static String getWeekPeriodLogPath() {
        return INSTANCE.weekPeriodLogPath;
    }

    public static void logReport(Map dataMap) throws JsonProcessingException {
        dataMap.put("date", Calendar.getInstance().getTime());
        reportslog.info(JSON.getMapper().writeValueAsString(dataMap) + ",");
    }

    public static void logReport(int status, String type, String message, String url) throws JsonProcessingException {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("date", Calendar.getInstance().getTime());
        dataMap.put("status", status);
        dataMap.put("type", type);
        dataMap.put("message", message);
        dataMap.put("url", url);
        reportslog.info(JSON.getMapper().writeValueAsString(dataMap) + ",");
    }

    public static void logTransaction(TransactionDto dto) throws JsonProcessingException {
        dto.setDateCreated(new Date());
        transactionslog.info(JSON.getMapper().writeValueAsString(dto) + ",");
    }

    public static void logCurrencyIssued(Currency currency) throws JsonProcessingException {
        CurrencyDto currencyDto = new CurrencyDto(currency);
        currencyIssuedlog.info(JSON.getMapper().writeValueAsString(currencyDto) + ",\n");
    }

    public static void weekLog(Level level, String msg, Throwable thrown) {
        weekPeriodLog.log(level, msg, thrown);
    }

}