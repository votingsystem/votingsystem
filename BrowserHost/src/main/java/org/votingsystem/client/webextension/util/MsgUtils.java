package org.votingsystem.client.webextension.util;

import org.votingsystem.dto.SocketMessageDto;
import org.votingsystem.model.TagVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.currency.MapUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    private static Logger log = Logger.getLogger(MsgUtils.class.getSimpleName());

    public static String getCurrencyChangeWalletMsg(SocketMessageDto messageDto) throws Exception {
        Map<String, BigDecimal> currencyMap = MapUtils.getCurrencyMap(messageDto.getCurrencySet());
        StringBuilder amountInfo = new StringBuilder();
        for(String currencyCode: currencyMap.keySet()) {
            amountInfo.append(" - " + currencyMap.get(currencyCode) + " " + currencyCode);
        }
        return ContextVS.getMessage("currency_wallet_change_msg", messageDto.getDeviceFromName(), amountInfo.toString());
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
        if(TagVS.WILDTAG.equals(tagName)) return ContextVS.getMessage("wildTagLbl").toLowerCase();
        else return tagName.toLowerCase();

    }

    public static String getWebSocketFormattedMessage(InboxMessage msg) {
        return "<html><span style='font-style: italic;color:#888;'>" + DateUtils.getDayWeekDateWithSecsStr(msg.getDate()) +
                " - <b>" + msg.getFrom() + "</b></span><br/><br/>" + msg.getMessage() + "</html>";
    }

    public static byte[] getWebExtensionMessagePrefix(int length) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ( length      & 0xFF);
        bytes[1] = (byte) ((length>>8)  & 0xFF);
        bytes[2] = (byte) ((length>>16) & 0xFF);
        bytes[3] = (byte) ((length>>24) & 0xFF);
        return bytes;
    }

}
