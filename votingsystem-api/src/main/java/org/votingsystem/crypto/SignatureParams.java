package org.votingsystem.crypto;

import org.bouncycastle.util.Store;
import org.votingsystem.model.Certificate;
import org.votingsystem.model.User;
import org.votingsystem.util.OperationType;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignatureParams {

    private OperationType operationType;
    private User.Type signerType;
    private String entityId;
    private X509Certificate signingCert;
    private Certificate certificateCA;
    private PrivateKey signingKey;
    private Store certs;
    private Boolean withTimeStampValidation = Boolean.TRUE;

    public SignatureParams(){}

    public SignatureParams(String entityId, User.Type signerType, OperationType operationType) {
        this.operationType = operationType;
        this.signerType = signerType;
        this.entityId = entityId;
    }

    public SignatureParams(String entityId, User.Type signerType, X509Certificate signingCert) {
        this.signingCert = signingCert;
        this.signerType = signerType;
        this.entityId = entityId;
    }

    public SignatureParams(X509Certificate signingCert, PrivateKey signingKey, Store certs) {
        this.signingCert = signingCert;
        this.signingKey = signingKey;
        this.certs = certs;
    }

    public X509Certificate getSigningCert() {
        return signingCert;
    }

    public SignatureParams setSigningCert(X509Certificate signingCert) {
        this.signingCert = signingCert;
        return this;
    }

    public PrivateKey getSigningKey() {
        return signingKey;
    }

    public void setSigningKey(PrivateKey signingKey) {
        this.signingKey = signingKey;
    }

    public Store getCerts() {
        return certs;
    }

    public void setCerts(Store certs) {
        this.certs = certs;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public User.Type getSignerType() {
        return signerType;
    }

    public void setSignerType(User.Type signerType) {
        this.signerType = signerType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public Certificate getCertificateCA() {
        return certificateCA;
    }

    public SignatureParams setCertificateCA(Certificate certificateCA) {
        this.certificateCA = certificateCA;
        return this;
    }

    public Boolean isWithTimeStampValidation() {
        return withTimeStampValidation;
    }

    public SignatureParams setWithTimeStampValidation(Boolean withTimeStampValidation) {
        this.withTimeStampValidation = withTimeStampValidation;
        return this;
    }

}
