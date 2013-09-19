package org.bouncycastle2.crypto.tls;

import org.bouncycastle2.crypto.DSA;
import org.bouncycastle2.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle2.crypto.params.ECPublicKeyParameters;
import org.bouncycastle2.crypto.signers.ECDSASigner;

class TlsECDSASigner extends TlsDSASigner
{
    public boolean isValidPublicKey(AsymmetricKeyParameter publicKey)
    {
        return publicKey instanceof ECPublicKeyParameters;
    }

    protected DSA createDSAImpl()
    {
        return new ECDSASigner();
    }
}
