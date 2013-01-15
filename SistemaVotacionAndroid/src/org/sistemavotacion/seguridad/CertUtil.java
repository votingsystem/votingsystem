package org.sistemavotacion.seguridad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertStore;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Set;
import javax.mail.MessagingException;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle2.asn1.ASN1Set;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.asn1.x500.X500Name;
import org.bouncycastle2.asn1.x509.BasicConstraints;
import org.bouncycastle2.asn1.x509.KeyUsage;
import org.bouncycastle2.asn1.x509.X509Extension;
import org.bouncycastle2.asn1.x509.X509Extensions;
import org.bouncycastle2.cert.X509CertificateHolder;
import org.bouncycastle2.cert.X509v1CertificateBuilder;
import org.bouncycastle2.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle2.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.bouncycastle2.jce.PrincipalUtil;
import org.bouncycastle2.jce.X509Principal;
import org.bouncycastle2.openssl.PEMWriter;
import org.bouncycastle2.operator.ContentSigner;
import org.bouncycastle2.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle2.x509.X509V1CertificateGenerator;
import org.bouncycastle2.x509.X509V3CertificateGenerator;
import org.bouncycastle2.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle2.x509.extension.SubjectKeyIdentifierStructure;

import android.util.Log;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/blob/master/licencia.txt
* Casi todo el código sacado de:
* http://www.amazon.com/exec/obidos/redirect?path=ASIN/0764596330&link_code=as2&camp=1789&tag=bouncycastleo-20&creative=9325
*/
public class CertUtil {
    
    public static String ROOT_ALIAS = "root";
    public static String END_ENTITY_ALIAS = "end";
    public static final int PERIODO_VALIDEZ = 7 * 24 * 60 * 60 * 1000;
    
    static public String SIG_ALGORITHM = "SHA1WithRSAEncryption";
    
    /**
     * Genera un certificado V1 para usarlo como certificado raíz de una CA
     */
    public static X509Certificate generateV1RootCert(KeyPair pair, 
    		long comienzo, int periodoValidez, String principal) throws Exception {
        X509V1CertificateGenerator  certGen = new X509V1CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(new X500Principal(principal));
        certGen.setNotBefore(new Date(comienzo));
        certGen.setNotAfter(new Date(comienzo + periodoValidez));
        certGen.setSubjectDN(new X500Principal(principal));
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(SIG_ALGORITHM);
        return certGen.generate(pair.getPrivate(), "BC");
    }
    
    public static X509Certificate[] generateCertificate(KeyPair keyPair,Date fechaIncio, 
    		Date fechaFin, String principal) throws Exception {
		X509v1CertificateBuilder certGen = new JcaX509v1CertificateBuilder(
	   			new X500Name(principal),
	   			BigInteger.valueOf(System.currentTimeMillis()),
	   			fechaIncio, fechaFin, new X500Name(principal),
	   			keyPair.getPublic());
   		JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder("SHA256withRSA");
   		ContentSigner contentSigner = contentSignerBuilder.build(keyPair.getPrivate());  
   		X509CertificateHolder certHolder = certGen.build(contentSigner); 
   		JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter();
   		X509Certificate cert = certConverter.getCertificate(certHolder);
   		X509Certificate[] certs = {cert};
   		return certs;
	}
    
    /**
     * Genera un certificado V3 para usarlo como certificado raíz de una CA
     */
    public static X509Certificate generateV3RootCert(KeyPair pair, 
    		long comienzo, int periodoValidez, String strSubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        Log.i("CertUitil.strSubjectDN", strSubjectDN);
        X509Principal x509Principal = new X509Principal(strSubjectDN);          
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(x509Principal);
        certGen.setNotBefore(new Date(comienzo));
        certGen.setNotAfter(new Date(comienzo + periodoValidez));
        certGen.setSubjectDN(x509Principal);
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(SIG_ALGORITHM);
        //The following fragment shows how to create one which indicates that 
        //the certificate containing it is a CA and that only one certificate can follow in the certificate path.
        certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(true, 0));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(pair.getPublic()));
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return certGen.generate(pair.getPrivate(), "BC");
    }

    /**
     * Genera un certificado V3 para usarlo como certificado de usuario final
     */
    public static X509Certificate generateEndEntityCert(PublicKey entityKey, 
    		PrivateKey caKey, X509Certificate caCert, long comienzo, int periodoValidez,
                String endEntitySubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(new Date(comienzo));
        certGen.setNotAfter(new Date(comienzo + periodoValidez));
        certGen.setSubjectDN(new X500Principal(endEntitySubjectDN));
        certGen.setPublicKey(entityKey);
        certGen.setSignatureAlgorithm("SHA1WithRSAEncryption");        
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(entityKey));
        certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        return certGen.generate(caKey, "BC");
    }
    
    /**
     * Genera un certificado V3 para usarlo como certificado de usuario final a partir
     * de una 'certificate signing request'
     */
    public static X509Certificate generateV3EndEntityCertFromCsr(PKCS10CertificationRequest csr, 
            PrivateKey caKey, X509Certificate caCert, long comienzo, int periodoValidez,
            String strSubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        PublicKey requestPublicKey = csr.getPublicKey();
        X509Principal x509Principal = new X509Principal(strSubjectDN);  
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        Log.i("generateV3EndEntityCertFromCsr::caCert.getSubjectX500Principal(): ", 
        		caCert.getSubjectX500Principal().toString());
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(new Date(comienzo));
        certGen.setNotAfter(new Date(comienzo + periodoValidez));
        certGen.setSubjectDN(x509Principal);
        certGen.setPublicKey(requestPublicKey);
        certGen.setSignatureAlgorithm(SIG_ALGORITHM);
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(requestPublicKey));
        certGen.addExtension(X509Extensions.BasicConstraints, true, 
                new BasicConstraints(false));//Certificado final
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        ASN1Set attributes = csr.getCertificationRequestInfo().getAttributes();
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
                }
            }
        }
        X509Certificate cert = certGen.generate(caKey, "BC");
        cert.verify(caCert.getPublicKey());
        Log.i("CertUtil.generateV3EndEntityCertFromCsr", "Verificacion OK");
        return cert;
    }

   public static CertPath verifyCertificate(X509Certificate cert, CertStore store, KeyStore trustedStore) 
        throws InvalidAlgorithmParameterException, KeyStoreException, MessagingException, CertPathBuilderException {
         
        if (cert == null || store == null || trustedStore == null) 
            throw new IllegalArgumentException("cert == "+cert+", store == "+store+", trustedStore == "+trustedStore);

        CertPathBuilder pathBuilder;

        // I create the CertPathBuilder object. It will be used to find a
        // certification path that starts from the signer's certificate and
        // leads to a trusted root certificate.
        try {
            pathBuilder = CertPathBuilder.getInstance("PKIX", "BC");
        } catch (Exception e) {
            throw new MessagingException("Error during the creation of the certpathbuilder.", e);
        }

        X509CertSelector xcs = new X509CertSelector();
        xcs.setCertificate(cert);
        //PKIXBuilderParameters(Set<TrustAnchor> trustAnchors, CertSelector targetConstraints) 
        PKIXBuilderParameters params = new PKIXBuilderParameters(trustedStore, xcs);
        params.addCertStore(store);
        params.setRevocationEnabled(false);
        try {
            CertPathBuilderResult result = pathBuilder.build(params);
            CertPath path = result.getCertPath();
            return path;
        } catch (CertPathBuilderException e) {
            // A certification path is not found, so null is returned.
            return null;
        } catch (InvalidAlgorithmParameterException e) {
            // If this exception is thrown an error has occured during
            // certification path search. 
            throw new MessagingException("Error during the certification path search.", e);
        }
        
    }
    
        /**
     * Verifies the validity of the given certificate, checking its signature
     * against the issuer's certificate.
     * 
     * @param cert
     *            the certificate to validate
     * @param store
     *            other certificates that can be used to create a chain of trust
     *            to a known trusted certificate.
     * @param trustedStore
     *            list of trusted (usually self-signed) certificates.
     * 
     * @return true if the certificate's signature is valid and can be validated
     *         using a trustedCertficated, false otherwise.
     */
    public static CertPath verifyCertificate(
            X509Certificate cert, CertStore store, Set<TrustAnchor> trustAnchors) 
        throws InvalidAlgorithmParameterException, KeyStoreException, MessagingException, CertPathBuilderException {
         
        if (cert == null || store == null || trustAnchors == null) 
            throw new IllegalArgumentException("cert == "+cert+", store == "+store+", trustAnchors == "+trustAnchors);

        CertPathBuilder pathBuilder;

        // I create the CertPathBuilder object. It will be used to find a
        // certification path that starts from the signer's certificate and
        // leads to a trusted root certificate.
        try {
            pathBuilder = CertPathBuilder.getInstance("PKIX", "BC");
        } catch (Exception e) {
            throw new MessagingException("Error during the creation of the certpathbuilder.", e);
        }

        X509CertSelector xcs = new X509CertSelector();
        xcs.setCertificate(cert);
        //PKIXBuilderParameters(Set<TrustAnchor> trustAnchors, CertSelector targetConstraints) 
        PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, xcs);
        params.addCertStore(store);
        params.setRevocationEnabled(false);
        try {
            CertPathBuilderResult result = pathBuilder.build(params);
            CertPath path = result.getCertPath();
            return path;
        } catch (CertPathBuilderException e) {
        	Log.e("CertUtil.verifyCertificate", e.getMessage(), e);
            // A certification path is not found, so null is returned.
            return null;
        } catch (InvalidAlgorithmParameterException e) {
        	Log.e("CertUtil.verifyCertificate", e.getMessage(), e);
            // If this exception is thrown an error has occured during
            // certification path search. 
            throw new MessagingException("Error during the certification path search.", e);
        }
        
    }
    
    public static byte[] fromX509CertToPEM (X509Certificate certificate) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(bOut));
        pemWrt.writeObject(certificate);
        pemWrt.close();
        bOut.close();
        return bOut.toByteArray();
    }

    public static X509Certificate fromPEMToX509Cert (byte[] pemFileBytes) throws Exception {
        InputStream in = new ByteArrayInputStream(pemFileBytes);
        CertificateFactory fact = CertificateFactory.getInstance("X.509","BC");
        X509Certificate x509Cert = (X509Certificate)fact.generateCertificate(in);
        return x509Cert;
    }
	
    public static Collection<X509Certificate> fromPEMChainToX509Certs (byte[] pemChainFileBytes) throws Exception {
        InputStream in = new ByteArrayInputStream(pemChainFileBytes);
        CertificateFactory fact = CertificateFactory.getInstance("X.509","BC");
        Collection<X509Certificate> x509Certs = 
                (Collection<X509Certificate>)fact.generateCertificates(in);
        return x509Certs;
    }
    	
    public static X509Certificate loadCertificateFromStream (InputStream inputStream) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<X509Certificate> certificateChain =
                (Collection<X509Certificate>) certificateFactory.generateCertificates(inputStream);
        X509Certificate cert = certificateChain.iterator().next();
        return cert;
    }
    
}
