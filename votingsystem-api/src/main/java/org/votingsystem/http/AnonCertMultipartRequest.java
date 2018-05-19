package org.votingsystem.http;

import com.fasterxml.jackson.core.type.TypeReference;
import eu.europa.esig.dss.DSSDocument;
import eu.europa.esig.dss.InMemoryDocument;
import org.votingsystem.throwable.ValidationException;
import org.votingsystem.util.Constants;
import org.votingsystem.util.IOUtils;
import org.votingsystem.xml.XML;

import javax.servlet.http.Part;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class AnonCertMultipartRequest {

    private final static Logger log = Logger.getLogger(AnonCertMultipartRequest.class.getName());

    public Set<String> getRequestCSRSet() {
        return requestCSRSet;
    }

    public enum Type {CURRENCY_REQUEST, ELECTION_IDENTIFICATION}
    private DSSDocument dssDocument;
    private byte[] csrBytes;
    private Set<String> requestCSRSet;

    public AnonCertMultipartRequest(Collection<Part> parts, Type type) throws Exception {
        switch (type) {
            case CURRENCY_REQUEST:
                for(Part part : parts) {
                    if(part.getName().contains(Constants.CSR_CURRENCY_FILE_NAME)) {
                        requestCSRSet = new XML().getMapper().readValue(IOUtils.toByteArray(part.getInputStream()),
                                new TypeReference<Set<String>>() {});
                    } else if(part.getName().contains(Constants.CURRENCY_REQUEST_FILE_NAME)) {
                        dssDocument = new InMemoryDocument(part.getInputStream());
                    } else {
                        throw new ValidationException("CURRENCY_REQUEST - bad request - file name: " + part.getName());
                    }
                }
                if(requestCSRSet == null)
                    throw new ValidationException("ERROR - missing file: " + Constants.CSR_CURRENCY_FILE_NAME);
                if(dssDocument == null)
                    throw new ValidationException("ERROR - missing file: " + Constants.CURRENCY_REQUEST_FILE_NAME);
                break;
            case ELECTION_IDENTIFICATION:
                for(Part part : parts) {
                    if(part.getName().contains(Constants.CSR_FILE_NAME)) {
                        csrBytes = IOUtils.toByteArray(part.getInputStream());
                    } else if(part.getName().contains(Constants.ANON_CERTIFICATE_REQUEST_FILE_NAME)) {
                        dssDocument = new InMemoryDocument(part.getInputStream());
                    } else {
                        throw new ValidationException("ELECTION_IDENTIFICATION - bad request - file name: " + part.getName());
                    }
                }
                if(csrBytes == null)
                    throw new ValidationException("ERROR - missing file: " + Constants.CSR_FILE_NAME);
                if(dssDocument == null)
                    throw new ValidationException("ERROR - missing file: " + Constants.ANON_CERTIFICATE_REQUEST_FILE_NAME);
                break;

            default: throw new IllegalArgumentException("unprocessed type " + type);
        }
    }

    public byte[] getCSRBytes() {
        return csrBytes;
    }

    public DSSDocument getDssDocument() {
        return dssDocument;
    }

}