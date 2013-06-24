package org.sistemavotacion.test.simulation.callable;

import com.itextpdf.text.pdf.PdfReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.Callable;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.callable.PDFSignedSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ManifestSigner implements Callable<Respuesta> {
    
    private static Logger logger = LoggerFactory.getLogger(ManifestSigner.class);

    private String nif;
    private String urlToSendDocument = null;
    private String reason = null;
    private String location = null;
    private PdfReader manifestToSign = null;
    private Respuesta respuesta;
        
    public ManifestSigner (String nif, String urlToSendDocument, 
            PdfReader manifestToSign, String reason, String location) throws Exception {
        this.nif = nif;
        this.reason = reason;
        this.location = location;
        this.manifestToSign = manifestToSign;
        this.urlToSendDocument = urlToSendDocument;
    }
    
    @Override
    public Respuesta call() throws Exception {
        KeyStore mockDnie = ContextoPruebas.INSTANCE.crearMockDNIe(nif);
        
        PrivateKey privateKey = (PrivateKey)mockDnie.getKey(
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS,
                ContextoPruebas.DEFAULTS.PASSWORD.toCharArray());
        Certificate[] signerCertChain = mockDnie.getCertificateChain(
                ContextoPruebas.DEFAULTS.END_ENTITY_ALIAS);

        X509Certificate destinationCert = Contexto.INSTANCE.
                    getAccessControl().getCertificate();
        PDFSignedSender worker = new PDFSignedSender(null,
                urlToSendDocument, reason, location, null,
                manifestToSign, privateKey, signerCertChain, 
                destinationCert);
        respuesta = worker.call();
        return respuesta;
    }
    
}