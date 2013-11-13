package org.votingsystem.simulation.callable;

import com.itextpdf.text.pdf.PdfReader;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;

import org.votingsystem.simulation.ContextService;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.simulation.callable.PDFSignedSender;
import org.apache.log4j.Logger;
import org.votingsystem.simulation.ApplicationContextHolder as ACH;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ManifestSigner implements Callable<ResponseVS> {
    
    private static Logger log = Logger.getLogger(ManifestSigner.class);

    private String nif;
    private String urlToSendDocument = null;
    private String reason = null;
    private String location = null;
    private PdfReader manifestToSign = null;
    private ResponseVS respuesta;
	private ContextService contextService = null;
	
        
    public ManifestSigner (String nif, String urlToSendDocument, 
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

        X509Certificate destinationCert = contextService.getAccessControl().getCertificate();
        PDFSignedSender worker = new PDFSignedSender(urlToSendDocument, 
				reason, location, null,  manifestToSign, privateKey, 
				signerCertChain, destinationCert);
        respuesta = worker.call();
        return respuesta;
    }
    
}