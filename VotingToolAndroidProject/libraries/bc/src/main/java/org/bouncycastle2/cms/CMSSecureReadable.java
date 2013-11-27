package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.io.InputStream;
import java.security.Provider;

interface CMSSecureReadable
{
    AlgorithmIdentifier getAlgorithm();
    Object getCryptoObject();
    CMSReadable getReadable(SecretKey key, Provider provider)
        throws CMSException;
    InputStream getInputStream()
            throws IOException, CMSException;
}
