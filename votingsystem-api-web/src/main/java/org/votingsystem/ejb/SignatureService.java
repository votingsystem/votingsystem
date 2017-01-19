package org.votingsystem.ejb;

import eu.europa.esig.dss.DSSDocument;
import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.model.XAdESDocument;
import org.votingsystem.throwable.DuplicatedDbItemException;
import org.votingsystem.throwable.SignatureException;
import org.votingsystem.throwable.XAdESValidationException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface SignatureService {

    public SignedDocument signXAdESAndSave(byte[] xmlToSign, SignatureParams signatureParams) throws SignatureException;
    public byte[] signXAdES(byte[] xmlToSign) throws SignatureException;
    public SignedDocument validateXAdESAndSave(DSSDocument signedDocument, SignatureParams signatureParams)
            throws XAdESValidationException, DuplicatedDbItemException;
    public XAdESDocument validateXAdES(final DSSDocument signedDocument, final SignatureParams signatureParams)
            throws XAdESValidationException;

}
