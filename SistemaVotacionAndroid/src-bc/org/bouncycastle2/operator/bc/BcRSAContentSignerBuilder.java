package org.bouncycastle2.operator.bc;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.crypto.Digest;
import org.bouncycastle2.crypto.Signer;
import org.bouncycastle2.crypto.signers.RSADigestSigner;
import org.bouncycastle2.operator.OperatorCreationException;

public class BcRSAContentSignerBuilder
    extends BcContentSignerBuilder
{
    public BcRSAContentSignerBuilder(AlgorithmIdentifier sigAlgId, AlgorithmIdentifier digAlgId)
    {
        super(sigAlgId, digAlgId);
    }

    protected Signer createSigner(AlgorithmIdentifier sigAlgId, AlgorithmIdentifier digAlgId)
        throws OperatorCreationException
    {
        Digest dig = BcUtil.createDigest(digAlgId);

        return new RSADigestSigner(dig);
    }
}
