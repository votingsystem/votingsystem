package org.votingsystem.signature.util;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Date;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public enum KeyGeneratorVS {
    
    INSTANCE;
    
    private KeyPairGenerator keyPairGenerator;
    private SecureRandom random;
    /** number of bytes serial number to generate, default 8 */
    private int noOctets = 8;
    
    private KeyGeneratorVS() { }
    
    public void init(String signName, String provider, int keySize, String algorithmRNG) throws
    		NoSuchAlgorithmException, NoSuchProviderException {
    	keyPairGenerator  = KeyPairGenerator.getInstance(signName, provider);
        keyPairGenerator.initialize(keySize, new SecureRandom());
        random = SecureRandom.getInstance(algorithmRNG);
        // from org.ejbca.core.ejb.ca.sign.SernoGenerator:
        // Using this seed we should get a different seed every time.
        // We are not concerned about the security of the random bits, only that
        // they are different every time.
        // Extracting 64 bit random numbers out of this should give us 2^32 (4
        // 294 967 296) serialnumbers before
        // collisions (which are seriously BAD), well anyhow sufficient for
        // pretty large scale installations.
        // Design criteria: 1. No counter to keep track on. 2. Multiple threads
        // can generate numbers at once, in
        // a clustered environment etc.
        long seed = Math.abs((new Date().getTime()) + this.hashCode());
        random.setSeed(seed);
    }
     
    public synchronized KeyPair genKeyPair () {
        return keyPairGenerator.genKeyPair();
    }

    public int getNextRandomInt() {
        return random.nextInt();
    }

    //8 bytes selected by a SecureRandom makes a good salt
    public byte[] getEncryptionSalt() {
        byte[] saltbytes = new byte[noOctets];
        random.nextBytes(new byte[noOctets]);
        return saltbytes;
    }

    public BigInteger getSerno() {
        final byte[] sernobytes = new byte[noOctets];
        random.nextBytes(sernobytes);
        BigInteger serno = new BigInteger(sernobytes).abs();
        return serno;
    }

    public byte[] getSalt() {
        random.setSeed(System.currentTimeMillis());
        final byte[] sernobytes = new byte[noOctets];
        random.nextBytes(sernobytes);
        return  sernobytes;
    }
    
}
