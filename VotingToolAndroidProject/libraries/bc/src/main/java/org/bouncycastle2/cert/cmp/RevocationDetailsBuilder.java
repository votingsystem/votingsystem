package org.bouncycastle2.cert.cmp;

import org.bouncycastle2.asn1.DERInteger;
import org.bouncycastle2.asn1.cmp.RevDetails;
import org.bouncycastle2.asn1.crmf.CertTemplateBuilder;
import org.bouncycastle2.asn1.x500.X500Name;
import org.bouncycastle2.asn1.x509.SubjectPublicKeyInfo;

import java.math.BigInteger;

public class RevocationDetailsBuilder
{
    private CertTemplateBuilder templateBuilder = new CertTemplateBuilder();
    
    public RevocationDetailsBuilder setPublicKey(SubjectPublicKeyInfo publicKey)
    {
        if (publicKey != null)
        {
            templateBuilder.setPublicKey(publicKey);
        }

        return this;
    }

    public RevocationDetailsBuilder setIssuer(X500Name issuer)
    {
        if (issuer != null)
        {
            templateBuilder.setIssuer(issuer);
        }

        return this;
    }

    public RevocationDetailsBuilder setSerialNumber(BigInteger serialNumber)
    {
        if (serialNumber != null)
        {
            templateBuilder.setSerialNumber(new DERInteger(serialNumber));
        }

        return this;
    }

    public RevocationDetailsBuilder setSubject(X500Name subject)
    {
        if (subject != null)
        {
            templateBuilder.setSubject(subject);
        }

        return this;
    }

    public RevocationDetails build()
    {
        return new RevocationDetails(new RevDetails(templateBuilder.build()));
    }
}
