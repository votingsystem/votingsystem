package org.bouncycastle2.cms.jcajce;

import org.bouncycastle2.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle2.cms.KeyTransRecipientInfoGenerator;
import org.bouncycastle2.operator.OperatorCreationException;
import org.bouncycastle2.operator.jcajce.JceAsymmetricKeyWrapper;

import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class JceKeyTransRecipientInfoGenerator
    extends KeyTransRecipientInfoGenerator
{
    public JceKeyTransRecipientInfoGenerator(X509Certificate recipientCert)
        throws CertificateEncodingException
    {
        super(new JcaX509CertificateHolder(recipientCert).getIssuerAndSerialNumber(), new JceAsymmetricKeyWrapper(recipientCert.getPublicKey()));
    }

    public JceKeyTransRecipientInfoGenerator(byte[] subjectKeyIdentifier, PublicKey publicKey)
    {
        super(subjectKeyIdentifier, new JceAsymmetricKeyWrapper(publicKey));
    }

    public JceKeyTransRecipientInfoGenerator setProvider(String providerName)
        throws OperatorCreationException
    {
        ((JceAsymmetricKeyWrapper)this.wrapper).setProvider(providerName);

        return this;
    }

    public JceKeyTransRecipientInfoGenerator setProvider(Provider provider)
        throws OperatorCreationException
    {
        ((JceAsymmetricKeyWrapper)this.wrapper).setProvider(provider);

        return this;
    }
}