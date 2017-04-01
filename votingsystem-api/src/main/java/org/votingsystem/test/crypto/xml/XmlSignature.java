package org.votingsystem.test.crypto.xml;


import org.kxml2.kdom.Element;
import org.votingsystem.crypto.CertUtils;
import org.votingsystem.util.Constants;

import java.security.cert.X509Certificate;
import java.util.Date;

public class XmlSignature {

    private String signatureId;
    private String documentMimeType;
    private String signatureMethod;
    private Date signingTime;
    private Date timeStampDate;
    private Element signatureElement;
    private Element signedPropertiesElement;
    private String documentDigestAlgorithm;
    private String signedPropertiesDigestAlgorithm;
    private X509Certificate signingCertificate;
    private Element signedPropertiesElementCanonicalized;
    private boolean signingCertSelfSigned;


    public XmlSignature() {}

    public String getSignatureId() {
        return signatureId;
    }

    public XmlSignature setSignatureId(String signatureId) {
        this.signatureId = signatureId;
        return this;
    }

    public String getDocumentMimeType() {
        return documentMimeType;
    }

    public XmlSignature setDocumentMimeType(String documentMimeType) {
        this.documentMimeType = documentMimeType;
        return this;
    }

    public String getSignatureMethod() {
        return signatureMethod;
    }

    public XmlSignature setSignatureMethod(String signatureMethod) {
        this.signatureMethod = signatureMethod;
        return this;
    }

    public Date getSigningTime() {
        return signingTime;
    }

    public XmlSignature setSigningTime(Date signingTime) {
        this.signingTime = signingTime;
        return this;
    }

    public Element getSignatureElement() {
        return signatureElement;
    }

    public XmlSignature setSignatureElement(Element signatureElement) {
        this.signatureElement = signatureElement;
        return this;
    }

    public Element getSignedPropertiesElement() {
        return signedPropertiesElement;
    }

    public XmlSignature setSignedPropertiesElement(Element signedPropertiesElement) {
        this.signedPropertiesElement = signedPropertiesElement;
        return this;
    }

    public String getDocumentDigestAlgorithm() {
        return documentDigestAlgorithm;
    }

    public XmlSignature setDocumentDigestAlgorithm(String documentDigestAlgorithm) {
        this.documentDigestAlgorithm = documentDigestAlgorithm;
        return this;
    }

    public String getSignedPropertiesDigestAlgorithm() {
        return signedPropertiesDigestAlgorithm;
    }

    public XmlSignature setSignedPropertiesDigestAlgorithm(String signedPropertiesDigestAlgorithm) {
        this.signedPropertiesDigestAlgorithm = signedPropertiesDigestAlgorithm;
        return this;
    }

    public X509Certificate getSigningCertificate() {
        return signingCertificate;
    }

    public XmlSignature setSigningCertificate(X509Certificate signingCertificate) throws Exception {
        this.signingCertificate = signingCertificate;
        String certExtData = CertUtils.getCertExtensionData(signingCertificate, Constants.ANON_CERT_OID);
        if(certExtData != null)
            setSigningCertSelfSigned(Boolean.valueOf(certExtData));
        return this;
    }

    public Element getSignedPropertiesElementCanonicalized() {
        return signedPropertiesElementCanonicalized;
    }

    public XmlSignature setSignedPropertiesElementCanonicalized(Element signedPropertiesElementCanonicalized) {
        this.signedPropertiesElementCanonicalized = signedPropertiesElementCanonicalized;
        return this;
    }

    public Date getTimeStampDate() {
        return timeStampDate;
    }

    public XmlSignature setTimeStampDate(Date timeStampDate) {
        this.timeStampDate = timeStampDate;
        return this;
    }

    public boolean isSigningCertSelfSigned() {
        return signingCertSelfSigned;
    }

    public XmlSignature setSigningCertSelfSigned(boolean signingCertSelfSigned) {
        this.signingCertSelfSigned = signingCertSelfSigned;
        return this;
    }
}
