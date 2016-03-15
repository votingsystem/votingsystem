package org.votingsystem.throwable;

/**

 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class InitPeriodException extends ExceptionVS {

    public InitPeriodException(String message) {
        super(message);
    }

    public InitPeriodException(String message, String metaInf) {
        super(message, metaInf);
    }

    public InitPeriodException(String message, String metaInf, Throwable cause) {
        super(message, metaInf, cause);
    }

    public InitPeriodException(String message, Throwable cause) {
        super(message, cause);
    }

}
