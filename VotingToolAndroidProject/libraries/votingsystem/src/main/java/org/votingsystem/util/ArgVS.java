package org.votingsystem.util;

import java.io.Serializable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ArgVS {

    private String key;
    private Serializable value;

    public ArgVS(String key, Serializable value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Serializable getValue() {
        return value;
    }

    public void setValue(Serializable value) {
        this.value = value;
    }
}
