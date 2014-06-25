package org.votingsystem.vicket.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jgzornoza on 25/06/14.
 */
public class WalletVS {

    Map<String, BigDecimal> currencyDataMap = new HashMap<String, BigDecimal>();
    Map<String, BigDecimal> tagDataMap = new HashMap<String, BigDecimal>();

    public BigDecimal getTagBalance(String tagName) {
        return currencyDataMap.get(tagName);
    }


    public BigDecimal getCurrencyBalance(String tagName) {
        return currencyDataMap.get(tagName);
    }


}
