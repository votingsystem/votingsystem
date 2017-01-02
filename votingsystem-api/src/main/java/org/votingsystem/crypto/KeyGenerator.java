package org.votingsystem.crypto;


import java.math.BigInteger;
import java.security.*;
import java.util.Date;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum KeyGenerator {
    
    INSTANCE;   
    
    private static Logger log = Logger.getLogger(KeyGenerator.class.getName());
    
    private KeyPairGenerator keyPairGenerator;
    private SecureRandom random;
    /** number of bytes serial number to generate, default 8 */
    private int noOctets = 8;
    
    private KeyGenerator() { }
    
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
        random.setSeed(new Date().getTime());
        final byte[] sernobytes = new byte[noOctets];
        random.nextBytes(sernobytes);
        return new BigInteger(sernobytes).abs();
    }

    public byte[] getSalt() {
        random.setSeed(System.currentTimeMillis());
        final byte[] sernobytes = new byte[noOctets];
        random.nextBytes(sernobytes);
        return  sernobytes;
    }
}
