package org.votingsystem.util.crypto;

import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.bc.BcCMSContentEncryptorBuilder;
import org.bouncycastle.cms.bc.BcRSAKeyTransRecipientInfoGenerator;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientId;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.openssl.PEMParser;
import org.votingsystem.dto.AESParamsDto;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class Encryptor {
 
    private static Logger log = Logger.getLogger(Encryptor.class.getName());

    private static final int ITERATION_COUNT = 1024;
    private static final int KEY_LENGTH = 128; // 192 and 256 bits may not be available

    private Recipient recipient;
    private RecipientId recipientId;
    private PrivateKey privateKey;

    public Encryptor(X509Certificate localCert, PrivateKey localPrivateKey) {
        this.privateKey = localPrivateKey;
        recipientId = new JceKeyTransRecipientId(localCert);
        recipient = new JceKeyTransEnvelopedRecipient(localPrivateKey).setProvider(ContextVS.PROVIDER);
    }

    public static byte[] encryptToCMS(byte[] bytesToEncrypt, PublicKey publicKey) throws Exception {
        CMSEnvelopedDataGenerator edGen = new CMSEnvelopedDataGenerator();
        edGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator("".getBytes(), publicKey));
        CMSEnvelopedData ed = edGen.generate(
                new CMSProcessableByteArray(bytesToEncrypt),
                new BcCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).build());
        return PEMUtils.getPEMEncoded(ed.toASN1Structure());
    }

    public static byte[] encryptToCMS(byte[] bytesToEncrypt, X509Certificate receiverCert) throws Exception {
        CMSEnvelopedDataGenerator edGen = new CMSEnvelopedDataGenerator();
        edGen.addRecipientInfoGenerator(new BcRSAKeyTransRecipientInfoGenerator(new JcaX509CertificateHolder(receiverCert)));
        CMSEnvelopedData ed = edGen.generate(
                new CMSProcessableByteArray(bytesToEncrypt),
                new BcCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC).build());
        return PEMUtils.getPEMEncoded(ed.toASN1Structure());
    }

    public byte[] decryptCMS(byte[] pemEncrypted) throws Exception {
        PEMParser PEMParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(pemEncrypted)));
        ContentInfo contentInfo = (ContentInfo) PEMParser.readObject();
        CMSEnvelopedDataParser ep = new CMSEnvelopedDataParser(contentInfo.getEncoded());
        RecipientInformationStore  recipients = ep.getRecipientInfos();
        Iterator                   it = recipients.getRecipients().iterator();
        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipient = (RecipientInformation)it.next();
            CMSTypedStream recData = recipient.getContentStream(
                    new JceKeyTransEnvelopedRecipient(privateKey).setProvider(ContextVS.PROVIDER));
            return FileUtils.getBytesFromStream(recData.getContentStream());
        }
        return result;
    }

    public static byte[] decryptCMS (byte[] pemEncrypted, PrivateKey privateKey) throws CMSException, IOException {
        PEMParser PEMParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(pemEncrypted)));
        ContentInfo contentInfo = (ContentInfo) PEMParser.readObject();
        CMSEnvelopedDataParser ep = new CMSEnvelopedDataParser(contentInfo.getEncoded());
        RecipientInformationStore  recipients = ep.getRecipientInfos();
        Iterator                   it = recipients.getRecipients().iterator();
        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipient = (RecipientInformation)it.next();
            CMSTypedStream recData = recipient.getContentStream(
                    new JceKeyTransEnvelopedRecipient(privateKey).setProvider(ContextVS.PROVIDER));
            return FileUtils.getBytesFromStream(recData.getContentStream());
        }
        return result;
    }

    public static EncryptedBundle pbeAES_Encrypt(char[] password, byte[] bytesToEncrypt) throws NoSuchAlgorithmException,
            InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidParameterSpecException,
            UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] salt = KeyGenerator.INSTANCE.getSalt();
        KeySpec spec = new PBEKeySpec(password, salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        return new EncryptedBundle(cipher.doFinal(bytesToEncrypt), iv, salt);
    }

    public static byte[] pbeAES_Decrypt(char[] password, EncryptedBundle bundle) throws
            NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException,
            UnsupportedEncodingException, InvalidKeySpecException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password, bundle.salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(bundle.iv));
        return cipher.doFinal(bundle.cipherText);
    }

    public static String encryptRSA(String plainText, PublicKey publicKey) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] cipherText = rsaCipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(cipherText);
    }

    public static String decryptRSA(String encryptedTextBase64, PrivateKey privateKey) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] plainText = rsaCipher.doFinal(Base64.getDecoder().decode(encryptedTextBase64));
        return new String(plainText, "UTF-8");
    }

    public static String encryptAES(String messageToEncrypt, AESParamsDto aesParams) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException, InvalidCipherTextException {
        PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        KeyParameter keyParam = new KeyParameter(aesParams.getAesKey().getEncoded());
        ParametersWithIV params = new ParametersWithIV(keyParam, aesParams.getIvParam().getIV());
        pbbc.init(true, params); //to decrypt put param to false
        byte[] input = messageToEncrypt.getBytes("UTF-8");
        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(input, 0, input.length, output, 0);
        pbbc.doFinal(output, bytesWrittenOut);
        return Base64.getEncoder().encodeToString(output);
    }

    //BC provider to avoid key length restrictions on normal jvm
    public static String decryptAES(String messageToDecrypt, AESParamsDto aesParams) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            UnsupportedEncodingException, InvalidCipherTextException {
        PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        KeyParameter keyParam = new KeyParameter(aesParams.getAesKey().getEncoded());
        CipherParameters params = new ParametersWithIV(keyParam, aesParams.getIvParam().getIV());
        pbbc.init(false, params); //to encrypt put param to true
        byte[] input =  Base64.getDecoder().decode(messageToDecrypt);
        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(input, 0, input.length, output, 0);
        pbbc.doFinal(output, bytesWrittenOut);
        int i = output.length - 1; //remove padding
        while (i >= 0 && output[i] == 0) { --i; }
        return new String(Arrays.copyOf(output, i + 1), "UTF-8");
    }
}
