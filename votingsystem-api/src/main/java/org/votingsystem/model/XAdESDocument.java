package org.votingsystem.model;

import eu.europa.esig.dss.DSSDocument;
import org.votingsystem.util.OperationType;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;


@Entity
@DiscriminatorValue("XADES_DOCUMENT")
public class XAdESDocument extends SignedDocument implements Serializable {

    private static Logger log = Logger.getLogger(XAdESDocument.class.getName());

    private static final long serialVersionUID = 1L;

    public XAdESDocument() {}

    public XAdESDocument(DSSDocument signedDocument, OperationType OperationType) throws IOException {
        super(signedDocument, OperationType);
    }

    public XAdESDocument(DSSDocument signedDocument, OperationType OperationType, String messageDigest)
            throws IOException {
        super(signedDocument, OperationType, messageDigest);
    }
}
