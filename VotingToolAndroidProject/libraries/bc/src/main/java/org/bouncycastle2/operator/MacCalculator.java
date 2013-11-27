package org.bouncycastle2.operator;

import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;

import java.io.OutputStream;

public interface MacCalculator
{
    AlgorithmIdentifier getAlgorithmIdentifier();

    /**
     * Returns a stream that will accept data for the purpose of calculating
     * the MAC for later verification. Use org.bouncycastle2.util.io.TeeOutputStream if you want to accumulate
     * the data on the fly as well.
     *
     * @return an OutputStream
     */
    OutputStream getOutputStream();

    /**
     * Return the calculated MAC based on what has been written to the stream.
     *
     * @return calculated MAC.
     */
    byte[] getMac();


    /**
     * Return the key used for calculating the MAC.
     *
     * @return the MAC key.
     */
    GenericKey getKey();
}