package org.votingsystem.client.model;


import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.log4j.Logger;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.model.PDFDocumentVS;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Map;

/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class SignedFile {
        
    private static Logger log = Logger.getLogger(SignedFile.class);
    
    private byte[] signedFileBytes = null;
    private String name = null;
    private Map operationDocument = null;
    private TimeStampToken timeStampToken = null;
    private SMIMEMessage smimeMessage = null;
    private boolean signatureVerified = false;


    public SignedFile(byte[] signedFileBytes, String name, Map operationDocument) throws Exception {
        smimeMessage = new SMIMEMessage(new ByteArrayInputStream(signedFileBytes));
        signatureVerified = smimeMessage.isValidSignature();
        this.operationDocument = operationDocument;
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

    public SMIMEMessage getSMIMEMessageWraper() {
        return smimeMessage;
    }
    
    public JSONObject getContent() throws Exception {
        JSONObject contentJSON = (JSONObject) JSONSerializer.toJSON(smimeMessage.getSignedContent());
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
        if(smimeMessage != null) return true;
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".p7m") && signatureVerified) return true;
        else return false;
    }
    
    public String getSignerNif() {
        return smimeMessage.getSigner().getNif();
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
        if(smimeMessage == null) return null;
        String signedContent = smimeMessage.getSignedContent();
        Object content = JSONSerializer.toJSON(signedContent);
        if(content instanceof JSONObject) {
            JSONObject contentJSON = (JSONObject)content;
            if(contentJSON.containsKey("optionSelectedId")) {
                return contentJSON.getLong("optionSelectedId");
            }
        } else {
            log.error(" File '" + name + "' content is instance of " + content.getClass());
        }
        return null;
    }
    
    public Long getSignerCertSerialNumber() {
        if(smimeMessage == null) return null;
        UserVS userVS = smimeMessage.getSigner();
        return userVS.getCertificate().getSerialNumber().longValue();
    }

    public Map getOperationDocument() {
        return operationDocument;
    }

    public void setOperationDocument(Map operationDocument) {
        this.operationDocument = operationDocument;
    }

}
