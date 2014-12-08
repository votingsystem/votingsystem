package org.votingsystem.throwable;

import org.votingsystem.throwable.ExceptionVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class ValidationExceptionVS extends ExceptionVS {

    public ValidationExceptionVS(Class clazz, String message) {
        super(message, clazz.getSimpleName() + "_ExceptionVS: ");
    }

    public ValidationExceptionVS(Class clazz, String message, String metaInf) {
        super(message, clazz.getSimpleName() + "_" + metaInf);
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
