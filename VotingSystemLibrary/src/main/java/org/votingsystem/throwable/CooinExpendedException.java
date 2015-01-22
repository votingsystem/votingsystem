package org.votingsystem.throwable;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CooinExpendedException extends ExceptionVS {

    public CooinExpendedException(String message) {
        super(message);
    }

    public CooinExpendedException(Class clazz, String message) {
        super(message, clazz.getSimpleName() + "_ExceptionVS: ");
    }

    public CooinExpendedException(Class clazz, String message, String metaInf) {
        super(message, clazz.getSimpleName() + "_" + metaInf);
    }

    public CooinExpendedException(String message, String metaInf) {
        super(message, metaInf);
    }

    public CooinExpendedException(String message, String metaInf, Throwable cause) {
        super(message, metaInf, cause);
    }

    public CooinExpendedException(String message, Throwable cause) {
        super(message, cause);
    }

}
