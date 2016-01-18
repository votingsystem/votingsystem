package org.votingsystem.client.webextension.control;

import javafx.scene.control.ChoiceBox;

import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyCodeChoiceBox extends ChoiceBox {

    public static final Map<String, String> currencyMap = new HashMap(){{
        put("Euro", "EUR");
        put("Dollar", "USD");
        put("Yuan", "CNY");
        put("Yen", "JPY");
    }};


    public CurrencyCodeChoiceBox() {
        super();
        setWidth(100);
        getItems().addAll(currencyMap.keySet());
        getSelectionModel().select("Euro");
    }

    public String getSelected() {
        return (String) currencyMap.get(getValue());
    }

}
