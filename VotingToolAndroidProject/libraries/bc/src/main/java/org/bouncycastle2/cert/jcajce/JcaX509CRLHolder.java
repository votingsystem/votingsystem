package org.bouncycastle2.cert.jcajce;

import org.bouncycastle2.asn1.x509.CertificateList;
import org.bouncycastle2.cert.X509CRLHolder;

import java.security.cert.CRLException;
import java.security.cert.X509CRL;

/**
 * JCA helper class for converting an X509CRL into a X509CRLHolder object.
 */
public class JcaX509CRLHolder
    extends X509CRLHolder
{
    /**
     * Base constructor.
     *
     * @param crl CRL to be used a the source for the holder creation.
     * @throws CRLException if there is a problem extracting the CRL information.
     */
    public JcaX509CRLHolder(X509CRL crl)
        throws CRLException
    {
        super(CertificateList.getInstance(crl.getEncoded()));
    }
}
