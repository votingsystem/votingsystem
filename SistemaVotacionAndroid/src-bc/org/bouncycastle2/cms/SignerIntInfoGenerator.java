package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.cms.SignerInfo;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;

interface SignerIntInfoGenerator
{
    SignerInfo generate(DERObjectIdentifier contentType, AlgorithmIdentifier digestAlgorithm,
        byte[] calculatedDigest) throws CMSStreamException;
}
