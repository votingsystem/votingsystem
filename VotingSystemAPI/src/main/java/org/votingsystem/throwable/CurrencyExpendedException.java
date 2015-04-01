package org.votingsystem.throwable;

/**

 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CurrencyExpendedException extends ExceptionVS {

    public CurrencyExpendedException(String message) {
        super(message);
    }

    public CurrencyExpendedException(String message, String metaInf) {
        super(message, metaInf);
    }

    public CurrencyExpendedException(String message, String metaInf, Throwable cause) {
        super(message, metaInf, cause);
    }

    public CurrencyExpendedException(String message, Throwable cause) {
        super(message, cause);
    }

}
