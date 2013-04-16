package org.sistemavotacion.smime;
 
import java.io.OutputStream;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.jcajce.DefaultJcaJceHelper;
import org.bouncycastle.operator.GenericKey;
import org.bouncycastle.operator.OutputEncryptor;

public class CMSOutputEncryptor   implements OutputEncryptor {
	
	
    private EnvelopedDataHelper helper = new EnvelopedDataHelper(new DefaultJcaJceHelper());
	
    private SecretKey encKey;
    private AlgorithmIdentifier algorithmIdentifier;
    private Cipher              cipher;

    CMSOutputEncryptor(ASN1ObjectIdentifier encryptionOID, int keySize, SecureRandom random)
        throws CMSException
    {
        KeyGenerator keyGen = helper.createKeyGenerator(encryptionOID);

        if (random == null)
        {
            random = new SecureRandom();
        }

        if (keySize < 0)
        {
            keyGen.init(random);
        }
        else
        {
            keyGen.init(keySize, random);
        }

        cipher = helper.createCipher(encryptionOID);
        encKey = keyGen.generateKey();
        AlgorithmParameters params = helper.generateParameters(encryptionOID, encKey, random);

        try
        {
            cipher.init(Cipher.ENCRYPT_MODE, encKey, params, random);
        }
        catch (GeneralSecurityException e)
        {
            throw new CMSException("unable to initialize cipher: " + e.getMessage(), e);
        }

        //
        // If params are null we try and second guess on them as some providers don't provide
        // algorithm parameter generation explicity but instead generate them under the hood.
        //
        if (params == null)
        {
            params = cipher.getParameters();
        }

        algorithmIdentifier = helper.getAlgorithmIdentifier(encryptionOID, params);
    }

    public AlgorithmIdentifier getAlgorithmIdentifier()
    {
        return algorithmIdentifier;
    }

    public OutputStream getOutputStream(OutputStream dOut)
    {
        return new CipherOutputStream(dOut, cipher);
    }

    public GenericKey getKey()
    {
        return new GenericKey(encKey);
    }

}

