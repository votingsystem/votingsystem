package org.currency.web.ejb;

import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.x509.CertificateSource;
import eu.europa.esig.dss.x509.CertificateToken;
import org.iban4j.IbanFormatException;
import org.iban4j.InvalidCheckDigitException;
import org.iban4j.UnsupportedCountryException;
import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.model.currency.Tag;
import org.votingsystem.throwable.ValidationException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

/**
 *  License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface ConfigCurrencyServer {

    public Certificate loadAuthorityCertificate(CertificateToken trustedCertificate) throws IOException,
            CertificateException, NoSuchAlgorithmException, NoSuchProviderException;

    public String validateIBAN(String IBAN) throws IbanFormatException, InvalidCheckDigitException,
            UnsupportedCountryException;

    public Tag getTag(String tagName) throws ValidationException;

    public User getSystemUser();

    public String getOcspServerURL();

    public String getTempDir();

    public String getStaticResourcesURL();

    public User createIBAN(User user) throws ValidationException;

    public String getApplicationDataPath();

    public String getApplicationDirPath();

    public String getEntityId();

    public String getBankCode();

    public String getBranchCode();

    public void addTrustedTimeStampIssuer(X509Certificate trustedTimeStampIssuer);

    public Set<TrustAnchor> getTrustedCertAnchors();

    public Certificate getCACertificate(Long certificateId);

    public String getTimestampServiceURL();

    public AbstractSignatureTokenConnection getSigningToken();

    public CertificateSource getTrustedCertSource();

    public Map<Long, X509Certificate> getTrustedTimeStampServers();

    public MetadataDto getMetadata();

    public void putEntityMetadata(MetadataDto metadata);

    public X509Certificate getSigningCert();

}
