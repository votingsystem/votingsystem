package org.sistemavotacion.pdf;

import com.itextpdf.text.pdf.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import org.sistemavotacion.util.DateUtils;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jgzornoza
 */
public class ValidadoraPDF {
    
    private static org.slf4j.Logger logger = LoggerFactory.getLogger(ValidadoraPDF.class);
    
    
    public static void main (String[] args) throws FileNotFoundException, IOException, Exception{
        //comprobarFirmas("/home/jgzornoza/Documentos/orangeFirmadoDNIeTimeStamped.pdf");
        
        PdfReader reader = new PdfReader("/home/jgzornoza/111.pdf");
        verificarTimeStamp(reader);
        
    }
    
    public static void comprobarFirmas(String pdfFilePath) {
        try {
            PdfReader reader = new PdfReader(pdfFilePath);
            AcroFields af = reader.getAcroFields();
            ArrayList names = af.getSignatureNames();
            for (int k = 0; k < names.size(); ++k) {
                String name = (String) names.get(k);
                logger.debug("Signature name: " + name);
                logger.debug("Signature covers whole document: "
                        + af.signatureCoversWholeDocument(name));
                logger.debug("Document revision: " + af.getRevision(name)
                        + " of " + af.getTotalRevisions());
                 PdfPKCS7 pk = af.verifySignature(name);
                 logger.debug("DigestAlgorithm: " + pk.getDigestAlgorithm());
                try {
                    logger.debug("verify: " + pk.verify());

                } catch (SignatureException ex) {
                    logger.error(ex.getMessage(), ex);
                }                
                /* Start pkcs7 extraction
                PdfDictionary v = af.getSignatureDictionary(name);

                PdfString contents = (PdfString) PdfReader.getPdfObject(v
                        .get(PdfName.CONTENTS));
                FileOutputStream fos = new FileOutputStream(filename +"_signeddata_"
                        + name + ".pk7");
                logger.debug(k+") Estrazione pkcs7: " + filename +"_signeddata_"
                        + name + ".pk7");
                fos.write(contents.getOriginalBytes());
                fos.flush();
                fos.close();*/
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);         
        } 

    }

        
    public static void verificarTimeStamp(PdfReader reader) throws IOException, NoSuchAlgorithmException, SignatureException {
    
        KeyStore kall = PdfPKCS7.loadCacertsKeyStore();
        AcroFields af = reader.getAcroFields();
        ArrayList names = af.getSignatureNames();
        for (int k = 0; k < names.size(); ++k) {
            String name = (String)names.get(k);
            logger.debug("Signature name: " + name);
            logger.debug("Signature covers whole document: " + af.signatureCoversWholeDocument(name));
            logger.debug("Document revision: " + af.getRevision(name) + " of " + af.getTotalRevisions());
            // Start revision extraction
            FileOutputStream out = new FileOutputStream("revision_" + af.getRevision(name) + ".pdf");
            byte bb[] = new byte[8192];
            InputStream ip = af.extractRevision(name);
            int n = 0;
            while ((n = ip.read(bb)) > 0)
                out.write(bb, 0, n);
            out.close();
            ip.close();
            // End revision extraction
            PdfPKCS7 pk = af.verifySignature(name);
            Calendar cal = pk.getSignDate();
            X509Certificate pkc[] = (X509Certificate[])pk.getSignCertificateChain();
            org.bouncycastle.tsp.TimeStampToken ts = pk.getTimeStampToken();
            logger.debug("Subject: " + PdfPKCS7.getSubjectFields(pk.getSigningCertificate()));
            logger.debug("Document modified: " + !pk.verify());
            if (ts != null) {
                boolean impr = pk.verifyTimestampImprint();
                logger.debug("Timestamp imprint verifies: " + impr);
                cal = pk.getTimeStampDate();
                logger.debug("Timestamp date: " + DateUtils.getStringFromDate(cal.getTime()));
                boolean tspCert = PdfPKCS7.verifyTimestampCertificates(pk.getTimeStampToken(), kall, null);
                logger.debug("LTV Timestamp Certificates verifified " + tspCert);
            } 
            else {
                logger.debug("Sin Timestamp");
                Object fails[] = PdfPKCS7.verifyCertificates(pkc, kall, null, cal);
                if (fails == null)
                    logger.debug("Certificates verified against the KeyStore");
                else
                    logger.debug("Certificate failed: " + fails[1]);
                /*BasicOCSPResp ocsp = pk.getOcsp();
                if (ocsp != null) {
                    InputStream inStream = new FileInputStream("C:/responder.cer");
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
                    inStream.close();
                    logger.debug("OCSP signature verifies: " + ocsp.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider("BC").build(cert.getPublicKey())));
                    logger.debug("OCSP revocation refers to this certificate: " + pk.isRevocationValid());
                }*/
            }
        }

    }
}