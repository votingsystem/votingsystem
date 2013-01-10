package org.bouncycastle2.cert.ocsp.jcajce;

import java.security.PublicKey;

import org.bouncycastle2.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle2.cert.ocsp.BasicOCSPRespBuilder;
import org.bouncycastle2.cert.ocsp.OCSPException;
import org.bouncycastle2.operator.DigestCalculator;

public class JcaBasicOCSPRespBuilder
    extends BasicOCSPRespBuilder
{
    public JcaBasicOCSPRespBuilder(PublicKey key, DigestCalculator digCalc)
        throws OCSPException
    {
        super(SubjectPublicKeyInfo.getInstance(key.getEncoded()), digCalc);
    }
}
