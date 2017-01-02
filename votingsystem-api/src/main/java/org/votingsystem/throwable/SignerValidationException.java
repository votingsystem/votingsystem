package org.votingsystem.throwable;

/**
 * Exceptions related to XML validations
 *
 * @author votingsystem
 */
public class SignerValidationException extends Exception {

    public SignerValidationException(String message) {
        super(message);
    }

    public SignerValidationException(String message, Throwable e) {
        super(message, e);
    }

}
