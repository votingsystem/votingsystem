package org.votingsystem.ejb;

import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.throwable.CertificateValidationException;
import org.votingsystem.throwable.SignerValidationException;

import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface SignerInfoService {

    public User checkSigner(User signer, User.Type userType, String entityId) throws SignerValidationException,
            CertificateValidationException;

    public User checkSigner(X509Certificate certificate, User.Type userType, String entityId)
            throws SignerValidationException, CertificateValidationException;

    public User checkSigner(X509Certificate certificate, User.Type userType, String entityId, Certificate certificateCA)
            throws SignerValidationException, CertificateValidationException;

    public Certificate verifyCertificate(X509Certificate certToCheck) throws Exception;

}