package org.sistemavotacion.seguridad;

import java.security.cert.*;
import java.security.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import org.bouncycastle.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.CertificateStatus;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPReqGenerator;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.RevokedStatus;
import org.bouncycastle.ocsp.SingleResp;
import org.bouncycastle.ocsp.UnknownStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class ClienteOCSP {

    private static Logger logger = LoggerFactory.getLogger(ClienteOCSP.class);

    public enum EstadoCertificado {OK, REVOCADO, DESCONOCIDO}

    private static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";


    public static EstadoCertificado validarCertificado(X509Certificate certificadoIntermedio,
            BigInteger numeroSerieCertificado, Date fechaComprobacion) throws Exception {
        OCSPReqGenerator ocspReqGen = new OCSPReqGenerator();
        ocspReqGen.addRequest(new CertificateID(
                CertificateID.HASH_SHA1, certificadoIntermedio, numeroSerieCertificado));
        OCSPReq ocspReq = ocspReqGen.generate();
        URL url = new URL(OCSP_DNIE_URL);
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
            EstadoCertificado estadoCertificado = null;
            basicOCSPResp = (BasicOCSPResp) ocspResponse.getResponseObject();
            for(SingleResp singleResponse : basicOCSPResp.getResponses()){
                Object stat = singleResponse.getCertStatus();
                if (stat == CertificateStatus.GOOD) {
                    estadoCertificado = EstadoCertificado.OK;
                }else if (stat instanceof RevokedStatus) {
                    Date fechaRevocacion = ((RevokedStatus)stat).getRevocationTime();
                    if (fechaComprobacion.after(fechaRevocacion)) 
                        estadoCertificado = EstadoCertificado.REVOCADO;
                    else estadoCertificado = EstadoCertificado.OK;
                }else if (stat instanceof UnknownStatus) {
                    estadoCertificado = EstadoCertificado.DESCONOCIDO;
                }
            }
            return estadoCertificado;
        } else return null;
    }

}
