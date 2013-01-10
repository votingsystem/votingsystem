package org.sistemavotacion.seguridad;

import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.bouncycastle2.asn1.ASN1Sequence;
import org.bouncycastle2.asn1.ASN1Set;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.asn1.x509.Attribute;
import org.bouncycastle2.asn1.x509.X509Extensions;
import org.bouncycastle2.jce.PKCS10CertificationRequest;
import org.bouncycastle2.openssl.PEMReader;

import android.util.Log;

/**
 * PKCS10 wrapper class for the BouncyCastle {@link PKCS10CertificationRequest} object.
 * @author jgzornoza
 */
public class PKCS10WrapperServer {

    static public String MSG_FALLO_VERIFICACION = "Fallo en la verificación de CSR";
    
    private PrivateKey caKey;
    private X509Certificate caCert;
    
    public PKCS10WrapperServer(PrivateKey caKey, X509Certificate caCert) {  
        this.caKey = caKey;
        this.caCert = caCert;
    }

    public byte[] firmarCsr (byte[] csrPEMBytes, long comienzo, int periodoValidez) throws Exception {
        PKCS10CertificationRequest csr = fromPEMToPKCS10CertificationRequest(csrPEMBytes);
        if (!csr.verify() && validarCsr(csr)!= null) {
        	Log.i("", MSG_FALLO_VERIFICACION);
            return null;
        }
        byte[] cadenaCertificacion = obtenerCadenaCertificacion(csr, caKey,
                caCert, comienzo, periodoValidez);
        return cadenaCertificacion;
    }
    
    /**
     * Genera un certificado V3 para usarlo como certificado de usuario final
     */
    private byte[] obtenerCadenaCertificacion(PKCS10CertificationRequest csr, 
            PrivateKey caKey, X509Certificate caCert, long comienzo, int periodoValidez) 
            throws Exception {       
        String strSubjectDN = validarCsr(csr);
        X509Certificate issuedCert = CertUtil.generateV3EndEntityCertFromCsr(
                csr, caKey, caCert, comienzo, periodoValidez, strSubjectDN);
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
    public X509Extensions getX509Extensions(PKCS10CertificationRequest csr) {
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
    
    
    private String validarCsr (PKCS10CertificationRequest csr) {
    	Log.i("", "validarCsr - subject: " + csr.getCertificationRequestInfo().getSubject());
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