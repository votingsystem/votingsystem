package org.votingsystem.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.signature.util.CertUtils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateVSDto {

    //SerialNumber as String to avoid Javascript problem handling such big numbers
    private String serialNumber;
    private String issuerSerialNumber;
    private String description;
    private String pemCert;
    private String subjectDN;
    private String issuerDN;
    private String sigAlgName;
    private Date notBefore;
    private Date notAfter;
    private CertificateVS.Type type;
    private CertificateVS.State state;
    private boolean isRoot;

    public CertificateVSDto() {}

    public CertificateVSDto(X509Certificate x509Cert) throws CertificateException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException {
        serialNumber = x509Cert.getSerialNumber().toString();
        isRoot = CertUtils.isSelfSigned(x509Cert);
        pemCert = new String(CertUtils.getPEMEncoded (x509Cert), "UTF-8");
        subjectDN = x509Cert.getSubjectDN().toString();
        issuerDN = x509Cert.getIssuerDN().toString();
        sigAlgName = x509Cert.getSigAlgName();
        notBefore = x509Cert.getNotBefore();
        notAfter = x509Cert.getNotAfter();
    }

    public CertificateVSDto(CertificateVS certificate) throws Exception {
        this(CertUtils.loadCertificate(certificate.getContent()));
        description = certificate.getDescription();
        type = certificate.getType();
        state = certificate.getState();
        if(certificate.getAuthorityCertificateVS() != null) issuerSerialNumber = certificate
                .getAuthorityCertificateVS().getSerialNumber().toString();
    }

    @JsonIgnore X509Certificate getX509Cert() throws Exception {
        return CertUtils.fromPEMToX509CertCollection(pemCert.getBytes()).iterator().next();
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getIssuerSerialNumber() {
        return issuerSerialNumber;
    }

    public String getDescription() {
        return description;
    }

    public String getPemCert() {
        return pemCert;
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

    public CertificateVS.Type getType() {
        return type;
    }

    public CertificateVS.State getState() {
        return state;
    }

    public boolean isRoot() {
        return isRoot;
    }
}
