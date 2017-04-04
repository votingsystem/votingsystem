package org.votingsystem.ejb;

import org.votingsystem.dto.CertExtensionDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.throwable.CertificateRequestException;
import org.votingsystem.throwable.CertificateValidationException;
import org.votingsystem.throwable.InsufficientPrivilegesException;
import org.votingsystem.throwable.SignerValidationException;

import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface SignerInfoService {

    public User checkSigner(X509Certificate x509Certificate, User.Type userType, String entityId)
            throws SignerValidationException, CertificateValidationException;

    public User checkIfAdmin(X509Certificate x509Certificate) throws InsufficientPrivilegesException;

    public void loadCertInfo(User user, CertExtensionDto deviceData) throws CertificateRequestException;

    public Certificate verifyCertificate(X509Certificate certToCheck) throws Exception;
}