package org.votingsystem.test.misc;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.*;

public class JCEPolicyTest {

    private static final int MAX_ALLOWED_KEY_LENGHT_WITHOUT_POLICY_FILES = 128;

    public static void main(String[] args) throws NoSuchPaddingException, InvalidKeyException, NoSuchAlgorithmException,
            IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidAlgorithmParameterException {
        cipherTest();
    }

    public static void simpleTest() {
        try {
            System.out.println("Testing JCEPolicyTest");
            int maxKeyLen = Cipher.getMaxAllowedKeyLength("AES");
            System.out.println("MaxAllowedKeyLength: " + maxKeyLen);
            if(!(maxKeyLen > MAX_ALLOWED_KEY_LENGHT_WITHOUT_POLICY_FILES)) {
                System.out.println("In order to run the applications you must install in your JVM the unrestricted policy flies");
            }
        } catch (Exception e){
            System.out.println("Sad world :(");
        }
    }

    //https://golb.hplar.ch/2017/10/JCE-policy-changes-in-Java-SE-8u151-and-8u152.html
    private static void cipherTest() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] input = "My super secret text".getBytes();

        SecureRandom random = SecureRandom.getInstanceStrong();
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128, random);
        SecretKey key = keyGen.generateKey();

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] cipherText = cipher.doFinal(input);

        // Decrypt
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] plainText = cipher.doFinal(cipherText);
        System.out.println(new String(plainText));
    }

}
