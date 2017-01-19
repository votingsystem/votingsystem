package org.votingsystem.idprovider.ejb;

import eu.europa.esig.dss.x509.CertificateToken;
import org.votingsystem.model.Certificate;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface ConfigIdProvider {

    public String getApplicationDirPath();
    public Certificate getCACertificate(Long certificateId);
    public Certificate loadAuthorityCertificate(CertificateToken certificateToken) throws IOException,
            CertificateException, NoSuchAlgorithmException, NoSuchProviderException;
    public X509Certificate getSigningCert();
    public String getEntityId();
    public String getOcspServerURL();
}
