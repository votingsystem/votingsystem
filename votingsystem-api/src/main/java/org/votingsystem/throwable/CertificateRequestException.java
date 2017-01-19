package org.votingsystem.throwable;

/**
 * Exceptions related to Certificate requests generation
 *
 * @author votingsystem
 */
public class CertificateRequestException extends Exception {

    public CertificateRequestException(String message) {
        super(message);
    }

    public CertificateRequestException(String message, Throwable e) {
        super(message, e);
    }

}
