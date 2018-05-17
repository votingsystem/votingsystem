package org.votingsystem.xades;

import eu.europa.esig.dss.*;
import eu.europa.esig.dss.token.AbstractSignatureTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.x509.tsp.TSPSource;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import org.votingsystem.util.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class XAdESSignature {

    private static final Logger log = Logger.getLogger(XAdESSignature.class.getName());

    public XAdESSignature() {}

    public byte[] sign(byte[] xmlToSign, AbstractSignatureTokenConnection signingToken, TSPSource tspSource)
            throws IOException {
        DSSPrivateKeyEntry privateKey = signingToken.getKeys().get(0);
        DSSDocument toBeSigned = new InMemoryDocument(xmlToSign);
        toBeSigned.setMimeType(MimeType.XML);
        /* try{
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(xmlToSign);
            byte[] digest =  messageDigest.digest();
            byte[] documentToSignDigestBase64encoded = org.bouncycastle.util.encoders.Base64.encode(digest);
            log.log(Level.FINEST, " --- xmlToSign digest: " + new String(documentToSignDigestBase64encoded));
            log.log(Level.FINEST, " --- toBeSigned.getDigest: " + toBeSigned.getDigest(DigestAlgorithm.SHA256));
        } catch(Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        } */
        XAdESSignatureParameters params = new XAdESSignatureParameters();
        params.setSignaturePackaging(SignaturePackaging.ENVELOPED);
        params.setSigningCertificate(privateKey.getCertificate());
        params.setCertificateChain(privateKey.getCertificateChain());
        //params.bLevel().setSigningDate(new Date());

        CommonCertificateVerifier commonCertificateVerifier = new CommonCertificateVerifier();
        XAdESService service = new XAdESService(commonCertificateVerifier);
        if(tspSource == null) {
            log.log(Level.FINE, "null TspSource, signature will not have timestamp");
            params.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
        } else {
            params.setSignatureLevel(SignatureLevel.XAdES_BASELINE_T);
            service.setTspSource(tspSource);
        }
        ToBeSigned dataToSign = service.getDataToSign(toBeSigned, params);
        log.log(Level.FINEST, " --- dataToSign: " + new String(dataToSign.getBytes()));

        SignatureValue signatureValue = signingToken.sign(dataToSign, params.getDigestAlgorithm(), privateKey);
        DSSDocument signedDocument = service.signDocument(toBeSigned, params, signatureValue);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(signedDocument.openStream(), baos);
        baos.close();
        return baos.toByteArray();
    }

}
