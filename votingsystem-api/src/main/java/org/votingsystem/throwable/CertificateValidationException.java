package org.votingsystem.throwable;

/**
 * Exceptions related to Certificate validation exceptions
 *
 * @author votingsystem
 */
public class CertificateValidationException extends Exception {

    public CertificateValidationException(String message) {
        super(message);
    }

    public CertificateValidationException(String message, Throwable e) {
        super(message, e);
    }

}
