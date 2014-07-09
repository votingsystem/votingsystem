package org.votingsystem.client.model;

import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfPKCS7;
import com.itextpdf.text.pdf.PdfReader;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.model.PDFDocumentVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessageWrapper;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class SignedFile {
        
    private static Logger logger = Logger.getLogger(SignedFile.class);
    
    private byte[] signedFileBytes = null;
    private String name = null;
    private TimeStampToken timeStampToken = null;
    private SMIMEMessageWrapper smimeMessageWraper = null;
    private boolean signatureVerified = false;
    private PdfPKCS7 pdfPKCS7 = null;
    private PDFDocumentVS pdfDocument = null;
    
    public SignedFile(byte[] signedFileBytes, String name) throws Exception {
        this.name = name;
        this.signedFileBytes = signedFileBytes;
        if(name.toLowerCase().endsWith(".pdf")) {
            pdfDocument = new PDFDocumentVS();
            PdfReader reader = new PdfReader(signedFileBytes);
            AcroFields acroFields = reader.getAcroFields();
            ArrayList<String> names = acroFields.getSignatureNames();
            for (String sigName : names) {
                logger.debug(" - PDF SignedFile - covers whole document: " +
                        acroFields.signatureCoversWholeDocument(sigName));
                pdfPKCS7 = acroFields.verifySignature(sigName, "BC");
                timeStampToken = pdfPKCS7.getTimeStampToken();
                X509Certificate signingCert = pdfPKCS7.getSigningCertificate();
                UserVS userVS  = UserVS.getUserVS(signingCert);
                pdfDocument.setUserVS(userVS);
                userVS.setTimeStampToken(timeStampToken);
                pdfDocument.setTimeStampToken(timeStampToken);
                if(!pdfPKCS7.verify()) {
                    signatureVerified = false;
                    pdfDocument.setState(PDFDocumentVS.State.ERROR);
                } else {
                    signatureVerified = true;
                    pdfDocument.setState(PDFDocumentVS.State.VALIDATED);
                }
            }
            
        } else if(name.toLowerCase().endsWith(".p7s")){
            smimeMessageWraper = new SMIMEMessageWrapper(new ByteArrayInputStream(signedFileBytes));
            signatureVerified = smimeMessageWraper.isValidSignature();
            if(signatureVerified) timeStampToken = smimeMessageWraper.getSigner().getTimeStampToken();
        } else {
            logger.error("#### file type unknown -> " + name + " trying with SMIMEMessageWrapper");
            smimeMessageWraper = new SMIMEMessageWrapper(new ByteArrayInputStream(signedFileBytes));
            signatureVerified = smimeMessageWraper.isValidSignature();
        }
    }

    public PdfPKCS7 getPdfPKCS7() {
        return pdfPKCS7;
    }

    public PDFDocumentVS getPdfDocument() {
        return pdfDocument;
    }
    
    public boolean isValidSignature() {
        return signatureVerified;
    }
    
    /**
     * @return the signedFileBytes
     */
    public byte[] getSignedFileBytes() {
        return signedFileBytes;
    }

    /**
     * @param signedFileBytes the signedFileBytes to set
     */
    public void setSignedFileBytes(byte[] signedFileBytes) {
        this.signedFileBytes = signedFileBytes;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public SMIMEMessageWrapper getSMIMEMessageWraper() {
        return smimeMessageWraper;
    }
    
    public JSONObject getContent() throws Exception {
        JSONObject contentJSON = (JSONObject) JSONSerializer.toJSON(smimeMessageWraper.getSignedContent());
        return contentJSON;
    }    
    
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isPDF() {
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".pdf") && signatureVerified) return true;
        else return false;
    }
    
    public boolean isSMIME() {
        if(smimeMessageWraper != null) return true;
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".p7m") && signatureVerified) return true;
        else return false;
    }
    
    public String getSignerNif() {
        if(isPDF()) return pdfDocument.getUserVS().getNif();
        else return smimeMessageWraper.getSigner().getNif();
    }
    
    public byte[] getFileBytes() {
        return signedFileBytes;
    }
    
    public String getNifFromRepresented() {
        if(name != null && name.contains("_RepDoc_")) return name.split("_RepDoc_")[1].split("\\.")[0];
        else return null;
    }
    
    public File getFile() throws Exception {
        File file = File.createTempFile("signedFile", ".votingSystem");
        file.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(signedFileBytes), file);
        return file;
    }

    public Long getSelectedOptionId() {
        if(smimeMessageWraper == null) return null;
        String signedContent = smimeMessageWraper.getSignedContent();
        Object content = JSONSerializer.toJSON(signedContent);
        if(content instanceof JSONObject) {
            JSONObject contentJSON = (JSONObject)content;
            if(contentJSON.containsKey("optionSelectedId")) {
                return contentJSON.getLong("optionSelectedId");
            }
        } else {
            logger.error(" File '" + name + "' content is instance of " + content.getClass());
        }
        return null;
    }
    
    public Long getSignerCertSerialNumber() {
        if(smimeMessageWraper == null) return null;
        UserVS userVS = smimeMessageWraper.getSigner();
        return userVS.getCertificate().getSerialNumber().longValue();
    }

}
