package org.votingsystem.throwable;

/**
 * @author votingsystem
 */
public class InsuficientPrivilegesException extends Exception {

    public InsuficientPrivilegesException(String message) {
        super(message);
    }

    public InsuficientPrivilegesException(String message, Throwable e) {
        super(message, e);
    }

}
