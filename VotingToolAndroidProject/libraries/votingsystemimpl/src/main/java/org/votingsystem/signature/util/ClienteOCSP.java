package org.votingsystem.signature.util;

import android.util.Log;
import org.bouncycastle2.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle2.ocsp.*;
import org.votingsystem.model.ContentTypeVS;
import org.votingsystem.model.ContextVS;
import org.votingsystem.model.CertificateVS;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ClienteOCSP {

    public static CertificateVS.State validarCertificado(X509Certificate certificate, BigInteger serialNumber,
                 Date checkDate) throws Exception {
        OCSPReqGenerator ocspReqGen = new OCSPReqGenerator();
        ocspReqGen.addRequest(new CertificateID(CertificateID.HASH_SHA1, certificate, serialNumber));
        OCSPReq ocspReq = ocspReqGen.generate();
        URL url = new URL(ContextVS.OCSP_DNIE_URL);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestProperty("Content-Type", ContentTypeVS.OCSP_REQUEST);
        con.setRequestProperty("Accept", ContentTypeVS.OCSP_RESPONSE);
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
            CertificateVS.State certificateState = null;
            basicOCSPResp = (BasicOCSPResp) ocspResponse.getResponseObject();
            for(SingleResp singleResponse : basicOCSPResp.getResponses()){
                Object stat = singleResponse.getCertStatus();
                if (stat == CertificateStatus.GOOD) {
                    certificateState = CertificateVS.State.OK;
                }else if (stat instanceof RevokedStatus) {
                    Date fechaRevocacion = ((RevokedStatus)stat).getRevocationTime();
                    if (checkDate.after(fechaRevocacion)) 
                        certificateState = CertificateVS.State.CANCELLED;
                    else certificateState = CertificateVS.State.OK;
                }else if (stat instanceof UnknownStatus) {
                    certificateState = CertificateVS.State.UNKNOWN;
                }
            }
            return certificateState;
        } else return null;
    }

}