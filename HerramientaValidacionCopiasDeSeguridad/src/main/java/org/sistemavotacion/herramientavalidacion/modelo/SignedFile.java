package org.sistemavotacion.herramientavalidacion.modelo;

import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfPKCS7;
import com.itextpdf.text.pdf.PdfReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.bouncycastle.tsp.TimeStampToken;
import org.sistemavotacion.smime.SMIMEMessageWrapper;
import org.sistemavotacion.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignedFile {
        
    private static Logger logger = LoggerFactory.getLogger(SignedFile.class);
    
    private byte[] signedFileBytes = null;
    private String name = null;
    private SMIMEMessageWrapper smimeMessageWraper = null;
    private boolean signatureVerified = false;

    
    public SignedFile(byte[] signedFileBytes, String name) throws Exception {
        this.name = name;
        this.signedFileBytes = signedFileBytes;
        if(name.toLowerCase().endsWith(".pdf")) {
            PdfReader reader = new PdfReader(signedFileBytes);
            AcroFields acroFields = reader.getAcroFields();
            ArrayList<String> names = acroFields.getSignatureNames();
            for (String sigName : names) {
                //logger.debug(" - SignedFile - covers whole document: " + 
                //        acroFields.signatureCoversWholeDocument(sigName));
                PdfPKCS7 pk = acroFields.verifySignature(sigName, "BC");
                if(!pk.verify()) {
                    //logger.error(" - checkSignature - VERIFICATION FAILED!!!");
                    signatureVerified = false;
                } else {
                    //logger.error("checkSignature - OK");
                    signatureVerified = true;
                } 
                //X509Certificate signingCert = pk.getSigningCertificate();
                //logger.debug(" checkSignature - signingCert: " + signingCert.getSubjectDN());
                //Calendar signDate = pk.getSignDate();
                //logger.debug(" - checkSignature - signingCert: " + signDate.getTime().toString());
                //X509Certificate[] pkc = (X509Certificate[])pk.getSignCertificateChain();
                TimeStampToken timeStampToken = pk.getTimeStampToken();
                //logger.debug(" - checkSignature - timeStampToken: " + 
                //        timeStampToken.getTimeStampInfo().getGenTime().toString());
                //KeyStore keyStore = firmaService.getTrustedCertsKeyStore()
                //Object[] fails = PdfPKCS7.verifyCertificates(pkc, keyStore, null, signDate);
                //if(fails != null) {...}
            }
            
        } else if(name.toLowerCase().endsWith(".p7m")){
            smimeMessageWraper = new SMIMEMessageWrapper(null,
                new ByteArrayInputStream(signedFileBytes), null);
            signatureVerified = smimeMessageWraper.isValidSignature();
        } else {
            logger.error(" #### UNKNOWN FILE TYPE -> " + name);
        }
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
    
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isPDF() {
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".pdf") && 
                signatureVerified) return true;
        else return false;
    }
    
    public boolean isSMIME() {
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".p7m") && 
                signatureVerified) return true;
        else return false;
    }
    
    public byte[] getFileBytes() {
        return signedFileBytes;
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
            if(contentJSON.containsKey("opcionSeleccionadaId")) {
                return contentJSON.getLong("opcionSeleccionadaId");
            }
        } else {
            logger.error(" =========== File '" + name + "' content is instance of " + 
                    content.getClass());
            logger.error(" =========== content:" + content);
        }
        return null;
    }
    
}
