package org.votingsystem.client.util;


import org.votingsystem.http.ContentType;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface DocumentContainer {

    public byte[] getDocumentBytes() throws Exception;
    public ContentType getContentType();

}
