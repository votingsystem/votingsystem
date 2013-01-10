package org.sistemavotacion.seguridad;

import java.io.ByteArrayInputStream;
import org.bouncycastle.cms.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CRLException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bouncycastle.asn1.cms.IssuerAndSerialNumber;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.BEROctetStringGenerator;
import org.bouncycastle.asn1.BERSet;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.util.io.Streams;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.bouncycastle.asn1.cms.SignerIdentifier;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.util.encoders.Base64;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class CMSUtils {

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

    private static final Runtime RUNTIME = Runtime.getRuntime();

    static int getMaximumMemory() {
        long maxMem = RUNTIME.maxMemory();
        if (maxMem > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int)maxMem;
    }

    static ContentInfo readContentInfo(byte[] input) throws CMSException {
        // enforce limit checking as from a byte array
        return readContentInfo(new ASN1InputStream(input));
    }

    static ContentInfo readContentInfo(InputStream input) throws CMSException {
        // enforce some limit checking
        return readContentInfo(new ASN1InputStream(input, getMaximumMemory()));
    }

    static List getCertificatesFromStore(CertStore certStore) throws
            CertStoreException, CMSException {
        List certs = new ArrayList();
        try {
            for (Iterator it = certStore.getCertificates(null).iterator(); it.hasNext();) {
                X509Certificate c = (X509Certificate)it.next();
                certs.add(X509CertificateStructure.getInstance(
                                                       ASN1Object.fromByteArray(c.getEncoded())));
            }
            return certs;
        }
        catch (IllegalArgumentException e) {
            throw new CMSException("error processing certs", e);
        }
        catch (IOException e) {
            throw new CMSException("error processing certs", e);
        }
        catch (CertificateEncodingException e) {
            throw new CMSException("error encoding certs", e);
        }
    }

    static List getCRLsFromStore(CertStore certStore) throws CertStoreException,
            CMSException {
        List crls = new ArrayList();
        try {
            for (Iterator it = certStore.getCRLs(null).iterator(); it.hasNext();) {
                X509CRL c = (X509CRL)it.next();
                crls.add(CertificateList.getInstance(ASN1Object.fromByteArray(c.getEncoded())));
            }
            return crls;
        } catch (IllegalArgumentException e) {
            throw new CMSException("error processing crls", e);
        } catch (IOException e) {
            throw new CMSException("error processing crls", e);
        } catch (CRLException e) {
            throw new CMSException("error encoding crls", e);
        }
    }

    public static ASN1Set createBerSetFromList(List derObjects) {
        ASN1EncodableVector v = new ASN1EncodableVector();
        for (Iterator it = derObjects.iterator(); it.hasNext();) {
            v.add((DEREncodable)it.next());
        }
        return new BERSet(v);
    }

    static ASN1Set createDerSetFromList(List derObjects) {
        ASN1EncodableVector v = new ASN1EncodableVector();
        for (Iterator it = derObjects.iterator(); it.hasNext();) {
            v.add((DEREncodable)it.next());
        }
        return new DERSet(v);
    }

    static OutputStream createBEROctetOutputStream(OutputStream s,
            int tagNo, boolean isExplicit, int bufferSize) throws IOException {
        BEROctetStringGenerator octGen = new BEROctetStringGenerator(s, tagNo, isExplicit);
        if (bufferSize != 0) {
            return octGen.getOctetOutputStream(new byte[bufferSize]);
        }
        return octGen.getOctetOutputStream();
    }

    static TBSCertificateStructure getTBSCertificateStructure(
        X509Certificate cert) throws CertificateEncodingException {
        try {
            return TBSCertificateStructure.getInstance(ASN1Object
                .fromByteArray(cert.getTBSCertificate()));
        } catch (IOException e) {
            throw new CertificateEncodingException(e.toString());
        }
    }

    private static ContentInfo readContentInfo(ASN1InputStream in) throws CMSException {
        try {
            return ContentInfo.getInstance(in.readObject());
        } catch (IOException e) {
            throw new CMSException("IOException reading content.", e);
        } catch (ClassCastException e) {
            throw new CMSException("Malformed content.", e);
        } catch (IllegalArgumentException e) {
            throw new CMSException("Malformed content.", e);
        }
    }

    public static byte[] streamToByteArray(InputStream in) throws IOException {
        return Streams.readAll(in);
    }

    public static byte[] streamToByteArray(InputStream in, int limit) throws IOException {
        return Streams.readAllLimited(in, limit);
    }

    public static Provider getProvider(String providerName) throws NoSuchProviderException {
        if (providerName != null) {
            Provider prov = Security.getProvider(providerName);
            if (prov != null) {
                return prov;
            }
            throw new NoSuchProviderException("provider " + providerName + " not found.");
        }
        return null;
    }

    public static Certificate obtenerCertificado (byte[] certBytes) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(new ByteArrayInputStream(certBytes)) ;
        return cert;
    }

    public static SignerIdentifier getSignerIdentifier(X509Certificate cert) {
        TBSCertificateStructure tbs;
        try {
            tbs = CMSUtils.getTBSCertificateStructure(cert);
        }
        catch (CertificateEncodingException e) {
            throw new IllegalArgumentException(
                "can't extract TBS structure from this cert");
        }
        //Aqui podría ser ...pkcs.IssuerAndSerialNumber
        IssuerAndSerialNumber encSid = new IssuerAndSerialNumber(tbs
                .getIssuer(), tbs.getSerialNumber().getValue());
        return new SignerIdentifier(encSid);
    }

   /**
    * Sacado de http://www.nakov.com/blog/2009/12/01/x509-certificate-validation-in-java-build-and-verify-chain-and-verify-clr-with-bouncy-castle/
    * Checks whether given X.509 certificate is self-signed.
    */
    public static boolean isSelfSigned(X509Certificate cert) throws Exception {
        try {
            // Try to verify certificate signature with its own public key
            PublicKey key = (PublicKey) cert.getPublicKey();
            cert.verify(key);
            return true;
        } catch (Exception keyEx) {
            // Invalid key –> not self-signed
            return false;
        }
    }

    public static MimeMessage createMimeMessage(String subject, Object content,
        String contentType, String from, String to) throws MessagingException {
        Properties props = System.getProperties();
        Session session = Session.getDefaultInstance(props, null);
        //Address fromUser = new InternetAddress("\"JJ\"<jj@gruposp2p.org>");
        Address fromUser = new InternetAddress(from);
        Address toUser = new InternetAddress(to);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(fromUser);
        message.setRecipient(Message.RecipientType.TO, toUser);
        message.setSubject(subject);
        message.setContent(content, contentType);
        message.saveChanges();
        return message;
    }

    public static String getDigestId (String digestAlgOID) {
        if (DIGEST_SHA1.equals(digestAlgOID)) return "SHA1";
        if (DIGEST_SHA224.equals(digestAlgOID)) return "SHA224";
        if (DIGEST_SHA256.equals(digestAlgOID)) return "SHA256";
        if (DIGEST_SHA384.equals(digestAlgOID)) return "SHA384";
        if (DIGEST_SHA512.equals(digestAlgOID)) return "SHA512";
        if (DIGEST_MD5.equals(digestAlgOID)) return "MD5";
        if (DIGEST_GOST3411.equals(digestAlgOID)) return "GOST3411";
        if (DIGEST_RIPEMD128.equals(digestAlgOID)) return "RIPEMD128";
        if (DIGEST_RIPEMD160.equals(digestAlgOID)) return "RIPEMD160";
        if (DIGEST_RIPEMD256.equals(digestAlgOID)) return "RIPEMD256";
        return null;
    }

    public static String getEncryptiontId (String encryptionAlgOID) {
        if (ENCRYPTION_RSA.equals(encryptionAlgOID)) return "RSA";
        if (ENCRYPTION_DSA.equals(encryptionAlgOID)) return "DSA";
        if (ENCRYPTION_ECDSA.equals(encryptionAlgOID)) return "ECDSA";
        if (ENCRYPTION_RSA_PSS.equals(encryptionAlgOID)) return "RSA_PSS";
        if (ENCRYPTION_GOST3410.equals(encryptionAlgOID)) return "GOST3410";
        if (ENCRYPTION_ECGOST3410.equals(encryptionAlgOID)) return "ECGOST3410";
        return null;
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

   public static CMSSignedData getCMSSignedDataFromBytes (byte[] signedData)
            throws Exception {
        MimeMessage email = getMimeMessageFromBytes(signedData);
        CMSSignedData cmsSignedData = new CMSSignedData(email.getInputStream());
        return cmsSignedData;
    }

    public static MimeMessage getMimeMessageFromBytes (byte[] signedData)
	    	throws Exception {
        ByteArrayInputStream bais = new ByteArrayInputStream(signedData);
        MimeMessage email = new MimeMessage(null, bais);
        return email;
    }
    
    public static String obtenerHashBase64 (
            String cadenaOrigen, String digestAlgorithm) 
            throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance(digestAlgorithm);
        byte[] resultDigest =  sha.digest( cadenaOrigen.getBytes() );
        return new String(Base64.encode(resultDigest));
    }
    
    public static String obtenerHashBase64 (String cadenaOrigen) 
            throws NoSuchAlgorithmException {
        return obtenerHashBase64(cadenaOrigen, "SHA-1");
    }
    
}
