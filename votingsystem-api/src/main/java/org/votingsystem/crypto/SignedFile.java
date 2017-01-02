package org.votingsystem.crypto;

import org.votingsystem.model.SignedDocument;
import org.votingsystem.util.FileUtils;
import org.votingsystem.util.OperationType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignedFile {

    private static Logger log = Logger.getLogger(SignedFile.class.getName());

    private byte[] signedFileBytes = null;
    private String name = null;
    private File file = null;
    private OperationType operationType;
    private SignedDocument signedDocument = null;


    public SignedFile() {}

    public boolean isValidSignature() {
        return signedDocument != null;
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


    public String getCaption( ) throws Exception {
        switch (operationType) {
            case SEND_VOTE:
                return "vote";
            default: return "signedDocument";
        }
    }

    public OperationType getOperationType() throws Exception {
        return operationType;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCMS() {
        if(signedDocument != null) return true;
        if(signedFileBytes == null) return false;
        if(name.toLowerCase().endsWith(".p7s") && (signedDocument != null)) return true;
        else return false;
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


    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public void setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
    }
}