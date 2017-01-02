package org.votingsystem.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HashUtils {

    public static byte[] getHash(byte[] content, String digestMethod) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(digestMethod);
        return messageDigest.digest(content);
    }

    public static String getHashBase64(byte[] content, String digestMethod) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(digestMethod);
        byte[] resultDigest = messageDigest.digest(content);
        return Base64.getEncoder().encodeToString(resultDigest);
    }

}
