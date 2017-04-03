package org.votingsystem.testlib.xml;

import eu.europa.esig.dss.DigestAlgorithm;
import org.bouncycastle.util.encoders.Base64;
import org.votingsystem.crypto.HashUtils;
import org.votingsystem.testlib.util.XMLUtils;
import org.votingsystem.util.Constants;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Date;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XAdESUtils {


    public static final String DS_NAMESPACE = "http://www.w3.org/2000/09/xmldsig#";
    public static final String XADES_NAMESPACE = "http://uri.etsi.org/01903/v1.3.2#";
    public static final String XAdES132 = "http://uri.etsi.org/01903/v1.3.2#";
    public static final String CANONICALIZATION_METHOD = "http://www.w3.org/2001/10/xml-exc-c14n#";
    public static final String SIGNED_PROPERTIES_REFERENCE_TYPE = "http://uri.etsi.org/01903#SignedProperties";
    public static final String DATA_DIGEST_ALGORITHM = "SHA-256";
    public static final String DOCUMENT_TO_SIGN_REFERENCE = "r-id-1";
    public static final String XML_MIME_TYPE = "text/xml";

    public static X509Certificate getSigningCertificate(Element KeyInfoElement, Element signingCertificateElement)
            throws Exception {
        Element x509DataElement = KeyInfoElement.getElement(null, "X509Data");
        Element certDigestElement = signingCertificateElement.getElement(null, "Cert").getElement(null, "CertDigest");
        String digestMethodId = certDigestElement.getElement(null, "DigestMethod").getAttributeValue(null, "Algorithm");
        String digestMethod = DigestAlgorithm.forXML(digestMethodId).getName();
        String digestValue = (String) certDigestElement.getElement(null, "DigestValue").getChild(0);
        for (int i = 0; i < x509DataElement.getChildCount(); i++) {
            Element x509Certificate = x509DataElement.getElement(i);
            byte[] certBytes = Base64.decode(((String) x509Certificate.getChild(0)).getBytes());
            String certDigest = HashUtils.getHashBase64(certBytes, digestMethod);
            if (digestValue.equals(certDigest)) {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certBytes));
            }
        }
        return null;
    }

    public static Element buildSigningCertificateElement(X509Certificate signingCertificate,
                                                         boolean canonicalized) throws NoSuchAlgorithmException, CertificateEncodingException {
        Document doc = new Document();
        Element signingCertificateElement = doc.createElement("", "xades:SigningCertificate");
        Element certElement = doc.createElement("", "xades:Cert");
        signingCertificateElement.addChild(Node.ELEMENT, certElement);
        Element certDigestElement = doc.createElement("", "xades:CertDigest");
        certElement.addChild(Node.ELEMENT, certDigestElement);
        Element digestMethodElement = doc.createElement("", "ds:DigestMethod");
        if (canonicalized) {
            digestMethodElement.setAttribute(null, "xmlns:ds", DS_NAMESPACE);
            digestMethodElement.addChild(Node.TEXT, "");
        }
        digestMethodElement.setAttribute(null, "Algorithm", DigestAlgorithm.SHA1.getXmlId());
        Element digestValueElement = doc.createElement("", "ds:DigestValue");
        if (canonicalized) {
            digestValueElement.setAttribute(null, "xmlns:ds", DS_NAMESPACE);
        }
        digestValueElement.addChild(Node.TEXT, HashUtils.getHashBase64(signingCertificate.getEncoded(), "SHA-1"));
        certDigestElement.addChild(Node.ELEMENT, digestMethodElement);
        certDigestElement.addChild(Node.ELEMENT, digestValueElement);

        Element issuerSerialElement = doc.createElement("", "xades:IssuerSerial");
        Element x509IssuerNameElement = doc.createElement("", "ds:X509IssuerName");
        if (canonicalized) {
            x509IssuerNameElement.setAttribute(null, "xmlns:ds", DS_NAMESPACE);
        }
        x509IssuerNameElement.addChild(Node.TEXT, signingCertificate.getIssuerX500Principal().getName());
        Element x509SerialNumberElement = doc.createElement("", "ds:X509SerialNumber");
        if (canonicalized) {
            x509SerialNumberElement.setAttribute(null, "xmlns:ds", DS_NAMESPACE);
        }
        x509SerialNumberElement.addChild(Node.TEXT, signingCertificate.getSerialNumber().toString());
        issuerSerialElement.addChild(Node.ELEMENT, x509IssuerNameElement);
        issuerSerialElement.addChild(Node.ELEMENT, x509SerialNumberElement);
        certElement.addChild(Node.ELEMENT, issuerSerialElement);

        return signingCertificateElement;
    }

    public static Element buildSignedDataObjectPropertiesElement(String documentToSignMimeType) {
        Document doc = new Document();
        Element signedDataObjectPropertiesElement = doc.createElement("", "xades:SignedDataObjectProperties");
        Element dataObjectFormatElement = doc.createElement("", "xades:DataObjectFormat");
        dataObjectFormatElement.setAttribute(null, "ObjectReference", "#" + XAdESUtils.DOCUMENT_TO_SIGN_REFERENCE);
        Element mimeTypeElement = doc.createElement("", "xades:MimeType");
        mimeTypeElement.addChild(Node.TEXT, documentToSignMimeType);
        dataObjectFormatElement.addChild(Node.ELEMENT, mimeTypeElement);
        signedDataObjectPropertiesElement.addChild(Node.ELEMENT, dataObjectFormatElement);
        return signedDataObjectPropertiesElement;
    }

    public static Element buildSignedPropertiesElement(X509Certificate signingCertificate, String signatureId,
                                                       Date signingTime, String documentToSignMimeType, boolean canonicalized) throws NoSuchAlgorithmException,
            CertificateEncodingException {
        Document doc = new Document();
        Element signedPropertiesElement = doc.createElement("", "xades:SignedProperties");
        if (canonicalized) {
            signedPropertiesElement.setAttribute(null, "xmlns:xades", XAdESUtils.XADES_NAMESPACE);
        }
        signedPropertiesElement.setAttribute(null, "Id", "xades-" + signatureId);

        Element signedSignaturePropertiesElement = doc.createElement("", "xades:SignedSignatureProperties");

        Element signingTimeElement = doc.createElement("", "xades:SigningTime");
        signingTimeElement.addChild(Node.TEXT, DateUtils.getISODateStr(signingTime));

        signedSignaturePropertiesElement.addChild(Node.ELEMENT, signingTimeElement);
        signedSignaturePropertiesElement.addChild(Node.ELEMENT,
                XAdESUtils.buildSigningCertificateElement(signingCertificate, canonicalized));

        signedPropertiesElement.addChild(Node.ELEMENT, signedSignaturePropertiesElement);
        signedPropertiesElement.addChild(Node.ELEMENT,
                XAdESUtils.buildSignedDataObjectPropertiesElement(documentToSignMimeType));

        return signedPropertiesElement;
    }

    public static Element buildSignedInfoElement(byte[] documentToSignBytes, String signatureId,
                                                 X509Certificate signingCertificate, String documentDigestAlgorithm, String signedPropertiesDigestAlgorithm,
                                                 Date signingTime, String documentToSignMimeType, boolean canonicalized) throws NoSuchAlgorithmException,
            IOException, CertificateEncodingException, ParseException {
        Document doc = new Document();
        Element signedInfoElement = doc.createElement("", "ds:SignedInfo");
        if (canonicalized) {
            signedInfoElement.setAttribute(null, "xmlns:ds", XAdESUtils.DS_NAMESPACE);
        }
        Element canonicalizationMethodElement = doc.createElement("", "ds:CanonicalizationMethod");
        canonicalizationMethodElement.setAttribute(null, "Algorithm", XAdESUtils.CANONICALIZATION_METHOD);
        if (canonicalized) {
            canonicalizationMethodElement.addChild(Node.TEXT, "");
        }
        signedInfoElement.addChild(Node.ELEMENT, canonicalizationMethodElement);

        Element signatureMethodElement = doc.createElement("", "ds:SignatureMethod");
        signatureMethodElement.setAttribute(null, "Algorithm", SignatureAlgorithm.RSA_SHA_256.getXmlId());
        if (canonicalized) {
            signatureMethodElement.addChild(Node.TEXT, "");
        }
        signedInfoElement.addChild(Node.ELEMENT, signatureMethodElement);

        signedInfoElement.addChild(Node.ELEMENT, buildDocumentToSignReferenceElement(documentToSignBytes,
                documentDigestAlgorithm, canonicalized));
        signedInfoElement.addChild(Node.ELEMENT, buildSignedPropertiesReferenceElement(signingCertificate, signatureId,
                signedPropertiesDigestAlgorithm, signingTime, documentToSignMimeType, canonicalized));
        return signedInfoElement;
    }

    private static Element buildDocumentToSignReferenceElement(byte[] documentToSignBytes, String digestAlgorithm,
                                                               boolean canonicalized) throws NoSuchAlgorithmException {
        Document doc = new Document();
        Element referenceElement = doc.createElement("", "ds:Reference");
        referenceElement.setAttribute(null, "Id", XAdESUtils.DOCUMENT_TO_SIGN_REFERENCE);
        referenceElement.setAttribute(null, "Type", "");
        referenceElement.setAttribute(null, "URI", "");

        Element transformsElement = doc.createElement("", "ds:Transforms");
        Element transform1Element = doc.createElement("", "ds:Transform");
        transform1Element.setAttribute(null, "Algorithm", "http://www.w3.org/TR/1999/REC-xpath-19991116");

        Element xPathElement = doc.createElement("", "ds:XPath");
        xPathElement.addChild(Node.TEXT, "not(ancestor-or-self::ds:Signature)");
        transform1Element.addChild(Node.ELEMENT, xPathElement);

        Element transformAlgorithmElement = doc.createElement("", "ds:Transform");
        transformAlgorithmElement.setAttribute(null, "Algorithm", XAdESUtils.CANONICALIZATION_METHOD);
        if (canonicalized) {
            transformAlgorithmElement.addChild(Node.TEXT, "");
        }

        transformsElement.addChild(Node.ELEMENT, transform1Element);
        transformsElement.addChild(Node.ELEMENT, transformAlgorithmElement);

        referenceElement.addChild(Node.ELEMENT, transformsElement);

        Element digestMethodElement = doc.createElement("", "ds:DigestMethod");
        if (canonicalized) {
            digestMethodElement.addChild(Node.TEXT, "");
        }
        digestMethodElement.setAttribute(null, "Algorithm", DigestAlgorithm.SHA256.getXmlId());
        referenceElement.addChild(Node.ELEMENT, digestMethodElement);

        Element digestValueElement = doc.createElement("", "ds:DigestValue");
        MessageDigest messageDigest = MessageDigest.getInstance(digestAlgorithm);
        messageDigest.update(documentToSignBytes);
        byte[] digest = messageDigest.digest();
        digestValueElement.addChild(Node.TEXT, java.util.Base64.getEncoder().encodeToString(digest));
        referenceElement.addChild(Node.ELEMENT, digestValueElement);
        return referenceElement;
    }

    public static Element buildSignedPropertiesReferenceElement(X509Certificate signingCertificate, String signatureId,
                                                                String digestAlgorithm, Date signingTime, String documentToSignMimeType, boolean canonicalized)
            throws NoSuchAlgorithmException, IOException, CertificateEncodingException, ParseException {
        Document doc = new Document();
        Element referenceElement = doc.createElement("", "ds:Reference");
        referenceElement.setAttribute(null, "Type", SIGNED_PROPERTIES_REFERENCE_TYPE);
        referenceElement.setAttribute(null, "URI", "#xades-" + signatureId);

        Element transformsElement = doc.createElement("", "ds:Transforms");
        Element transformAlgorithmElement = doc.createElement("", "ds:Transform");
        transformAlgorithmElement.setAttribute(null, "Algorithm", XAdESUtils.CANONICALIZATION_METHOD);
        if (canonicalized) {
            transformAlgorithmElement.addChild(Node.TEXT, "");
        }
        transformsElement.addChild(Node.ELEMENT, transformAlgorithmElement);
        referenceElement.addChild(Node.ELEMENT, transformsElement);

        Element digestMethodElement = doc.createElement("", "ds:DigestMethod");
        digestMethodElement.setAttribute(null, "Algorithm", DigestAlgorithm.forName(digestAlgorithm).getXmlId());
        if (canonicalized) {
            digestMethodElement.addChild(Node.TEXT, "");
        }
        referenceElement.addChild(Node.ELEMENT, digestMethodElement);

        Element digestValueElement = doc.createElement("", "ds:DigestValue");
        referenceElement.addChild(Node.ELEMENT, digestValueElement);
        Element signedPropertiesElement = XAdESUtils.buildSignedPropertiesElement(signingCertificate,
                signatureId, signingTime, documentToSignMimeType, true);
        byte[] signedPropertiesBytes = XMLUtils.serialize(signedPropertiesElement, false);
        digestValueElement.addChild(Node.TEXT, HashUtils.getHashBase64(
                signedPropertiesBytes, Constants.DATA_DIGEST_ALGORITHM));
        return referenceElement;
    }
}