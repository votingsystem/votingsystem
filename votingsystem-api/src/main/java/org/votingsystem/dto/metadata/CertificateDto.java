package org.votingsystem.dto.metadata;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.crypto.CertUtils;
import org.votingsystem.crypto.PEMUtils;
import org.votingsystem.model.Certificate;

import java.io.IOException;
import java.io.Serializable;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateDto implements Serializable {

    private static final long serialVersionUID = 1L;

    //SerialNumber as String to avoid Javascript problem handling such big numbers
    private String serialNumber;
    private String issuerSerialNumber;
    private String x509CertificatePEM;
    private String subjectDN;
    private String issuerDN;
    private String sigAlgName;
    private Date notBefore;
    private Date notAfter;
    private Certificate.Type type;
    private Certificate.State state;
    private boolean isRoot;

    public CertificateDto() {}

    public CertificateDto(X509Certificate x509Cert) throws CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException {
        serialNumber = x509Cert.getSerialNumber().toString();
        isRoot = CertUtils.isSelfSigned(x509Cert);
        x509CertificatePEM = new String(PEMUtils.getPEMEncoded (x509Cert), "UTF-8");
        subjectDN = x509Cert.getSubjectDN().toString();
        issuerDN = x509Cert.getIssuerDN().toString();
        sigAlgName = x509Cert.getSigAlgName();
        notBefore = x509Cert.getNotBefore();
        notAfter = x509Cert.getNotAfter();
    }

    public CertificateDto(Certificate certificate) throws Exception {
        this(CertUtils.loadCertificate(certificate.getContent()));
        type = certificate.getType();
        state = certificate.getState();
        if(certificate.getAuthorityCertificate() != null) issuerSerialNumber = certificate
                .getAuthorityCertificate().getSerialNumber().toString();
    }

    @JsonIgnore
    X509Certificate getX509Cert() throws Exception {
        return PEMUtils.fromPEMToX509CertCollection(x509CertificatePEM.getBytes()).iterator().next();
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getIssuerSerialNumber() {
        return issuerSerialNumber;
    }

    public String getX509CertificatePEM() {
        return x509CertificatePEM;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public String getSigAlgName() {
        return sigAlgName;
    }

    public Date getNotBefore() {
        return notBefore;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public Certificate.Type getType() {
        return type;
    }

    public Certificate.State getState() {
        return state;
    }

    public boolean isRoot() {
        return isRoot;
    }
}
