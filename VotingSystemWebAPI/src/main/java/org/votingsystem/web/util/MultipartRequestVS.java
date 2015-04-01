package org.votingsystem.web.util;

import org.apache.commons.io.IOUtils;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.throwable.ExceptionVS;
import org.votingsystem.throwable.ValidationExceptionVS;
import org.votingsystem.util.ContextVS;

import javax.servlet.http.Part;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class MultipartRequestVS {

    private final static Logger log = Logger.getLogger(MultipartRequestVS.class.getSimpleName());

    public enum Type {CURRENCY_REQUEST, ACCESS_REQUEST, REPRESENTATIVE_DATA, ANONYMOUS_DELEGATION}
    private SMIMEMessage smime;
    private byte[] csrBytes;
    private byte[] imageBytes;

    public MultipartRequestVS(Collection<Part> parts, Type type) throws Exception {
        switch (type) {
            case CURRENCY_REQUEST:
                for(Part part : parts) {
                    if(part.getName().contains(ContextVS.CSR_FILE_NAME)) {
                        csrBytes = IOUtils.toByteArray(part.getInputStream());
                    } else if(part.getName().contains(ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME)) {
                        smime = new SMIMEMessage(part.getInputStream());
                    } else {
                        throw new ValidationExceptionVS("CURRENCY_REQUEST - bad request - file name: " + part.getName());
                    }
                }
                if(csrBytes == null) throw new ValidationExceptionVS("ERROR - missing file: " + ContextVS.CSR_FILE_NAME);
                if(smime == null) throw new ValidationExceptionVS("ERROR - missing file: " + ContextVS.CURRENCY_REQUEST_DATA_FILE_NAME);
                break;
            case ACCESS_REQUEST:
                for(Part part : parts) {
                    if(part.getName().contains(ContextVS.CSR_FILE_NAME)) {
                        csrBytes = IOUtils.toByteArray(part.getInputStream());
                    } else if(part.getName().contains(ContextVS.ACCESS_REQUEST_FILE_NAME)) {
                        smime = new SMIMEMessage(part.getInputStream());
                    } else {
                        throw new ValidationExceptionVS("ACCESS_REQUEST - bad request - file name: " + part.getName());
                    }
                }
                if(csrBytes == null) throw new ValidationExceptionVS("ERROR - missing file: " + ContextVS.CSR_FILE_NAME);
                if(smime == null) throw new ValidationExceptionVS("ERROR - missing file: " + ContextVS.ACCESS_REQUEST_FILE_NAME);
                break;
            case REPRESENTATIVE_DATA:
                for(Part part : parts) {
                    if(part.getName().contains(ContextVS.IMAGE_FILE_NAME)) {
                        imageBytes = IOUtils.toByteArray(part.getInputStream());
                        if(imageBytes.length > ContextVS.IMAGE_MAX_FILE_SIZE) {
                            throw new ValidationExceptionVS("ERROR - imageSizeExceededMsg - received:" +
                                    imageBytes.length/1024 + " - max: " + ContextVS.IMAGE_MAX_FILE_SIZE_KB);
                        }
                    } else if(part.getName().contains(ContextVS.REPRESENTATIVE_DATA_FILE_NAME)) {
                        smime = new SMIMEMessage(part.getInputStream());
                    } else {
                        throw new ValidationExceptionVS("REPRESENTATIVE_DATA - bad request - file name: " + part.getName());
                    }
                }
                if(imageBytes == null) throw new ValidationExceptionVS("ERROR - missing file: " + ContextVS.IMAGE_FILE_NAME);
                if(smime == null) throw new ValidationExceptionVS("ERROR - missing file: " + ContextVS.REPRESENTATIVE_DATA_FILE_NAME);
                break;
            case ANONYMOUS_DELEGATION:
                for(Part part : parts) {
                    if(part.getName().contains(ContextVS.CSR_FILE_NAME)) {
                        csrBytes = IOUtils.toByteArray(part.getInputStream());
                    } else if(part.getName().contains(ContextVS.REPRESENTATIVE_DATA_FILE_NAME)) {
                        smime = new SMIMEMessage(part.getInputStream());
                    } else {
                        throw new ValidationExceptionVS("ANONYMOUS_DELEGATION - bad request - file name: " + part.getName());
                    }
                }
                if(csrBytes == null) throw new ValidationExceptionVS("ERROR - missing file: " + ContextVS.CSR_FILE_NAME);
                if(smime == null) throw new ValidationExceptionVS("ERROR - missing file: " + ContextVS.REPRESENTATIVE_DATA_FILE_NAME);
                break;
            default: throw new ExceptionVS("unprocessed type " + type);
        }
    }

    public SMIMEMessage getSMIME() {
        return smime;
    }

    public byte[] getCSRBytes() {
        return csrBytes;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

}