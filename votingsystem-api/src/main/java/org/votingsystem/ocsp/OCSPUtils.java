package org.votingsystem.ocsp;

import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.*;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.votingsystem.model.Certificate;
import org.votingsystem.util.Constants;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;


/**
* License: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class OCSPUtils {

    public static Certificate.State validateCert(X509CertificateHolder intermediateCertHolder,
                                 BigInteger serialNumber, Date dateCheck) throws Exception {
        DigestCalculatorProvider digCalcProv = new JcaDigestCalculatorProviderBuilder().setProvider(Constants.PROVIDER).build();

        CertificateID id = new CertificateID(digCalcProv.get(CertificateID.HASH_SHA1), intermediateCertHolder, serialNumber);
        OCSPReqBuilder gen = new OCSPReqBuilder();
        gen.addRequest(id);
        OCSPReq ocspReq = gen.build();
        URL url = new URL(Constants.OCSP_DNIE_URL);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestProperty("Content-Type", "application/ocsp-request");

        con.setDoOutput(true);
        OutputStream out = con.getOutputStream();
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out));
        dataOut.write(ocspReq.getEncoded());
        dataOut.flush();
        dataOut.close();
        InputStream in = (InputStream) con.getContent();
        OCSPResp ocspResponse = new OCSPResp(in);
        BasicOCSPResp basicOCSPResp ;
        if(ocspResponse.getStatus() == OCSPResponseStatus.SUCCESSFUL) {
            Certificate.State certificateState = null;
            basicOCSPResp = (BasicOCSPResp) ocspResponse.getResponseObject();
            for(SingleResp singleResponse : basicOCSPResp.getResponses()){
                Object stat = singleResponse.getCertStatus();
                if (stat == CertificateStatus.GOOD) {
                    certificateState = Certificate.State.OK;
                } else if (stat instanceof RevokedStatus) {
                    Date revocationDate = ((RevokedStatus)stat).getRevocationTime();
                    if (dateCheck.after(revocationDate))
                        certificateState = Certificate.State.CANCELED;
                    else
                        certificateState = Certificate.State.OK;
                } else if (stat instanceof UnknownStatus) {
                    certificateState = Certificate.State.UNKNOWN;
                }
            }
            return certificateState;
        } else return Certificate.State.UNKNOWN;
    }

}