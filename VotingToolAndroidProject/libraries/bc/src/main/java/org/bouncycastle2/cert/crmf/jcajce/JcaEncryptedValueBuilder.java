package org.bouncycastle2.cert.crmf.jcajce;

import org.bouncycastle2.asn1.crmf.EncryptedValue;
import org.bouncycastle2.cert.crmf.CRMFException;
import org.bouncycastle2.cert.crmf.EncryptedValueBuilder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle2.operator.KeyWrapper;
import org.bouncycastle2.operator.OutputEncryptor;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class JcaEncryptedValueBuilder
    extends EncryptedValueBuilder
{
    public JcaEncryptedValueBuilder(KeyWrapper wrapper, OutputEncryptor encryptor)
    {
        super(wrapper, encryptor);
    }

    public EncryptedValue build(X509Certificate certificate)
        throws CertificateEncodingException, CRMFException
    {
        return build(new JcaX509CertificateHolder(certificate));
    }
}
