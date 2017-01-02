package org.votingsystem.ejb;

import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateSource;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.model.Certificate;

import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Set;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface Config {

    public String getApplicationDirPath();
    public String getEntityId();
    public void addTrustedTimeStampIssuer(X509Certificate trustedTimeStampIssuer);
    public Set<TrustAnchor> getTrustedCertAnchors();
    public Certificate getCACertificate(Long certificateId);
    public String getTimestampServiceURL();
    public AbstractSignatureTokenConnection getSigningToken();
    public CertificateSource getTrustedCertSource();
    public Set<X509Certificate> getTrustedTimeStampServers();
    public MetadataDto getMetadata();
    public void putEntityMetadata(MetadataDto metadata);
    public X509Certificate getSigningCert();

}
