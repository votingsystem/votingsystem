package org.votingsystem.signature.util;

import android.util.Log;
import org.bouncycastle2.cms.CMSAlgorithm;
import org.bouncycastle2.cms.CMSEnvelopedDataParser;
import org.bouncycastle2.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle2.cms.CMSException;
import org.bouncycastle2.cms.CMSTypedStream;
import org.bouncycastle2.cms.KeyTransRecipientId;
import org.bouncycastle2.cms.Recipient;
import org.bouncycastle2.cms.RecipientId;
import org.bouncycastle2.cms.RecipientInformation;
import org.bouncycastle2.cms.RecipientInformationStore;
import org.bouncycastle2.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle2.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle2.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle2.crypto.CipherParameters;
import org.bouncycastle2.crypto.InvalidCipherTextException;
import org.bouncycastle2.crypto.engines.AESEngine;
import org.bouncycastle2.crypto.modes.CBCBlockCipher;
import org.bouncycastle2.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle2.crypto.params.KeyParameter;
import org.bouncycastle2.crypto.params.ParametersWithIV;
import org.bouncycastle2.mail.smime.SMIMEEnveloped;
import org.bouncycastle2.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle2.operator.OperatorCreationException;
import org.bouncycastle2.util.Strings;
import org.bouncycastle2.util.encoders.Base64;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.EncryptedBundleVS;
import org.votingsystem.signature.smime.EncryptedBundle;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ResponseVS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.Header;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class Encryptor {
	
	public static final String TAG = Encryptor.class.getSimpleName();

	private  Encryptor() { }
	
	public static byte[] encryptSMIME(MimeMessage msgToEncrypt,
			X509Certificate receiverCert) throws Exception {
		Log.d(TAG + ".encryptSMIMEFile ", "receiver: " + receiverCert.getSubjectDN());
    	SMIMEEnvelopedGenerator encryptor = new SMIMEEnvelopedGenerator();
    	encryptor.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
    			receiverCert).setProvider(ContextVS.PROVIDER));
        MimeBodyPart encryptedPart = encryptor.generate(msgToEncrypt,
                new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
        /* Set all original MIME headers in the encrypted message */
        Enumeration headers = msgToEncrypt.getAllHeaderLines();
        while (headers.hasMoreElements()) {
            String headerLine = (String)headers.nextElement();
            Log.d(TAG + ".encryptSMIMEFile", "headerLine: " + headerLine);
            //Make sure not to override any content-* headers from the original message
            if (!Strings.toLowerCase(headerLine).startsWith("content-")) {
            	encryptedPart.addHeaderLine(headerLine);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        baos.close();
		return baos.toByteArray();
	}

    public static EncryptedBundleVS decryptEncryptedBundle(EncryptedBundleVS encryptedBundleVS,
               PublicKey publicKey, PrivateKey receiverPrivateKey) throws Exception {
        byte[] messageBytes = null;
        switch(encryptedBundleVS.getType()) {
            case FILE:
                messageBytes = decryptFile(encryptedBundleVS.getEncryptedMessageBytes(),
                        publicKey, receiverPrivateKey);
                encryptedBundleVS.setStatusCode(ResponseVS.SC_OK);
                encryptedBundleVS.setDecryptedMessageBytes(messageBytes);
                break;
            case SMIME_MESSAGE:
                SMIMEMessage smimeMessage = decryptSMIME(encryptedBundleVS.getEncryptedMessageBytes(),
                        receiverPrivateKey);
                encryptedBundleVS.setStatusCode(ResponseVS.SC_OK);
                encryptedBundleVS.setDecryptedSMIMEMessage(smimeMessage);
                break;
            case TEXT:
                messageBytes = decryptFile(encryptedBundleVS.getEncryptedMessageBytes(),
                        publicKey, receiverPrivateKey);
                encryptedBundleVS.setStatusCode(ResponseVS.SC_OK);
                encryptedBundleVS.setDecryptedMessageBytes(messageBytes);
                break;
        }
        return encryptedBundleVS;
    }

    public static List<EncryptedBundleVS> decryptEncryptedBundleList(
            List<EncryptedBundleVS> encryptedBundleVSList, PublicKey publicKey,
            PrivateKey receiverPrivateKey) throws Exception {
        List<EncryptedBundleVS> result = new ArrayList<EncryptedBundleVS>();
        for(EncryptedBundleVS encryptedBundleVS : encryptedBundleVSList) {
            result.add(decryptEncryptedBundle(encryptedBundleVS, publicKey, receiverPrivateKey));
        }
        return result;
    }
	
    public static byte[] encryptMessage(byte[] text, 
            X509Certificate receiverCert, Header... headers) throws Exception {
    	Log.d(TAG + ".encryptMessage ", "encryptMessage");
		Properties props = System.getProperties();
		Session session = Session.getDefaultInstance(props, null);
		MimeMessage mimeMessage = new MimeMessage(session);
		mimeMessage.setText(new String(text));
        if (headers != null) {
            for (Header header : headers) {
                if (header != null) mimeMessage.setHeader(header.getName(), header.getValue());
            }
        }
		//mimeMessage.setSentDate(new Date());// set the Date: header
		SMIMEEnvelopedGenerator encryptor = new SMIMEEnvelopedGenerator();
		encryptor.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
		        receiverCert).setProvider(ContextVS.PROVIDER));
		/* Encrypt the message */
		MimeBodyPart encryptedPart = encryptor.generate(mimeMessage,
		        new JceCMSContentEncryptorBuilder(
		        CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
	}

    public static byte[] encryptToCMS(byte[] dataToEncrypt, X509Certificate receiverCert)
            throws CertificateEncodingException, OperatorCreationException, CMSException, IOException {
        CMSEnvelopedDataStreamGenerator dataStreamGen = new CMSEnvelopedDataStreamGenerator();
        dataStreamGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(receiverCert).
                setProvider(ContextVS.PROVIDER));
        ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
        OutputStream out = dataStreamGen.open(bOut, new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
        out.write(dataToEncrypt);
        out.close();
        return bOut.toByteArray();
    }

    /**
    * Method to decrypt SMIME signed messages
    */
   public static SMIMEMessage decryptSMIME(byte[] encryptedMessageBytes,
            PrivateKey receiverPrivateKey) throws Exception {
	   Log.d(TAG + ".decryptSMIME ", "decryptSMIME ");
	   InputStream inputStream = new ByteArrayInputStream(decryptMessage(
                encryptedMessageBytes, receiverPrivateKey));
       return new SMIMEMessage(inputStream);
   }

   /**
    * helper method to decrypt SMIME signed messages
    */
   public static byte[] decryptMessage(byte[] encryptedMessageBytes,
           PrivateKey receiverPrivateKey) throws Exception {
	   Log.d(TAG + ".decryptMessage ", "decryptMessage ");
       /*RecipientId recId = null;
       if(receiverCert != null)
           recId = new JceKeyTransRecipientId(receiverCert);*/
       Recipient recipient = new JceKeyTransEnvelopedRecipient(
               receiverPrivateKey).setProvider(ContextVS.PROVIDER);
       MimeMessage msg = new MimeMessage(null, new ByteArrayInputStream(encryptedMessageBytes));
       SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
       RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
       RecipientInformation        recipientInfo = null;
       //if(recId != null) recipientInfo = recipients.get(recId);
       if(recipientInfo == null && recipients.getRecipients().size() == 1) {
           recipientInfo = (RecipientInformation) recipients.getRecipients().iterator().next();
       }
       byte[] messageBytes = recipientInfo.getContent(recipient);
       return messageBytes;
   }
	
    public static byte[] decryptFile (byte[] encryptedFile, PublicKey publicKey,
            PrivateKey receiverPrivateKey) throws Exception {
		Log.d(TAG + ".decryptFile ", " #### decryptFile ");
        RecipientId recId = new KeyTransRecipientId(publicKey.getEncoded());
        Recipient recipient = new JceKeyTransEnvelopedRecipient(
                receiverPrivateKey).setProvider(ContextVS.PROVIDER);
        MimeMessage msg = new MimeMessage(null, new ByteArrayInputStream(encryptedFile));
        SMIMEEnveloped smimeEnveloped = new SMIMEEnveloped(msg);
        RecipientInformationStore   recipients = smimeEnveloped.getRecipientInfos();
        RecipientInformation        recipientInfo = null;
        recipientInfo = recipients.get(recId);
        if(recipientInfo == null && recipients.getRecipients().size() == 1) {
            recipientInfo = (RecipientInformation) 
                recipients.getRecipients().iterator().next();
        }
        RecipientId recipientId = null;
        if(recipientInfo.getRID() != null) {
            recipientId = recipientInfo.getRID();
        }
        byte[] result = recipientInfo.getContent(recipient);
        return result;
    }

    public static byte[] decryptCMS(PrivateKey privateKey, byte[] base64EncryptedData)
            throws Exception {
        byte[] cmsEncryptedData = Base64.decode(base64EncryptedData);
        CMSEnvelopedDataParser ep = new CMSEnvelopedDataParser(cmsEncryptedData);
        RecipientInformationStore  recipients = ep.getRecipientInfos();
        Collection c = recipients.getRecipients();
        Iterator it = c.iterator();
        byte[] result = null;
        if (it.hasNext()) {
            RecipientInformation   recipient = (RecipientInformation)it.next();
            //assertEquals(recipient.getKeyEncryptionAlgOID(), PKCSObjectIdentifiers.rsaEncryption.getId());
            CMSTypedStream recData = recipient.getContentStream(
                    new JceKeyTransEnvelopedRecipient(privateKey).setProvider(ContextVS.ANDROID_PROVIDER));
            InputStream           dataStream = recData.getContentStream();
            ByteArrayOutputStream dataOut = new ByteArrayOutputStream();
            byte[]                buf = new byte[4096];
            int len = 0;
            while ((len = dataStream.read(buf)) >= 0) {
                dataOut.write(buf, 0, len);
            }
            dataOut.close();
            result = dataOut.toByteArray();
            //assertEquals(true, Arrays.equals(data, dataOut.toByteArray()));
        }
        return result;
    }

    public static EncryptedBundle pbeAES_Encrypt(String password, byte[] bytesToEncrypt)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidParameterSpecException,
            UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] salt = KeyGeneratorVS.INSTANCE.getSalt();
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ContextVS.
                SYMETRIC_ENCRYPTION_ITERATION_COUNT, ContextVS.SYMETRIC_ENCRYPTION_KEY_LENGTH);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        return new EncryptedBundle(cipher.doFinal(bytesToEncrypt), iv, salt);
    }

    public static byte[] pbeAES_Decrypt(String password, EncryptedBundle bundle) throws
            NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException,
            IllegalBlockSizeException, UnsupportedEncodingException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), bundle.getSalt(), ContextVS.
                SYMETRIC_ENCRYPTION_ITERATION_COUNT, ContextVS.SYMETRIC_ENCRYPTION_KEY_LENGTH);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(bundle.getIV()));
        return cipher.doFinal(bundle.getCipherText());
    }

    /*public static String encryptAES(String messageToEncrypt, AESParams params) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, params.getKey(), params.getIV());
        byte[] encryptedMessage = cipher.doFinal(messageToEncrypt.getBytes("UTF-8"));
        return new String(org.bouncycastle2.util.encoders.Base64.encode(encryptedMessage));
    }*/

    /*public static String decryptAES(String messageToDecrypt, AESParams params) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, params.getKey(), params.getIV());
        byte[] encryptedMessageBytes = org.bouncycastle2.util.encoders.Base64.decode(
                messageToDecrypt.getBytes("UTF-8"));
        byte[] decryptedBytes = cipher.doFinal(encryptedMessageBytes);
        return new String(decryptedBytes, "UTF8");
    }*/

    //BC provider to avoid key length restrictions on normal jvm
    public static String encryptAES(String messageToEncrypt, AESParams aesParams) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            UnsupportedEncodingException, InvalidCipherTextException {
        PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        KeyParameter keyParam = new KeyParameter(aesParams.getKey().getEncoded());
        ParametersWithIV params = new ParametersWithIV(keyParam, aesParams.getIV().getIV());
        pbbc.init(true, params); //to decrypt put param to false
        byte[] input = messageToEncrypt.getBytes("UTF-8");
        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(input, 0, input.length, output, 0);
        pbbc.doFinal(output, bytesWrittenOut);
        return new String(Base64.encode(output));
    }

    //BC provider to avoid key length restrictions on normal jvm
    public static String decryptAES(String messageToDecrypt, AESParams aesParams) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            UnsupportedEncodingException, InvalidCipherTextException {
        PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
        KeyParameter keyParam = new KeyParameter(aesParams.getKey().getEncoded());
        CipherParameters params = new ParametersWithIV(keyParam, aesParams.getIV().getIV());
        pbbc.init(false, params); //to encrypt put param to true
        byte[] input = Base64.decode(messageToDecrypt.getBytes("UTF-8"));
        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(input, 0, input.length, output, 0);
        pbbc.doFinal(output, bytesWrittenOut);
        int i = output.length - 1; //remove padding
        while (i >= 0 && output[i] == 0) { --i; }
        return new String(Arrays.copyOf(output, i + 1), "UTF-8");
    }
}
