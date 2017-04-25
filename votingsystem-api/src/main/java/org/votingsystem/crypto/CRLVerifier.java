package org.votingsystem.crypto;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.*;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.throwable.ValidationException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;


public class CRLVerifier {

    /**
     * Extracts the CRL distribution points from the certificate (if available) and checks the certificate revocation
     * status against the CRLs coming from the distribution points.
     *
     * @param cert the certificate to be checked for revocation
     * @throws ValidationException if the certificate is revoked
     */
    public static void verifyCertificateCRLs(X509Certificate cert) throws ValidationException {
        try {
            String crlURL = getCRLURL(cert);
            ResponseDto response = HttpConn.getInstance().doGetRequest(crlURL, null);
            if(ResponseDto.SC_OK == response.getStatusCode()) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509CRL crl = (X509CRL) cf.generateCRL(new ByteArrayInputStream(response.getMessageBytes()));
                if (crl.isRevoked(cert)) {
                    throw new ValidationException("The certificate is revoked by CRL: " + crlURL);
                }
            }
            throw new ValidationException("Error fetching CRL from: " + crlURL);
        } catch (Exception ex) {
            throw new ValidationException( "Can not verify CRL for certificate: " + cert.getSubjectX500Principal(), ex);
        }
    }

    /**
     * Gets the URL of the Certificate Revocation List for a Certificate
     * @param certificate	the Certificate
     * @return	the String where you can check if the certificate was revoked
     * @throws CertificateParsingException
     * @throws IOException
     */
    public static String getCRLURL(X509Certificate certificate) throws CertificateParsingException {
        ASN1Primitive obj;
        try {
            obj = CertificateUtils.getExtensionValue(certificate, Extension.cRLDistributionPoints.getId());
        } catch (IOException e) {
            obj = null;
        }
        if (obj == null) {
            return null;
        }
        CRLDistPoint dist = CRLDistPoint.getInstance(obj);
        DistributionPoint[] dists = dist.getDistributionPoints();
        for (DistributionPoint p : dists) {
            DistributionPointName distributionPointName = p.getDistributionPoint();
            if (DistributionPointName.FULL_NAME != distributionPointName.getType()) {
                continue;
            }
            GeneralNames generalNames = (GeneralNames)distributionPointName.getName();
            GeneralName[] names = generalNames.getNames();
            for (GeneralName name : names) {
                if (name.getTagNo() != GeneralName.uniformResourceIdentifier) {
                    continue;
                }
                DERIA5String derStr = DERIA5String.getInstance((ASN1TaggedObject)name.toASN1Primitive(), false);
                return derStr.getString();
            }
        }
        return null;
    }

}