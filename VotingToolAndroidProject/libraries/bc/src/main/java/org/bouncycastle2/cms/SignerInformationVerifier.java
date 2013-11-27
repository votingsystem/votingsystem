package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.operator.*;

public class SignerInformationVerifier
{
    private ContentVerifierProvider verifierProvider;
    private DigestCalculatorProvider digestProvider;

    public SignerInformationVerifier(ContentVerifierProvider verifierProvider, DigestCalculatorProvider digestProvider)
    {
        this.verifierProvider = verifierProvider;
        this.digestProvider = digestProvider;
    }

    public boolean hasAssociatedCertificate()
    {
        return verifierProvider.hasAssociatedCertificate();
    }

    public X509CertificateHolder getAssociatedCertificate()
    {
        return verifierProvider.getAssociatedCertificate();
    }

    public ContentVerifier getContentVerifier(AlgorithmIdentifier algorithmIdentifier)
        throws OperatorCreationException
    {
        return verifierProvider.get(algorithmIdentifier);
    }

    public DigestCalculator getDigestCalculator(AlgorithmIdentifier algorithmIdentifier)
        throws OperatorCreationException
    {
        return digestProvider.get(algorithmIdentifier);
    }
}
