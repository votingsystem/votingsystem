package org.votingsystem.signature.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.model.UserVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.JSON;
import org.votingsystem.util.TypeVS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignedFile {

    private static Logger log = Logger.getLogger(SignedFile.class.getSimpleName());

    private byte[] signedFileBytes = null;
    private String name = null;
    private Map operationDocument = null;
    private SMIMEMessage smimeMessage = null;
    private boolean signatureVerified = false;


    public SignedFile(byte[] signedFileBytes, String name, Map operationDocument) throws Exception {
        smimeMessage = new SMIMEMessage(signedFileBytes);
        signatureVerified = smimeMessage.isValidSignature();
        this.operationDocument = operationDocument;
        this.name = name;
    }

    public boolean isValidSignature() {
        return signatureVerified;
    }

    public byte[] getSignedFileBytes() {
        return signedFileBytes;
    }

    public void setSignedFileBytes(byte[] signedFileBytes) {
        this.signedFileBytes = signedFileBytes;
    }

    public String getName() {
        return name;
    }

    public SMIMEMessage getSMIME() {
        return smimeMessage;
    }

    public String getCaption( ) throws Exception {
        TypeVS operation = getTypeVS();
        if(operation == null) return null;
        switch (operation) {
            case SEND_VOTE:
                return ContextVS.getMessage("voteLbl");
            default: return ContextVS.getMessage("signedDocumentCaption");
        }
    }

    public TypeVS getTypeVS() throws Exception {
        Map dataMap = getContent();
        if(dataMap.containsKey("operation")) {
            return TypeVS.valueOf((String) dataMap.get("operation"));
        }
        return null;
    }

    public Map getContent() throws Exception {
        return  JSON.getMapper().readValue(smimeMessage.getSignedContent(),
                new TypeReference<HashMap<String, Object>>() {});
    }

    public TimeStampToken getTimeStampToken () {
        return smimeMessage.getTimeStampToken();
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSMIME() {
        if(smimeMessage != null) return true;
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".p7m") && signatureVerified) return true;
        else return false;
    }

    public String getSignerNif() throws Exception {
        return smimeMessage.getSigner().getNif();
    }

    public byte[] getFileBytes() {
        return signedFileBytes;
    }

    public String getNifFromRepresented() {
        if(name != null && name.contains("_delegation_")) return name.split("_delegation_")[0];
        else return null;
    }

    public File getFile() throws Exception {
        File file = File.createTempFile("signedFile", ".votingSystem");
        file.deleteOnExit();
        FileUtils.copyStreamToFile(new ByteArrayInputStream(signedFileBytes), file);
        return file;
    }

    public Long getSelectedOptionId() throws Exception {
        if(smimeMessage == null) return null;
        String signedContent = smimeMessage.getSignedContent();
        Map dataMap = getContent();
        if(dataMap.containsKey("optionSelectedId")) {
            return Long.valueOf(((Integer)dataMap.get("optionSelectedId")).longValue());
        }
        return null;
    }

    public Long getSignerCertSerialNumber() throws Exception {
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