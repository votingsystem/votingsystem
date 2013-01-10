package org.sistemavotacion.seguridad;

import java.security.cert.*;
import java.security.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;

import org.bouncycastle2.asn1.ocsp.OCSPResponseStatus;
import org.bouncycastle2.ocsp.BasicOCSPResp;
import org.bouncycastle2.ocsp.CertificateID;
import org.bouncycastle2.ocsp.CertificateStatus;
import org.bouncycastle2.ocsp.OCSPReq;
import org.bouncycastle2.ocsp.OCSPReqGenerator;
import org.bouncycastle2.ocsp.OCSPResp;
import org.bouncycastle2.ocsp.RevokedStatus;
import org.bouncycastle2.ocsp.SingleResp;
import org.bouncycastle2.ocsp.UnknownStatus;

import android.util.Log;

/**
* @author jgzornoza
* Licencia: http://bit.ly/j9jZQH
*/
public class ClienteOCSP {

    public enum EstadoCertificado {OK, REVOCADO, DESCONOCIDO}

    private static final String OCSP_DNIE_URL = "http://ocsp.dnie.es";

    public static void main(String [] args){
        Security.addProvider(new org.bouncycastle2.jce.provider.BouncyCastleProvider());
        try {
            X509Certificate caCert = readCert("/home/jj/temp/prueba/ca.pem");
            X509Certificate interCert = readCert("/home/jj/temp/prueba/inter.pem");
            X509Certificate clientCert = readCert("/home/jj/temp/prueba/jj.pem");
            Log.i("", "Estado certificado: " +
                validarCertificado(interCert, clientCert.getSerialNumber(),
                new Date(System.currentTimeMillis())).toString());
        } catch (Exception ex){
        	Log.e("", ex.getMessage(), ex);
        }
    }

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
	    
    private static X509Certificate readCert(String fileName) throws FileNotFoundException, CertificateException {
        InputStream is = new FileInputStream(fileName);
        BufferedInputStream bis = new BufferedInputStream(is);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(bis);
        return cert;
    }

}
