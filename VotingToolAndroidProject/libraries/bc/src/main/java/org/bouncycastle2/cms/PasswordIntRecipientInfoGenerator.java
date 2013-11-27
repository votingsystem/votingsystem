package org.bouncycastle2.cms;

import org.bouncycastle2.asn1.*;
import org.bouncycastle2.asn1.cms.PasswordRecipientInfo;
import org.bouncycastle2.asn1.cms.RecipientInfo;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.asn1.x509.AlgorithmIdentifier;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;

class PasswordIntRecipientInfoGenerator
    implements IntRecipientInfoGenerator
{
    private AlgorithmIdentifier keyDerivationAlgorithm;
    private SecretKey keyEncryptionKey;

    PasswordIntRecipientInfoGenerator()
    {
    }

    void setKeyDerivationAlgorithm(AlgorithmIdentifier keyDerivationAlgorithm)
    {
        this.keyDerivationAlgorithm = keyDerivationAlgorithm;
    }

    void setKeyEncryptionKey(SecretKey keyEncryptionKey)
    {
        this.keyEncryptionKey = keyEncryptionKey;
    }

    public RecipientInfo generate(SecretKey contentEncryptionKey, SecureRandom random,
            Provider prov) throws GeneralSecurityException
    {
        // TODO Consider passing in the wrapAlgorithmOID instead

        CMSEnvelopedHelper helper = CMSEnvelopedHelper.INSTANCE;
        String wrapAlgName = helper.getRFC3211WrapperName(keyEncryptionKey.getAlgorithm());
        Cipher keyCipher = helper.createSymmetricCipher(wrapAlgName, prov);        
        keyCipher.init(Cipher.WRAP_MODE, keyEncryptionKey, random);
        byte[] encryptedKeyBytes = keyCipher.wrap(contentEncryptionKey);

        ASN1OctetString encryptedKey = new DEROctetString(encryptedKeyBytes);


        ASN1EncodableVector v = new ASN1EncodableVector();
        v.add(new DERObjectIdentifier(keyEncryptionKey.getAlgorithm()));
        v.add(new DEROctetString(keyCipher.getIV()));
        AlgorithmIdentifier keyEncryptionAlgorithm = new AlgorithmIdentifier(
            PKCSObjectIdentifiers.id_alg_PWRI_KEK, new DERSequence(v));


        return new RecipientInfo(new PasswordRecipientInfo(keyDerivationAlgorithm,
            keyEncryptionAlgorithm, encryptedKey));
    }

}
