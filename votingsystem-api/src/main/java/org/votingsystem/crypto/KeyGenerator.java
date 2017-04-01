package org.votingsystem.crypto;

import java.math.BigInteger;
import java.security.*;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum KeyGenerator {
    
    INSTANCE;
    
    private KeyPairGenerator keyPairGenerator;
    private SecureRandom random;
    //Default number of bytes serial number to generate
    private int noOctets = 8;
    
    KeyGenerator() { }
    
    public void init(String signName, String provider, int keySize, String algorithmRNG) throws
    		NoSuchAlgorithmException, NoSuchProviderException {
        keyPairGenerator  = KeyPairGenerator.getInstance(signName, provider);
        keyPairGenerator.initialize(keySize, new SecureRandom());
        random = SecureRandom.getInstance(algorithmRNG);
    }
     
    public synchronized KeyPair genKeyPair () {
         return keyPairGenerator.genKeyPair();
     } 
     
    public int getNextRandomInt() {
         return random.nextInt();
     }

    public int getNextRandomInt(int size) {
        return random.nextInt(size);
    }

    public BigInteger getSerno() {
        return new BigInteger(getSalt()).abs();
    }

    public byte[] getSalt() {
        random.setSeed(System.currentTimeMillis());
        final byte[] sernobytes = new byte[noOctets];
        random.nextBytes(sernobytes);
        return  sernobytes;
    }

}