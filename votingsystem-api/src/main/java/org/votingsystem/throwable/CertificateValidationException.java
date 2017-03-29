package org.votingsystem.throwable;

/**
 * Exceptions related to Certificate validation exceptions
 *
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class CertificateValidationException extends Exception {

    public CertificateValidationException(String message) {
        super(message);
    }

    public CertificateValidationException(String message, Throwable e) {
        super(message, e);
    }

}
