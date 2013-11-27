package org.bouncycastle2.cms.bc;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle2.operator.bc.BcRSAAsymmetricKeyWrapper;

import java.io.IOException;

public class BcRSAKeyTransRecipientInfoGenerator
    extends BcKeyTransRecipientInfoGenerator
{
    public BcRSAKeyTransRecipientInfoGenerator(byte[] subjectKeyIdentifier, AlgorithmIdentifier encAlgId, AsymmetricKeyParameter publicKey)
    {
        super(subjectKeyIdentifier, new BcRSAAsymmetricKeyWrapper(encAlgId, publicKey));
    }

    public BcRSAKeyTransRecipientInfoGenerator(X509CertificateHolder recipientCert)
        throws IOException
    {
        super(recipientCert, new BcRSAAsymmetricKeyWrapper(recipientCert.getSubjectPublicKeyInfo().getAlgorithmId(), recipientCert.getSubjectPublicKeyInfo()));
    }
}
