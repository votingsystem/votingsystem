package org.bouncycastle2.cms.bc;

import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cms.KeyTransRecipientInfoGenerator;
import org.bouncycastle2.operator.bc.BcAsymmetricKeyWrapper;

public abstract class BcKeyTransRecipientInfoGenerator
    extends KeyTransRecipientInfoGenerator
{
    public BcKeyTransRecipientInfoGenerator(X509CertificateHolder recipientCert, BcAsymmetricKeyWrapper wrapper)
    {
        super(recipientCert.getIssuerAndSerialNumber(), wrapper);
    }

    public BcKeyTransRecipientInfoGenerator(byte[] subjectKeyIdentifier, BcAsymmetricKeyWrapper wrapper)
    {
        super(subjectKeyIdentifier, wrapper);
    }
}