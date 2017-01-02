package org.votingsystem.throwable;

import org.votingsystem.model.XAdESDocument;

/**
 * Exceptions related to XAdES signature validations
 *
 * @author votingsystem
 */
public class XAdESValidationException extends Exception {

    private XAdESDocument xAdESDocument;

    public XAdESValidationException(String message, XAdESDocument xAdESDocument) {
        super(message);
        this.xAdESDocument = xAdESDocument;
    }

    public XAdESValidationException(String message, Throwable e, XAdESDocument xAdESDocument) {
        super(message, e);
        this.xAdESDocument = xAdESDocument;
    }

    public XAdESDocument getXAdESDocument() {
        return xAdESDocument;
    }
}
