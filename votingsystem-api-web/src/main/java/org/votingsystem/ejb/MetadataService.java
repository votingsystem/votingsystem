package org.votingsystem.ejb;

import org.votingsystem.dto.metadata.MetadataDto;
import org.votingsystem.throwable.HttpRequestException;
import org.votingsystem.throwable.XMLValidationException;

import java.io.IOException;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface MetadataService {

    public MetadataDto getMetadataFromURL(String metadataURL, boolean validateSignature, boolean withTimeStampValidation)
            throws XMLValidationException, HttpRequestException;

    public byte[] getMetadataSigned() throws IOException;
}
