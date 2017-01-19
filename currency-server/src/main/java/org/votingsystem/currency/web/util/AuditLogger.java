package org.votingsystem.currency.web.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.votingsystem.dto.currency.CurrencyDto;
import org.votingsystem.dto.currency.TransactionDto;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.JSON;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AuditLogger {

    private static final Logger log = Logger.getLogger(AuditLogger.class.getName());

    private static Logger currencyIssuedlog = Logger.getLogger("currency_issued");
    private static Logger transactionslog = Logger.getLogger("transactions");
    private static Logger weekPeriodLog = Logger.getLogger("week_period");
    private static Logger reportslog = Logger.getLogger("reports");


    //values from logging.properties
    public static final String LOG_FILE_SUFIX = ".yyyy-MM-dd";
    public static final String CURRENCY_ISSUED_LOG_FILE_PATH = "/var/local/voting_system/currency_server/reports/currency_issued.log";
    public static final String TRANSACTIONS_LOG_FILE_PATH = "/var/local/voting_system/currency_server/reports/transactions.log";
    public static final String WEEK_PERIOD_LOG_FILE_PATH = "/var/local/voting_system/currency_server/reports/week_period.log";
    public static final String REPORTS_FILE_LOG_PATH = "/var/local/voting_system/currency_server/reports/reports.log";

    public static File getReportsLogFile() {
        return new File(AuditLogger.REPORTS_FILE_LOG_PATH);
    }

    public static File getWeekPeriodLogFile() {
        return new File(AuditLogger.WEEK_PERIOD_LOG_FILE_PATH);
    }

    public static File getTransactionsLogFile() {
        return new File(AuditLogger.TRANSACTIONS_LOG_FILE_PATH);
    }

    public static File getCurrencyIssuedLogFile() {
        return new File(AuditLogger.CURRENCY_ISSUED_LOG_FILE_PATH);
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
        dto.setDateCreated(ZonedDateTime.now());
        transactionslog.info(JSON.getMapper().writeValueAsString(dto) + ",");
    }

    public static void logCurrencyIssued(Currency currency) throws JsonProcessingException {
        CurrencyDto currencyDto = new CurrencyDto(currency);
        currencyIssuedlog.info(JSON.getMapper().writeValueAsString(currencyDto) + ",");
    }

    public static void weekLog(Level level, String msg, Throwable thrown) {
        weekPeriodLog.log(level, msg, thrown);
    }

}