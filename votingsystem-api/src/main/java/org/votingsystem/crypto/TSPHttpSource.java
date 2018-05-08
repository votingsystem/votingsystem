package org.votingsystem.crypto;

import eu.europa.esig.dss.DSSException;
import eu.europa.esig.dss.DigestAlgorithm;
import eu.europa.esig.dss.x509.tsp.TSPSource;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.votingsystem.dto.ResponseDto;
import org.votingsystem.http.HttpConn;
import org.votingsystem.http.MediaType;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
public class TSPHttpSource implements TSPSource {

    private static final Logger log = Logger.getLogger(TSPHttpSource.class.getName());

    public String timeStampServiceURL;

    public TSPHttpSource(String timeStampServiceURL) {
        this.timeStampServiceURL = timeStampServiceURL;
    }

    @Override
    public TimeStampToken getTimeStampResponse(DigestAlgorithm digestAlgorithm, byte[] digest) throws DSSException {
        try {
            TimeStampRequestGenerator reqgen = new TimeStampRequestGenerator();
            reqgen.setCertReq(true);
            TimeStampRequest timeStampRequest = reqgen.generate(
                    new ASN1ObjectIdentifier(digestAlgorithm.getOid()), digest);
            ResponseDto responseDto = HttpConn.getInstance().doPostRequest(timeStampRequest.getEncoded(),
                    MediaType.TIMESTAMP_QUERY, timeStampServiceURL);
            if(ResponseDto.SC_OK == responseDto.getStatusCode()) {
                //timeStampToken = new TimeStampToken(new CMSSignedData(bytesToken));
                //ASN1InputStream asn1InputStream = new ASN1InputStream(new ByteArrayInputStream(bytesToken));
                //ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(asn1InputStream);
                //TimeStampResp timeStampResp = TimeStampResp.getInstance(asn1InputStream);
                byte[] bytesToken = responseDto.getMessageBytes();
                TimeStampResponse timeStampResponse = new TimeStampResponse(bytesToken);
                return timeStampResponse.getTimeStampToken();
            } else {
                log.log(Level.SEVERE, "Error fetching Timestamp: " + responseDto.getMessage());
                throw new DSSException("Error fetching Timestamp");
            }
        } catch (Exception ex) {
            throw new DSSException(ex);
        }
    }

}
