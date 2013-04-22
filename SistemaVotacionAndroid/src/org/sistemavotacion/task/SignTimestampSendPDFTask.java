package org.sistemavotacion.task;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
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
import org.sistemavotacion.android.Aplicacion;
import org.sistemavotacion.android.R;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.seguridad.PDF_CMSSignedGenerator;
import org.sistemavotacion.seguridad.EncryptionHelper;
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

public class SignTimestampSendPDFTask extends AsyncTask<String, Void, String> 
	implements TaskListener {

	public static final String TAG = "SignTimestampSendPDFTask";
	
    public static final PdfName PDF_SIGNATURE_NAME = PdfName.ADBE_PKCS7_SHA1;
    public static final String PDF_SIGNATURE_DIGEST    = "SHA1";
    public static final String TIMESTAMP_PDF_HASH = TSPAlgorithms.SHA1;
	
    private Integer id;
	private TaskListener listener = null;
    private Exception exception = null;
    int statusCode = Respuesta.SC_ERROR_PETICION; 
    private String message = null;
    private String urlTimeStampServer;
    private String location;
    private String reason;

    private PdfReader pdfReader;
    private File signedFile;
    private Context context;
    
    X509Certificate signerCert;
    PrivateKey signerPrivatekey;
    Certificate[] signerCertsChain;
    
    public SignTimestampSendPDFTask(Context context, Integer id, String urlTimeStampServer, 
    		String reason, String location, X509Certificate signerCert, PrivateKey signerPrivatekey, 
    		Certificate[] signerCertsChain, PdfReader reader, File signedFile, TaskListener listener) {
    	this.context = context;
    	this.id = id;
    	this.urlTimeStampServer = urlTimeStampServer;
    	this.reason = reason;
    	this.location = location;
    	this.signerCert = signerCert;
    	this.signerPrivatekey = signerPrivatekey;
    	this.signerCertsChain = signerCertsChain;
    	this.pdfReader = reader;
    	this.signedFile = signedFile;
    	this.listener = listener;
    }
	
	@Override
	protected String doInBackground(String... urls) {
		String urlSignedDocument = urls[0];
        try {
        	File timeStampedSignedFile = signWithTimestamp(pdfReader, signedFile, 
        			signerCert, signerPrivatekey, signerCertsChain);
        	File pdfEncryptedFile = File.createTempFile("pdfEncryptedFile", ".eml");
        	pdfEncryptedFile.deleteOnExit();
        	EncryptionHelper.encryptFile(timeStampedSignedFile, pdfEncryptedFile, 
        			Aplicacion.getControlAcceso().getCertificado());
	        new SendFileTask(null, this, pdfEncryptedFile).execute(urlSignedDocument);
        	Log.d(TAG + ".signWithTimestamp(...)", " - sending PDF file timeStamped and signed");
        }catch (Exception ex) {
			ex.printStackTrace();
			exception = ex;
		}
        return null;
	}
	
    public File signWithTimestamp(PdfReader pdfReader, File signedFile, 
    		X509Certificate signerCert, PrivateKey signerPrivatekey, 
    		Certificate[] signerCertsChain) throws Exception {
        Log.d(TAG + ".signWithTimestamp(...)", " - certsChain.length: " + signerCertsChain.length);
        PDF_CMSSignedGenerator signedGenerator = new PDF_CMSSignedGenerator(PDF_SIGNATURE_DIGEST);

        PdfStamper stp = PdfStamper.createSignature(pdfReader, 
                new FileOutputStream(signedFile), '\0');
        stp.setEncryption(null, null,PdfWriter.ALLOW_PRINTING, false);
        final PdfSignatureAppearance sap = stp.getSignatureAppearance();  
        sap.setVisibleSignature(new Rectangle(100, 10, 400, 40), 1, null);       
        
        
        if(location != null) sap.setLocation(location);
        sap.setCrypto(null, signerCertsChain, null, PdfSignatureAppearance.WINCER_SIGNED);
        if(reason != null) sap.setReason(reason);
        //java.util.Calendar cal = java.util.Calendar.getInstance();
        //sap.setSignDate(cal);
        //sap.setContact("This is the Contact");
        sap.setAcro6Layers(true);
        final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, PDF_SIGNATURE_NAME);
        //dic.setDate(new PdfDate(sap.getSignDate()));
        dic.setName(PdfPKCS7.getSubjectFields((X509Certificate)signerCert).getField("CN"));
        Log.d(TAG, "signAndTimestamp - Firmante: " + PdfPKCS7.getSubjectFields((X509Certificate)signerCert).getField("CN"));
        sap.setCryptoDictionary(dic);
        int csize = 10000;
        HashMap exc = new HashMap();
        //Nota del javadoc -> due to the hex string coding this size should be byte_size*2+2.
        exc.put(PdfName.CONTENTS, new Integer(csize * 2 + 2));
        String firmante = PdfPKCS7.getSubjectFields((X509Certificate)signerCert).getField("CN").replace("(FIRMA)", "");
        sap.setLayer2Text(context.getString(R.string.pdf_signed_by_lbl) + ":\n" + firmante); 
        
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
                       
                        
                        HttpResponse response = HttpHelper.sendByteArray(
                        		timeStampRequest.getEncoded(), urlTimeStampServer);
                        statusCode = response.getStatusLine().getStatusCode();
                        
                        if (Respuesta.SC_OK == response.getStatusLine().getStatusCode()) {
                            byte[] bytesToken = EntityUtils.toByteArray(response.getEntity());
                            TimeStampToken timeStampToken = new TimeStampToken(
                                new CMSSignedData(bytesToken));
                            
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
                        }
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
        
        CMSSignedData signedData = signedGenerator.obtenerCMSSignedData(signatureHash, 
        		unsAttr, signerPrivatekey, signerCert, signerCertsChain);

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
        return signedFile;
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
	
    @Override
    protected void onPostExecute(String result) {
    	Log.d(TAG + ".onPostExecute(...)", " - statusCode: " + statusCode);
    }
    
    public int getStatusCode() {
    	return statusCode;
    }
    
    public File getSignedFile() {
    	return signedFile;
    }
    
	public String getMessage() {
		if(exception != null) return exception.getMessage();
		return message;
	}
	
	public Integer getId() {
		return id;
	}

	@Override
	public void processTaskMessages(List<String> messages, AsyncTask task) { }

	@Override
	public void showTaskResult(AsyncTask task) {
		Log.d(TAG + ".showTaskResult(...)", " - task: " + task.getClass());
		if(task instanceof SendFileTask) {
			SendFileTask sendFileTask = (SendFileTask)task;
			Log.d(TAG + ".showTaskResult(...)", " - sendFileTask: " + sendFileTask.getStatusCode());
			statusCode = sendFileTask.getStatusCode();
			message = sendFileTask.getMessage();
	    	listener.showTaskResult(this);
		}
		
	}
	
}