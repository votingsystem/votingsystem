package org.sistemavotacion.seguridad;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CRLException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
* Casi todo el código sacado de:
* http://www.amazon.com/exec/obidos/redirect?path=ASIN/0764596330&link_code=as2&camp=1789&tag=bouncycastleo-20&creative=9325
*/
public class CertUtil {
    
    private static Logger logger = LoggerFactory.getLogger(CertUtil.class);
    
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
    
    /**
     * Genera un certificado V3 para usarlo como certificado raíz de una CA
     */
    public static X509Certificate generateV3RootCert(KeyPair pair, 
    		Date fechaIncio, Date fechaFin, String strSubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        logger.debug("strSubjectDN: " + strSubjectDN);
        X509Principal x509Principal = new X509Principal(strSubjectDN);          
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(x509Principal);
        certGen.setNotBefore(fechaIncio);
        certGen.setNotAfter(fechaFin);
        logger.debug("fechaIncio: " + fechaIncio.toString() + 
        		" - fechaFin: " + fechaFin.toString());
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
    		PrivateKey caKey, X509Certificate caCert, Date fechaInicio, 
    		Date fechaFin, String endEntitySubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(fechaInicio);
        certGen.setNotAfter(fechaFin);
        //"CN=Certificado de Actor - TasaTobin"
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
            PrivateKey caKey, X509Certificate caCert, Date fechaInicio, Date fechaFin,
            String strSubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        PublicKey requestPublicKey = csr.getPublicKey();
        X509Principal x509Principal = new X509Principal(strSubjectDN);  
        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
        logger.debug("generateV3EndEntityCertFromCsr::caCert.getSubjectX500Principal(): " + caCert.getSubjectX500Principal());
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(fechaInicio);
        certGen.setNotAfter(fechaFin);
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
        logger.debug("Verificacion OK");
        return cert;
    }

    public static byte[] fromX509CertToPEM (X509Certificate certificate) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(bOut));
        pemWrt.writeObject(certificate);
        pemWrt.close();
        bOut.close();
        return bOut.toByteArray();
    }
    
    public static byte[] fromX509CertCollectionToPEM (Collection<X509Certificate> certificates) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(bOut));
        for(X509Certificate certificate:certificates) {
        	pemWrt.writeObject(certificate);
        }
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
	
    public static Collection<X509Certificate> fromPEMToX509CertCollection (
    		byte[] pemChainFileBytes) throws Exception {
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
    
    /**
     * Verifies the validity of the given certificate, checking its signature
     * against the issuer's certificate.
     * 
     * @param cert
     *            the certificate to validate
     * @param trustedStore
     *            list of trusted (usually self-signed) certificates.
     * @param checkCRL
     *            boolean to tell system to check or not check CRL's
     * 
     * @return result 
     * 		   PKIXCertPathValidatorResult	if the certificate's signature is 
     * 		   valid and can be validated using a trustedCertficated, false otherwise.
     */
    public static PKIXCertPathValidatorResult verifyCertificate(X509Certificate cert, 
            Set<X509Certificate> trustedCerts, boolean checkCRL) throws Exception {
        Set<TrustAnchor> anchors = new HashSet<TrustAnchor>();
        for(X509Certificate certificate: trustedCerts) {
            TrustAnchor anchor = new TrustAnchor(certificate, null);
            anchors.add(anchor);
        }
        PKIXParameters params = new PKIXParameters(anchors);
        
        SVCertExtensionChecker checker = new SVCertExtensionChecker();
        params.addCertPathChecker(checker);        
        
        params.setRevocationEnabled(checkCRL); // if false tell system do not check CRL's
        CertPathValidator certPathValidator
            = CertPathValidator.getInstance("PKIX","BC");
        //List<Certificate> certificates = new ArrayList<Certificate>();
        //certificates.add(cert);
        CertificateFactory certFact = CertificateFactory.getInstance("X.509");
        CertPath certPath = certFact.generateCertPath(Arrays.asList(cert));
        CertPathValidatorResult result = certPathValidator.validate(certPath, params);
        // Get the CA used to validate this path
        //PKIXCertPathValidatorResult pkixResult = (PKIXCertPathValidatorResult)result;
        //TrustAnchor ta = pkixResult.getTrustAnchor();
        //X509Certificate certCaResult = ta.getTrustedCert();
        //logger.debug("certCaResult: " + certCaResult.getSubjectDN().toString()+
        //        "- numserie: " + certCaResult.getSerialNumber().longValue());
        return (PKIXCertPathValidatorResult)result;
    }
    
	/**
	 * Checks whether given X.509 certificate is self-signed.
	 * 
	 * http://www.nakov.com/blog/2009/12/01/x509-certificate-validation-in-java-build-and-verify-chain-and-verify-clr-with-bouncy-castle/
	 */
	public static boolean isSelfSigned(X509Certificate cert)
			throws CertificateException, NoSuchAlgorithmException,
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
	public static X509CRL downloadCRLFromWeb(String crlURL)
			throws MalformedURLException, IOException, CertificateException,
			CRLException {
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
	
	//To bypass id_kp_timeStamping ExtendedKeyUsage exception
	private static class SVCertExtensionChecker extends PKIXCertPathChecker {
		
		Set supportedExtensions;
		
		SVCertExtensionChecker() {
			supportedExtensions = new HashSet();
			supportedExtensions.add(X509Extensions.ExtendedKeyUsage);
		}
		
		public void init(boolean forward) throws CertPathValidatorException {
		 //To change body of implemented methods use File | Settings | File Templates.
	    }

		public boolean isForwardCheckingSupported(){
			return true;
		}

		public Set getSupportedExtensions()	{
			return null;
		}

		public void check(Certificate cert, Collection<String> unresolvedCritExts)
				throws CertPathValidatorException {
			for(String ext : unresolvedCritExts) {
				if(X509Extensions.ExtendedKeyUsage.toString().equals(ext)) {
					logger.debug("------------- ExtendedKeyUsage removed from validation");
					unresolvedCritExts.remove(ext);
				}
			}
		}

	}
	
}