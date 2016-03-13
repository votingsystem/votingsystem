package org.votingsystem.util.crypto;

import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.votingsystem.util.ContextVS;

import java.io.*;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
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

    public static PrivateKey fromPEMToRSAPrivateKey(String pemPrivateKey) throws Exception {
        KeyPair kp = (KeyPair) new PEMParser(new InputStreamReader(new ByteArrayInputStream(pemPrivateKey.getBytes()))).readObject();
        return kp.getPrivate();
    }

    public static PublicKey fromPEMToRSAPublicKey(String pemPublicKey) throws Exception {
        KeyFactory factory = KeyFactory.getInstance("RSA", "BC");
        PEMParser PEMParser = new PEMParser(new InputStreamReader(new ByteArrayInputStream(pemPublicKey.getBytes())));
        RSAPublicKey jcerSAPublicKey = (RSAPublicKey) PEMParser.readObject();
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(jcerSAPublicKey.getEncoded());
        PublicKey publicKey = factory.generatePublic(pubKeySpec);
        return publicKey;
    }

}
