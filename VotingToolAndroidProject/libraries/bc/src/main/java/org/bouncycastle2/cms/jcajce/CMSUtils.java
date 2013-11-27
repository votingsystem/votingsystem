package org.bouncycastle2.cms.jcajce;

import org.bouncycastle2.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle2.asn1.x509.TBSCertificateStructure;
import org.bouncycastle2.asn1.x509.X509CertificateStructure;
import org.bouncycastle2.operator.GenericKey;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

class CMSUtils
{
    static TBSCertificateStructure getTBSCertificateStructure(
        X509Certificate cert)
        throws CertificateEncodingException
    {
            return TBSCertificateStructure.getInstance(cert.getTBSCertificate());
    }

    static Key getJceKey(GenericKey key)
    {
        if (key.getRepresentation() instanceof Key)
        {
            return (Key)key.getRepresentation();
        }

        if (key.getRepresentation() instanceof byte[])
        {
            return new SecretKeySpec((byte[])key.getRepresentation(), "ENC");
        }

        throw new IllegalArgumentException("unknown generic key type");
    }

    static IssuerAndSerialNumber getIssuerAndSerialNumber(X509Certificate cert)
        throws CertificateEncodingException
    {
        X509CertificateStructure certStruct = X509CertificateStructure.getInstance(cert.getEncoded());

        return new IssuerAndSerialNumber(certStruct.getIssuer(), certStruct.getSerialNumber());
    }
}