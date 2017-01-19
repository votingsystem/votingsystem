package org.votingsystem.throwable;

public class HttpRequestException extends ExceptionBase {

    public HttpRequestException(String message) {
        super(message);
    }

    public HttpRequestException(String message, Throwable e) {
        super(message, e);
    }

}
