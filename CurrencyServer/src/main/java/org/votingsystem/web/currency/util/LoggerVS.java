package org.votingsystem.web.currency.util;

import com.fasterxml.jackson.core.JsonProcessingException;
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


    public static String reporstLogPath;
    public static String transactionsLogPath;
    public static String currencyIssuedLogPath;
    public static String weekPeriodLogPath;

    static class TransactionFormatter extends Formatter {
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
            transactionsHandler.setFormatter(new TransactionFormatter());
            transactionslog.addHandler(transactionsHandler);
            //handler.setLevel (java.util.logging.Level.FINE);


            FileHandler currencyIssuedHandler = new FileHandler(currencyIssuedLogPath);
            currencyIssuedHandler.setFormatter(new TransactionFormatter());
            currencyIssuedlog.addHandler(currencyIssuedHandler);

            FileHandler reportsHandler = new FileHandler(reporstLogPath);
            reportsHandler.setFormatter(new TransactionFormatter());
            reportslog.addHandler(reportsHandler);

            FileHandler weekPeriodHandler = new FileHandler(weekPeriodLogPath);
            weekPeriodHandler.setFormatter(new TransactionFormatter());
            weekPeriodLog.addHandler(weekPeriodHandler);

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

    public static void logTransaction(TransactionDto dto) throws JsonProcessingException {
        dto.setDateCreated(new Date());
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
