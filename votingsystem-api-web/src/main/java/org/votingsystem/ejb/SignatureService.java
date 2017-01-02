package org.votingsystem.ejb;

import eu.europa.esig.dss.DSSDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.XAdESDocument;
import org.votingsystem.throwable.DuplicatedDbItemException;
import org.votingsystem.throwable.XAdESValidationException;

import java.io.IOException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface SignatureService {

    public byte[] signXAdES(byte[] xmlToSign) throws IOException;
    public SignedDocument validateAndSaveXAdES(DSSDocument signedDocument, SignatureParams signatureParams)
            throws XAdESValidationException, DuplicatedDbItemException;
    public XAdESDocument validateXAdES(final DSSDocument signedDocument, final SignatureParams signatureParams)
            throws XAdESValidationException;

}
