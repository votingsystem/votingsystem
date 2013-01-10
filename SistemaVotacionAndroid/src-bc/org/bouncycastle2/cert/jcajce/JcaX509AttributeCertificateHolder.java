package org.bouncycastle2.cert.jcajce;

import java.io.IOException;

import org.bouncycastle2.asn1.x509.AttributeCertificate;
import org.bouncycastle2.x509.X509AttributeCertificate;
import org.bouncycastle2.cert.X509AttributeCertificateHolder;

/**
 * JCA helper class for converting an old style X509AttributeCertificate into a X509AttributeCertificateHolder object.
 */
public class JcaX509AttributeCertificateHolder
    extends X509AttributeCertificateHolder
{
    /**
     * Base constructor.
     *
     * @param cert AttributeCertificate to be used a the source for the holder creation.
     * @throws IOException if there is a problem extracting the attribute certificate information.
     */
    public JcaX509AttributeCertificateHolder(X509AttributeCertificate cert)
        throws IOException
    {
        super(AttributeCertificate.getInstance(cert.getEncoded()));
    }
}
