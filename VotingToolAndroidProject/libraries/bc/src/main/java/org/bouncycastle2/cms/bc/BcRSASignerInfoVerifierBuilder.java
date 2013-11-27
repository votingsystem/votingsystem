package org.bouncycastle2.cms.bc;

import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cms.SignerInformationVerifier;
import org.bouncycastle2.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle2.operator.DigestAlgorithmIdentifierFinder;
import org.bouncycastle2.operator.DigestCalculatorProvider;
import org.bouncycastle2.operator.OperatorCreationException;
import org.bouncycastle2.operator.bc.BcRSAContentVerifierProviderBuilder;

import java.security.cert.CertificateException;

public class BcRSASignerInfoVerifierBuilder
{
    private BcRSAContentVerifierProviderBuilder contentVerifierProviderBuilder;
    private DigestCalculatorProvider digestCalculatorProvider;

    public BcRSASignerInfoVerifierBuilder(DigestAlgorithmIdentifierFinder digestAlgorithmFinder, DigestCalculatorProvider digestCalculatorProvider)
    {
        this.contentVerifierProviderBuilder = new BcRSAContentVerifierProviderBuilder(digestAlgorithmFinder);
        this.digestCalculatorProvider = digestCalculatorProvider;
    }

    public SignerInformationVerifier build(X509CertificateHolder certHolder)
        throws OperatorCreationException, CertificateException
    {
        return new SignerInformationVerifier(contentVerifierProviderBuilder.build(certHolder), digestCalculatorProvider);
    }

    public SignerInformationVerifier build(AsymmetricKeyParameter pubKey)
        throws OperatorCreationException
    {
        return new SignerInformationVerifier(contentVerifierProviderBuilder.build(pubKey), digestCalculatorProvider);
    }
}