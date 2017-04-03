package org.votingsystem.ocsp;

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.client.http.DataLoader;
import eu.europa.esig.dss.client.http.commons.DSSNotifier;
import org.votingsystem.http.HttpConn;
import org.votingsystem.dto.ResponseDto;

import java.util.List;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class DNIeOCSPDataLoader implements DataLoader, DSSNotifier {

    private String contentType;

    public DNIeOCSPDataLoader(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public byte[] get(String url) {
        return new byte[0];
    }

    @Override
    public DataAndUrl get(List<String> urlStrings) {
        return null;
    }

    @Override
    public byte[] get(String url, boolean refresh) {
        return new byte[0];
    }

    @Override
    public byte[] post(String url, byte[] content) {
        ResponseDto response = HttpConn.getInstance().doPostRequest(content, contentType, url);
        try {
            if(ResponseDto.SC_OK != response.getStatusCode())
                throw new Exception(response.getMessage());
            return response.getMessageBytes();
        } catch (Exception ex) {
            throw new DSSException(ex);
        }
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public void update() {

    }
}