package org.votingsystem.simulation.callable;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfDate;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfPKCS7;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignature;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;

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
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.PDF_CMSSignedGenerator;
import org.votingsystem.simulation.ContextService;
import org.apache.log4j.Logger;
import org.votingsystem.simulation.ApplicationContextHolder;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.DateUtils;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.simulation.ApplicationContextHolder as ACH;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PDFSignedSender implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(PDFSignedSender.class);

    private String urlToSendDocument;
    private String location;
    private String reason;
    private char[] password;
    private PdfReader pdfReader;
    private PrivateKey signerPrivatekey;
    private X509Certificate destinationCert = null;
    private Certificate[] signerCertChain;
    private PDF_CMSSignedGenerator signGenerator = null;
    
    public PDFSignedSender(String urlToSendDocument, 
            String reason, String location, 
            char[] password, PdfReader reader, PrivateKey signerPrivatekey, 
            Certificate[] signerCertChain,  X509Certificate destinationCert)
            throws NoSuchAlgorithmException, NoSuchAlgorithmException, 
            NoSuchAlgorithmException, NoSuchProviderException, IOException, Exception {
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
        signGenerator = new PDF_CMSSignedGenerator(
                signerPrivatekey, signerCertChain, 
				ContextService.PDF_SIGNATURE_MECHANISM, 
                ContextService.PDF_SIGNATURE_DIGEST, 
				ContextService.PDF_DIGEST_OID);
        File fileToSend = File.createTempFile("signedPDF", ".pdf");
        fileToSend.deleteOnExit();
		
        PdfStamper stp = PdfStamper.createSignature(pdfReader, 
                new FileOutputStream(fileToSend), '\0' as char);
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
        final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, 
			PdfName.ADBE_PKCS7_SHA1);
        //dic.setDate(new PdfDate(sap.getSignDate()));
        dic.setName(PdfPKCS7.getSubjectFields((X509Certificate)signerCertChain[0]).getField("CN"));
        log.debug("signAndTimestamp - Firmante: " + PdfPKCS7.getSubjectFields(
                (X509Certificate)signerCertChain[0]).getField("CN"));
        sap.setCryptoDictionary(dic);
        int csize = 10000;
        HashMap exc = new HashMap();
        //Nota del javadoc -> due to the hex string coding this size should be byte_size*2+2.
        exc.put(PdfName.CONTENTS, new Integer(csize * 2 + 2));
        String firmante = PdfPKCS7.getSubjectFields((X509Certificate)signerCertChain[0]).
                getField("CN");
        if(firmante != null && firmante.contains("(FIRMA)")) {
            firmante = firmante.replace("(FIRMA)", "");
        } else firmante = getNifUsuario(((X509Certificate)signerCertChain[0]));
	
        sap.setLayer2Text(ACH.getMessage("signedByPDFLabel", null) + ":\n" + firmante); 
        
        final ResponseVS respuesta = new ResponseVS(ResponseVS.SC_OK);
        CMSAttributeTableGenerator unsAttr= new CMSAttributeTableGenerator() {

                public AttributeTable getAttributes(final Map parameters) throws CMSAttributeTableGenerationException {
                    AttributeTable attributeTable = null;
                    // Gets the signature bytes
                    byte[] signatureBytes = (byte[]) parameters.get(SIGNATURE);
                    DERObject obj;                   
                    try {
                        // digests the signature
                        MessageDigest d = MessageDigest.getInstance(
							ContextService.PDF_SIGNATURE_DIGEST);
                        byte[] digest = d.digest(signatureBytes);
                        
                        TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
                        //reqgen.setReqPolicy(m_sPolicyOID);
                        TimeStampRequest timeStampRequest = reqgen.generate(
							ContextService.TIMESTAMP_PDF_HASH, digest);
                        MessageTimeStamper messageTimeStamper = 
                                new MessageTimeStamper(timeStampRequest);
                        ResponseVS responseVS = messageTimeStamper.call();
                        if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                            log.error("Error timestamping", responseVS.getMessage());
                            return null;
                        }
                        TimeStampToken timeStampToken = messageTimeStamper.getTimeStampToken();
                        final Calendar cal = new GregorianCalendar();
                        cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
                        log.debug("*** TimeStamp: " + DateUtils.getStringFromDate(cal.getTime()));

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
                        log.error(ex.getMessage(), ex);
                        respuesta.appendErrorMessage(ex.getMessage());
                   }
                   return attributeTable;
                }
            };
        
        if(ResponseVS.SC_OK != respuesta.getStatusCode()) return respuesta;
        dic.setDate(new PdfDate(sap.getSignDate()));
        sap.preClose(exc);
        MessageDigest md = MessageDigest.getInstance(ContextService.PDF_SIGNATURE_DIGEST);
        byte[] signatureHash = md.digest(FileUtils.getBytesFromInputStream(sap.getRangeStream()));
        
        CMSSignedData signedData = signGenerator.genSignedData(signatureHash, unsAttr);
        //ValidadoraCMS validadora = new ValidadoraCMS(certificadoCA);
        //log.info("validadora.isValid(signedData): " + validadora.isValid(signedData));

        byte[] pk = signedData.getEncoded();
        byte[] outc = new byte[csize];
        PdfDictionary dic2 = new PdfDictionary();
        System.arraycopy(pk, 0, outc, 0, pk.length);
        dic2.put(PdfName.CONTENTS, new PdfString(outc).setHexWriting(true));
        sap.close(dic2);
        String contentType = null;
        byte[] bytesToSend = null;
        if(destinationCert != null) {
            log.debug("---- with destinationCert -> encrypting response");
            bytesToSend = Encryptor.encryptFile(fileToSend,destinationCert);
            contentType = ContentTypeVS.PDF_SIGNED_AND_ENCRYPTED;
        } else {
            contentType = ContentTypeVS.PDF_SIGNED;
            bytesToSend = FileUtils.getBytesFromFile(fileToSend);
        }

        ResponseVS senderResponse = ACH.getSimulationContext().getHttpHelper().sendByteArray(
                bytesToSend, contentType, urlToSendDocument);
        return senderResponse;
    }
    
    public static String getNifUsuario (X509Certificate certificate) {
    	String subjectDN = certificate.getSubjectDN().getName();
    	return subjectDN.split("SERIALNUMBER=")[1].split(",")[0];
    }
    

    @Override public ResponseVS call() throws Exception {
       ResponseVS respuesta = doInBackground();
       return respuesta;
    }
    
}
