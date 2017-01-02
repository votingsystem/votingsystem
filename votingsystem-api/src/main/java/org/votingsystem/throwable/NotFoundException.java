package org.votingsystem.throwable;

public class NotFoundException extends ExceptionBase {

    public NotFoundException(String message, Exception ex) {
        super(message, ex);
    }

    public NotFoundException(String message) {
        super(message);
    }

}
