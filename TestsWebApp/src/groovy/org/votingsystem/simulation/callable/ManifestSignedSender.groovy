package org.votingsystem.simulation.callable

import com.itextpdf.text.pdf.PdfReader
import org.apache.log4j.Logger
import org.votingsystem.callable.PDFSignedSender
import org.votingsystem.model.ContextVS
import org.votingsystem.model.ResponseVS
import org.votingsystem.simulation.SignatureVSService
import org.votingsystem.util.ApplicationContextHolder
import org.votingsystem.util.ApplicationContextHolder as ACH

import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.concurrent.Callable
/**
* @author jgzornoza
* Licencia: https://github.com/votingsystem/votingsystem/wiki/Licencia
*/
public class ManifestSignedSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(ManifestSignedSender.class);

    private String nif;
    private String urlToSendDocument = null;
    private String reason = null;
    private String location = null;
    private PdfReader manifestToSign = null;
    private ResponseVS responseVS;
        
    public ManifestSignedSender(String nif, String urlToSendDocument,
            PdfReader manifestToSign, String reason, String location) throws Exception {
        this.nif = nif;
        this.reason = reason;
        this.location = location;
        this.manifestToSign = manifestToSign;
        this.urlToSendDocument = urlToSendDocument;
    }
    
    @Override public ResponseVS call() throws Exception {
        SignatureVSService signatureVSService = (SignatureVSService)ApplicationContextHolder.getBean("signatureVSService")
        KeyStore mockDnie = signatureVSService.generateKeyStore(nif)
        PrivateKey privateKey =(PrivateKey)mockDnie.getKey(ContextVS.END_ENTITY_ALIAS,ContextVS.PASSWORD.toCharArray());
        Certificate[] signerCertChain = mockDnie.getCertificateChain(ContextVS.END_ENTITY_ALIAS);
        X509Certificate destinationCert = ContextVS.getInstance().getAccessControl().getX509Certificate();
        PDFSignedSender worker = new PDFSignedSender(urlToSendDocument,
                ContextVS.getInstance().getAccessControl().getTimeStampServiceURL(),
                reason,location,null,manifestToSign, privateKey, signerCertChain, destinationCert);
        responseVS = worker.call();
        return responseVS;
    }
    
}