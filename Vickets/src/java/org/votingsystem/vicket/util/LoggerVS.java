package org.votingsystem.vicket.util;

import grails.converters.JSON;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.votingsystem.model.VicketTagVS;
import org.votingsystem.util.DateUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class LoggerVS {

    private static Logger reportslog = Logger.getLogger("reportsLog");
    private static Logger transactionslog = Logger.getLogger("transactionsLog");
    private static Logger vicketRequestslog = Logger.getLogger("vicketsRequestLog");
    private static Logger vicketsIssuedlog = Logger.getLogger("vicketsIssuedLog");


    public static void logReportVS(Map dataMap) {
        dataMap.put("date", Calendar.getInstance().getTime());
        reportslog.info(JSONSerializer.toJSON(dataMap).toString() + ",");
    }

    public static void logReportVS(int status, String type, String message, String url) {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("date", Calendar.getInstance().getTime());
        dataMap.put("status", status);
        dataMap.put("type", type);
        dataMap.put("message", message);
        dataMap.put("url", url);
        reportslog.info(JSONSerializer.toJSON(dataMap).toString() + ",");
    }

    public static void logTransactionVS(Map dataMap) {
        dataMap.put("date", Calendar.getInstance().getTime());
        transactionslog.info(JSONSerializer.toJSON(dataMap).toString() + ",");
    }

    public static void logTransactionVS(long id, String state, String type, String fromUser, String toUser,
         String currency, BigDecimal amount, VicketTagVS tag, Date dateCreated, String subject, boolean isParent) {
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
        dataMap.put("dateCreated", DateUtils.getDateStr(dateCreated));
        dataMap.put("isParent", isParent);
        transactionslog.info(JSONSerializer.toJSON(dataMap).toString() + ",");
    }

    public static void logVicketRequest(long id, String fromUser, String currency, BigDecimal amount,
            VicketTagVS tag, Date dateCreated) {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("id", id);
        dataMap.put("fromUser", fromUser);
        dataMap.put("currency", currency);
        dataMap.put("amount", amount.setScale(2, RoundingMode.FLOOR).toString());
        dataMap.put("tag", tag.getName());
        dataMap.put("dateCreated", DateUtils.getDateStr(dateCreated));
        vicketRequestslog.info(JSONSerializer.toJSON(dataMap).toString() + ",");

    }

    public static void logVicketIssued(long id, String currency, BigDecimal amount,
                VicketTagVS tag, Date dateCreated, Date validTo) {
        Map<String,Object> dataMap = new HashMap();
        dataMap.put("id", id);
        dataMap.put("currency", currency);
        dataMap.put("amount", amount.setScale(2, RoundingMode.FLOOR).toString());
        dataMap.put("tag", tag.getName());
        dataMap.put("dateCreated", dateCreated);
        dataMap.put("validTo", validTo);
        vicketsIssuedlog.info(JSONSerializer.toJSON(dataMap).toString() + ",");
    }

}
