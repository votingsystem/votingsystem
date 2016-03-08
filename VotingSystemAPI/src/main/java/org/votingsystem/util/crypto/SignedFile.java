package org.votingsystem.util.crypto;

import com.fasterxml.jackson.core.type.TypeReference;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.model.UserVS;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.FileUtils;
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

    private static Logger log = Logger.getLogger(SignedFile.class.getName());

    private byte[] signedFileBytes = null;
    private String name = null;
    private File file = null;
    private CMSSignedMessage cmsMessage = null;
    private boolean signatureVerified = false;

    public SignedFile(byte[] signedFileBytes, String name) throws Exception {
        cmsMessage = new CMSSignedMessage(signedFileBytes);
        signatureVerified = cmsMessage.isValidSignature();
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

    public CMSSignedMessage getCMS() {
        return cmsMessage;
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
        return cmsMessage.getSignedContent(new TypeReference<HashMap<String, Object>>() {});
    }

    public TimeStampToken getTimeStampToken () throws Exception {
        return cmsMessage.getTimeStampToken();
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCMS() {
        if(cmsMessage != null) return true;
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".p7m") && signatureVerified) return true;
        else return false;
    }

    public String getSignerNif() throws Exception {
        return cmsMessage.getSigner().getNif();
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
        if(cmsMessage == null) return null;
        String signedContent = cmsMessage.getSignedContentStr();
        Map dataMap = getContent();
        if(dataMap.containsKey("optionSelectedId")) {
            return Long.valueOf(((Integer)dataMap.get("optionSelectedId")).longValue());
        }
        return null;
    }

    public Long getSignerCertSerialNumber() throws Exception {
        if(cmsMessage == null) return null;
        UserVS userVS = cmsMessage.getSigner();
        return userVS.getCertificate().getSerialNumber().longValue();
    }

}