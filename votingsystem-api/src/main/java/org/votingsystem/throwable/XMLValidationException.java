package org.votingsystem.throwable;

/**
 * Exceptions related to XML validations
 *
 * @author votingsystem
 */
public class XMLValidationException extends Exception {

    public XMLValidationException(String message) {
        super(message);
    }

    public XMLValidationException(String message, Throwable e) {
        super(message, e);
    }

}
