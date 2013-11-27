package org.votingsystem.applet.callable;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.*;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSAttributeTableGenerationException;
import org.bouncycastle.cms.CMSAttributeTableGenerator;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.applet.signature.DNIePDFContentSigner;
import org.votingsystem.applet.util.HttpHelper;
import org.votingsystem.applet.votingtool.Applet;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.ContentSignerVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.PDFContentSigner;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.Callable;

import static org.votingsystem.model.ContextVS.*;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PDFSignedSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(PDFSignedSender.class);
    
    private Integer id;
    private String urlToSendDocument;
    private String location;
    private String reason;
    private char[] password;
    private PdfReader pdfReader;
    private PrivateKey signerPrivatekey;
    private X509Certificate destinationCert = null;
    private Certificate[] signerCertChain;
    private ContentSignerVS systemSignedGenerator = null;
    
    public PDFSignedSender(Integer id, String urlToSendDocument, 
            String reason, String location, 
            char[] password, PdfReader reader, PrivateKey signerPrivatekey, 
            Certificate[] signerCertChain,  X509Certificate destinationCert)
            throws NoSuchAlgorithmException, NoSuchAlgorithmException, 
            NoSuchAlgorithmException, NoSuchProviderException, IOException, Exception {
        this.id = id;
        this.urlToSendDocument = urlToSendDocument;
        this.signerPrivatekey = signerPrivatekey;
        this.signerCertChain = signerCertChain;
        this.location = location;
        this.password = password;
        this.pdfReader = reader;
        this.reason = reason;
        this.signerCertChain = signerCertChain;
        this.destinationCert = destinationCert;
    }
    
    private ResponseVS doInBackground() throws Exception {
        
        if(signerPrivatekey != null && signerCertChain != null) {
            logger.debug("Generating PrivateKey VotingSystemSignedGenerator");
            PDFContentSigner signedGenerator = new PDFContentSigner( signerPrivatekey, signerCertChain,
                    PDF_SIGNATURE_MECHANISM, PDF_SIGNATURE_DIGEST, PDF_DIGEST_OID);
            systemSignedGenerator = signedGenerator;
        } else {
            logger.debug("Generating smartcard VotingSystemSignedGenerator");
            DNIePDFContentSigner sessionHelper = DNIePDFContentSigner.getInstance(password, Applet.DNIe_SESSION_MECHANISM);
            signerCertChain = sessionHelper.getCertificateChain();
            systemSignedGenerator = sessionHelper;
        }
        File fileToSend = File.createTempFile("signedPDF", ".pdf");
        fileToSend.deleteOnExit();
        PdfStamper stp = PdfStamper.createSignature(pdfReader, 
                new FileOutputStream(fileToSend), '\0');
        stp.setEncryption(null, null,PdfWriter.ALLOW_PRINTING, false);
        final PdfSignatureAppearance sap = stp.getSignatureAppearance();  
        sap.setVisibleSignature(new Rectangle(100, 10, 400, 40), 1, null);       
        
        
        if(location != null) sap.setLocation(location);
        sap.setCrypto(null, signerCertChain, null, PdfSignatureAppearance.WINCER_SIGNED);
        if(reason != null) sap.setReason(reason);
        //java.util.Calendar cal = java.util.Calendar.getInstance();
        //sap.setSignDate(cal);
        //sap.setContact("This is the Contact");
        sap.setAcro6Layers(true);
        final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE,  Applet.PDF_SIGNATURE_NAME);
        //dic.setDate(new PdfDate(sap.getSignDate()));
        dic.setName(PdfPKCS7.getSubjectFields((X509Certificate)signerCertChain[0]).getField("CN"));
        logger.debug("signAndTimestamp - Firmante: " + PdfPKCS7.getSubjectFields(
                (X509Certificate)signerCertChain[0]).getField("CN"));
        sap.setCryptoDictionary(dic);
        int csize = 10000;
        HashMap exc = new HashMap();
        //Nota del javadoc -> due to the hex string coding this size should be byte_size*2+2.
        exc.put(PdfName.CONTENTS, new Integer(csize * 2 + 2));
        String signerVS = PdfPKCS7.getSubjectFields((X509Certificate)signerCertChain[0]).
                getField("CN");
        if(signerVS != null && signerVS.contains("(FIRMA)")) {
            signerVS = signerVS.replace("(FIRMA)", "");
        } else signerVS = getUserNIF(((X509Certificate)signerCertChain[0]));
        sap.setLayer2Text(ContextVS.getInstance().getMessage(
                "signedByPDFLabel") + ":\n" + signerVS);
        
        final ResponseVS responseVS = new ResponseVS(ResponseVS.SC_OK);
        CMSAttributeTableGenerator unsAttr= new CMSAttributeTableGenerator() {

                public AttributeTable getAttributes(final Map parameters) throws CMSAttributeTableGenerationException {
                    AttributeTable attributeTable = null;
                    // Gets the signature bytes
                    byte[] signatureBytes = (byte[]) parameters.get(SIGNATURE);
                    DERObject obj;                   
                    try {
                        // digests the signature
                        MessageDigest d = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
                        byte[] digest = d.digest(signatureBytes);
                        
                        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
                        //reqgen.setReqPolicy(m_sPolicyOID);
                        TimeStampRequest timeStampRequest = reqgen.generate(TIMESTAMP_PDF_HASH, digest);
                        MessageTimeStamper messageTimeStamper = 
                                new MessageTimeStamper(timeStampRequest);
                        ResponseVS responseVS = messageTimeStamper.call();
                        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                            logger.error("Error timestamping: " + responseVS.getMessage());
                            return null;
                        }
                        TimeStampToken timeStampToken = messageTimeStamper.getTimeStampToken();
                        final Calendar cal = new GregorianCalendar();
                        cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
                        logger.debug("*** TimeStamp: " + DateUtils.getStringFromDate(cal.getTime()));

                        obj = new ASN1InputStream(timeStampToken.getEncoded()).readObject();
                        // Creates the signatureTimestampToken attribute
                        DERSet s = new DERSet(obj);                        
                        Attribute att = new Attribute(PKCSObjectIdentifiers.
                            id_aa_signatureTimeStampToken, s);
                        Hashtable oh = new Hashtable();
                        //oh.put(PKCSObjectIdentifiers.id_aa_signatureTimeStampToken, att);
                        oh.put(new DERObjectIdentifier("1.2.840.113549.1.9.16.2.14"), att);
                        attributeTable = new AttributeTable(oh); 

                   } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        responseVS.appendMessage(ex.getMessage());
                   }
                   return attributeTable;
                }
            };
        
        if(ResponseVS.SC_OK != responseVS.getStatusCode()) return responseVS;
        dic.setDate(new PdfDate(sap.getSignDate()));
        sap.preClose(exc);
        MessageDigest md = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
        byte[] signatureHash = md.digest(FileUtils.getBytesFromInputStream(sap.getRangeStream()));
        
        CMSSignedData signedData = systemSignedGenerator.genSignedData(signatureHash, unsAttr);
        //ValidadoraCMS validadora = new ValidadoraCMS(certCA);
        //logger.info("validadora.isValid(signedData): " + validadora.isValid(signedData));

        byte[] pk = signedData.getEncoded();
        byte[] outc = new byte[csize];
        PdfDictionary dic2 = new PdfDictionary();
        System.arraycopy(pk, 0, outc, 0, pk.length);
        dic2.put(PdfName.CONTENTS, new PdfString(outc).setHexWriting(true));
        sap.close(dic2);
        String contentType = null;
        byte[] bytesToSend = null;
        if(destinationCert != null) {
            logger.debug("---- with destinationCert -> encrypting response");
            bytesToSend = Encryptor.encryptFile(fileToSend,destinationCert);
            contentType = ContentTypeVS.PDF_SIGNED_AND_ENCRYPTED;
        } else {
            contentType = ContentTypeVS.PDF_SIGNED;
            bytesToSend = FileUtils.getBytesFromFile(fileToSend);
        }

        ResponseVS senderResponse = HttpHelper.getInstance().sendByteArray(
                bytesToSend, contentType, urlToSendDocument);
        return senderResponse;
    }
    
    private String getUserNIF (X509Certificate certificate) {
    	String subjectDN = certificate.getSubjectDN().getName();
    	return subjectDN.split("SERIALNUMBER=")[1].split(",")[0];
    }
    

    @Override public ResponseVS call() throws Exception {
       ResponseVS responseVS = doInBackground();
       return responseVS;
    }
    
}
