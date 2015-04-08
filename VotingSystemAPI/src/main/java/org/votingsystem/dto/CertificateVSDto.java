package org.votingsystem.dto;

import org.votingsystem.model.CertificateVS;
import org.votingsystem.signature.util.CertUtils;

import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
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

    public CertificateVSDto(CertificateVS certificate) throws Exception {
        X509Certificate x509Cert = CertUtils.loadCertificate(certificate.getContent());
        serialNumber = x509Cert.getSerialNumber().toString();
        isRoot = CertUtils.isSelfSigned(x509Cert);
        description = certificate.getDescription();
        pemCert = new String(CertUtils.getPEMEncoded (x509Cert), "UTF-8");
        type = certificate.getType();
        state = certificate.getState();
        subjectDN = x509Cert.getSubjectDN().toString();
        issuerDN = x509Cert.getIssuerDN().toString();
        sigAlgName = x509Cert.getSigAlgName();
        notBefore = x509Cert.getNotBefore();
        notAfter = x509Cert.getNotAfter();
        if(certificate.getAuthorityCertificateVS() != null) issuerSerialNumber = certificate
                .getAuthorityCertificateVS().getSerialNumber().toString();
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