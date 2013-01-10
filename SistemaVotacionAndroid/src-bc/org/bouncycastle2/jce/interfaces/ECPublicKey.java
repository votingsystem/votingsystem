package org.bouncycastle2.jce.interfaces;

import java.security.PublicKey;

import org.bouncycastle2.math.ec.ECPoint;

/**
 * interface for elliptic curve public keys.
 */
public interface ECPublicKey
    extends ECKey, PublicKey
{
    /**
     * return the public point Q
     */
    public ECPoint getQ();
}
