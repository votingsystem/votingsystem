package org.sistemavotacion.test.simulation.launcher;

import com.itextpdf.text.pdf.PdfReader;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import org.sistemavotacion.Contexto;
import org.sistemavotacion.test.ContextoPruebas;
import org.sistemavotacion.modelo.Respuesta;
import org.sistemavotacion.worker.PDFSignedSenderWorker;
import org.sistemavotacion.worker.VotingSystemWorker;
import org.sistemavotacion.worker.VotingSystemWorkerListener;
import org.sistemavotacion.worker.VotingSystemWorkerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class SignatureManifestLauncher implements Callable<Respuesta>, 
        VotingSystemWorkerListener {
    
    private static Logger logger = LoggerFactory.getLogger(SignatureManifestLauncher.class);

    public enum Worker implements VotingSystemWorkerType{
        PDF_SIGNED_SENDER}
    
    
    private final CountDownLatch countDownLatch = new CountDownLatch(1); // just one time

    private String nif;
    private String urlToSendDocument = null;
    private String reason = null;
    private String location = null;
    private PdfReader manifestToSign = null;
    private Respuesta respuesta;
        
    public SignatureManifestLauncher (String nif, String urlToSendDocument, 
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
        new PDFSignedSenderWorker(Worker.PDF_SIGNED_SENDER,
                urlToSendDocument, reason, location, null,
                manifestToSign, privateKey, signerCertChain, 
                destinationCert, this).execute();
        
        countDownLatch.await();
        return getResult();
    }


    @Override public void processVotingSystemWorkerMsg(List<String> messages) {
        for(String message : messages)  {
            logger.debug("process -> " + message);
        }
    }

    @Override
    public void showResult(VotingSystemWorker worker) {
        logger.debug("showResult - statusCode: " + worker.getStatusCode() + 
        " - nif: " + nif + " - worker: " + worker);
        respuesta = new Respuesta(worker.getStatusCode(), " - from:" + nif + 
                " - " + worker.getMessage());
        countDownLatch.countDown();
    }


    private Respuesta getResult() {
        return respuesta;
    }
}