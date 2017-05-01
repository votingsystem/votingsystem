package org.votingsystem.crypto;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.util.Store;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.votingsystem.throwable.CertificateValidationException;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
* http://www.amazon.com/exec/obidos/redirect?path=ASIN/0764596330&link_code=as2&camp=1789&tag=bouncycastleo-20&creative=9325
*/
public class CertificateUtils {

    private static Logger log = java.util.logging.Logger.getLogger(CertificateUtils.class.getName());

    /**
     * Generate V3 certificate for users
     */
    public static X509Certificate generateUserCert(PublicKey entityKey, PrivateKey caKey, X509Certificate caCert,
                       Date dateBegin, Date dateFinish, String endEntitySubjectDN, String ocspServer) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(KeyGenerator.INSTANCE.getSerno());
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        certGen.setSubjectDN(new X500Principal(endEntitySubjectDN));
        certGen.setPublicKey(entityKey);
        certGen.setSignatureAlgorithm(Constants.CERT_GENERATION_SIG_ALGORITHM);

        //Authority Information Access
        //AccessDescription caIssuers = new AccessDescription(AccessDescription.id_ad_caIssuers,
        //        new GeneralName(GeneralName.uniformResourceIdentifier, new DERIA5String("https://votingsyste.ddns.net/ca.cer")));
        AccessDescription ocsp = new AccessDescription(AccessDescription.id_ad_ocsp,
                new GeneralName(GeneralName.uniformResourceIdentifier, new DERIA5String(ocspServer)));
        ASN1EncodableVector aia_ASN = new ASN1EncodableVector();
        //aia_ASN.add(caIssuers);
        aia_ASN.add(ocsp);
        certGen.addExtension(Extension.authorityInfoAccess, false, new DERSequence(aia_ASN));

        certGen.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert));
        SubjectKeyIdentifier subjectKeyIdentifier = new BcX509ExtensionUtils().createSubjectKeyIdentifier(
                SubjectPublicKeyInfo.getInstance(entityKey.getEncoded()));
        certGen.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
        certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certGen.addExtension(Extension.keyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        return certGen.generate(caKey, Constants.PROVIDER);
    }

    /**
     * Generate V3 certificate for TimeStamp signing
     */
    public static X509Certificate generateTimeStampServerCert(PublicKey entityKey, PrivateKey caKey,
                      X509Certificate caCert, Date dateBegin, Date dateFinish, String endEntitySubjectDN,
                      String ocspServer) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        certGen.setSubjectDN(new X500Principal(endEntitySubjectDN));
        certGen.setPublicKey(entityKey);
        certGen.setSignatureAlgorithm(Constants.CERT_GENERATION_SIG_ALGORITHM);

        //Authority Information Access
        //AccessDescription caIssuers = new AccessDescription(AccessDescription.id_ad_caIssuers,
        //        new GeneralName(GeneralName.uniformResourceIdentifier, new DERIA5String("https://votingsyste.ddns.net/ca.cer")));
        AccessDescription ocsp = new AccessDescription(AccessDescription.id_ad_ocsp,
                new GeneralName(GeneralName.uniformResourceIdentifier, new DERIA5String(ocspServer)));
        ASN1EncodableVector aia_ASN = new ASN1EncodableVector();
        //aia_ASN.add(caIssuers);
        aia_ASN.add(ocsp);
        certGen.addExtension(Extension.authorityInfoAccess, false, new DERSequence(aia_ASN));

        certGen.addExtension(Extension.authorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCert));
        SubjectKeyIdentifier subjectKeyIdentifier = new BcX509ExtensionUtils().createSubjectKeyIdentifier(
                SubjectPublicKeyInfo.getInstance(entityKey.getEncoded()));
        certGen.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
        certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        certGen.addExtension(Extension.keyUsage, true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        certGen.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping));
        return certGen.generate(caKey, Constants.PROVIDER);
    }

    /**
     * Generate V3 certificate for root CA Authority
     */
    public static X509Certificate generateV3RootCert(KeyPair pair, Date dateBegin, Date dateFinish,
             String strSubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        log.info("strSubjectDN: " + strSubjectDN);
        X509Principal x509Principal = new X509Principal(strSubjectDN);
        certGen.setSerialNumber(KeyGenerator.INSTANCE.getSerno());
        certGen.setIssuerDN(x509Principal);
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        log.info("dateBegin: " + dateBegin.toString() + " - dateFinish: " + dateFinish.toString());
        certGen.setSubjectDN(x509Principal);
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(Constants.CERT_GENERATION_SIG_ALGORITHM);

        //The following fragment shows how to create one which indicates that
        //the certificate containing it is a CA and that only one certificate can follow in the certificate path.
        certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(true, 0));
        certGen.addExtension(Extension.subjectKeyIdentifier, false,
                new SubjectKeyIdentifier(HashUtils.getSHA1(pair.getPublic().getEncoded())));
        certGen.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return certGen.generate(pair.getPrivate(), Constants.PROVIDER);
    }

    /**
     * Generate V1 certificate for root CA Authority
     */
    public static X509Certificate generateV1RootCert(KeyPair pair, Date dateBegin, Date dateFinish, String principal)
            throws Exception {
        X509V1CertificateGenerator  certGen = new X509V1CertificateGenerator();
        certGen.setSerialNumber(KeyGenerator.INSTANCE.getSerno());
        certGen.setIssuerDN(new X500Principal(principal));
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        certGen.setSubjectDN(new X500Principal(principal));
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(Constants.CERT_GENERATION_SIG_ALGORITHM);
        return certGen.generate(pair.getPrivate(), Constants.PROVIDER);
    }

    /**
     * Generate V3 Certificate from CSR
     */
    public static X509Certificate signCSR(PKCS10CertificationRequest csr, String organizationalUnit, PrivateKey caKey,
            X509Certificate caCert, LocalDateTime localDateTimeBegin, LocalDateTime localDateTimeFinish,
            String ocspServer, Attribute... attrs) throws Exception {
        Date dateBegin = DateUtils.getUTCDate(localDateTimeBegin);
        Date dateFinish = DateUtils.getUTCDate(localDateTimeFinish);
        String subjectDN = csr.toASN1Structure().getCertificationRequestInfo().getSubject().toString();
        if (subjectDN == null)
            throw new ValidationException("ERROR VERIFYING CSR - null SubjectDN");
        PublicKey publicKey = getPublicKey(csr);
        ContentVerifierProvider verifier = new JcaContentVerifierProviderBuilder().setProvider(Constants.PROVIDER).build(publicKey);
        if (!csr.isSignatureValid(verifier))
            throw new Exception("ERROR VERIFYING CSR - fail signature check");


        if(organizationalUnit != null)
            subjectDN = "OU=" + organizationalUnit + "," + subjectDN;

        //X500NameBuilder nameBuilder = new X500NameBuilder();
        //nameBuilder.addRDN(BCStyle.EmailAddress, "feedback-crypto@bouncycastle.org");
        //nameBuilder.addRDN(BCStyle.DN_QUALIFIER, subjectDN);
        //X500Name x500Name = nameBuilder.build();

        X500Name x500Name = new X500Name(subjectDN);

        X500Name issuer500name = new JcaX509CertificateHolder(caCert).getSubject();
        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(issuer500name, KeyGenerator.INSTANCE.getSerno(),
                dateBegin, dateFinish, x500Name, publicKey);

        certGen.addExtension(Extension.authorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert));

        //Authority Information Access
        //AccessDescription caIssuers = new AccessDescription(AccessDescription.id_ad_caIssuers,
        //        new GeneralName(GeneralName.uniformResourceIdentifier, new DERIA5String("https://votingsyste.ddns.net/ca.cer")));
        AccessDescription ocsp = new AccessDescription(AccessDescription.id_ad_ocsp,
                new GeneralName(GeneralName.uniformResourceIdentifier, new DERIA5String(ocspServer)));
        ASN1EncodableVector aia_ASN = new ASN1EncodableVector();
        //aia_ASN.add(caIssuers);
        aia_ASN.add(ocsp);
        certGen.addExtension(Extension.authorityInfoAccess, false, new DERSequence(aia_ASN));

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
                    Extensions extensions = Extensions.getInstance(attr.getAttrValues().getObjectAt(0));
                    Enumeration e = extensions.oids();
                    while (e.hasMoreElements()) {
                        ASN1ObjectIdentifier oid = (ASN1ObjectIdentifier) e.nextElement();
                        Extension ext = extensions.getExtension(oid);
                        certGen.addExtension(oid, ext.isCritical(), ext.getExtnValue().getOctets());
                    }
                } else if(attr.getAttrType().toString().startsWith(Constants.VOTING_SYSTEM_BASE_OID)) {
                    certGen.addExtension(attr.getAttrType(), false, attr.getAttrValues());
                }
            }
        }
        if(attrs != null) {
            for(Attribute attribute : attrs) {
                certGen.addExtension(attribute.getAttrType(), false, attribute.getAttrValues());
            }
        }
        X509CertificateHolder certHldr = certGen.build(new JcaContentSignerBuilder(
                Constants.CERT_GENERATION_SIG_ALGORITHM).setProvider(Constants.PROVIDER).build(caKey));
        X509Certificate cert = new JcaX509CertificateConverter().setProvider(Constants.PROVIDER).getCertificate(certHldr);
        cert.verify(caCert.getPublicKey());
        return cert;
    }

    public static PublicKey getPublicKey(PKCS10CertificationRequest pkcs10CSR) throws IOException,
            NoSuchAlgorithmException, InvalidKeySpecException {
        RSAKeyParameters rsaParams = (RSAKeyParameters) PublicKeyFactory.createKey(pkcs10CSR.getSubjectPublicKeyInfo());
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                rsaParams.getModulus(), rsaParams.getExponent()));
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
     * @return result PKIXCertPathValidatorResult if the certificate's signature is valid otherwise throws CertificateValidationException.
     */
    public static PKIXCertPathValidatorResult verifyCertificate(Set<TrustAnchor> anchors,
             boolean checkCRL, List<X509Certificate> certs) throws CertificateValidationException {
        return verifyCertificate(anchors, checkCRL, certs, new Date());
    }

    public static PKIXCertPathValidatorResult verifyCertificate(Set<TrustAnchor> anchors,
              boolean checkCRL, List<X509Certificate> certs, Date signingDate) throws CertificateValidationException {
        try {
            PKIXParameters pkixParameters = new PKIXParameters(anchors);
            pkixParameters.setDate(signingDate);
            pkixParameters.setRevocationEnabled(checkCRL); // if false tell system do not check CRL's
            CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX", Constants.PROVIDER);
            CertificateFactory certFact = CertificateFactory.getInstance("X.509");
            CertPath certPath = certFact.generateCertPath(certs);
            CertPathValidatorResult result = certPathValidator.validate(certPath, pkixParameters);
            // Get the CA used to validate this path
            //PKIXCertPathValidatorResult pkixResult = (PKIXCertPathValidatorResult)result;
            //TrustAnchor ta = pkixResult.getTrustAnchor();
            //X509Cert certCaResult = ta.getTrustedCert();
            //log.info("certCaResult: " + certCaResult.getSubjectDN().toString()+
            //        "- serialNumber: " + certCaResult.getSerialNumber().longValue());
            return (PKIXCertPathValidatorResult)result;
        } catch(Exception ex) {
            String msg = "Empty cert list";
            if(certs != null && !certs.isEmpty()) msg = ex.getMessage() + " - cert: " +
                    certs.iterator().next().getSubjectDN();
            throw new CertificateValidationException(msg, ex);
        }
    }

	/**
	 * Checks whether given X.509 certificate is self-signed.
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

    public static PKIXCertPathBuilderResult verifyCertificateChain(byte[] certChainPEM) throws Exception {
        Iterator<X509Certificate> certIterator = PEMUtils.fromPEMToX509CertCollection(certChainPEM).iterator();
        X509Certificate cert = certIterator.next();
        Set<X509Certificate> additionalCerts = new HashSet<>();
        while(certIterator.hasNext()) {
            additionalCerts.add(certIterator.next());
        }
        return verifyCertificateChain(cert, additionalCerts);
    }

    public static PKIXCertPathBuilderResult verifyCertificateChain(X509Certificate cert,
            Set<X509Certificate> additionalCerts) throws ValidationException {
        try {
            if (isSelfSigned(cert)) {
                throw new ValidationException("The certificate is self-signed - " + cert.getSubjectDN());
            }
            Set<X509Certificate> trustedRootCerts = new HashSet<>();
            Set<X509Certificate> intermediateCerts = new HashSet<>();
            for (X509Certificate additionalCert : additionalCerts) {
                if (isSelfSigned(additionalCert)) {
                    trustedRootCerts.add(additionalCert);
                } else {
                    intermediateCerts.add(additionalCert);
                }
            }
            PKIXCertPathBuilderResult verifiedCertChain = verifyCertificate(cert, trustedRootCerts, intermediateCerts);

            // Check whether the certificate is revoked by the CRL given in its CRL distribution point extension
            //CRLVerifier.verifyCertificateCRLs(cert);

            // The chain is built and verified. Return it as a result
            return verifiedCertChain;
        } catch (Exception ex) {
            throw new ValidationException("Error verifying the certificate: " + cert.getSubjectX500Principal(), ex);
        }
    }

    /**
     * Attempts to build a certification chain for given certificate and to verify
     * it. Relies on a set of root CA certificates (trust anchors) and a set of
     * intermediate certificates (to be used as part of the chain).
     * @param cert - certificate for validation
     * @param trustedRootCerts - set of trusted root CA certificates
     * @param intermediateCerts - set of intermediate certificates
     * @return the certification chain (if verification is successful)
     * @throws GeneralSecurityException - if the verification is not successful
     *      (e.g. certification path cannot be built or some certificate in the
     *      chain is expired)
     */
    private static PKIXCertPathBuilderResult verifyCertificate(X509Certificate cert, Set<X509Certificate> trustedRootCerts,
                               Set<X509Certificate> intermediateCerts) throws GeneralSecurityException {
        // Create the selector that specifies the starting certificate
        X509CertSelector selector = new X509CertSelector();
        selector.setCertificate(cert);

        // Create the trust anchors (set of root CA certificates)
        Set<TrustAnchor> trustAnchors = new HashSet<>();
        for (X509Certificate trustedRootCert : trustedRootCerts) {
            trustAnchors.add(new TrustAnchor(trustedRootCert, null));
        }

        // Configure the PKIX certificate builder algorithm parameters
        PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(trustAnchors, selector);

        // Disable CRL checks (this is done manually as additional step)
        pkixParams.setRevocationEnabled(false);

        // Specify a list of intermediate certificates
        CertStore intermediateCertStore = CertStore.getInstance("Collection",
                new CollectionCertStoreParameters(intermediateCerts), "BC");
        pkixParams.addCertStore(intermediateCertStore);

        // Build and verify the certification chain
        CertPathBuilder builder = CertPathBuilder.getInstance("PKIX", "BC");
        PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) builder.build(pkixParams);
        return result;
    }

    /**
     * @param certificate	the certificate from which we need the ExtensionValue
     * @param oid the Object Identifier value for the extension.
     * @return	the extension value as an ASN1Primitive object
     * @throws IOException
     */
    public static ASN1Primitive getExtensionValue(X509Certificate certificate, String oid) throws IOException {
        byte[] bytes = certificate.getExtensionValue(oid);
        if (bytes == null) {
            return null;
        }
        ASN1InputStream aIn = new ASN1InputStream(new ByteArrayInputStream(bytes));
        ASN1OctetString octs = (ASN1OctetString) aIn.readObject();
        return new ASN1InputStream(new ByteArrayInputStream(octs.getOctets())).readObject();
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

    public static String getCertExtensionData(X509Certificate x509Certificate, String extensionOID) throws IOException {
        byte[] extensionValue =  x509Certificate.getExtensionValue(extensionOID);
        if(extensionValue == null) return null;
        ASN1Primitive asn1Primitive = X509ExtensionUtil.fromExtensionValue(extensionValue);
        if(asn1Primitive instanceof DLSet) {
            return ((DLSet) asn1Primitive).getObjectAt(0).toString();
        }
        return null;
    }

    public static <T> T getCertExtensionData(Class<T> type, PKCS10CertificationRequest csr, String oid) throws IOException {
        org.bouncycastle.asn1.pkcs.Attribute[] attributes = csr.getAttributes(new ASN1ObjectIdentifier(oid));
        if(attributes.length > 0) {
            String certAttributeJSONStr = ((DERUTF8String) attributes[0].getAttrValues().getObjectAt(0)).getString();
            return JSON.getMapper().readValue(certAttributeJSONStr, type);
        }
        log.info("missing attribute with oid: " + oid);
        return null;
    }

    /**
     * Method that load all PEM formated certificates found on a folder
     *
     * @param certificatesFolder folder with the PEM certificates
     * @return  a list with all the PEM certificates found on the folder
     */
    public static Collection<X509Certificate> loadCertificatesFromFolder(File certificatesFolder) {
        File[] listOfFiles = certificatesFolder.listFiles();
        if(listOfFiles != null) {
            List<X509Certificate> certificateList = new ArrayList<>();
            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    if(listOfFiles[i].getName().toLowerCase().endsWith(".pem")) {
                        try{
                            X509Certificate fileSystemX509TrustedCerts =
                                    PEMUtils.fromPEMToX509Cert(FileUtils.getBytesFromFile(listOfFiles[i]));
                            certificateList.add(fileSystemX509TrustedCerts);
                        } catch (Exception ex) {
                            log.log(Level.SEVERE, ex.getMessage(), ex);
                        }
                    } else log.log(Level.INFO, "found unexpected file name: " + listOfFiles[i].getAbsolutePath());
                } else if (listOfFiles[i].isDirectory()) {
                    log.log(Level.SEVERE, "found unexpected dir on certificates dir");
                }
            }
            return certificateList;
        } else return Collections.emptyList();
    }

    public static String getHash(X509Certificate x509Certificate) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] der = x509Certificate.getEncoded();
        md.update(der);
        return Base64.getEncoder().encodeToString(md.digest());
    }

    public static boolean equals(X509Certificate cert, X509Certificate certToCheck) throws CertificateEncodingException {
        return Arrays.equals(cert.getEncoded(), certToCheck.getEncoded());
    }

    public static Collection findCert(X509Certificate x509Cert, Store certStore) throws Exception {
        X509CertificateHolder holder = new X509CertificateHolder(x509Cert.getEncoded());
        SignerId signerId = new SignerId(holder.getIssuer(), x509Cert.getSerialNumber());
        return certStore.getMatches(signerId);
    }

}