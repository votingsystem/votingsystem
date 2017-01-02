package org.votingsystem.ejb;

import org.votingsystem.crypto.SignatureParams;
import org.votingsystem.model.User;
import org.votingsystem.throwable.CertificateValidationException;
import org.votingsystem.throwable.SignerValidationException;

import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface SignerInfoService {

    public User checkSigner(X509Certificate x509Certificate, User.Type userType, String entityId)
            throws SignerValidationException, CertificateValidationException;

    public User checkSigner(SignatureParams signatureParams) throws SignerValidationException;
}
