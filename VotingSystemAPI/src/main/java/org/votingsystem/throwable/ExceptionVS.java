package org.votingsystem.throwable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ExceptionVS extends Exception {

    private String metInf;

    public ExceptionVS(String message) {
        super(message);
    }

    public ExceptionVS(String message, String metaInf) {
        super(message);
        this.metInf = metaInf;
    }

    public ExceptionVS(String message, String metaInf, Throwable cause) {
        super(message, cause);
        this.metInf = metaInf;
    }

    public ExceptionVS(String message, Throwable cause) {
        super(message, cause);
    }

    public String getMetInf() {
        return metInf;
    }

    public void setMetInf(String metInf) {
        this.metInf = metInf;
    }

}
