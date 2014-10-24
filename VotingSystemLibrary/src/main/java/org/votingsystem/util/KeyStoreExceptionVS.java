package org.votingsystem.util;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class KeyStoreExceptionVS extends Exception {

    private String metInf;

    public KeyStoreExceptionVS(String message) {
        super(message);
    }

    public KeyStoreExceptionVS(String message, String metaInf) {
        super(message);
        this.metInf = metaInf;
    }

    public KeyStoreExceptionVS(String message, String metaInf, Throwable cause) {
        super(message, cause);
        this.metInf = metaInf;
    }

    public KeyStoreExceptionVS(String message, Throwable cause) {
        super(message, cause);
    }

    public String getMetInf() {
        return metInf;
    }

    public void setMetInf(String metInf) {
        this.metInf = metInf;
    }

}
