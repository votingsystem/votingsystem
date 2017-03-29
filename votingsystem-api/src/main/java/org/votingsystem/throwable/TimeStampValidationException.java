package org.votingsystem.throwable;

/**
 * Exceptions related to TimeStampToken validation exceptions
 *
 * @author votingsystem
 */
public class TimeStampValidationException extends Exception {

    public TimeStampValidationException(String message) {
        super(message);
    }

    public TimeStampValidationException(String message, Throwable e) {
        super(message, e);
    }

}
