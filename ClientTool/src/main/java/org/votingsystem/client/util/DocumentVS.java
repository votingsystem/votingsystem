package org.votingsystem.client.util;

import org.votingsystem.model.ContentTypeVS;

/**
 * @author jgzornoza
 * Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public interface DocumentVS {

    public byte[] getDocumentBytes() throws Exception;
    public ContentTypeVS getContentTypeVS();

}
