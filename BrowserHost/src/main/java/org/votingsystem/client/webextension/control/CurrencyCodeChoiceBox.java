package org.votingsystem.client.webextension.control;

import javafx.scene.control.ChoiceBox;

import java.util.HashMap;
import java.util.Map;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyCodeChoiceBox extends ChoiceBox {

    public static final Map currencyMap = new HashMap<>();


    public CurrencyCodeChoiceBox() {
        super();
        currencyMap.put("Euro", "EUR");
        currencyMap.put("Dollar", "USD");
        currencyMap.put("Yuan", "CNY");
        currencyMap.put("Yen", "JPY");
        getItems().addAll(currencyMap.keySet());
        setWidth(100);
        getSelectionModel().select("Euro");
    }

    public String getSelected() {
        return (String) currencyMap.get(getValue());
    }

}
