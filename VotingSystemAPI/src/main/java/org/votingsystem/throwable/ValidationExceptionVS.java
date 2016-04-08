package org.votingsystem.throwable;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ValidationExceptionVS extends ExceptionVS {


    public ValidationExceptionVS(String message) {
        super(message);
    }

    public ValidationExceptionVS(String message, Throwable cause) {
        super(message, cause);
    }

}
