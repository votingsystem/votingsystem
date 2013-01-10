package org.sistemavotacion.seguridad;

import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.asn1.x509.Attribute;
import org.bouncycastle.openssl.PEMReader;

/**
 * PKCS10 wrapper class for the BouncyCastle {@link PKCS10CertificationRequest} object.
 * @author jgzornoza
 */
public class PKCS10WrapperServer {
    
    private static Logger logger = LoggerFactory.getLogger(PKCS10WrapperServer.class);

    static public String MSG_FALLO_VERIFICACION = "Fallo en la verificación de CSR";
    
    private PrivateKey caKey;
    private X509Certificate caCert;
    
    public PKCS10WrapperServer(PrivateKey caKey, X509Certificate caCert) {  
        this.caKey = caKey;
        this.caCert = caCert;
    }

    public byte[] firmarValidandoCsr (byte[] csrPEMBytes, Date fechaInicio, Date fechaFin) throws Exception {
        PKCS10CertificationRequest csr = fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        if (!csr.verify() && validarCsr(csr)!= null) {
            logger.error(MSG_FALLO_VERIFICACION);
            return null;
        }
        byte[] cadenaCertificacion = obtenerCadenaCertificacion(csr, caKey,
                caCert, fechaInicio, fechaFin);
        return cadenaCertificacion;
    }
    
    /**
     * Genera un certificado V3 para usarlo como certificado de usuario final
     */
    private byte[] obtenerCadenaCertificacion(PKCS10CertificationRequest csr, 
            PrivateKey caKey, X509Certificate caCert, Date fechaInicio, Date fechaFin) 
            throws Exception {       
        String strSubjectDN = validarCsr(csr);
        X509Certificate issuedCert = CertUtil.generateV3EndEntityCertFromCsr(
                csr, caKey, caCert, fechaInicio, fechaFin, strSubjectDN);
        byte[] issuedCertPemBytes = CertUtil.fromX509CertToPEM(issuedCert);
        byte[] caCertPemBytes = CertUtil.fromX509CertToPEM(caCert);
        byte[] c = new byte[issuedCertPemBytes.length + caCertPemBytes.length];
        //System.arraycopy(Object src, int srcPos, Object dest, int destPos, int length) 
        System.arraycopy(issuedCertPemBytes, 0, c, 0, issuedCertPemBytes.length);
        System.arraycopy(caCertPemBytes, 0, c, issuedCertPemBytes.length, caCertPemBytes.length);
        return c;
    }
    
        
    /**
     * Gets the X509Extensions included in the PKCS10.
     * 
     * @return The X509Extensions or <code>null</code> if there is no
     *         X509Extensions.
     */
    public static X509Extensions getX509Extensions(PKCS10CertificationRequest csr) {
        X509Extensions x509Extensions = null;
        ASN1Set attributes = csr.getCertificationRequestInfo().getAttributes();
        if (attributes.size() > 0) {
            ASN1Sequence attributeSequence = (ASN1Sequence) attributes.getObjectAt(0);
            Attribute attribute = new Attribute(attributeSequence);
            DERObjectIdentifier oid = attribute.getAttrType();
            if (oid.equals(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest)) {
                ASN1Set attributeValues = attribute.getAttrValues();
                if (attributeValues.size() > 0) {
                    ASN1Sequence x509extensionsSequence = (ASN1Sequence) attributeValues.getObjectAt(0);
                    x509Extensions = new X509Extensions(x509extensionsSequence);

                }
            }
        }
        return x509Extensions;
    }
    
    /**
     * Gets the X509 Extensions contained in a CSR (Certificate Signing Request).
     *
     * @param certificateSigningRequest the CSR.
     * @return the X509 Extensions in the request.
     * @throws CertificateException if the extensions could not be found.
     */
    public static X509Extensions getX509ExtensionsFromCsr(
          final PKCS10CertificationRequest certificateSigningRequest ) throws CertificateException {
       final CertificationRequestInfo certificationRequestInfo = certificateSigningRequest
             .getCertificationRequestInfo();
       final ASN1Set attributesAsn1Set = certificationRequestInfo.getAttributes();
       // The `Extension Request` attribute is contained within an ASN.1 Set,
       // usually as the first element.
       X509Extensions certificateRequestExtensions = null;
       for (int i = 0; i < attributesAsn1Set.size(); ++i) {
          // There should be only only one attribute in the set. (that is, only
          // the `Extension Request`, but loop through to find it properly)
          final DEREncodable derEncodable = attributesAsn1Set.getObjectAt( i );
          if (derEncodable instanceof DERSequence) {
             final Attribute attribute = new Attribute( (DERSequence) attributesAsn1Set
                   .getObjectAt( i ) );
             if (attribute.getAttrType().equals( PKCSObjectIdentifiers.pkcs_9_at_extensionRequest )){
                // The `Extension Request` attribute is present.
                final ASN1Set attributeValues = attribute.getAttrValues();
                // The X509Extensions are contained as a value of the ASN.1 Set.
                // Assume that it is the first value of the set.
                if (attributeValues.size() >= 1) {
                   certificateRequestExtensions = new X509Extensions( (ASN1Sequence) attributeValues
                         .getObjectAt( 0 ) );
                   // No need to search any more.
                   break;
                }
             }
          }
       }
       if (null == certificateRequestExtensions) {
          throw new CertificateException( "Could not obtain X509 Extensions from the CSR" );
       }
       return certificateRequestExtensions;
    }
    
    public static X509Extensions getX509Extensions(byte[] csrPEMBytes) throws Exception {
    	PKCS10CertificationRequest csr = fromPEMToPKCS10CertificationRequest(csrPEMBytes);
    	return getX509ExtensionsFromCsr(csr);
    }
     
    private String validarCsr (PKCS10CertificationRequest csr) {
        logger.debug("validarCsr - subject: " + csr.getCertificationRequestInfo().getSubject());
        String result = null;
        result = csr.getCertificationRequestInfo().getSubject().toString();
        return result;
    }
    
    public static PKCS10CertificationRequest fromPEMToPKCS10CertificationRequest (
            byte[] csrBytes) throws Exception {
        PEMReader pemReader = new PEMReader(new StringReader(new String(csrBytes)));
        Object pemObject = null;
        pemObject = pemReader.readObject();
        PKCS10CertificationRequest csr = (PKCS10CertificationRequest)pemObject;
        return csr;
    }

}