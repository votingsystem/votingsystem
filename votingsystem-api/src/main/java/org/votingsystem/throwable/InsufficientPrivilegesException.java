package org.votingsystem.throwable;

/**
 * @author votingsystem
 */
public class InsufficientPrivilegesException extends Exception {

    public InsufficientPrivilegesException(String message) {
        super(message);
    }

    public InsufficientPrivilegesException(String message, Throwable e) {
        super(message, e);
    }

}
