package org.bouncycastle2.cert.jcajce;

import org.bouncycastle2.asn1.x500.X500Name;
import org.bouncycastle2.cert.X509v2CRLBuilder;

import javax.security.auth.x500.X500Principal;
import java.util.Date;

public class JcaX509v2CRLBuilder
    extends X509v2CRLBuilder
{
    public JcaX509v2CRLBuilder(X500Principal issuer, Date now)
    {
        super(X500Name.getInstance(issuer.getEncoded()), now);
    }
}
