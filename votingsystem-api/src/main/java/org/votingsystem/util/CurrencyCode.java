package org.votingsystem.util;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum CurrencyCode {

    EUR("Euro"), USD("Dollar"), CNY("Yuan"), JPY("Yen");

    private CurrencyCode(String name) {
        this.name = name;
    }
    private String name;

    public String getName() {
        return name;
    }

}
