package org.votingsystem.client.util;

import org.apache.log4j.Logger;
import org.votingsystem.cooin.model.Cooin;
import org.votingsystem.model.ContextVS;
import org.votingsystem.util.WebSocketMessage;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MsgUtils {

    private static Logger log = Logger.getLogger(MsgUtils.class);

    public static String getCooinChangeWalletMsg(WebSocketMessage webSocketMessage) {
        Map<String, BigDecimal> currencyMap = new HashMap<String, BigDecimal>();
        for(Cooin cooin : webSocketMessage.getCooinList()){
            if(currencyMap.containsKey(cooin.getCurrencyCode())) currencyMap.put(cooin.getCurrencyCode(),
                    currencyMap.get(cooin.getCurrencyCode()).add(cooin.getAmount()));
            else currencyMap.put(cooin.getCurrencyCode(), cooin.getAmount());
        }
        StringBuilder amountInfo = new StringBuilder();
        for(String currencyCode: currencyMap.keySet()) {
            amountInfo.append(" - " + currencyMap.get(currencyCode) + " " + currencyCode);
        }
        return ContextVS.getMessage("cooin_wallet_change_msg", webSocketMessage.getDeviceFromName(), amountInfo.toString());
    }

}
