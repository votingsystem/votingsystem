package org.sistemavotacion.worker;

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
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import static org.sistemavotacion.Contexto.*;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.swing.SwingWorker;
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
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.DNIePDFSessionHelper;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://raw.github.com/jgzornoza/SistemaVotacion/master/licencia.txt
*/
public class PDFSignerDNIeWorker extends SwingWorker<Integer, String>  
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(PDFSignerDNIeWorker.class);

    private Integer id;
    private String urlTimeStampServer;
    private String location;
    private String reason;
    private VotingSystemWorkerListener workerListener;
    private TimeStampToken timeStampToken = null;
    private Attribute timeStampAsAttribute = null;
    private AttributeTable timeStampAsAttributeTable = null;
    private TimeStampRequest timeStampRequest = null;
    private char[] password;
    private PdfReader pdfReader;
    private File signedFile;
    private byte[] bytesDocumento;
    private int statusCode = Respuesta.SC_ERROR;
    private String message = null;
    private Exception exception = null;
    
    public PDFSignerDNIeWorker(Integer id, String urlTimeStampServer, 
            VotingSystemWorkerListener workerListener, String reason, String location, char[] password,
            PdfReader reader, File signedFile)
            throws NoSuchAlgorithmException, NoSuchAlgorithmException, 
            NoSuchAlgorithmException, NoSuchProviderException, IOException, Exception {
        this.id = id;
        this.urlTimeStampServer = urlTimeStampServer;
        this.workerListener = workerListener;  
        this.location = location;
        this.password = password;
        this.pdfReader = reader;
        this.reason = reason;
        this.signedFile = signedFile;
    }
    
    @Override public void process(List<String> messages) {
        workerListener.process(messages);
    }
    
    @Override protected Integer doInBackground() throws Exception {
        DNIePDFSessionHelper sessionHelper = new DNIePDFSessionHelper(password, DNIe_SESSION_MECHANISM);
        Certificate[] certs = sessionHelper.getCertificateChain();
        PdfStamper stp = PdfStamper.createSignature(pdfReader, 
                new FileOutputStream(signedFile), '\0');
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
        final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, PDF_SIGNATURE_NAME);
        //dic.setDate(new PdfDate(sap.getSignDate()));
        dic.setName(PdfPKCS7.getSubjectFields((X509Certificate)certs[0]).getField("CN"));
        logger.debug("signAndTimestamp - Firmante: " + PdfPKCS7.getSubjectFields((X509Certificate)certs[0]).getField("CN"));
        sap.setCryptoDictionary(dic);
        int csize = 10000;
        HashMap exc = new HashMap();
        //Nota del javadoc -> due to the hex string coding this size should be byte_size*2+2.
        exc.put(PdfName.CONTENTS, new Integer(csize * 2 + 2));
        String firmante = PdfPKCS7.getSubjectFields((X509Certificate)certs[0]).getField("CN").replace("(FIRMA)", "");
        sap.setLayer2Text(getString("signedByPDFLabel") + ":\n" + firmante); 

        
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
                        timeStampRequest = reqgen.generate(TIMESTAMP_PDF_HASH, digest);
                        
                        Respuesta respuesta = Contexto.getInstancia().
                                getHttpHelper().sendByteArray(
                                timeStampRequest.getEncoded(), "timestamp-query", 
                                urlTimeStampServer);
                        
                        if (Respuesta.SC_OK == respuesta.getCodigoEstado()) {
                            byte[] bytesToken =respuesta.getBytesArchivo();
                            timeStampToken = new TimeStampToken(
                                new CMSSignedData(bytesToken));
                            
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
                        }
                   } catch(Exception ex) {
                        logger.error(ex.getMessage(), ex);
                        exception = ex;
                   }
                    return attributeTable;
                }
            };
        
        dic.setDate(new PdfDate(sap.getSignDate()));
        sap.preClose(exc);
        MessageDigest md = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
        byte[] signatureHash = md.digest(FileUtils.getBytesFromInputStream(sap.getRangeStream()));
        
        CMSSignedData signedData = sessionHelper.obtenerCMSSignedData(signatureHash, unsAttr);
        //ValidadoraCMS validadora = new ValidadoraCMS(sessionHelper.certificadoCA);
        //logger.info("validadora.isValid(signedData): " + validadora.isValid(signedData));

        byte[] pk = signedData.getEncoded();
        byte[] outc = new byte[csize];
        PdfDictionary dic2 = new PdfDictionary();
        System.arraycopy(pk, 0, outc, 0, pk.length);
        dic2.put(PdfName.CONTENTS, new PdfString(outc).setHexWriting(true));
        sap.close(dic2);
        return Respuesta.SC_OK;
    }

    
    @Override//on the EDT
    protected void done() {
        try {
            statusCode = get();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            exception =  ex;
        } finally {
             workerListener.showResult(this);
        }
    }
    
    public File getSignedAndTimeStampedPDF() {
        return signedFile;
    }

    @Override
    public String getMessage() {
        if(exception != null) return exception.getMessage();
        else return message;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }
    
}
