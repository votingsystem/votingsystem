package org.sistemavotacion.task;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.mail.Header;
import javax.mail.internet.MimeBodyPart;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampToken;
import org.bouncycastle2.asn1.ASN1InputStream;
import org.bouncycastle2.asn1.DERObject;
import org.bouncycastle2.asn1.DERObjectIdentifier;
import org.bouncycastle2.asn1.DERSet;
import org.bouncycastle2.asn1.cms.Attribute;
import org.bouncycastle2.asn1.cms.AttributeTable;
import org.bouncycastle2.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle2.cms.CMSAttributeTableGenerationException;
import org.bouncycastle2.cms.CMSAttributeTableGenerator;
import org.bouncycastle2.cms.CMSSignedData;
import org.bouncycastle2.cms.CMSSignedDataGenerator;
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.callable.MessageTimeStamper;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.Encryptor;
import org.sistemavotacion.seguridad.PDF_CMSSignedGenerator;
import org.sistemavotacion.util.DateUtils;
import org.sistemavotacion.util.FileUtils;
import org.sistemavotacion.util.HttpHelper;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

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

public class SignTimestampSendPDFTask extends AsyncTask<String, Void, Respuesta> {

	public static final String TAG = "SignTimestampSendPDFTask";
	
    public static final PdfName PDF_SIGNATURE_NAME     = PdfName.ADBE_PKCS7_SHA1;
    public static final String PDF_SIGNATURE_DIGEST    = "SHA1";
    public static final String PDF_SIGNATURE_MECHANISM = "SHA1withRSA";
    public static final String TIMESTAMP_PDF_HASH      = TSPAlgorithms.SHA1;
    public static final String PDF_DIGEST_OID          = CMSSignedDataGenerator.DIGEST_SHA1;

    private String location;
    private String reason;

    private PdfReader pdfReader;
    private Context context;
    
    private X509Certificate signerCert;
    private PrivateKey signerPrivatekey;
    private Certificate[] signerCertChain;
    
    public SignTimestampSendPDFTask(Context context,
    		String reason, String location, PrivateKey signerPrivatekey, 
    		Certificate[] signerCertChain, PdfReader reader) {
    	this.context = context;
    	this.reason = reason;
    	this.location = location;
    	this.signerCert = (X509Certificate) signerCertChain[0];
    	this.signerPrivatekey = signerPrivatekey;
    	this.signerCertChain = signerCertChain;
    	this.pdfReader = reader;
    }
	
	@Override protected Respuesta doInBackground(String... urls) {
		String serviceURL = urls[0];
		Respuesta respuesta = null;
        try {
        	byte[] timeStampedSignedPDF = signWithTimestamp(pdfReader, 
        			signerCert, signerPrivatekey, signerCertChain);
        	Header header = new Header(Aplicacion.VOTING_HEADER_LABEL,"SignedPDF");
            MimeBodyPart mimeBodyPart = Encryptor.encryptBase64Message(
            		timeStampedSignedPDF,Aplicacion.getControlAcceso().
            		getCertificado(), header);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mimeBodyPart.writeTo(baos);
            byte[] bytesToSend = baos.toByteArray();
            baos.close();
            HttpResponse response = HttpHelper.sendByteArray(bytesToSend, 
            		Aplicacion.PDF_SIGNED_AND_ENCRYPTED_CONTENT_TYPE, serviceURL);
            respuesta = new Respuesta(
            		response.getStatusLine().getStatusCode(), 
            		EntityUtils.toString(response.getEntity()));
        }catch (Exception ex) {
			ex.printStackTrace();
			respuesta = new Respuesta(
					Respuesta.SC_ERROR, ex.getMessage());
		}
        return respuesta;
	}
	
    public byte[] signWithTimestamp(PdfReader pdfReader, 
    		X509Certificate signerCert, PrivateKey signerPrivatekey, 
    		Certificate[] signerCertChain) throws Exception {
        Log.d(TAG + ".signWithTimestamp(...)", " - certsChain.length: " + signerCertChain.length);
        PDF_CMSSignedGenerator signedGenerator = new PDF_CMSSignedGenerator(signerPrivatekey,
        		signerCertChain, PDF_SIGNATURE_MECHANISM, PDF_SIGNATURE_DIGEST, PDF_DIGEST_OID);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfStamper stp = PdfStamper.createSignature(pdfReader, baos, '\0');
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
        dic.setName(PdfPKCS7.getSubjectFields(signerCert).getField("CN"));
        Log.d(TAG, "signWithTimestamp - Firmante: " + PdfPKCS7.getSubjectFields(signerCert).getField("CN"));
        sap.setCryptoDictionary(dic);
        int csize = 10000;
        HashMap exc = new HashMap();
        //Nota del javadoc -> due to the hex string coding this size should be byte_size*2+2.
        exc.put(PdfName.CONTENTS, new Integer(csize * 2 + 2));
        String firmante = PdfPKCS7.getSubjectFields(signerCert).getField("CN");
        
        if(firmante != null && firmante.contains("(FIRMA)")) {
            firmante = firmante.replace("(FIRMA)", "");
        } else firmante = getNifUsuario(signerCert);
        
        
        sap.setLayer2Text(context.getString(R.string.pdf_signed_by_lbl) + ":\n" + firmante); 
        
        CMSAttributeTableGenerator unsAttr= new CMSAttributeTableGenerator() {

                public AttributeTable getAttributes(final Map parameters) throws CMSAttributeTableGenerationException {
                    AttributeTable attributeTable = null;
                    // Gets the signature bytes
                    byte[] signatureBytes = (byte[]) parameters.get(SIGNATURE);
                    DERObject obj;                   
                    try {
                        MessageDigest d = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
                        byte[] digest = d.digest(signatureBytes);

                        MessageTimeStamper messageTimeStamper = 
                                new MessageTimeStamper(TIMESTAMP_PDF_HASH, digest);
                        Respuesta respuesta = messageTimeStamper.call();
                        if(Respuesta.SC_OK != respuesta.getCodigoEstado()) {
                        	Log.d(TAG + ".signWithTimestamp(...)", " - Error timestamping: " + 
                        			respuesta.getMensaje());
                            return null;
                        }
                        TimeStampToken timeStampToken = messageTimeStamper.getTimeStampToken();
                        
                        final Calendar cal = new GregorianCalendar();
                        cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
                        Log.d(TAG, "*** TimeStamp: " + DateUtils.getStringFromDate(cal.getTime()));
                    
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
                	   ex.printStackTrace();
                	   throw new CMSAttributeTableGenerationException(ex.getMessage());
                   }
                    return attributeTable;
                }
            };
        
        dic.setDate(new PdfDate(sap.getSignDate()));
        sap.preClose(exc);
        MessageDigest md = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
        byte[] signatureHash = md.digest(FileUtils.getBytesFromInputStream(sap.getRangeStream()));
        
        CMSSignedData signedData = signedGenerator.genSignedData(
        		signatureHash, unsAttr);

        /*CertStore certStore = signedData.getCertificatesAndCRLs("Collection", "BC"); 
        SignerInformationStore signers = signedData.getSignerInfos(); 
        Collection c = signers.getSigners(); 
        Iterator it = c.iterator(); 
        while (it.hasNext()) { 
            SignerInformation signer = (SignerInformation) it.next(); 
            Collection certCollection = certStore.getCertificates(signer.getSID()); 
            Iterator certIt = certCollection.iterator(); 
            X509Certificate cert = (X509Certificate) certIt.next();
            Log.d(TAG, "cert: " + cert.getSubjectDN().toString());
            Log.d(TAG, "signer.verify(...): " + signer.verify(cert, "BC"));
        }*/

        byte[] pk = signedData.getEncoded();
        byte[] outc = new byte[csize];
        PdfDictionary dic2 = new PdfDictionary();
        System.arraycopy(pk, 0, outc, 0, pk.length);
        dic2.put(PdfName.CONTENTS, new PdfString(outc).setHexWriting(true));
        sap.close(dic2);
        byte[] result = baos.toByteArray();
        baos.close();
        return result;
    }

    public static void sign(PdfReader reader, FileOutputStream outputStream, 
    		PrivateKey key, Certificate[] chain) throws Exception {
        Log.d(TAG + ".sign(...)", " - chain: " + chain.length);     
        PdfStamper stp = PdfStamper.createSignature(reader, outputStream, '\0');
        PdfSignatureAppearance signatureAppearance = stp.getSignatureAppearance();
        signatureAppearance.setCrypto(key, chain, null, PdfSignatureAppearance.WINCER_SIGNED);
        //sap.setReason("Support this");
        //sap.setLocation("Higueruela");
        signatureAppearance.setSignDate( new GregorianCalendar() ); 
        signatureAppearance.setVisibleSignature(new Rectangle(50, 40, 300, 140), 1, null);
        stp.close();
     }
	
    public static String getNifUsuario (X509Certificate certificate) {
    	String subjectDN = certificate.getSubjectDN().getName();
		String nif = null;
    	if(subjectDN.split("CN=nif:").length > 1) {
			nif = subjectDN.split("CN=nif:")[1];
			if (nif.split(",").length > 1) {
				nif = nif.split(",")[0];
			}
		} else if(subjectDN.split("SERIALNUMBER=").length > 1) {
			nif = subjectDN.split("SERIALNUMBER=")[1];
			if (nif.split(",").length > 1) {
				nif = nif.split(",")[0];
			}
		}
    	return nif;
    }

}