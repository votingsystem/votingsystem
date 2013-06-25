package org.sistemavotacion.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Random;
import org.sistemavotacion.Contexto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * @author jgzornoza
 */
public enum VotingSystemKeyGenerator {
    
    INSTANCE;   
    
    private static Logger logger = LoggerFactory.getLogger(
            VotingSystemKeyGenerator.class);
    
    private KeyPairGenerator keyPairGenerator;
    private Random random; 
    
    private VotingSystemKeyGenerator() {
        try {
            keyPairGenerator  = KeyPairGenerator.getInstance(
                Contexto.SIG_NAME, Contexto.PROVIDER);
            keyPairGenerator.initialize(Contexto.KEY_SIZE, new SecureRandom());
            random = new Random();
        } catch (Exception ex) {
            LoggerFactory.getLogger(VotingSystemKeyGenerator.class).error(ex.getMessage(), ex);
        }
    }     
     
     public synchronized KeyPair genKeyPair () {
         return keyPairGenerator.genKeyPair();
     } 
     
     public int getNextRandomInt() {
         return random.nextInt();
     }
     
}
