package org.votingsystem.test.misc;

import org.votingsystem.cms.CMSSignedMessage;
import org.votingsystem.dto.EncryptedContentDto;
import org.votingsystem.model.ResponseVS;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.test.util.TestUtils;
import org.votingsystem.util.*;
import org.votingsystem.util.crypto.Encryptor;
import org.votingsystem.util.crypto.PEMUtils;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PKCS7SendEncrypt {

    private static Logger log =  Logger.getLogger(PKCS7SendEncrypt.class.getName());

    private static SimulationData simulationData;
    private static ExecutorCompletionService completionService;

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        simulationData = new SimulationData();
        simulationData.setServerURL("https://192.168.1.5/CurrencyServer");
        simulationData.setMaxPendingResponses(20);
        simulationData.setNumRequestsProjected(500);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);
        TestUtils.fetchCurrencyServer();
        initSimulation();
    }
    private static void initSimulation(){
        log.info("initSimulation");
        if(!(simulationData.getNumRequestsProjected() > 0)) {
            log.info("WITHOUT NumberOfRequestsProjected");
            return;
        }
        log.info("initSimulation - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        ExecutorService executorService = Executors.newFixedThreadPool(100);
        completionService = new ExecutorCompletionService<ResponseVS>(executorService);
        executorService.execute(() -> {
            try {
                sendRequest();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        executorService.execute(() -> {
            try {
                waitForResponses();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void sendRequest() throws Exception {
        log.info("sendRequest - NumRequestsProjected: " + simulationData.getNumRequestsProjected());
        X509Certificate serverCert = ContextVS.getInstance().getDefaultServer().getX509Certificate();
        while(simulationData.getNumRequests() < simulationData.getNumRequestsProjected()) {
            if((simulationData.getNumRequests() - simulationData.
                    getNumRequestsCollected()) <= simulationData.getMaxPendingResponses()) {
                completionService.submit(() -> {
                    String requestNIF = NifUtils.getNif(simulationData.getAndIncrementNumRequests().intValue());
                    SignatureService signatureService = SignatureService.load(requestNIF);
                    EncryptedContentDto encryptedContentDto = new EncryptedContentDto();
                    encryptedContentDto.setPublicKeyPEM(new String(PEMUtils.getPEMEncoded(
                            signatureService.getCertSigner().getPublicKey())));
                    encryptedContentDto.setUUID(requestNIF);
                    CMSSignedMessage cmsSignedMessage = signatureService.signDataWithTimeStamp(JSON.getMapper().writeValueAsBytes(
                            encryptedContentDto));
                    byte[] encryptedRequestBytes = Encryptor.encryptToCMS(cmsSignedMessage.getEncoded(), serverCert);
                    ResponseVS responseVS = HttpHelper.getInstance().sendData(encryptedRequestBytes,
                            ContentType.JSON_ENCRYPTED, simulationData.getServerURL() + "/rest/testEncryptor/encrypted");
                    if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                        if(responseVS.getContentType() == ContentType.JSON_SIGNED_ENCRYPTED) {
                            byte[] decryptedData = Encryptor.decryptCMS(responseVS.getMessageBytes(),
                                    signatureService.getPrivateKey());
                            CMSSignedMessage result = new CMSSignedMessage(decryptedData);
                            result.isValidSignature();
                        } else if(responseVS.getContentType() == ContentType.JSON_SIGNED) {
                            CMSSignedMessage result = new CMSSignedMessage(responseVS.getMessageBytes());
                            result.isValidSignature();
                        } else log.info("ContentType: " + responseVS.getContentType() + " - result: " +
                                new String(responseVS.getMessageBytes()));
                    }
                    return responseVS;
                });
            } else Thread.sleep(300);
        }
    }


    private static void waitForResponses() throws Exception {
        log.info("waitForResponses - NumRequestsProjected: " +
                simulationData.getNumRequestsProjected());
        while (simulationData.getNumRequestsProjected() > simulationData.getNumRequestsCollected()) {
            try {
                Future<ResponseVS> f = completionService.take();
                ResponseVS responseVS = f.get();
                if (ResponseVS.SC_OK == responseVS.getStatusCode()) {
                    simulationData.getAndIncrementNumRequestsOK();
                } else simulationData.finishAndExit(ResponseVS.SC_ERROR, "ERROR: " + responseVS.getMessage());
            } catch(Exception ex) {
                log.log(Level.SEVERE, ex.getMessage(), ex);
                simulationData.finishAndExit(ResponseVS.SC_EXCEPTION, "EXCEPTION: " + ex.getMessage());
            }
        }
        simulationData.finishAndExit(ResponseVS.SC_OK, null);
    }
    
}



