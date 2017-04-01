package org.votingsystem.test.crypto.xml;

import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TimeStampToken;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.votingsystem.test.util.XMLUtils;
import org.votingsystem.throwable.ValidationException;
import org.xmlpull.v1.XmlPullParserException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignatureValidator {

    private static final Logger log = Logger.getLogger(SignatureValidator.class.getName());


    private String plainXML;
    private Document signedDocument;

    /**
     * Make sure that the document to sign:
     * - Hasn't empty tags (Replace selfclosing tags)
     * - Hasn't whitespace between elements.
     * - Has attributes in alphabetical order.
     */
    public SignatureValidator(byte[] bytesSigned) throws NoSuchAlgorithmException, IOException, XmlPullParserException {
        String documentStr = new String(bytesSigned, "UTF-8").replaceAll("<\\?xml(.+?)\\?>", "").trim();
        int signParts = documentStr.split("</ds:Signature>").length;
        documentStr = documentStr.split("<ds:Signature ")[0] + documentStr.split("</ds:Signature>")[signParts - 1];
        //make sure we replace selfclosing tags
        this.plainXML = documentStr.replaceAll("(?six)<(\\w+)([^<]*?)/>", "<$1$2></$1>").trim();
        log.info("plainXml: " + plainXML);
        signedDocument = XMLUtils.parse(bytesSigned);
    }

    public Set<XmlSignature> validate() throws Exception {
        Element rootElement = signedDocument.getRootElement();
        Set<XmlSignature> result = new HashSet<>();
        for(int i = 0; i < rootElement.getChildCount(); i++) {
            Object childElement = rootElement.getChild(i);
            if(childElement instanceof Element) {
                if(((Element)childElement).getName().equals("Signature")) {
                    result.add(validate((Element)childElement));
                }
            }
        }
        return result;
    }

    public XmlSignature validate(Element signatureElement) throws Exception {
        XmlSignature result = new XmlSignature();
        result.setSignatureId(signatureElement.getAttributeValue(null, "Id"));

        Element signedInfo = signatureElement.getElement(XAdESUtils.DS_NAMESPACE, "SignedInfo");
        result.setSignatureMethod(SignatureAlgorithm.forXML(signedInfo.getElement(null, "SignatureMethod")
                .getAttributeValue(null, "Algorithm")).getName());

        Element objectElement = signatureElement.getElement(XAdESUtils.DS_NAMESPACE, "Object");
        Element qualifyingProperties = objectElement.getElement(XAdESUtils.XADES_NAMESPACE, "QualifyingProperties");

        Element signedProperties = qualifyingProperties.getElement(XAdESUtils.XADES_NAMESPACE, "SignedProperties");
        Element signedSignatureProperties = signedProperties.getElement(null, "SignedSignatureProperties");

        result.setSigningCertificate(XAdESUtils.getSigningCertificate(signatureElement.getElement(null, "KeyInfo"),
                signedSignatureProperties.getElement(null, "SigningCertificate")));
        result.setSigningTime(DateUtils.getDate((String) signedSignatureProperties.getElement(
                XAdESUtils.XADES_NAMESPACE, "SigningTime").getChild(0)));
        result.setSignedPropertiesElement(qualifyingProperties.getElement(XAdESUtils.XADES_NAMESPACE, "SignedProperties"));

        Element signedDataObjectProperties = result.getSignedPropertiesElement().getElement(
                XAdESUtils.XADES_NAMESPACE, "SignedDataObjectProperties");
        for (int i = 0; i < signedDataObjectProperties.getChildCount(); i++) {
            if (signedDataObjectProperties.getChild(i) instanceof Element) {
                Element element = (Element) signedDataObjectProperties.getChild(i);
                if (element.getName().equals("DataObjectFormat") && ("#" + XAdESUtils.DOCUMENT_TO_SIGN_REFERENCE).equals(
                        element.getAttributeValue(null, "ObjectReference"))) {
                    result.setDocumentMimeType((String) element.getElement(XAdESUtils.XADES_NAMESPACE, "MimeType").getChild(0));
                }
            }
        }
        result.setSignedPropertiesElementCanonicalized(XAdESUtils.buildSignedPropertiesElement(result.getSigningCertificate(),
                result.getSignatureId(), result.getSigningTime(), result.getDocumentMimeType(), true));
        //we need to do this before validating the signature
        validateSignedInfo(signatureElement.getElement(XAdESUtils.DS_NAMESPACE, "SignedInfo"), result);

        Element signatureValue = signatureElement.getElement(XAdESUtils.DS_NAMESPACE, "SignatureValue");
        if (!("value-" + result.getSignatureId()).equals(signatureValue.getAttributeValue(null, "Id")))
            throw new ValidationException("Expected signature Id: value-" + result.getSignatureId() + " - found: " +
                    signatureValue.getAttributeValue(null, "Id"));
        byte[] signatureValueBytes = org.bouncycastle.util.encoders.Base64.decode(((String) signatureValue.getChild(0)).getBytes());

        Element signedInfoElementCanonicalized = XAdESUtils.buildSignedInfoElement(plainXML.getBytes(),
                result.getSignatureId(), result.getSigningCertificate(), result.getDocumentDigestAlgorithm(),
                result.getSignedPropertiesDigestAlgorithm(), result.getSigningTime(), result.getDocumentMimeType(), true);
        byte[] signedBytes = XMLUtils.serialize(signedInfoElementCanonicalized, false);
        Signature signature = Signature.getInstance(result.getSignatureMethod());
        signature.initVerify(result.getSigningCertificate().getPublicKey());
        signature.update(signedBytes);
        boolean signatureOk = signature.verify(signatureValueBytes);
        log.info("signature OK: " + signatureOk);
        return result;
    }

    private void validateSignedInfo(Element signedInfoElement, XmlSignature xmlSignature)
            throws NoSuchAlgorithmException, IOException, ValidationException {
        for (int i = 0; i < signedInfoElement.getChildCount(); i++) {
            if (signedInfoElement.getChild(i) instanceof Element) {
                Element referenceElement = (Element) signedInfoElement.getChild(i);
                if (referenceElement.getName().equals("Reference")) {
                    if (referenceElement.getAttributeValue(null, "Type").equals(
                            XAdESUtils.SIGNED_PROPERTIES_REFERENCE_TYPE)) {
                        validateSignedProperties(referenceElement, xmlSignature);
                    } else {
                        //check xml document digest
                        ReferenceDigestData referenceDigestData = new ReferenceDigestData(referenceElement);
                        xmlSignature.setDocumentDigestAlgorithm(referenceDigestData.getDigestMethod());
                        referenceDigestData.checkDigest(plainXML.getBytes());
                    }
                }
            }
        }
    }

    private Date getTimeStampDate(Element unsignedPropertiesElement) throws Exception {
        if(unsignedPropertiesElement != null) {
            Element unsignedSignaturePropertiesElement = unsignedPropertiesElement.getElement(
                    XAdESUtils.XADES_NAMESPACE, "UnsignedSignatureProperties");
            if(unsignedSignaturePropertiesElement != null) {
                Element signatureTimeStampElement = unsignedSignaturePropertiesElement.getElement(
                        XAdESUtils.XADES_NAMESPACE, "SignatureTimeStamp");
                if(signatureTimeStampElement != null) {
                    String base64TimeStamp = XMLUtils.getTextChild(signatureTimeStampElement, "EncapsulatedTimeStamp");
                    byte[] timeStamp = org.bouncycastle.util.encoders.Base64.decode(base64TimeStamp);
                    TimeStampToken testTsToken = new TimeStampToken(new CMSSignedData(timeStamp));
                    return testTsToken.getTimeStampInfo().getGenTime();
                }
            }
        }
        return null;
    }

    private void validateSignedProperties(Element referenceElement, XmlSignature xmlSignature) throws IOException,
            NoSuchAlgorithmException,
            ValidationException {
        byte[] content = XMLUtils.serialize(xmlSignature.getSignedPropertiesElementCanonicalized(), false);
        log.info("content: " + new String(content));
        ReferenceDigestData referenceDigestData = new ReferenceDigestData(referenceElement);
        xmlSignature.setSignedPropertiesDigestAlgorithm(referenceDigestData.getDigestMethod());
        referenceDigestData.checkDigest(content);
    }

    public static class ReferenceDigestData {

        private String digestMethod = null;
        private String digestValue = null;

        public ReferenceDigestData(Element referenceElement) {
            for (int j = 0; j < referenceElement.getChildCount(); j++) {
                if (referenceElement.getChild(j) instanceof Element) {
                    Element referenceChild = (Element) referenceElement.getChild(j);
                    if (referenceChild.getName().equals("DigestMethod")) {
                        digestMethod = DigestAlgorithm.forXML(referenceChild
                                .getAttributeValue(null, "Algorithm")).getName();
                    } else if (referenceChild.getName().equals("DigestValue")) {
                        digestValue = (String) referenceChild.getChild(0);
                    }
                }
            }
        }

        public String getDigestMethod() {
            return digestMethod;
        }

        public String getDigestValue() {
            return digestValue;
        }

        public void checkDigest(byte[] content) throws NoSuchAlgorithmException, ValidationException {
            MessageDigest messageDigest = MessageDigest.getInstance(digestMethod);
            messageDigest.update(content);
            byte[] digest = messageDigest.digest();
            String calculatedDigest = Base64.getEncoder().encodeToString(digest);
            if (!calculatedDigest.equals(digestValue)) {
                throw new ValidationException("Expected digestValue: " + digestValue +
                        " - calculated digest: " + calculatedDigest);
            }
        }
    }

}