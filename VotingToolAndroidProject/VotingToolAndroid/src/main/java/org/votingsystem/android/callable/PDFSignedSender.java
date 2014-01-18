package org.votingsystem.android.callable;

import android.content.Context;
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
import org.votingsystem.android.AppContextVS;
import org.votingsystem.android.R;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.util.Encryptor;
import org.votingsystem.signature.util.KeyStoreUtil;
import org.votingsystem.signature.util.PDF_CMSSignedGenerator;
import org.votingsystem.signature.util.VotingSystemKeyStoreException;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.HttpHelper;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.mail.Header;
import javax.mail.internet.MimeBodyPart;

import static org.votingsystem.model.ContextVS.KEY_STORE_FILE;
import static org.votingsystem.model.ContextVS.USER_CERT_ALIAS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
 */
public class PDFSignedSender implements Callable<ResponseVS> {

    public static final String TAG = "PDFSignedSender";

    public static final PdfName PDF_SIGNATURE_NAME     = PdfName.ADBE_PKCS7_SHA1;
    public static final String PDF_SIGNATURE_DIGEST    = "SHA1";
    public static final String PDF_SIGNATURE_MECHANISM = "SHA1withRSA";
    public static final String TIMESTAMP_PDF_HASH      = TSPAlgorithms.SHA1;
    public static final String PDF_DIGEST_OID          = CMSSignedDataGenerator.DIGEST_SHA1;

    private String location;
    private String reason;
    private AppContextVS contextVS;
    private char[] password;
    byte[] pdfBytes = null;
    private String serviceURL = null;

    public PDFSignedSender(byte[] pdfBytes, String serviceURL, char[] password, String reason,
            String location, AppContextVS context) {
        this.pdfBytes = pdfBytes;
        this.serviceURL = serviceURL;
        this.contextVS = context;
        this.reason = reason;
        this.location = location;
        this.password = password;
    }

    @Override public ResponseVS call() {
        ResponseVS responseVS = null;
        try {
            FileInputStream fis = contextVS.openFileInput(KEY_STORE_FILE);
            byte[] keyStoreBytes = FileUtils.getBytesFromInputStream(fis);
            KeyStore keyStore = KeyStoreUtil.getKeyStoreFromBytes(keyStoreBytes, password);
            PrivateKey signerPrivatekey = (PrivateKey)keyStore.getKey(USER_CERT_ALIAS, password);
            //X509Certificate signerCert = (X509Certificate) keyStore.getCertificate(USER_CERT_ALIAS);
            Certificate[] signerCertChain = keyStore.getCertificateChain(USER_CERT_ALIAS);
            X509Certificate signerCert = (X509Certificate) signerCertChain[0];
            PdfReader pdfReader = new PdfReader(pdfBytes);
            byte[] timeStampedSignedPDF = signWithTimestamp(pdfReader,
                    signerCert, signerPrivatekey, signerCertChain);
            Header header = new Header(ContextVS.VOTING_HEADER_LABEL, ContextVS.BASE64_ENCODED_CONTENT_TYPE);
            MimeBodyPart mimeBodyPart = Encryptor.encryptBase64Message(
                    timeStampedSignedPDF, contextVS.getAccessControl().
                    getCertificate(), header);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mimeBodyPart.writeTo(baos);
            byte[] bytesToSend = baos.toByteArray();
            baos.close();
            responseVS = HttpHelper.sendData(bytesToSend, ContentTypeVS.PDF_SIGNED_AND_ENCRYPTED,
                    serviceURL);
        } catch(VotingSystemKeyStoreException ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR,
                    contextVS.getString(R.string.pin_error_msg));
        } catch (Exception ex) {
            ex.printStackTrace();
            responseVS = new ResponseVS(ResponseVS.SC_ERROR, ex.getMessage());
        } finally { return responseVS; }
    }

    public byte[] signWithTimestamp(PdfReader pdfReader, X509Certificate signerCert,
            PrivateKey signerPrivatekey, Certificate[] signerCertChain) throws Exception {
        Log.d(TAG + ".signWithTimestamp(...)", "certsChain.length: " + signerCertChain.length);
        PDF_CMSSignedGenerator signedGenerator = new PDF_CMSSignedGenerator(signerPrivatekey,
                signerCertChain, PDF_SIGNATURE_MECHANISM, PDF_SIGNATURE_DIGEST, PDF_DIGEST_OID);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfStamper stp = PdfStamper.createSignature(pdfReader, baos, '\0');
        stp.setEncryption(null, null, PdfWriter.ALLOW_PRINTING, false);
        final PdfSignatureAppearance signatureAppearance = stp.getSignatureAppearance();
        signatureAppearance.setVisibleSignature(new Rectangle(100, 10, 400, 40), 1, null);

        if(location != null) signatureAppearance.setLocation(location);
        signatureAppearance.setCrypto(null, signerCertChain, null, PdfSignatureAppearance.WINCER_SIGNED);
        if(reason != null) signatureAppearance.setReason(reason);
        //java.util.Calendar cal = java.util.Calendar.getInstance();
        //signatureAppearance.setSignDate(cal);
        //signatureAppearance.setContact("This is the Contact");
        signatureAppearance.setAcro6Layers(true);
        final PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE, PDF_SIGNATURE_NAME);
        //dic.setDate(new PdfDate(signatureAppearance.getSignDate()));
        dic.setName(PdfPKCS7.getSubjectFields(signerCert).getField("CN"));
        Log.d(TAG, "signWithTimestamp - UserVS: " + PdfPKCS7.getSubjectFields(signerCert).getField("CN"));
        signatureAppearance.setCryptoDictionary(dic);
        int csize = 10000;
        HashMap exc = new HashMap();
        //Nota del javadoc -> due to the hex string coding this size should be byte_size*2+2.
        exc.put(PdfName.CONTENTS, new Integer(csize * 2 + 2));
        String signerVS = PdfPKCS7.getSubjectFields(signerCert).getField("CN");

        if(signerVS != null && signerVS.contains("(FIRMA)")) {
            signerVS = signerVS.replace("(FIRMA)", "");
        } else signerVS = getUserNIF(signerCert);

        signatureAppearance.setLayer2Text(
                contextVS.getString(R.string.pdf_signed_by_lbl) + ":\n" + signerVS);

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
                            new MessageTimeStamper(TIMESTAMP_PDF_HASH, digest, contextVS);
                    ResponseVS responseVS = messageTimeStamper.call();
                    if(ResponseVS.SC_OK != responseVS.getStatusCode()) {
                        Log.d(TAG + ".signWithTimestamp(...)", " - Error timestamping: " +
                                responseVS.getMessage());
                        return null;
                    }
                    TimeStampToken timeStampToken = messageTimeStamper.getTimeStampToken();

                    final Calendar cal = new GregorianCalendar();
                    cal.setTime(timeStampToken.getTimeStampInfo().getGenTime());
                    Log.d(TAG, "*** TimeStamp: " + DateUtils.getDateStr(cal.getTime()));

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

        dic.setDate(new PdfDate(signatureAppearance.getSignDate()));
        signatureAppearance.preClose(exc);
        MessageDigest md = MessageDigest.getInstance(PDF_SIGNATURE_DIGEST);
        byte[] signatureHash = md.digest(FileUtils.getBytesFromInputStream(signatureAppearance.getRangeStream()));

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
        signatureAppearance.close(dic2);
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
        //signatureAppearance.setReason("Support this");
        //signatureAppearance.setLocation("Higueruela");
        signatureAppearance.setSignDate( new GregorianCalendar() );
        signatureAppearance.setVisibleSignature(new Rectangle(50, 40, 300, 140), 1, null);
        stp.close();
    }

    private String getUserNIF (X509Certificate certificate) {
        String subjectDN = certificate.getSubjectDN().getName();
        String nif = null;
        if(subjectDN.split("SERIALNUMBER=").length > 1) {
            nif = subjectDN.split("SERIALNUMBER=")[1];
            if (nif.split(",").length > 1) {
                nif = nif.split(",")[0];
            }
        }
        return nif;
    }
}
