package org.votingsystem.model;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum CurrencyCode {

    EUR("Euro"), USD("Dollar"), CNY("Dollar"), JPY("Yen");

    private CurrencyCode(String name) {
        this.name = name;
    }
    private String name;

    public String getName() {
        return name;
    }

}
