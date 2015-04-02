package org.votingsystem.client.util;

import org.votingsystem.model.TagVS;
import org.votingsystem.model.currency.Currency;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    private static Logger log = Logger.getLogger(MsgUtils.class.getSimpleName());

    public static String getCurrencyChangeWalletMsg(WebSocketMessage webSocketMessage) {
        Map<String, BigDecimal> currencyMap = Currency.getCurrencyMap(webSocketMessage.getCurrencySet());
        StringBuilder amountInfo = new StringBuilder();
        for(String currencyCode: currencyMap.keySet()) {
            amountInfo.append(" - " + currencyMap.get(currencyCode) + " " + currencyCode);
        }
        return ContextVS.getMessage("currency_wallet_change_msg", webSocketMessage.getDeviceFromName(), amountInfo.toString());
    }

    public static String getPlainWalletNotEmptyMsg(Map<String, BigDecimal> currencyMap) {
        StringBuilder amountInfo = new StringBuilder();
        for(String currencyCode: currencyMap.keySet()) {
            if(amountInfo.length() > 0) amountInfo.append(" - ");
            amountInfo.append(currencyMap.get(currencyCode) + " " + currencyCode);
        }
        return ContextVS.getMessage("plain_wallet_not_empty_msg", amountInfo.toString());
    }

    public static String truncateLog(String message) {
        if(message == null) return null;
        else return message.length() > 300 ? message.substring(0, 300) + "..." : message;
    }


    public static String getTagDescription(String tagName) {
        if(TagVS.WILDTAG.equals(tagName)) return ContextVS.getMessage("wildTagLbl");
        else return tagName;

    }

    public static String getWebSocketFormattedMessage(InboxMessage msg) {
        return "<html><span style='font-style: italic;color:#888;'>" + DateUtils.getDayWeekDateStr(msg.getDate()) +
                " - <b>" + msg.getFrom() + "</b></span><br/><br/>" + msg.getMessage() + "</html>";
    }

}
