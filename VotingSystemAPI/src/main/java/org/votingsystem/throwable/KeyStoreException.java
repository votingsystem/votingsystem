package org.votingsystem.throwable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class KeyStoreException extends Exception {

    private String metInf;

    public KeyStoreException(String message) {
        super(message);
    }

    public KeyStoreException(String message, String metaInf) {
        super(message);
        this.metInf = metaInf;
    }

    public KeyStoreException(String message, String metaInf, Throwable cause) {
        super(message, cause);
        this.metInf = metaInf;
    }

    public KeyStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getMetInf() {
        return metInf;
    }

    public void setMetInf(String metInf) {
        this.metInf = metInf;
    }

}
