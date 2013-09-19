package org.bouncycastle2.asn1.pkcs;

import org.bouncycastle2.asn1.ASN1Encodable;
import org.bouncycastle2.asn1.ASN1Sequence;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;

public class KeyDerivationFunc
    extends AlgorithmIdentifier
{
    KeyDerivationFunc(
        ASN1Sequence  seq)
    {
        super(seq);
    }
    
    public KeyDerivationFunc(
        DERObjectIdentifier id,
        ASN1Encodable       params)
    {
        super(id, params);
    }
}
