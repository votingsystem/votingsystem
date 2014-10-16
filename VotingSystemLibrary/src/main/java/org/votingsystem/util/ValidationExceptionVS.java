package org.votingsystem.util;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ValidationExceptionVS extends ExceptionVS {

    public ValidationExceptionVS(Class clazz, String message) {
        super(clazz.getSimpleName() + "_ExceptionVS: " + message);
    }

    public ValidationExceptionVS(Class clazz, String message, String metaInf) {
        super(clazz.getSimpleName() + "_ExceptionVS: " + message, metaInf);
    }

    public ValidationExceptionVS(String message, String metaInf) {
        super(message, metaInf);
    }

    public ValidationExceptionVS(String message, String metaInf, Throwable cause) {
        super(message, metaInf, cause);
    }

    public ValidationExceptionVS(String message, Throwable cause) {
        super(message, cause);
    }

}
