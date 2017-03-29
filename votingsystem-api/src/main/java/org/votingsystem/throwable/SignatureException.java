package org.votingsystem.throwable;

/**
 * Exceptions related to signing document
 *
 * @author votingsystem
 */
public class SignatureException extends Exception {

    public SignatureException(String message) {
        super(message);
    }

    public SignatureException(String message, Throwable e) {
        super(message, e);
    }

}
