package org.votingsystem.simulation.callable

import com.itextpdf.text.pdf.PdfReader
import org.apache.log4j.Logger
import org.votingsystem.model.ResponseVS
import org.votingsystem.simulation.ContextService
import org.votingsystem.util.ApplicationContextHolder as ACH

import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.concurrent.Callable
/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ManifestSignedSender implements Callable<ResponseVS> {
    
    private static Logger logger = Logger.getLogger(ManifestSignedSender.class);

    private String nif;
    private String urlToSendDocument = null;
    private String reason = null;
    private String location = null;
    private PdfReader manifestToSign = null;
    private ResponseVS responseVS;
	private ContextService contextService = null;
	
        
    public ManifestSignedSender(String nif, String urlToSendDocument,
            PdfReader manifestToSign, String reason, String location) throws Exception {
        this.nif = nif;
        this.reason = reason;
        this.location = location;
        this.manifestToSign = manifestToSign;
        this.urlToSendDocument = urlToSendDocument;
		contextService = ACH.getSimulationContext();
    }
    
    @Override public ResponseVS call() throws Exception {
        KeyStore mockDnie = contextService.generateTestDNIe(nif);
        
        PrivateKey privateKey = (PrivateKey)mockDnie.getKey(
                contextService.END_ENTITY_ALIAS, contextService.PASSWORD.toCharArray());
        Certificate[] signerCertChain = mockDnie.getCertificateChain(
                contextService.END_ENTITY_ALIAS);

        X509Certificate destinationCert = contextService.getAccessControl().getX509Certificate();
        PDFSignedSender worker = new PDFSignedSender(urlToSendDocument, 
				reason, location, null,  manifestToSign, privateKey, 
				signerCertChain, destinationCert);
        responseVS = worker.call();
        return responseVS;
    }
    
}