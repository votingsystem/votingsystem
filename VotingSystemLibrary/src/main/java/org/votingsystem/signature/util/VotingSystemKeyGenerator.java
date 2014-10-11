package org.votingsystem.signature.util;

import org.apache.log4j.Logger;

import java.math.BigInteger;
import java.security.*;
import java.util.Date;

/**
 *
 * @author jgzornoza
 */
public enum VotingSystemKeyGenerator {
    
    INSTANCE;   
    
    private static Logger log = Logger.getLogger(VotingSystemKeyGenerator.class);
    
    private KeyPairGenerator keyPairGenerator;
    private SecureRandom random;
    /** number of bytes serial number to generate, default 8 */
    private int noOctets = 8;
    
    private VotingSystemKeyGenerator() { }
    
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

    public BigInteger getSerno() {
        random.setSeed(new Date().getTime());
        final byte[] sernobytes = new byte[noOctets];
        random.nextBytes(sernobytes);
        BigInteger serno = new BigInteger(sernobytes).abs();
        return serno;
    }

}
