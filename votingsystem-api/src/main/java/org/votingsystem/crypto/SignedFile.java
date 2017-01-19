package org.votingsystem.crypto;

import org.votingsystem.http.MediaType;
import org.votingsystem.model.SignedDocument;
import org.votingsystem.util.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class SignedFile {

    private static Logger log = Logger.getLogger(SignedFile.class.getName());

    public enum Type {
        VOTE(MediaType.XML), CURRENCY_TRANSACTION(MediaType.XML), XML(MediaType.XML), JSON(MediaType.JSON);

        private String mediaType;

        Type(String mediaType) {
            this.mediaType = mediaType;
        }

        public String getMediaType() {
            return mediaType;
        }
    }

    private byte[] body = null;
    private String name = null;
    private File file = null;
    private Type type;
    private SignedDocument signedDocument = null;


    public SignedFile() {}

    public SignedFile(byte[] body, String fileName) {
        this.body = body;
        this.name = fileName;
    }

    public boolean isValidSignature() {
        return signedDocument != null;
    }

    public byte[] getBody() {
        return body;
    }

    public String getName() {
        return name;
    }

    public String getCaption( ) throws Exception {
        switch (type) {
            case VOTE:
                return "vote";
            default: return "signed-document";
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public File getFile() throws Exception {
        if(file == null) {
            file = File.createTempFile("signedFile", ".votingSystem");
            file.deleteOnExit();
            FileUtils.copyStreamToFile(new ByteArrayInputStream(body), file);
        }
        return file;
    }

    public SignedDocument getSignedDocument() {
        return signedDocument;
    }

    public SignedFile setSignedDocument(SignedDocument signedDocument) {
        this.signedDocument = signedDocument;
        return this;
    }

    public SignedFile setFile(File file) {
        this.file = file;
        return this;
    }

    public Type getType() {
        return type;
    }

    public SignedFile setType(Type type) {
        this.type = type;
        return this;
    }

}