package org.votingsystem.signature.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Random;
import org.apache.log4j.Logger;

/**
 *
 * @author jgzornoza
 */
public enum VotingSystemKeyGenerator {
    
    INSTANCE;   
    
    private static Logger logger = Logger.getLogger(VotingSystemKeyGenerator.class);
    
    private KeyPairGenerator keyPairGenerator;
    private Random random; 
    
    private VotingSystemKeyGenerator() {

    }     
    
    public void init(String signName, String provider, int keySize) throws 
    		NoSuchAlgorithmException, NoSuchProviderException {
    	keyPairGenerator  = KeyPairGenerator.getInstance(signName, provider);
        keyPairGenerator.initialize(keySize, new SecureRandom());
        random = new Random();
    }
     
     public synchronized KeyPair genKeyPair () {
         return keyPairGenerator.genKeyPair();
     } 
     
     public int getNextRandomInt() {
         return random.nextInt();
     }
     
}
