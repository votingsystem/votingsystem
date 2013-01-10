package org.bouncycastle2.operator;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;

public abstract class SymmetricKeyWrapper
    implements KeyWrapper
{
    private AlgorithmIdentifier algorithmId;

    protected SymmetricKeyWrapper(AlgorithmIdentifier algorithmId)
    {
        this.algorithmId = algorithmId;
    }

    public AlgorithmIdentifier getAlgorithmIdentifier()
    {
        return algorithmId;
    }
}
