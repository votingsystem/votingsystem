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
import java.security.PrivateKey;
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
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampToken;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.DNIePDFSessionHelper;
import org.sistemavotacion.seguridad.PDF_CMSSignedGenerator;
import org.sistemavotacion.seguridad.VotingSystemCMSSignedGenerator;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class PDFSignerWorker extends SwingWorker<Respuesta, String>  
        implements VotingSystemWorker {
    
    private static Logger logger = LoggerFactory.getLogger(PDFSignerWorker.class);

    public static final String PDF_SIGNATURE_MECHANISM = "SHA1withRSA";
    public static final String PDF_DIGEST_OID = CMSSignedDataGenerator.DIGEST_SHA1;
    public static final String PDF_SIGNATURE_DIGEST    = "SHA1";
    
    private Integer id;
    private Respuesta respuesta = null;    
    private String urlTimeStampServer;
    private String location;
    private String reason;
    private VotingSystemWorkerListener workerListener;
    private TimeStampToken timeStampToken = null;
    private TimeStampRequest timeStampRequest = null;
    private char[] password;
    private PdfReader pdfReader;
    private File signedFile;
    private PrivateKey signerPrivatekey;
    //private X509Certificate userCert;
    private Certificate[] signerCertChain;
    private VotingSystemCMSSignedGenerator systemSignedGenerator = null;
    
    public PDFSignerWorker(Integer id, String urlTimeStampServer, 
            VotingSystemWorkerListener workerListener, String reason, String location, 
            char[] password, PdfReader reader, PrivateKey signerPrivatekey,
            Certificate[] signerCertChain)
            throws NoSuchAlgorithmException, NoSuchAlgorithmException, 
            NoSuchAlgorithmException, NoSuchProviderException, IOException, Exception {
        this.id = id;
        this.signerPrivatekey = signerPrivatekey;
        this.signerCertChain = signerCertChain;
        //this.userCert = userCert;
        this.urlTimeStampServer = urlTimeStampServer;
        this.workerListener = workerListener;  
        this.location = location;
        this.password = password;
        this.pdfReader = reader;
        this.reason = reason;
        this.signerCertChain = signerCertChain;
        this.signedFile = File.createTempFile("signedPDF", ".pdf");
        signedFile.deleteOnExit();
    }
    
    @Override public void process(List<String> messages) {
        workerListener.process(messages);
    }
    
    @Override protected Respuesta doInBackground() throws Exception {
        if(signerPrivatekey != null && signerCertChain != null) {
            logger.debug("Generating PrivateKey VotingSystemSignedGenerator");
            PDF_CMSSignedGenerator signedGenerator = new PDF_CMSSignedGenerator(
                    signerPrivatekey, signerCertChain, PDF_SIGNATURE_MECHANISM, 
                    PDF_SIGNATURE_DIGEST, PDF_DIGEST_OID);
            systemSignedGenerator = signedGenerator;
        } else {
            logger.debug("Generating smartcard VotingSystemSignedGenerator");
            DNIePDFSessionHelper sessionHelper = new DNIePDFSessionHelper(password, DNIe_SESSION_MECHANISM);
            signerCertChain = sessionHelper.getCertificateChain();
            systemSignedGenerator = sessionHelper;
        }
        
        PdfStamper stp = PdfStamper.createSignature(pdfReader, 
                new FileOutputStream(signedFile), '\0');
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
        final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, PDF_SIGNATURE_NAME);
        //dic.setDate(new PdfDate(sap.getSignDate()));
        dic.setName(PdfPKCS7.getSubjectFields((X509Certificate)signerCertChain[0]).getField("CN"));
        logger.debug("signAndTimestamp - Firmante: " + PdfPKCS7.getSubjectFields(
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
        sap.setLayer2Text(Contexto.INSTANCE.getString(
                "signedByPDFLabel") + ":\n" + firmante); 
        
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
                        
                        Respuesta respuesta = Contexto.INSTANCE.
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
                        respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
                   }
                    return attributeTable;
                }
            };
        
        dic.setDate(new PdfDate(sap.getSignDate()));
        sap.preClose(exc);
        MessageDigest md = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
        byte[] signatureHash = md.digest(FileUtils.getBytesFromInputStream(sap.getRangeStream()));
        
        CMSSignedData signedData = systemSignedGenerator.genSignedData(signatureHash, unsAttr);
        //ValidadoraCMS validadora = new ValidadoraCMS(certificadoCA);
        //logger.info("validadora.isValid(signedData): " + validadora.isValid(signedData));

        byte[] pk = signedData.getEncoded();
        byte[] outc = new byte[csize];
        PdfDictionary dic2 = new PdfDictionary();
        System.arraycopy(pk, 0, outc, 0, pk.length);
        dic2.put(PdfName.CONTENTS, new PdfString(outc).setHexWriting(true));
        sap.close(dic2);
        respuesta = new Respuesta(Respuesta.SC_OK);
        return respuesta;
    }
    
    public static String getNifUsuario (X509Certificate certificate) {
    	String subjectDN = certificate.getSubjectDN().getName();
    	return subjectDN.split("SERIALNUMBER=")[1].split(",")[0];
    }
    
    @Override protected void done() {//on the EDT
        try {
            respuesta = get();
        }catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            respuesta = new Respuesta(Respuesta.SC_ERROR, ex.getMessage());
        } 
        workerListener.showResult(this);
    }
    
    public File getSignedAndTimeStampedPDF() {
        return signedFile;
    }

   @Override public String getMessage() {
        if(respuesta == null) return null;
        else return respuesta.getMensaje();
    }

    @Override public int getId() {
        return this.id;
    }

    @Override  public int getStatusCode() {
        if(respuesta == null) return Respuesta.SC_ERROR;
        else return respuesta.getCodigoEstado();
    }
    
    @Override public Respuesta getRespuesta() {
        return respuesta;
    }
}
