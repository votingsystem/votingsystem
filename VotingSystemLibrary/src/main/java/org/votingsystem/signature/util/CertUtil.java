package org.votingsystem.signature.util;

import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.openssl.PasswordFinder;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.bouncycastle.x509.extension.SubjectKeyIdentifierStructure;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import net.sf.json.JSONObject;
import org.votingsystem.util.ExceptionVS;

import javax.security.auth.x500.X500Principal;
import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.*;
import java.util.*;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
* http://www.amazon.com/exec/obidos/redirect?path=ASIN/0764596330&link_code=as2&camp=1789&tag=bouncycastleo-20&creative=9325
*/
public class CertUtil {
    
    private static Logger logger = Logger.getLogger(CertUtil.class);

    /**
     * Generate V3 certificate for users
     */
    public static X509Certificate generateEndEntityCert(PublicKey entityKey, PrivateKey caKey, X509Certificate caCert,
            Date dateBegin, Date dateFinish, String endEntitySubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        certGen.setSerialNumber(VotingSystemKeyGenerator.INSTANCE.getSerno());
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        certGen.setSubjectDN(new X500Principal(endEntitySubjectDN));
        certGen.setPublicKey(entityKey);
        certGen.setSignatureAlgorithm(ContextVS.CERT_GENERATION_SIG_ALGORITHM);
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, new SubjectKeyIdentifierStructure(entityKey));
        certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(false));
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        return certGen.generate(caKey, ContextVS.PROVIDER);
    }

    /**
     * Generate V3 certificate for root CA Authority
     */
    public static X509Certificate generateV3RootCert(KeyPair pair, Date dateBegin, Date dateFinish,
             String strSubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        logger.debug("strSubjectDN: " + strSubjectDN);
        X509Principal x509Principal = new X509Principal(strSubjectDN);
        certGen.setSerialNumber(VotingSystemKeyGenerator.INSTANCE.getSerno());
        
        certGen.setIssuerDN(x509Principal);
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        logger.debug("dateBegin: " + dateBegin.toString() + " - dateFinish: " + dateFinish.toString());
        certGen.setSubjectDN(x509Principal);
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(ContextVS.CERT_GENERATION_SIG_ALGORITHM);
        //The following fragment shows how to create one which indicates that 
        //the certificate containing it is a CA and that only one certificate can follow in the certificate path.
        certGen.addExtension(X509Extensions.BasicConstraints, true, new BasicConstraints(true, 0));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(pair.getPublic()));
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return certGen.generate(pair.getPrivate(), ContextVS.PROVIDER);
    }
    
    /**
     * Genera un certificado V3 a partir de una CSR (Certificate Signing Request)
     */
    public static X509Certificate generateV3EndEntityCertFromCsr(PKCS10CertificationRequest csr, 
            PrivateKey caKey, X509Certificate caCert, Date dateBegin, Date dateFinish,
            String strSubjectDN) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        PublicKey requestPublicKey = csr.getPublicKey();
        X509Principal x509Principal = new X509Principal(strSubjectDN);
        certGen.setSerialNumber(VotingSystemKeyGenerator.INSTANCE.getSerno());
//        logger.debug("generateV3EndEntityCertFromCsr - SubjectX500Principal(): " + caCert.getSubjectX500Principal());
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        certGen.setSubjectDN(x509Principal);
        certGen.setPublicKey(requestPublicKey);
        certGen.setSignatureAlgorithm(ContextVS.CERT_GENERATION_SIG_ALGORITHM);
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(requestPublicKey));
        certGen.addExtension(X509Extensions.BasicConstraints, true, 
                new BasicConstraints(false));//Certificado final
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
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
        X509Certificate cert = certGen.generate(caKey, ContextVS.PROVIDER);
        cert.verify(caCert.getPublicKey());
        return cert;
    }

    /**
     * Genera un certificado V1 para usarlo como certificado raíz de una CA
     */
    public static X509Certificate generateV1RootCert(KeyPair pair, long comienzo, int period,
             String principal) throws Exception {
        X509V1CertificateGenerator  certGen = new X509V1CertificateGenerator();
        certGen.setSerialNumber(VotingSystemKeyGenerator.INSTANCE.getSerno());
        certGen.setIssuerDN(new X500Principal(principal));
        certGen.setNotBefore(new Date(comienzo));
        certGen.setNotAfter(new Date(comienzo + period));
        certGen.setSubjectDN(new X500Principal(principal));
        certGen.setPublicKey(pair.getPublic());
        certGen.setSignatureAlgorithm(ContextVS.CERT_GENERATION_SIG_ALGORITHM);
        return certGen.generate(pair.getPrivate(), ContextVS.PROVIDER);
    }

    /**
     * Genera un certificado V3 para usarlo como emisor de sellos de tiempo
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
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, 
                new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false, 
                new SubjectKeyIdentifierStructure(entityKey));
        certGen.addExtension(X509Extensions.BasicConstraints, true, 
                new BasicConstraints(false));
        certGen.addExtension(X509Extensions.KeyUsage, true, 
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        certGen.addExtension(X509Extensions.ExtendedKeyUsage, true,
                new ExtendedKeyUsage(new DERSequence(KeyPurposeId.id_kp_timeStamping)));
        return certGen.generate(caKey, ContextVS.PROVIDER);
    }

    /**
     * Generate V3 Certificate from CSR
     */
    public static X509Certificate signCSR(PKCS10CertificationRequest csr, String organizationalUnit, PrivateKey caKey,
               X509Certificate caCert, Date dateBegin, Date dateFinish, DERTaggedObject... certExtensions) throws Exception {
        String strSubjectDN = csr.getCertificationRequestInfo().getSubject().toString();
        if (!csr.verify() || strSubjectDN == null) throw new Exception("ERROR VERIFYING CSR");
        if(organizationalUnit != null) strSubjectDN = organizationalUnit + "," + strSubjectDN;
        X509Certificate issuedCert = generateV3EndEntityCertFromCsr(csr, caKey, caCert, dateBegin, dateFinish,
                strSubjectDN, certExtensions);
        return issuedCert;
    }

    /**
     * Genera un certificado V3 a partir de una CSR (Certificate Signing Request)
     */
    public static X509Certificate generateV3EndEntityCertFromCsr(PKCS10CertificationRequest csr,
            PrivateKey caKey, X509Certificate caCert, Date dateBegin, Date dateFinish, String strSubjectDN,
            DERTaggedObject... certExtensions) throws Exception {
        X509V3CertificateGenerator  certGen = new X509V3CertificateGenerator();
        PublicKey requestPublicKey = csr.getPublicKey();
        X509Principal x509Principal = new X509Principal(strSubjectDN);
        certGen.setSerialNumber(VotingSystemKeyGenerator.INSTANCE.getSerno());
        logger.debug("generateV3EndEntityCertFromCsr - SubjectX500Principal(): " + caCert.getSubjectX500Principal());
        certGen.setIssuerDN(PrincipalUtil.getSubjectX509Principal(caCert));
        certGen.setNotBefore(dateBegin);
        certGen.setNotAfter(dateFinish);
        certGen.setSubjectDN(x509Principal);
        certGen.setPublicKey(requestPublicKey);
        certGen.setSignatureAlgorithm(ContextVS.CERT_GENERATION_SIG_ALGORITHM);
        certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false, new AuthorityKeyIdentifierStructure(caCert));
        certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
                new SubjectKeyIdentifierStructure(requestPublicKey));
        certGen.addExtension(X509Extensions.BasicConstraints, true,
                new BasicConstraints(false));//Certificado final
        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        ASN1Set attributes = csr.getCertificationRequestInfo().getAttributes();
        if (attributes != null) {
            for (int i = 0; i != attributes.size(); i++) {
                if(attributes.getObjectAt(i) instanceof DERTaggedObject) {
                    DERTaggedObject taggedObject = (DERTaggedObject)attributes.getObjectAt(i);
                    ASN1ObjectIdentifier oid = new  ASN1ObjectIdentifier(ContextVS.VOTING_SYSTEM_BASE_OID +
                            taggedObject.getTagNo());
                    certGen.addExtension(oid, true, taggedObject);
                } else {
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
        }
        if(certExtensions != null) {
            for(DERTaggedObject taggedObject: certExtensions) {
                ASN1ObjectIdentifier oid = new  ASN1ObjectIdentifier(ContextVS.VOTING_SYSTEM_BASE_OID +
                        taggedObject.getTagNo());
                certGen.addExtension(oid, true, taggedObject);
            }
        }
        X509Certificate cert = certGen.generate(caKey, ContextVS.PROVIDER);
        cert.verify(caCert.getPublicKey());
        return cert;
    }

    public static byte[] getPEMEncoded (Object objectToEncode) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        PEMWriter pemWrt = new PEMWriter(new OutputStreamWriter(bOut));
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

    public static PKCS10CertificationRequest fromPEMToPKCS10CertificationRequest (byte[] csrBytes) throws Exception {
        PEMReader pemReader = new PEMReader(new InputStreamReader(new ByteArrayInputStream(csrBytes)));
        PKCS10CertificationRequest result = (PKCS10CertificationRequest)pemReader.readObject();
        pemReader.close();
        return result;
    }

    public static X509Certificate fromPEMToX509Cert (byte[] pemFileBytes) throws Exception {
        InputStream in = new ByteArrayInputStream(pemFileBytes);
        CertificateFactory fact = CertificateFactory.getInstance("X.509",ContextVS.PROVIDER);
        X509Certificate x509Cert = (X509Certificate)fact.generateCertificate(in);
        in.close();
        return x509Cert;
    }

    public static PrivateKey fromPEMToRSAPrivateKey(File file) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(file));
        KeyPair kp = (KeyPair) new PEMReader(br).readObject();
        br.close();
        return kp.getPrivate();
    }

    public static PrivateKey fromPEMToRSAPrivateKey(File file, PasswordFinder pFinder) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(file));
        KeyPair kp = (KeyPair) new PEMReader(br, pFinder).readObject();
        br.close();
        return kp.getPrivate();
    }

    public static Collection<X509Certificate> fromPEMToX509CertCollection (byte[] pemChainFileBytes) throws Exception {
        InputStream in = new ByteArrayInputStream(pemChainFileBytes);
        CertificateFactory fact = CertificateFactory.getInstance("X.509",ContextVS.PROVIDER);
        Collection<X509Certificate> x509Certs = (Collection<X509Certificate>)fact.generateCertificates(in);
        in.close();
        return x509Certs;
    }
    	
    public static X509Certificate loadCertificateFromStream (InputStream inputStream) throws Exception {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<X509Certificate> certificateChain =
                (Collection<X509Certificate>) certificateFactory.generateCertificates(inputStream);
        X509Certificate cert = certificateChain.iterator().next();
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
     * Verifies the validity of the given certificate, checking its signature
     * against the issuer's certificate.
     *
     * @param certs the certificate list to validate
     * @param anchors Set of trusted (usually self-signed) certificates.
     * @param checkCRL boolean to tell system to check or not check CRL's
     *
     * @return result PKIXCertPathValidatorResult	if the certificate's signature is
     * 		   valid and can be validated using a trustedCertificated, false otherwise.
     */
    public static ResponseVS verifyCertificate(Set<TrustAnchor> anchors,
             boolean checkCRL, List<X509Certificate> certs) throws Exception {
        try {
            PKIXParameters pkixParameters = new PKIXParameters(anchors);
            CertExtensionCheckerVS checker = new CertExtensionCheckerVS();
            pkixParameters.addCertPathChecker(checker);
            pkixParameters.setRevocationEnabled(checkCRL); // if false tell system do not check CRL's
            CertPathValidator certPathValidator = CertPathValidator.getInstance("PKIX", ContextVS.PROVIDER);
            CertificateFactory certFact = CertificateFactory.getInstance("X.509");
            CertPath certPath = certFact.generateCertPath(certs);
            CertPathValidatorResult result = certPathValidator.validate(certPath, pkixParameters);
            // Get the CA used to validate this path
            //PKIXCertPathValidatorResult pkixResult = (PKIXCertPathValidatorResult)result;
            //TrustAnchor ta = pkixResult.getTrustAnchor();
            //X509Certificate certCaResult = ta.getTrustedCert();
            //logger.debug("certCaResult: " + certCaResult.getSubjectDN().toString()+
            //        "- serialNumber: " + certCaResult.getSerialNumber().longValue());
            Map resultMap = new HashMap();
            resultMap.put("extensionChecker", checker);
            resultMap.put("pkixResult", (PKIXCertPathValidatorResult)result);
            ResponseVS response = new ResponseVS(ResponseVS.SC_OK);
            response.setData(resultMap);
            return response;
        } catch(Exception ex) {
            String msg = "Null cert list";
            if(certs != null && !certs.isEmpty()) msg = "Certificate validation failed with cert: " +
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

    public static JSONObject getCertExtensionData(X509Certificate x509Certificate, String extensionOID) throws IOException {
        JSONObject result = null;
        byte[] extensionValue =  x509Certificate.getExtensionValue(extensionOID);
        if(extensionValue == null) return null;
        DERObject derObject =  toDERObject(extensionValue);
        if (derObject instanceof DEROctetString) {
            DEROctetString derOctetString = (DEROctetString)derObject;
            DERTaggedObject derTaggedObject = (DERTaggedObject)toDERObject(derOctetString.getOctets());
            result = (JSONObject) JSONSerializer.toJSON(((DERUTF8String)derTaggedObject.getObject()).getString());
        }
        return result;
    }

    public static DERObject toDERObject(byte[] data) throws IOException, IOException {
        ByteArrayInputStream inStream = new ByteArrayInputStream(data);
        ASN1InputStream DIS = new ASN1InputStream(inStream);
        return DIS.readObject();
    }

}