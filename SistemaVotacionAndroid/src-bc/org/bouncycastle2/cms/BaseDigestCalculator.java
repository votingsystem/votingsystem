package org.bouncycastle2.cms;

import org.bouncycastle2.util.Arrays;

class BaseDigestCalculator
    implements IntDigestCalculator
{
    private final byte[] digest;

    BaseDigestCalculator(byte[] digest)
    {
        this.digest = digest;
    }

    public byte[] getDigest()
    {
        return Arrays.clone(digest);
    }
}
