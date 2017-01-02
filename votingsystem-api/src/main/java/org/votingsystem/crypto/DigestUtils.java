package org.votingsystem.crypto;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

    public static byte[] getSHA256Base64(byte[] content) throws NoSuchAlgorithmException {
        byte[] digest =  getSHA256(content);
        byte[] digestBase64 = org.bouncycastle.util.encoders.Base64.encode(digest);
        return digestBase64;
    }

    public static byte[] getSHA256(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(content);
        byte[] digest =  messageDigest.digest();
        return digest;
    }

    public static String getSHA1Base64Str(byte[] content) throws NoSuchAlgorithmException {
        return new String(getSHA1Base64(content));
    }

    public static byte[] getSHA1Base64(byte[] content) throws NoSuchAlgorithmException {
        byte[] digest =  getSHA1(content);
        byte[] digestBase64 = org.bouncycastle.util.encoders.Base64.encode(digest);
        return digestBase64;
    }

    public static byte[] getSHA1(byte[] content) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.update(content);
        byte[] digest =  messageDigest.digest();
        return digest;
    }
}
