package org.bouncycastle2.cert.crmf;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle2.operator.InputDecryptor;

public interface ValueDecryptorGenerator
{
    InputDecryptor getValueDecryptor(AlgorithmIdentifier keyAlg, AlgorithmIdentifier symmAlg, byte[] encKey)
        throws CRMFException;
}
