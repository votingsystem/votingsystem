package org.votingsystem.util.crypto;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.JSON;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
* http://www.amazon.com/exec/obidos/redirect?path=ASIN/0764596330&link_code=as2&camp=1789&tag=bouncycastleo-20&creative=9325
*/
public class CertUtils {

    private static Logger log = java.util.logging.Logger.getLogger(CertUtils.class.getName());

    /**
     * Generate V3 certificate for users
     */
    public static X509Certificate generateEndEntityCert(PublicKey entityKey, PrivateKey caKey, X509Certificate caCert,
            Date dateBegin, Date dateFinish, String endEntitySubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(KeyGeneratorVS.INSTANCE.getSerno());
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        certGen.setSubjectDN(new X500Principal(endEntitySubjectDN));
        certGen.setPublicKey(entityKey);
        certGen.setSignatureAlgorithm(ContextVS.CERT_GENERATION_SIG_ALGORITHM);
        certGen.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert));
        SubjectKeyIdentifier subjectKeyIdentifier = new BcX509ExtensionUtils().createSubjectKeyIdentifier(
                SubjectPublicKeyInfo.getInstance(entityKey.getEncoded()));
        certGen.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
        certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certGen.addExtension(Extension.keyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        return certGen.generate(caKey, ContextVS.PROVIDER);
    }

    /**
     * Generate V3 certificate for root CA Authority
     */
    public static X509Certificate generateV3RootCert(KeyPair pair, Date dateBegin, Date dateFinish,
             String strSubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        log.info("strSubjectDN: " + strSubjectDN);
        X509Principal x509Principal = new X509Principal(strSubjectDN);
        certGen.setSerialNumber(KeyGeneratorVS.INSTANCE.getSerno());
        certGen.setIssuerDN(x509Principal);
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        log.info("dateBegin: " + dateBegin.toString() + " - dateFinish: " + dateFinish.toString());
        certGen.setSubjectDN(x509Principal);
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(ContextVS.CERT_GENERATION_SIG_ALGORITHM);
        //The following fragment shows how to create one which indicates that
        //the certificate containing it is a CA and that only one certificate can follow in the certificate path.
        certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true, 0));
        certGen.addExtension(Extension.subjectKeyIdentifier, false,
                new SubjectKeyIdentifier(getDigest(pair.getPublic().getEncoded())));
        certGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return certGen.generate(pair.getPrivate(), ContextVS.PROVIDER);
    }


    /**
     * Generate V1 certificate for root CA Authority
     */
    public static X509Certificate generateV1RootCert(KeyPair pair, long comienzo, int period,
             String principal) throws Exception {
        X509V1CertificateGenerator  certGen = new X509V1CertificateGenerator();
        certGen.setSerialNumber(KeyGeneratorVS.INSTANCE.getSerno());
        certGen.setIssuerDN(new X500Principal(principal));
        certGen.setNotBefore(new Date(comienzo));
        certGen.setNotAfter(new Date(comienzo + period));
        certGen.setSubjectDN(new X500Principal(principal));
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(ContextVS.CERT_GENERATION_SIG_ALGORITHM);
        return certGen.generate(pair.getPrivate(), ContextVS.PROVIDER);
    }

    /**
     * Generate V3 certificate for TimeStamp signing
     */
    public static X509Certificate generateTimeStampingCert(PublicKey entityKey, PrivateKey caKey,
           X509Certificate caCert, long begin, long period, String endEntitySubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(new Date(begin));
        certGen.setNotAfter(new Date(begin + period));
        certGen.setSubjectDN(new X500Principal(endEntitySubjectDN));
        certGen.setPublicKey(entityKey);
        certGen.setSignatureAlgorithm(ContextVS.CERT_GENERATION_SIG_ALGORITHM);
        certGen.addExtension(Extension.authorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCert));
        SubjectKeyIdentifier subjectKeyIdentifier = new BcX509ExtensionUtils().createSubjectKeyIdentifier(
                SubjectPublicKeyInfo.getInstance(entityKey.getEncoded()));
        certGen.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
        certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certGen.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        certGen.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));
        return certGen.generate(caKey, ContextVS.PROVIDER);
    }

    /**
     * Generate V3 Certificate from CSR
     */
    public static X509Certificate signCSR(PKCS10CertificationRequest csr, String organizationalUnit, PrivateKey caKey,
               X509Certificate caCert, Date dateBegin, Date dateFinish, Attribute... attrs) throws Exception {
        String subjectDN = csr.toASN1Structure().getCertificationRequestInfo().getSubject().toString();
        if (subjectDN == null) throw new Exception("ERROR VERIFYING CSR - SubjectDN null");
        PublicKey publicKey = getPublicKey(csr);
        ContentVerifierProvider verifier = new JcaContentVerifierProviderBuilder().setProvider(ContextVS.PROVIDER).build(publicKey);
        if (!csr.isSignatureValid(verifier)) throw new Exception("ERROR VERIFYING CSR - fail chicknature check");
        if(organizationalUnit != null) subjectDN = organizationalUnit + "," + subjectDN;
        X509Certificate issuedCert = generateV3EndEntityCertFromCsr(csr, caKey, caCert, dateBegin, dateFinish, subjectDN);
        return issuedCert;
    }

    public static PublicKey getPublicKey(PKCS10CertificationRequest pkcs10CSR) throws IOException,
            NoSuchAlgorithmException, InvalidKeySpecException {
        RSAKeyParameters rsaParams = (RSAKeyParameters) PublicKeyFactory.createKey(pkcs10CSR.getSubjectPublicKeyInfo());
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                rsaParams.getModulus(), rsaParams.getExponent()));
    }
    /**
     * Generate V3 Certificate from CSR
     */
    public static X509Certificate generateV3EndEntityCertFromCsr(PKCS10CertificationRequest csr,
            PrivateKey caKey, X509Certificate caCert, Date dateBegin, Date dateFinish, String subjectDN,
             Attribute... attrs) throws Exception{
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        PublicKey publicKey = getPublicKey(csr);
        X509Principal x509Principal = new X509Principal(subjectDN);
        certGen.setSerialNumber(KeyGeneratorVS.INSTANCE.getSerno());
        log.info("generateV3EndEntityCertFromCsr - issuer: " + caCert.getSubjectX500Principal());
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        certGen.setSubjectDN(x509Principal);
        certGen.setPublicKey(publicKey);
        certGen.setSignatureAlgorithm(ContextVS.CERT_GENERATION_SIG_ALGORITHM);
        certGen.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert));
        SubjectKeyIdentifier subjectKeyIdentifier = new BcX509ExtensionUtils().createSubjectKeyIdentifier(
                SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));
        certGen.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
        certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));//Certificado final
        certGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        ASN1Set attributes = csr.toASN1Structure().getCertificationRequestInfo().getAttributes();
        if (attributes != null) {
            for (int i = 0; i != attributes.size(); i++) {
                Attribute attr = Attribute.getInstance(attributes.getObjectAt(i));
                if (attr.getAttrType().equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
                    X509Extensions extensions = X509Extensions.getInstance(attr.getAttrValues().getObjectAt(0));
                    Enumeration e = extensions.oids();
                    while (e.hasMoreElements()) {
                        DERObjectIdentifier oid = (DERObjectIdentifier) e.nextElement();
                        X509Extension ext = extensions.getExtension(oid);
                        certGen.addExtension(oid, ext.isCritical(), ext.getValue().getOctets());
                    }
                } else if(attr.getAttrType().toString().startsWith(ContextVS.VOTING_SYSTEM_BASE_OID)) {
                    certGen.addExtension(attr.getAttrType(), false, attr.getAttrValues());
                }
            }
        }
        if(attrs != null) {
            for(Attribute attribute : attrs) {
                certGen.addExtension(attribute.getAttrType(), false, attribute.getAttrValues());
            }
        }
        X509Certificate cert = certGen.generate(caKey, ContextVS.PROVIDER);
        cert.verify(caCert.getPublicKey());
        return cert;
    }

    public static X509Certificate loadCertificate (byte[] certBytes) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(certBytes);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<X509Certificate> certificateChain =
                (Collection<X509Certificate>) certificateFactory.generateCertificates(inputStream);
        X509Certificate x509Cert = certificateChain.iterator().next();
        inputStream.close();
        return x509Cert;
    }

    /**
     *
     * Verifies the validity of the given certificate, checking its signature
     * against the issuer's certificate.
     *
     * @param certs the certificate list to validate
     * @param anchors Set of trusted (usually self-signed) certificates.
     * @param checkCRL boolean to tell system to check or not check CRL's
     *
     * @return result PKIXCertPathValidatorResult if the certificate's signature is valid otherwise throws ExceptionVS.
     */
    public static PKIXCertPathValidatorResult verifyCertificate(Set<TrustAnchor> anchors,
             boolean checkCRL, List<X509Certificate> certs) throws ExceptionVS {
        return verifyCertificate(anchors, checkCRL, certs, new Date());
    }

    public static PKIXCertPathValidatorResult verifyCertificate(Set<TrustAnchor> anchors,
              boolean checkCRL, List<X509Certificate> certs, Date signingDate) throws ExceptionVS {
        try {
            PKIXParameters pkixParameters = new PKIXParameters(anchors);
            pkixParameters.setDate(signingDate);
            pkixParameters.setRevocationEnabled(checkCRL); // if false tell system do not check CRL's
            CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX", ContextVS.PROVIDER);
            CertificateFactory certFact = CertificateFactory.getInstance("X.509");
            CertPath certPath = certFact.generateCertPath(certs);
            CertPathValidatorResult result = certPathValidator.validate(certPath, pkixParameters);
            // Get the CA used to validate this path
            //PKIXCertPathValidatorResult pkixResult = (PKIXCertPathValidatorResult)result;
            //TrustAnchor ta = pkixResult.getTrustAnchor();
            //X509Certificate certCaResult = ta.getTrustedCert();
            //log.info("certCaResult: " + certCaResult.getSubjectDN().toString()+
            //        "- serialNumber: " + certCaResult.getSerialNumber().longValue());
            return (PKIXCertPathValidatorResult)result;
        } catch(Exception ex) {
            String msg = "Empty cert list";
            if(certs != null && !certs.isEmpty()) msg = ex.getMessage() + " - cert: " +
                    certs.iterator().next().getSubjectDN();
            throw new ExceptionVS(msg, ex);
        }
    }

	/**
	 * Checks whether given X.509 certificate is self-signed.
	 * 
	 * http://www.nakov.com/blog/2009/12/01/x509-certificate-validation-in-java-build-and-verify-chain-and-verify-clr-with-bouncy-castle/
	 */
	public static boolean isSelfSigned(X509Certificate cert) throws CertificateException, NoSuchAlgorithmException,
			NoSuchProviderException {
		try {
			// Try to verify certificate signature with its own public key
			PublicKey key = cert.getPublicKey();
			cert.verify(key);
			return true;
		} catch (SignatureException sigEx) {
			// Invalid signature --> not self-signed
			return false;
		} catch (InvalidKeyException keyEx) {
			// Invalid key --> not self-signed
			return false;
		}
	}
    
	/**
	 * Downloads a CRL from given HTTP/HTTPS/FTP URL, e.g.
	 * http://crl.infonotary.com/crl/identity-ca.crl
	 * 
	 * http://www.nakov.com/blog/2009/12/01/x509-certificate-validation-in-java-build-and-verify-chain-and-verify-clr-with-bouncy-castle/
	 */
	public static X509CRL downloadCRLFromWeb(String crlURL) throws MalformedURLException, IOException,
            CertificateException, CRLException {
		URL url = new URL(crlURL);
		InputStream crlStream = url.openStream();
		try {
			CertificateFactory cf = CertificateFactory.getInstance("X.509");
			X509CRL crl = (X509CRL) cf.generateCRL(crlStream);
			return crl;
		} finally {
			crlStream.close();
		}
	}

    public static <T> T getCertExtensionData(Class<T> type, X509Certificate x509Certificate,
                                             String extensionOID) throws Exception {
        byte[] extensionValue =  x509Certificate.getExtensionValue(extensionOID);
        if(extensionValue == null) return null;
        ASN1Primitive asn1Primitive = X509ExtensionUtil.fromExtensionValue(extensionValue);
        if(asn1Primitive instanceof DLSet) {
            return JSON.getMapper().readValue(((DLSet) asn1Primitive).getObjectAt(0).toString(), type);
        }
        return null;
    }

    public static String getCertExtensionData(X509Certificate x509Certificate, String extensionOID) throws Exception {
        byte[] extensionValue =  x509Certificate.getExtensionValue(extensionOID);
        if(extensionValue == null) return null;
        ASN1Primitive asn1Primitive = X509ExtensionUtil.fromExtensionValue(extensionValue);
        if(asn1Primitive instanceof DLSet) {
            return ((DLSet) asn1Primitive).getObjectAt(0).toString();
        }
        return null;
    }

    public static <T> T getCertExtensionData(Class<T> type, PKCS10CertificationRequest csr, String oid) throws Exception {
        org.bouncycastle.asn1.pkcs.Attribute[] attributes = csr.getAttributes(new ASN1ObjectIdentifier(oid));
        if(attributes.length > 0) {
            String certAttributeJSONStr = ((DERUTF8String) attributes[0].getAttrValues().getObjectAt(0)).getString();
            return JSON.getMapper().readValue(certAttributeJSONStr, type);
        }
        log.info("missing attribute with oid: " + oid);
        return null;
    }

    private static byte[] getDigest(byte[] bytes) {
        Digest digest = new SHA1Digest();
        byte[] resBuf = new byte[digest.getDigestSize()];

        digest.update(bytes, 0, bytes.length);
        digest.doFinal(resBuf, 0);
        return resBuf;
    }

}