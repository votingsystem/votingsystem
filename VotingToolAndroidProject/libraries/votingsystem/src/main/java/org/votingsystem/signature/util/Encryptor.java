package org.votingsystem.signature.util;

import android.util.Base64;
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
import org.bouncycastle2.crypto.BlockCipher;
import org.bouncycastle2.crypto.DataLengthException;
import org.bouncycastle2.crypto.InvalidCipherTextException;
import org.bouncycastle2.crypto.engines.AESEngine;
import org.bouncycastle2.crypto.paddings.PKCS7Padding;
import org.bouncycastle2.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle2.crypto.params.KeyParameter;
import org.bouncycastle2.mail.smime.SMIMEEnveloped;
import org.bouncycastle2.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle2.operator.OperatorCreationException;
import org.bouncycastle2.util.Strings;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    private static final int ITERATION_COUNT = 1024;
    private static final int KEY_LENGTH = 128; // 192 and 256 bits may not be available

	private  Encryptor() { }
	
	public static byte[] encryptSMIME(SMIMEMessage msgToEncrypt,
			X509Certificate receiverCert) throws Exception {
		Log.d(TAG + ".encryptSMIMEFile ", " #### encryptSMIMEFile ");
    	/* Create the encryptor */
    	SMIMEEnvelopedGenerator encryptor = new SMIMEEnvelopedGenerator();
    	encryptor.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
    			receiverCert).setProvider(ContextVS.PROVIDER));
        /* Encrypt the message */
        MimeBodyPart encryptedPart = encryptor.generate(msgToEncrypt,
                new JceCMSContentEncryptorBuilder(
                CMSAlgorithm.DES_EDE3_CBC).setProvider(ContextVS.PROVIDER).build());
        /* Set all original MIME headers in the encrypted message */
        Enumeration headers = msgToEncrypt.getAllHeaderLines();
        while (headers.hasMoreElements()) {
            String headerLine = (String)headers.nextElement();
            Log.d(TAG + ".encryptSMIMEFile", " - headerLine: " + headerLine);
            /* 
             * Make sure not to override any content-* headers from the
             * original message
             */
            if (!Strings.toLowerCase(headerLine).startsWith("content-")) {
            	encryptedPart.addHeaderLine(headerLine);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptedPart.writeTo(baos);
        byte[] result = baos.toByteArray();
        baos.close();
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
            for(Header header : headers) {
                if(header != null) mimeMessage.setHeader(header.getName(), header.getValue());
            }
        }
		// set the Date: header
		//mimeMessage.setSentDate(new Date());
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

    public static byte[] encryptToCMS(byte[] dataToEncrypt, PublicKey  receptorPublicKey) throws Exception {
        CMSEnvelopedDataStreamGenerator dataStreamGen = new CMSEnvelopedDataStreamGenerator();
        dataStreamGen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(
                "".getBytes(), receptorPublicKey).setProvider(ContextVS.PROVIDER));
        ByteArrayOutputStream  bOut = new ByteArrayOutputStream();
        OutputStream out = dataStreamGen.open(bOut,
                new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).
                        setProvider(ContextVS.PROVIDER).build());
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
        byte[] cmsEncryptedData = Base64.decode(base64EncryptedData, Base64.DEFAULT);
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

    /**
     * http://stackoverflow.com/questions/992019/java-256-bit-aes-password-based-encryption?rq=1
     * Share the password (a char[]) and salt (a byte[]—8 bytes selected by a SecureRandom makes a
     * good salt—which doesn't need to be kept secret) with the recipient
     */
    public static Map encrypt(String textToEncrypt, char[] password, byte[] salt) throws
            NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, UnsupportedEncodingException, InvalidParameterSpecException {
        //Security.addProvider(new BouncyCastleProvider());
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        //byte[] salt = KeyGeneratorVS.INSTANCE.getEncryptionSalt();
        KeySpec spec = new PBEKeySpec(password, salt, ContextVS.SYMETRIC_ENCRYPTION_ITERATION_COUNT,
                ContextVS.SYMETRIC_ENCRYPTION_KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        /* Encrypt the message. */
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        //send the encryptedText and the iv to the recipient.
        Map responseMap = new HashMap();
        byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
        byte[] encryptedText = cipher.doFinal(textToEncrypt.getBytes(ContextVS.UTF_8));
        responseMap.put("iv", iv);
        responseMap.put("encryptedText", encryptedText);
        responseMap.put("salt", salt);
        return responseMap;
    }

    public static EncryptedBundle pbeAES_Encrypt(String password, byte[] bytesToEncrypt)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidParameterSpecException,
            UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] salt = KeyGeneratorVS.INSTANCE.getSalt();
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
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
        KeySpec spec = new PBEKeySpec(password.toCharArray(), bundle.getSalt(), ITERATION_COUNT, KEY_LENGTH);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(bundle.getIV()));
        return cipher.doFinal(bundle.getCipherText());
    }

    public static JSONObject getEncryptedJSONDataBundle(String textToEncrypt, char[] password,
            byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, UnsupportedEncodingException, InvalidParameterSpecException,
            JSONException {
        //byte[] salt = KeyGeneratorVS.INSTANCE.getEncryptionSalt();
        Map encryptedDataMap = encrypt(textToEncrypt, password, salt);
        byte[] iv = (byte[]) encryptedDataMap.get("iv");
        String ivBase64 = Base64.encodeToString(iv, Base64.DEFAULT);
        byte[] encryptedText = (byte[]) encryptedDataMap.get("encryptedText");
        String encryptedTextBase64 = Base64.encodeToString(encryptedText, Base64.DEFAULT);
        String saltBase64 = Base64.encodeToString(salt, Base64.DEFAULT);
        JSONObject resultJSON = new JSONObject();
        resultJSON.put("iv", ivBase64);
        resultJSON.put("encryptedText", encryptedTextBase64);
        resultJSON.put("salt", saltBase64);
        return resultJSON;
    }

    public static String decryptDataBundle(JSONObject jsonData, char[] password) throws
            DataLengthException, IllegalStateException, InvalidCipherTextException,
            InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException,
            UnsupportedEncodingException, InvalidAlgorithmParameterException, JSONException {
        byte[] encryptedText = Base64.decode(jsonData.getString("encryptedText"), Base64.DEFAULT);
        byte[] salt = Base64.decode(jsonData.getString("salt"), Base64.DEFAULT);
        byte[] iv = Base64.decode(jsonData.getString("iv"), Base64.DEFAULT);
        return decrypt(encryptedText, password, salt, iv);
    }

    public static String decrypt(byte[] encryptedTextBytes, char[] password, byte[] salt,
            byte[] iv) throws DataLengthException, IllegalStateException, InvalidCipherTextException,
            InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException,
            UnsupportedEncodingException, InvalidAlgorithmParameterException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(password, salt, ContextVS.SYMETRIC_ENCRYPTION_ITERATION_COUNT,
                ContextVS.SYMETRIC_ENCRYPTION_KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        /* Decrypt the message, given derived key and initialization vector. */
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
        String plaintext = new String(cipher.doFinal(encryptedTextBytes), ContextVS.UTF_8);
        return plaintext;
    }

    public static String encryptAES(String messageToEncrypt, AESParams params) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, params.getKey(), params.getIV());
        byte[] encryptedMessage = cipher.doFinal(messageToEncrypt.getBytes("UTF-8"));
        return new String(org.bouncycastle2.util.encoders.Base64.encode(encryptedMessage));
    }

    public static String decryptAES(String messageToDecrypt, AESParams params) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, params.getKey(), params.getIV());
        byte[] encryptedMessageBytes = org.bouncycastle2.util.encoders.Base64.decode(
                messageToDecrypt.getBytes("UTF-8"));
        byte[] decryptedBytes = cipher.doFinal(encryptedMessageBytes);
        return new String(decryptedBytes, "UTF8");
    }

    //decrypts base64 encoded AES message
    public static String decryptAESPKCS7(String messageToDecrypt, AESParams params) throws
            NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException, BadPaddingException, IllegalBlockSizeException,
            UnsupportedEncodingException, InvalidCipherTextException {
        BlockCipher AESCipher = new AESEngine();
        PaddedBufferedBlockCipher pbbc = new PaddedBufferedBlockCipher(AESCipher, new PKCS7Padding());
        KeyParameter key = new KeyParameter(params.getKey().getEncoded());
        pbbc.init(false, key); //to encrypt put param to true
        byte[] input = org.bouncycastle2.util.encoders.Base64.decode(
                messageToDecrypt.getBytes("UTF-8"));
        byte[] output = new byte[pbbc.getOutputSize(input.length)];
        int bytesWrittenOut = pbbc.processBytes(input, 0, input.length, output, 0);
        pbbc.doFinal(output, bytesWrittenOut);
        return new String(output, "UTF-8");
    }
}
