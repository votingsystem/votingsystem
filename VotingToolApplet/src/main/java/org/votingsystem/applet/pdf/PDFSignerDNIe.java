package org.votingsystem.applet.pdf;

import static org.votingsystem.model.ContextVS.*;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import iaik.pkcs.pkcs11.Mechanism;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.cms.CMSAttributeTableGenerationException;
import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.CMSSignedData;
import org.apache.log4j.Logger;
import org.votingsystem.applet.votingtool.VotingToolContext;
import org.votingsystem.util.FileUtils;

/**
 *
 * @author jgzornoza
 */
public class PDFSignerDNIe {
    
    private static Logger logger = Logger.getLogger(PDFSignerDNIe.class);
    
     public static void sign(String reason, String location, char[] password, 
         PdfReader reader, OutputStream fout) 
         throws NoSuchAlgorithmException, NoSuchAlgorithmException, 
         NoSuchAlgorithmException, NoSuchProviderException, IOException, Exception {
        DNIePDFSessionHelper sessionHelper = new DNIePDFSessionHelper(password, 
                VotingToolContext.DNIe_SESSION_MECHANISM);
        Certificate[] certs = sessionHelper.getCertificateChain();
        PdfStamper stp = PdfStamper.createSignature(reader, fout, '\0');        
        stp.setEncryption(null, null,PdfWriter.ALLOW_PRINTING, false);
        PdfSignatureAppearance sap = stp.getSignatureAppearance();  
        sap.setVisibleSignature(new Rectangle(100, 5, 400, 35), 1, null);   
        if(location != null) sap.setLocation(location);
        sap.setCrypto(null, certs, null, PdfSignatureAppearance.WINCER_SIGNED);
        if(reason != null) sap.setReason(reason);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        sap.setSignDate(cal);
        //sap.setContact("This is the Contact");
        sap.setAcro6Layers(true);
        PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, 
                VotingToolContext.PDF_SIGNATURE_NAME);
        dic.setDate(new PdfDate(sap.getSignDate()));
        //dic.setName(PdfPKCS7.getSubjectFields((X509Certificate)certs[0]).getField("CN"));
        String firmante = PdfPKCS7.getSubjectFields((X509Certificate)certs[0]).getField("CN").replace("(FIRMA)", "");
        sap.setLayer2Text("Firmado electrónicamente por:\n" + firmante); 
        logger.debug("Firmante: " + PdfPKCS7.getSubjectFields((X509Certificate)certs[0]).getField("CN"));
        sap.setCryptoDictionary(dic);
        int csize = 10000;
        HashMap exc = new HashMap();
        exc.put(PdfName.CONTENTS, new Integer(csize * 2 + 2));
        sap.preClose(exc);
        MessageDigest md = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
        CMSSignedData signedData = sessionHelper.genSignedData(
                md.digest(FileUtils.getBytesFromInputStream(sap.getRangeStream())), null);
        //ValidadoraCMS validadora = new ValidadoraCMS(sessionHelper.certificadoCA);
        //logger.info("validadora.isValid(signedData): " + validadora.isValid(signedData));
        byte[] pk = signedData.getEncoded();
        byte[] outc = new byte[csize];
        PdfDictionary dic2 = new PdfDictionary();
        System.arraycopy(pk, 0, outc, 0, pk.length);
        dic2.put(PdfName.CONTENTS, new PdfString(outc).setHexWriting(true));
        sap.close(dic2);
        sessionHelper.closeSession();
    }
     
     public static void signAndTimestamp(String reason, String location, char[] password, 
         PdfReader reader, FileOutputStream fout) 
         throws NoSuchAlgorithmException, NoSuchAlgorithmException, 
         NoSuchAlgorithmException, NoSuchProviderException, IOException, Exception {

        DNIePDFSessionHelper sessionHelper = new DNIePDFSessionHelper(password, 
                VotingToolContext.DNIe_SESSION_MECHANISM);
        Certificate[] certs = sessionHelper.getCertificateChain();
        PdfStamper stp = PdfStamper.createSignature(reader, fout, '\0');
        stp.setEncryption(null, null,PdfWriter.ALLOW_PRINTING, false);
        final PdfSignatureAppearance sap = stp.getSignatureAppearance();  
        sap.setVisibleSignature(new Rectangle(100, 10, 400, 40), 1, null);       
        
        
        if(location != null) sap.setLocation(location);
        sap.setCrypto(null, certs, null, PdfSignatureAppearance.WINCER_SIGNED);
        if(reason != null) sap.setReason(reason);
        //java.util.Calendar cal = java.util.Calendar.getInstance();
        //sap.setSignDate(cal);
        //sap.setContact("This is the Contact");
        sap.setAcro6Layers(true);
        final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, 
                VotingToolContext.PDF_SIGNATURE_NAME);
        //dic.setDate(new PdfDate(sap.getSignDate()));
        dic.setName(PdfPKCS7.getSubjectFields((X509Certificate)certs[0]).getField("CN"));
        logger.debug("signAndTimestamp - Firmante: " + PdfPKCS7.getSubjectFields((X509Certificate)certs[0]).getField("CN"));
        sap.setCryptoDictionary(dic);
        int csize = 10000;
        HashMap exc = new HashMap();
        //Nota del javadoc -> due to the hex string coding this size should be byte_size*2+2.
        exc.put(PdfName.CONTENTS, new Integer(csize * 2 + 2));
        String firmante = PdfPKCS7.getSubjectFields((X509Certificate)certs[0]).getField("CN").replace("(FIRMA)", "");
        sap.setLayer2Text("Firmado electrónicamente por:\n" + firmante); 
        
        CMSAttributeTableGenerator unsAttr= new CMSAttributeTableGenerator() {

                public AttributeTable getAttributes(final Map parameters) throws CMSAttributeTableGenerationException {
                    AttributeTable attributeTable = null;
                    // Gets the signature bytes
                    byte[] signatureBytes = (byte[]) parameters.get(SIGNATURE);

                    DERObject obj;
                    try {
                        // digests the signature
                        MessageDigest d = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
                        byte[] hash = d.digest(signatureBytes);
                        /*
                        //TODO esto lo tiene que coger del servidor
                        TimeStampToken timeStampToken = new TimeStampToken(
                                new CMSSignedData(TimeStampService.obtenerTimeStamp(hash)));

                        obj = new ASN1InputStream(timeStampToken.getEncoded()).readObject();
                        
                        final Calendar cal = new GregorianCalendar();
                        cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
                        logger.debug("Time: " + DateUtils.getStringFromDate(cal.getTime()));
                        
                        
                        // Creates the signatureTimestampToken attribute
                        DERSet s = new DERSet(obj);
                        Attribute att = new Attribute(TimeStampService.id_signatureTimeStampToken, s);
                        Hashtable oh = new Hashtable();
                        oh.put(TimeStampService.id_signatureTimeStampToken, att);
                        attributeTable = new AttributeTable(oh);*/ 
                        return attributeTable;                       
                    }
                    catch (Exception e) {
                        throw new CMSAttributeTableGenerationException(e.getMessage(),  e);
                    }
                }
            };
        
        dic.setDate(new PdfDate(sap.getSignDate()));
        sap.preClose(exc);
        MessageDigest md = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
        byte[] signatureHash = md.digest(FileUtils.getBytesFromInputStream(sap.getRangeStream()));
        
        CMSSignedData signedData = sessionHelper.genSignedData(signatureHash, unsAttr);
        //ValidadoraCMS validadora = new ValidadoraCMS(sessionHelper.certificadoCA);
        //logger.info("validadora.isValid(signedData): " + validadora.isValid(signedData));

        byte[] pk = signedData.getEncoded();
        byte[] outc = new byte[csize];
        PdfDictionary dic2 = new PdfDictionary();
        System.arraycopy(pk, 0, outc, 0, pk.length);
        dic2.put(PdfName.CONTENTS, new PdfString(outc).setHexWriting(true));
        sap.close(dic2);
     }
     
}