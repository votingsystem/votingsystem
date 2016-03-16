package org.votingsystem.util.crypto;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.util.ContextVS;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class PEMUtils {

    public static byte[] getPEMEncoded (Object objectToEncode) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        JcaPEMWriter pemWrt = new JcaPEMWriter(new OutputStreamWriter(bOut));
        if(objectToEncode instanceof Collection) {
            Collection objectToEncodeColection = ((Collection)objectToEncode);
            for(Object object : objectToEncodeColection) {
                pemWrt.writeObject(object);
            }
        } else pemWrt.writeObject(objectToEncode);
        pemWrt.close();
        bOut.close();
        return bOut.toByteArray();
    }

    public static byte[] getPEMEncoded (CMSSignedData cmsSignedData) throws IOException {
        return PEMUtils.getPEMEncoded(cmsSignedData.toASN1Structure());
    }

    public static String getPEMEncodedStr (Object objectToEncode) throws IOException {
        return new String(PEMUtils.getPEMEncoded(objectToEncode));
    }

    public static String getPEMEncodedStr (CMSSignedData cmsSignedData) throws IOException {
        return new String(PEMUtils.getPEMEncoded(cmsSignedData.toASN1Structure()));
    }

    public static PKCS10CertificationRequest fromPEMToPKCS10CertificationRequest (byte[] csrBytes) throws Exception {
        PEMParser PEMParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(csrBytes)));
        PKCS10CertificationRequest result = (PKCS10CertificationRequest)PEMParser.readObject();
        PEMParser.close();
        return result;
    }

    public static X509Certificate fromPEMToX509Cert (byte[] pemFileBytes) throws Exception {
        InputStream in = new ByteArrayInputStream(pemFileBytes);
        CertificateFactory fact = CertificateFactory.getInstance("X.509", ContextVS.PROVIDER);
        X509Certificate x509Cert = (X509Certificate)fact.generateCertificate(in);
        in.close();
        return x509Cert;
    }

    public static Collection<X509Certificate> fromPEMToX509CertCollection (byte[] pemChainFileBytes) throws Exception {
        InputStream in = new ByteArrayInputStream(pemChainFileBytes);
        CertificateFactory fact = CertificateFactory.getInstance("X.509",ContextVS.PROVIDER);
        Collection<X509Certificate> x509Certs = (Collection<X509Certificate>)fact.generateCertificates(in);
        in.close();
        return x509Certs;
    }

    public static PrivateKey fromPEMToRSAPrivateKey(String privateKeyPEM) throws Exception {
        KeyPair kp = (KeyPair) new PEMParser(new InputStreamReader(new ByteArrayInputStream(privateKeyPEM.getBytes()))).readObject();
        return kp.getPrivate();
    }

    public static PublicKey fromPEMToRSAPublicKey(String publicKeyPEM) throws Exception {
        PEMParser PEMParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(publicKeyPEM.getBytes())));
        Object object = PEMParser.readObject();
        if(object instanceof SubjectPublicKeyInfo) {
            return generatePublicKey((SubjectPublicKeyInfo)object);
        } else {
            KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
            RSAPublicKey jcerSAPublicKey = (RSAPublicKey) PEMParser.readObject();
            X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(jcerSAPublicKey.getEncoded());
            PublicKey publicKey = factory.generatePublic(pubKeySpec);
            return publicKey;
        }
    }

    public static PublicKey generatePublicKey(SubjectPublicKeyInfo pkInfo)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec keyspec;
        try {
            keyspec = new X509EncodedKeySpec(pkInfo.getEncoded());
        } catch (IOException e) {
            throw new InvalidKeySpecException(e.getMessage(), e);
        }
        ASN1ObjectIdentifier aid = pkInfo.getAlgorithm().getAlgorithm();
        KeyFactory kf;
        if (PKCSObjectIdentifiers.rsaEncryption.equals(aid)) {
            kf = KeyFactory.getInstance("RSA");
        } else if (X9ObjectIdentifiers.id_dsa.equals(aid)) {
            kf = KeyFactory.getInstance("DSA");
        } else if (X9ObjectIdentifiers.id_ecPublicKey.equals(aid)) {
            kf = KeyFactory.getInstance("ECDSA");
        } else {
            throw new InvalidKeySpecException("unsupported key algorithm: " + aid);
        }
        return kf.generatePublic(keyspec);
    }
}
