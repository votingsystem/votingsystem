package org.sistemavotacion.smime;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CRLException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Null;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BEROctetStringGenerator;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERTags;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509AttributeCertificateHolder;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;
import org.bouncycastle.util.io.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CMSUtils {
    
    private static Logger logger = LoggerFactory.getLogger(CMSUtils.class);
    
    private static final Runtime RUNTIME = Runtime.getRuntime();
    
    public static final String  DIGEST_SHA1 = OIWObjectIdentifiers.idSHA1.getId();
    public static final String  DIGEST_SHA224 = NISTObjectIdentifiers.id_sha224.getId();
    public static final String  DIGEST_SHA256 = NISTObjectIdentifiers.id_sha256.getId();
    public static final String  DIGEST_SHA384 = NISTObjectIdentifiers.id_sha384.getId();
    public static final String  DIGEST_SHA512 = NISTObjectIdentifiers.id_sha512.getId();
    public static final String  DIGEST_MD5 = PKCSObjectIdentifiers.md5.getId();
    public static final String  DIGEST_GOST3411 = CryptoProObjectIdentifiers.gostR3411.getId();
    public static final String  DIGEST_RIPEMD128 = TeleTrusTObjectIdentifiers.ripemd128.getId();
    public static final String  DIGEST_RIPEMD160 = TeleTrusTObjectIdentifiers.ripemd160.getId();
    public static final String  DIGEST_RIPEMD256 = TeleTrusTObjectIdentifiers.ripemd256.getId();

    public static final String  ENCRYPTION_RSA = PKCSObjectIdentifiers.rsaEncryption.getId();
    public static final String  ENCRYPTION_DSA = X9ObjectIdentifiers.id_dsa_with_sha1.getId();
    public static final String  ENCRYPTION_ECDSA = X9ObjectIdentifiers.ecdsa_with_SHA1.getId();
    public static final String  ENCRYPTION_RSA_PSS = PKCSObjectIdentifiers.id_RSASSA_PSS.getId();
    public static final String  ENCRYPTION_GOST3410 = CryptoProObjectIdentifiers.gostR3410_94.getId();
    public static final String  ENCRYPTION_ECGOST3410 = CryptoProObjectIdentifiers.gostR3410_2001.getId();

    private static final String  ENCRYPTION_ECDSA_WITH_SHA1 = X9ObjectIdentifiers.ecdsa_with_SHA1.getId();
    private static final String  ENCRYPTION_ECDSA_WITH_SHA224 = X9ObjectIdentifiers.ecdsa_with_SHA224.getId();
    private static final String  ENCRYPTION_ECDSA_WITH_SHA256 = X9ObjectIdentifiers.ecdsa_with_SHA256.getId();
    private static final String  ENCRYPTION_ECDSA_WITH_SHA384 = X9ObjectIdentifiers.ecdsa_with_SHA384.getId();
    private static final String  ENCRYPTION_ECDSA_WITH_SHA512 = X9ObjectIdentifiers.ecdsa_with_SHA512.getId();
    
    static int getMaximumMemory()
    {
        long maxMem = RUNTIME.maxMemory();
        
        if (maxMem > Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }
        
        return (int)maxMem;
    }
    
    static ContentInfo readContentInfo(
        byte[] input)
        throws CMSException
    {
        // enforce limit checking as from a byte array
        return readContentInfo(new ASN1InputStream(input));
    }

    static ContentInfo readContentInfo(
        InputStream input)
        throws CMSException
    {
        // enforce some limit checking
        return readContentInfo(new ASN1InputStream(input, getMaximumMemory()));
    } 

    public static Certificate getCertificate (byte[] certBytes) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(certBytes)) ;
        return cert;
    }
    
    public static SignerIdentifier getSignerIdentifier(X509Certificate cert) {
        TBSCertificateStructure tbs;
        tbs = CMSUtils.getTBSCertificateStructure(cert);

        //Aqui podría ser ...pkcs.IssuerAndSerialNumber
        IssuerAndSerialNumber encSid = new IssuerAndSerialNumber(tbs
                .getIssuer(), tbs.getSerialNumber().getValue());
        return new SignerIdentifier(encSid);
    }
    
    public static ASN1Set createBerSetFromList(List derObjects) {
        ASN1EncodableVector v = new ASN1EncodableVector();
        for (Iterator it = derObjects.iterator(); it.hasNext();) {
            v.add((DEREncodable)it.next());
        }
        return new BERSet(v);
    }
    
    static List getCertificatesFromStore(CertStore certStore)
        throws CertStoreException, CMSException
    {
        List certs = new ArrayList();

        try
        {
            for (Iterator it = certStore.getCertificates(null).iterator(); it.hasNext();)
            {
                X509Certificate c = (X509Certificate)it.next();

                certs.add(X509CertificateStructure.getInstance(
                                                       ASN1Object.fromByteArray(c.getEncoded())));
            }

            return certs;
        }
        catch (IllegalArgumentException e)
        {
            throw new CMSException("error processing certs", e);
        }
        catch (IOException e)
        {
            throw new CMSException("error processing certs", e);
        }
        catch (CertificateEncodingException e)
        {
            throw new CMSException("error encoding certs", e);
        }
    }

    static List getCertificatesFromStore(Store certStore)
        throws CMSException
    {
        List certs = new ArrayList();

        try
        {
            for (Iterator it = certStore.getMatches(null).iterator(); it.hasNext();)
            {
                X509CertificateHolder c = (X509CertificateHolder)it.next();

                certs.add(c.toASN1Structure());
            }

            return certs;
        }
        catch (ClassCastException e)
        {
            throw new CMSException("error processing certs", e);
        }
    }

    static List getAttributeCertificatesFromStore(Store attrStore)
        throws CMSException
    {
        List certs = new ArrayList();

        try
        {
            for (Iterator it = attrStore.getMatches(null).iterator(); it.hasNext();)
            {
                X509AttributeCertificateHolder attrCert = (X509AttributeCertificateHolder)it.next();

                certs.add(new DERTaggedObject(false, 2, attrCert.toASN1Structure()));
            }

            return certs;
        }
        catch (ClassCastException e)
        {
            throw new CMSException("error processing certs", e);
        }
    }

    static List getCRLsFromStore(CertStore certStore)
        throws CertStoreException, CMSException
    {
        List crls = new ArrayList();

        try
        {
            for (Iterator it = certStore.getCRLs(null).iterator(); it.hasNext();)
            {
                X509CRL c = (X509CRL)it.next();

                crls.add(CertificateList.getInstance(ASN1Object.fromByteArray(c.getEncoded())));
            }

            return crls;
        }
        catch (IllegalArgumentException e)
        {
            throw new CMSException("error processing crls", e);
        }
        catch (IOException e)
        {
            throw new CMSException("error processing crls", e);
        }
        catch (CRLException e)
        {
            throw new CMSException("error encoding crls", e);
        }
    }

    static List getCRLsFromStore(Store crlStore)
        throws CMSException
    {
        List certs = new ArrayList();

        try
        {
            for (Iterator it = crlStore.getMatches(null).iterator(); it.hasNext();)
            {
                X509CRLHolder c = (X509CRLHolder)it.next();

                certs.add(c.toASN1Structure());
            }

            return certs;
        }
        catch (ClassCastException e)
        {
            throw new CMSException("error processing certs", e);
        }
    }
    
    
    static ASN1Set createDerSetFromList(List derObjects)
    {
        ASN1EncodableVector v = new ASN1EncodableVector();

        for (Iterator it = derObjects.iterator(); it.hasNext();)
        {
            v.add((DEREncodable)it.next());
        }

        return new DERSet(v);
    }

    static OutputStream createBEROctetOutputStream(OutputStream s,
            int tagNo, boolean isExplicit, int bufferSize) throws IOException
    {
        BEROctetStringGenerator octGen = new BEROctetStringGenerator(s, tagNo, isExplicit);

        if (bufferSize != 0)
        {
            return octGen.getOctetOutputStream(new byte[bufferSize]);
        }

        return octGen.getOctetOutputStream();
    }

    static TBSCertificateStructure getTBSCertificateStructure(
        X509Certificate cert)
    {
        try
        {
            return TBSCertificateStructure.getInstance(
                ASN1Object.fromByteArray(cert.getTBSCertificate()));
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(
                "can't extract TBS structure from this cert");
        }
    }

    static IssuerAndSerialNumber getIssuerAndSerialNumber(X509Certificate cert)
    {
        TBSCertificateStructure tbsCert = getTBSCertificateStructure(cert);
        return new IssuerAndSerialNumber(tbsCert.getIssuer(), tbsCert.getSerialNumber().getValue());
    }

    private static ContentInfo readContentInfo(
        ASN1InputStream in)
        throws CMSException
    {
        try
        {
            return ContentInfo.getInstance(in.readObject());
        }
        catch (IOException e)
        {
            throw new CMSException("IOException reading content.", e);
        }
        catch (ClassCastException e)
        {
            throw new CMSException("Malformed content.", e);
        }
        catch (IllegalArgumentException e)
        {
            throw new CMSException("Malformed content.", e);
        }
    }
    
    public static byte[] streamToByteArray(
        InputStream in) 
        throws IOException
    {
        return Streams.readAll(in);
    }

    public static byte[] streamToByteArray(
        InputStream in,
        int         limit)
        throws IOException
    {
        return Streams.readAllLimited(in, limit);
    }

    public static Provider getProvider(String providerName)
        throws NoSuchProviderException
    {
        if (providerName != null)
        {
            Provider prov = Security.getProvider(providerName);

            if (prov != null)
            {
                return prov;
            }

            throw new NoSuchProviderException("provider " + providerName + " not found.");
        }

        return null; 
    }

    
    public static boolean verifyDigest(byte[] digest, 
            AlgorithmIdentifier encryptionAlgorithm, 
            AlgorithmIdentifier  digestAlgorithm, PublicKey key, byte[] signature, 
            String sigProvider)  throws NoSuchAlgorithmException, 
            NoSuchProviderException, CMSException, NoSuchPaddingException, InvalidKeyException, 
            IllegalBlockSizeException, BadPaddingException {
        //SignerInfo info
        //AlgorithmIdentifier encryptionAlgorithm = info.getDigestEncryptionAlgorithm();
        //AlgorithmIdentifier  digestAlgorithm = info.getDigestAlgorithm();
        try {
            
            String encryptionAlgorithmId = getEncryptiontId(encryptionAlgorithm.getAlgorithm().getId());
            
            

            logger.debug("encryptionAlgorithmId: " + encryptionAlgorithmId);
            if (encryptionAlgorithmId.equals("RSA")) {
                Cipher c;
                if (sigProvider != null) {
                    c = Cipher.getInstance("RSA/ECB/PKCS1Padding", sigProvider);
                }
                else {
                    c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                }
                c.init(Cipher.DECRYPT_MODE, key);
                DigestInfo digInfo = derDecode(c.doFinal(signature));

                logger.debug("digInfo - AlgorithmId: " + digInfo.getAlgorithmId().getAlgorithm().toString());                
                logger.debug("veriefied - AlgorithmId: " + digestAlgorithm.getObjectId());
                logger.debug("- encryptionAlgorithm: " + encryptionAlgorithm.getAlgorithm());

                
                if (!digInfo.getAlgorithmId().getObjectId().equals(
                        digestAlgorithm.getObjectId())) {
                    return false;
                }
                if (!isNull(digInfo.getAlgorithmId().getParameters())) {
                    return false;
                }

                byte[]  sigHash = digInfo.getDigest();
                String sigHashStr = new String(Base64.encode(sigHash));
                logger.debug("digInfo - hash: " + sigHashStr);
                
                String signerHashStr = new String(Base64.encode(digest));
                logger.debug("signer - hash: " + signerHashStr);
                
                boolean isHashOK = MessageDigest.isEqual(digest, sigHash);
                return isHashOK;
            } else if (encryptionAlgorithmId.equals("DSA")) {
                Signature sig;
                if (sigProvider != null) {
                    sig = Signature.getInstance("NONEwithDSA", sigProvider);
                } else {
                    sig = Signature.getInstance("NONEwithDSA", sigProvider);
                }
                sig.initVerify(key);
                sig.update(digest);
                return sig.verify(signature);
            }
            else {
                throw new CMSException("algorithm: " + encryptionAlgorithm + " not supported in base signatures.");
            }
        } catch (NoSuchAlgorithmException e) {
            throw e;
        } catch (NoSuchProviderException e) {
            throw e;
        } catch (GeneralSecurityException e) {
            throw new CMSException("Exception processing signature: " + e, e);
        } catch (IOException e)  {
            throw new CMSException("Exception decoding signature: " + e, e);
        }
    }
    
    
    private static Signature createRawSig(AlgorithmIdentifier algorithm, PublicKey publicKey) {
        logger.debug("**** createRawSig - algorithm: " + algorithm.getAlgorithm().getId());
        Signature rawSig = null;
        try {
            rawSig = createRawSignature(algorithm);
            rawSig.initVerify(publicKey);
        } catch (Exception ex) {
            logger.debug(ex.getMessage(), ex);
            rawSig = null;
        }
        return rawSig;
    }
    
    public static Signature createRawSignature(AlgorithmIdentifier algorithm) {
        Signature sig = null;
        try  {
            String algName = getEncryptiontId(algorithm.getAlgorithm().getId());
            if(algName != null) {
                if (algName.toUpperCase().contains("WITH")) { 
                    algName = "NONE" + algName.substring(algName.indexOf("WITH"));
                } else algName = "NONEWITH" + algName;
            }
            logger.debug("**** createRawSignature: " + algName);
            sig = Signature.getInstance(algName);
        } catch (Exception ex) {
            logger.debug(ex.getMessage(), ex);
            return null;
        }
        return sig;
    }
        
    public static Cipher createAsymmetricCipher(String encryptionOid, Provider provider)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
        String asymName = getAsymmetricEncryptionAlgName(encryptionOid);
        if (!asymName.equals(encryptionOid)) {
            try {
                // this is reversed as the Sun policy files now allow unlimited strength RSA
                return getCipherInstance(asymName, provider);
            }
            catch (NoSuchAlgorithmException e)
            {
                // Ignore
            }
        }
        return getCipherInstance(encryptionOid, provider);
    }
        
    public static String getAsymmetricEncryptionAlgName(String encryptionAlgOID) {
        if (PKCSObjectIdentifiers.rsaEncryption.getId().equals(encryptionAlgOID)) {
            return "RSA/ECB/PKCS1Padding";
        }
        return encryptionAlgOID;    
    }
    
    public static Cipher getCipherInstance(String algName, Provider provider)
        throws NoSuchAlgorithmException, NoSuchPaddingException {
        if (provider != null){
            return Cipher.getInstance(algName, provider);
        }
        else {
            return Cipher.getInstance(algName);
        }
    }

    
    public static boolean verifyDigest(SignerInformation signer, X509Certificate cert, 
            String provider)  throws NoSuchAlgorithmException, 
            NoSuchProviderException, CMSException, NoSuchPaddingException, InvalidKeyException, 
            IllegalBlockSizeException, BadPaddingException {
        AttributeTable  attributes = signer.getSignedAttributes();
        byte[] hash = null;
       
        
        byte[] signature = signer.getSignature();
        if (attributes != null) {
            Attribute messageDigestAttribute = attributes.get(CMSAttributes.messageDigest);
            hash = ((ASN1OctetString)messageDigestAttribute.getAttrValues().getObjectAt(0)).getOctets();
        }
        logger.debug("DigestAlg: " + getDigestId(signer.getDigestAlgOID()) + 
                " - EncryptionAlg: " + getEncryptiontId(signer.getEncryptionAlgOID()));
        AlgorithmIdentifier digestAlgorithmIdentifier = 
                getDigestAlgorithmIdentifier(signer.getDigestAlgOID());
        AlgorithmIdentifier encriptionAlgorithmIdentifier = 
                getEncryptionAlgorithmIdentifier(signer.getEncryptionAlgOID());
        logger.debug("digestAlgorithmIdentifier: " + digestAlgorithmIdentifier + 
                " - encriptionAlgorithmIdentifier: " + encriptionAlgorithmIdentifier);
        PublicKey key = cert.getPublicKey();
        return verifyDigest(hash, encriptionAlgorithmIdentifier, 
                digestAlgorithmIdentifier, key, signature, provider);
    }
    
    public static boolean isNull( DEREncodable o){
        return (o instanceof ASN1Null) || (o == null);
    }

    public static DigestInfo derDecode(byte[] encoding) throws IOException {
        if (encoding[0] != (DERTags.CONSTRUCTED | DERTags.SEQUENCE)) {
            throw new IOException("not a digest info object");
        }
        ASN1InputStream aIn = new ASN1InputStream(encoding);
        return new DigestInfo((ASN1Sequence)aIn.readObject());
    }
    
    public static String getHashBase64 (String cadenaOrigen, 
            String digestAlgorithm) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance(digestAlgorithm);
        byte[] resultDigest =  sha.digest( cadenaOrigen.getBytes() );
        return new String(Base64.encode(resultDigest));
    }


   public static String obtenerInfoCertificado (X509Certificate certificado) {
        StringBuilder infoCertificado = new StringBuilder();
        infoCertificado.append("<html><b>Asunto: </b>")
                .append(certificado.getSubjectDN().toString())
                .append("<br/><b>Emisor del certificado: </b>")
                .append(certificado.getIssuerDN().toString())
                .append("<br/><b>Número de serie del certificado: </b>")
                .append(certificado.getSerialNumber().toString())
                .append("<br/><b>Valido desde: </b>")
                .append(certificado.getNotBefore())
                .append("<br/><b>Valido hasta: </b>")
                .append(certificado.getNotAfter())
                .append("<br/></html>");
        return infoCertificado.toString();
    }
   
    public static String getEncryptiontId (String encryptionAlgOID) {
        String algName = null;        
        if (ENCRYPTION_RSA.equals(encryptionAlgOID)) algName = "RSA";
        if (ENCRYPTION_DSA.equals(encryptionAlgOID)) algName = "DSA";
        if (ENCRYPTION_ECDSA.equals(encryptionAlgOID)) algName = "ECDSA";
        if (ENCRYPTION_RSA_PSS.equals(encryptionAlgOID)) algName = "RSA_PSS";
        if (ENCRYPTION_GOST3410.equals(encryptionAlgOID)) algName = "GOST3410";
        if (ENCRYPTION_ECGOST3410.equals(encryptionAlgOID)) algName = "ECGOST3410";
        if (algName != null) {
            return algName;
        }
        return encryptionAlgOID;
    }
    
    public static AlgorithmIdentifier getEncryptionAlgorithmIdentifier (String digestAlgOID) {
        if (ENCRYPTION_RSA.equals(digestAlgOID)) 
             return new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, new DERNull());
        else return null;
    }

    public static String getDigestId (String digestAlgOID) {
        String algName = null;        
        if (DIGEST_SHA1.equals(digestAlgOID)) algName =  "SHA1";
        if (DIGEST_SHA224.equals(digestAlgOID)) algName = "SHA224";
        if (DIGEST_SHA256.equals(digestAlgOID)) algName = "SHA256";
        if (DIGEST_SHA384.equals(digestAlgOID)) algName = "SHA384";
        if (DIGEST_SHA512.equals(digestAlgOID)) algName = "SHA512";
        if (DIGEST_MD5.equals(digestAlgOID)) algName = "MD5";
        if (DIGEST_GOST3411.equals(digestAlgOID)) algName = "GOST3411";
        if (DIGEST_RIPEMD128.equals(digestAlgOID)) algName = "RIPEMD128";
        if (DIGEST_RIPEMD160.equals(digestAlgOID)) algName = "RIPEMD160";
        if (DIGEST_RIPEMD256.equals(digestAlgOID)) algName = "RIPEMD256";
        if (algName != null) {
            return algName;
        }
        return digestAlgOID;
    }

    public static AlgorithmIdentifier fixAlgID(AlgorithmIdentifier algId) {
        if (algId.getParameters() == null) {
            return new AlgorithmIdentifier(algId.getObjectId(), DERNull.INSTANCE);
        }
        return algId;
    }
        
    public static AlgorithmIdentifier getDigestAlgorithmIdentifier (String digestAlgOID) {
        if (DIGEST_SHA1.equals(digestAlgOID)) 
             return new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1, new DERNull());
        if (DIGEST_SHA256.equals(digestAlgOID)) 
             return new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha256, new DERNull());
        if (DIGEST_SHA512.equals(digestAlgOID)) 
             return new AlgorithmIdentifier(NISTObjectIdentifiers.id_sha512, new DERNull());
        else return null;
    }
    
}
