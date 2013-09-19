package org.bouncycastle2.cms;

import java.security.NoSuchAlgorithmException;

interface IntDigestCalculator
{
    byte[] getDigest()
        throws NoSuchAlgorithmException;
}
