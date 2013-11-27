package org.votingsystem.signature.util;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.ocsp.*;
import org.votingsystem.model.CertificateVS;
import org.votingsystem.model.ContextVS;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ClienteOCSP {

    private static Logger logger = Logger.getLogger(ClienteOCSP.class);

    public static CertificateVS.State validarCertificado(X509Certificate intermediateCert, BigInteger serialNumber,
                    Date dateCheck) throws Exception {
        OCSPReqGenerator ocspReqGen = new OCSPReqGenerator();
        ocspReqGen.addRequest(new CertificateID(CertificateID.HASH_SHA1, intermediateCert, serialNumber));
        OCSPReq ocspReq = ocspReqGen.generate();
        URL url = new URL(ContextVS.OCSP_DNIE_URL);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestProperty("Content-Type", "application/ocsp-request");
        con.setRequestProperty("Accept", "application/ocsp-response");
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
                    if (dateCheck.after(fechaRevocacion)) 
                        certificateState = CertificateVS.State.CANCELLED;
                    else certificateState = CertificateVS.State.OK;
                }else if (stat instanceof UnknownStatus) {
                    certificateState = CertificateVS.State.UNKNOWN;
                }
            }
            return certificateState;
        } else return null;
    }

    public static void main(String [] args){
        try {
            X509Certificate caCert = readCert("./test/ca.pem");
            X509Certificate interCert = readCert("./test/inter.pem");
            X509Certificate clientCert = readCert("./test/client.pem");
            logger.info("Cert state: " + validarCertificado(interCert, clientCert.getSerialNumber(),
                    new Date(System.currentTimeMillis())).toString());
        } catch (Exception ex){
            logger.error(ex.getMessage(), ex);
        }
    }


    private static X509Certificate readCert(String fileName) throws FileNotFoundException, CertificateException {
        InputStream is = new FileInputStream(fileName);
        BufferedInputStream bis = new BufferedInputStream(is);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(bis);
        return cert;
    }

}
