package org.votingsystem.test.certificate;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509ExtensionUtils;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcDigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * https://cipherious.wordpress.com/2013/05/20/constructing-an-x-509-certificate-using-bouncy-castle/
 */
public class CertificateGenerator {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) throws OperatorCreationException, IOException, CertificateException, NoSuchAlgorithmException {
        //Generate Keypair
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA");
        kpGen.initialize(2048, random);
        KeyPair keyPair = kpGen.generateKeyPair();
        PublicKey RSAPubKey = keyPair.getPublic();
        PrivateKey RSAPrivateKey = keyPair.getPrivate();

        //Serial Number
        BigInteger serialNumber = BigInteger.valueOf(Math.abs(random.nextInt()));

        //Subject and Issuer DN
        X500Name subjectDN = new X500Name("C=US,O=Cyberdyne,OU=PKI,CN=SecureCA");
        X500Name issuerDN = new X500Name("C=US,O=Cyberdyne,OU=PKI,CN=SecureCA");

        //Validity
        Date notBefore = new Date(System.currentTimeMillis());
        Date notAfter = new Date(System.currentTimeMillis() + (((1000L*60*60*24*30))*12)*3);

        //SubjectPublicKeyInfo
        SubjectPublicKeyInfo subjPubKeyInfo = new SubjectPublicKeyInfo(ASN1Sequence.getInstance(RSAPubKey.getEncoded()));

        X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(issuerDN, serialNumber,
                notBefore, notAfter, subjectDN, subjPubKeyInfo);

        DigestCalculator digCalc = new BcDigestCalculatorProvider().get(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1));
        X509ExtensionUtils x509ExtensionUtils = new X509ExtensionUtils(digCalc);

        //Subject Key Identifier
        certGen.addExtension(Extension.subjectKeyIdentifier, false, x509ExtensionUtils.createSubjectKeyIdentifier(subjPubKeyInfo));

        //Authority Key Identifier
        certGen.addExtension(Extension.authorityKeyIdentifier, false, x509ExtensionUtils.createAuthorityKeyIdentifier(subjPubKeyInfo));

        //Key Usage
        certGen.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyCertSign | KeyUsage.cRLSign));

        //Extended Key Usage
        KeyPurposeId[] EKU = new KeyPurposeId[2];
        EKU[0] = KeyPurposeId.id_kp_emailProtection;
        EKU[1] = KeyPurposeId.id_kp_serverAuth;

        certGen.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(EKU));

        //Basic Constraints
        certGen.addExtension(Extension.basicConstraints, true, new BasicConstraints(0));

        //Certificate Policies
        PolicyInformation[] certPolicies = new PolicyInformation[2];
        certPolicies[0] = new PolicyInformation(new ASN1ObjectIdentifier("2.16.840.1.101.2.1.11.5"));
        certPolicies[1] = new PolicyInformation(new ASN1ObjectIdentifier("2.16.840.1.101.2.1.11.18"));

        certGen.addExtension(Extension.certificatePolicies, false, new CertificatePolicies(certPolicies));

        //Subject Alternative Name
        GeneralName[] genNames = new GeneralName[2];
        genNames[0] = new GeneralName(GeneralName.rfc822Name, new DERIA5String("john.smith@gmail.com"));
        genNames[1] = new GeneralName(GeneralName.directoryName, new X500Name("C=US,O=Cyberdyne,OU=PKI,CN=SecureCA"));

        certGen.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(genNames));

        //Authority Information Access
        //AccessDescription caIssuers = new AccessDescription(AccessDescription.id_ad_caIssuers,
        //        new GeneralName(GeneralName.uniformResourceIdentifier, new DERIA5String("http://www.somewebsite.com/ca.cer")));
        AccessDescription ocsp = new AccessDescription(AccessDescription.id_ad_ocsp,
                new GeneralName(GeneralName.uniformResourceIdentifier, new DERIA5String("http://ocsp.somewebsite.com")));

        ASN1EncodableVector aia_ASN = new ASN1EncodableVector();
        //aia_ASN.add(caIssuers);
        aia_ASN.add(ocsp);

        certGen.addExtension(Extension.authorityInfoAccess, false, new DERSequence(aia_ASN));

        //CRL Distribution Points
        DistributionPointName distPointOne = new DistributionPointName(new GeneralNames(
                new GeneralName(GeneralName.uniformResourceIdentifier,"http://crl.somewebsite.com/master.crl")));
        DistributionPointName distPointTwo = new DistributionPointName(new GeneralNames(
                new GeneralName(GeneralName.uniformResourceIdentifier,
                        "ldap://crl.somewebsite.com/cn%3dSecureCA%2cou%3dPKI%2co%3dCyberdyne%2cc%3dUS?certificaterevocationlist;binary")));

        DistributionPoint[] distPoints = new DistributionPoint[2];
        distPoints[0] = new DistributionPoint(distPointOne, null, null);
        distPoints[1] = new DistributionPoint(distPointTwo, null, null);

        certGen.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(distPoints));

        //Content Signer
        ContentSigner sigGen = new JcaContentSignerBuilder("SHA1WithRSAEncryption").setProvider("BC").build(RSAPrivateKey);

        //Certificate
        X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certGen.build(sigGen));

        //Output Certificate to file
        File destFile = new File("cert.crt");
        System.out.println("destFile.getAbsolutePath: " + destFile.getAbsolutePath());
        FileUtils.writeByteArrayToFile(destFile, certificate.getEncoded());
    }

}
