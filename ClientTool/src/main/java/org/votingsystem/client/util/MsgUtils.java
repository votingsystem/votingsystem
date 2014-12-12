package org.votingsystem.client.util;

import org.apache.log4j.Logger;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.WebSocketMessage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    private static Logger log = Logger.getLogger(MsgUtils.class);

    public static String getCooinChangeWalletMsg(WebSocketMessage webSocketMessage) {
        Map<String, BigDecimal> currencyMap = Cooin.getCurrencyMap(webSocketMessage.getCooinList());
        StringBuilder amountInfo = new StringBuilder();
        for(String currencyCode: currencyMap.keySet()) {
            amountInfo.append(" - " + currencyMap.get(currencyCode) + " " + currencyCode);
        }
        return ContextVS.getMessage("cooin_wallet_change_msg", webSocketMessage.getDeviceFromName(), amountInfo.toString());
    }

    public static String getPlainWalletNotEmptyMsg(Map<String, BigDecimal> currencyMap) {
        StringBuilder amountInfo = new StringBuilder();
        for(String currencyCode: currencyMap.keySet()) {
            if(amountInfo.length() > 0) amountInfo.append(" - ");
            amountInfo.append(currencyMap.get(currencyCode) + " " + currencyCode);
        }
        return ContextVS.getMessage("plain_wallet_not_empty_msg", amountInfo.toString());
    }

}
