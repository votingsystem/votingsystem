package org.votingsystem.test.crypto.xml;


import eu.europa.esig.dss.DigestAlgorithm;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.kxml2.kdom.Node;
import org.votingsystem.crypto.HashUtils;
import org.votingsystem.crypto.MockDNIe;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.ContentType;
import org.votingsystem.http.HttpConn;
import org.votingsystem.test.util.XMLUtils;
import org.votingsystem.util.Constants;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Licence: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignatureBuilder {

    private static final Logger log = Logger.getLogger(SignatureBuilder.class.getName());

    private byte[] documentToSignBytes;
    private String signatureId;
    private Element signatureValueElementCanonicalized;
    private String signatureAlgorithm;
    private String documentToSignMimeType;
    private String timeStampServiceURL;
    private Document doc;
    private Date signingTime;
    private PrivateKey privateKey;
    private X509Certificate signingCertificate;
    private List<X509Certificate> signingCertificateChain;

    /**
     * Make sure that the document to sign:
     * - Hasn't empty tags (Replace selfclosing tags)
     * - Hasn't whitespace between elements.
     * - Has attributes in alphabetical order.
     *
     * @param documentToSignBytes
     * @param documentToSignMimeType
     * @param signatureAlgorithm
     * @param privateKey
     * @param signingCertificate
     * @param signingCertificateChain
     * @param timeStampServiceURL     p.e: https://voting.ddns.net/timestamp-server/api/timestamp
     * @throws IOException
     * @throws ParseException
     */
    public SignatureBuilder(byte[] documentToSignBytes, String documentToSignMimeType,
            String signatureAlgorithm, PrivateKey privateKey, X509Certificate signingCertificate,
            List<X509Certificate> signingCertificateChain, String timeStampServiceURL) throws IOException, ParseException {
        Security.addProvider(new BouncyCastleProvider());
        this.documentToSignBytes = documentToSignBytes;
        this.documentToSignMimeType = documentToSignMimeType;
        this.signatureAlgorithm = signatureAlgorithm;
        doc = new Document();
        signatureId = "id-" + UUID.randomUUID().toString().replace("-", "");
        this.privateKey = privateKey;
        this.signingCertificate = signingCertificate;
        this.signingCertificateChain = signingCertificateChain;
        signingTime = new Date();
        this.timeStampServiceURL = timeStampServiceURL;
    }

    /**
     * Make sure that the document to sign:
     * - Hasn't empty tags (Replace selfclosing tags)
     * - Hasn't whitespace between elements.
     * - Has attributes in alphabetical order.
     */
    public SignatureBuilder(byte[] documentToSignBytes, String documentToSignMimeType, String signatureAlgorithm,
            MockDNIe mockDNIe, String timeStampServiceURL) throws IOException, ParseException {
        Security.addProvider(new BouncyCastleProvider());
        this.documentToSignBytes = documentToSignBytes;
        this.documentToSignMimeType = documentToSignMimeType;
        this.signatureAlgorithm = signatureAlgorithm;
        doc = new Document();
        signatureId = "id-" + UUID.randomUUID().toString().replace("-", "");
        this.privateKey = mockDNIe.getPrivateKey();
        this.signingCertificate = mockDNIe.getX509Certificate();
        this.signingCertificateChain = mockDNIe.getX509CertificateChain();
        this.timeStampServiceURL = timeStampServiceURL;
    }

    public byte[] build() throws Exception {
        signingTime = new Date();
        Element signatureElement = doc.createElement("", "ds:Signature");
        signatureElement.setAttribute(null, "Id", signatureId);
        signatureElement.setAttribute(null, "xmlns:ds", XAdESUtils.DS_NAMESPACE);

        signatureElement.addChild(Node.ELEMENT, XAdESUtils.buildSignedInfoElement(documentToSignBytes,
                signatureId, signingCertificate, XAdESUtils.DATA_DIGEST_ALGORITHM,
                XAdESUtils.DATA_DIGEST_ALGORITHM, signingTime, documentToSignMimeType, false));

        signatureElement.addChild(Node.ELEMENT, buildSignatureValueElement());
        signatureElement.addChild(Node.ELEMENT, buildKeyInfoElement());
        signatureElement.addChild(Node.ELEMENT, buildQualifyingPropertiesElement());

        doc.addChild(Node.ELEMENT, signatureElement);
        byte[] signatureBytes = XMLUtils.serialize(doc, false);

        org.kxml2.kdom.Document document = XMLUtils.parse(documentToSignBytes);
        Element rootElement = document.getRootElement();
        org.kxml2.kdom.Document signatureDocument = XMLUtils.parse(signatureBytes);
        rootElement.addChild(Node.ELEMENT, signatureDocument.getRootElement());

        return XMLUtils.serialize(document);
    }

    private Element buildSignatureValueElement() throws CertificateEncodingException, IOException,
            NoSuchAlgorithmException, InvalidKeyException, SignatureException, ParseException {
        Element signatureValueElement = doc.createElement("", "ds:SignatureValue");
        signatureValueElement.setAttribute(null, "Id", "value-" + signatureId);

        signatureValueElementCanonicalized = doc.createElement("", "ds:SignatureValue");
        signatureValueElementCanonicalized.setAttribute(null, "xmlns:ds", XAdESUtils.DS_NAMESPACE);
        signatureValueElementCanonicalized.setAttribute(null, "Id", "value-" + signatureId);

        Element signedInfoElementCanonicalized = XAdESUtils.buildSignedInfoElement(documentToSignBytes, signatureId,
                signingCertificate, XAdESUtils.DATA_DIGEST_ALGORITHM, XAdESUtils.DATA_DIGEST_ALGORITHM,
                signingTime, documentToSignMimeType, true);
        byte[] bytesToSign = XMLUtils.serialize(signedInfoElementCanonicalized, false);
        log.info("bytesToSign: " + new String(bytesToSign));

        Signature sig = Signature.getInstance(signatureAlgorithm);
        sig.initSign(privateKey);
        sig.update(bytesToSign);
        byte[] signedBytes = sig.sign();
        byte[] signedBytesEncoded = org.bouncycastle.util.encoders.Base64.encode(signedBytes);
        signatureValueElement.addChild(Node.TEXT, new String(signedBytesEncoded));
        signatureValueElementCanonicalized.addChild(Node.TEXT, new String(signedBytesEncoded));
        return signatureValueElement;
    }

    private Element buildKeyInfoElement() throws CertificateEncodingException {
        Element keyInfoElement = doc.createElement("", "ds:KeyInfo");
        Element x509DataElement = doc.createElement("", "ds:X509Data");
        for (X509Certificate certificate : signingCertificateChain) {
            Element X509CertificateElement = doc.createElement("", "ds:X509Certificate");
            X509CertificateElement.addChild(Node.TEXT, Base64.getEncoder().encodeToString(certificate.getEncoded()));
            x509DataElement.addChild(Node.ELEMENT, X509CertificateElement);
        }
        keyInfoElement.addChild(Node.ELEMENT, x509DataElement);
        return keyInfoElement;
    }

    private Element buildQualifyingPropertiesElement() throws Exception {
        Element objectElement = doc.createElement("", "ds:Object");
        Element qualifyingPropertiesElement = doc.createElement("", "xades:QualifyingProperties");
        qualifyingPropertiesElement.setAttribute(null, "xmlns:xades", XAdESUtils.XAdES132);
        qualifyingPropertiesElement.setAttribute(null, "Target", "#" + signatureId);
        qualifyingPropertiesElement.addChild(Node.ELEMENT, XAdESUtils.buildSignedPropertiesElement(signingCertificate,
                signatureId, signingTime, documentToSignMimeType, false));
        qualifyingPropertiesElement.addChild(Node.ELEMENT, buildUnsignedProperties());

        objectElement.addChild(Node.ELEMENT, qualifyingPropertiesElement);
        return objectElement;
    }

    private Element buildUnsignedProperties() throws Exception {
        String timeStampId = UUID.randomUUID().toString();
        Element unsignedPropertiesElement = doc.createElement("", "xades:UnsignedProperties");
        Element unsignedSignaturePropertiesElement = doc.createElement("", "xades:UnsignedSignatureProperties");
        unsignedPropertiesElement.addChild(Node.ELEMENT, unsignedSignaturePropertiesElement);
        Element signatureTimeStampElement = doc.createElement("", "xades:SignatureTimeStamp");
        signatureTimeStampElement.setAttribute(null, "Id", "TS-" + timeStampId);
        unsignedSignaturePropertiesElement.addChild(Node.ELEMENT, signatureTimeStampElement);

        Element canonicalizationMethodElement = doc.createElement("", "ds:CanonicalizationMethod");
        canonicalizationMethodElement.setAttribute(null, "Algorithm", XAdESUtils.CANONICALIZATION_METHOD);
        Element encapsulatedTimeStampElement = doc.createElement("", "xades:EncapsulatedTimeStamp");
        encapsulatedTimeStampElement.setAttribute(null, "Id", "ETS-" + timeStampId);
        signatureTimeStampElement.addChild(Node.ELEMENT, canonicalizationMethodElement);
        signatureTimeStampElement.addChild(Node.ELEMENT, encapsulatedTimeStampElement);

        byte[] signatureValueBytes = XMLUtils.serialize(signatureValueElementCanonicalized, false);
        byte[] digest = HashUtils.getHash(signatureValueBytes, Constants.DATA_DIGEST_ALGORITHM);
        if (timeStampServiceURL != null) {
            TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
            reqgen.setCertReq(true);
            TimeStampRequest timeStampRequest = reqgen.generate(
                    new ASN1ObjectIdentifier(DigestAlgorithm.SHA256.getOid()).toString(), digest);
            ResponseDto response = HttpConn.getInstance().doPostRequest(
                    timeStampRequest.getEncoded(), ContentType.TIMESTAMP_QUERY.getName(), timeStampServiceURL);
            if (ResponseDto.SC_OK == response.getStatusCode()) {
                byte[] bytesToken = response.getMessageBytes();
                TimeStampResponse timeStampResponse = new TimeStampResponse(bytesToken);
                TimeStampToken timeStampToken = timeStampResponse.getTimeStampToken();
                encapsulatedTimeStampElement.addChild(Node.TEXT, Base64.getEncoder().encodeToString(timeStampToken.getEncoded()));
            } else throw new Exception(response.getMessage());
        } else {
            log.info("Null timeStampServiceURL - XML signature without timestamp");
        }
        return unsignedPropertiesElement;
    }

}
