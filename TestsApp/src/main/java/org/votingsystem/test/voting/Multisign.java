package org.votingsystem.test.voting;

import org.votingsystem.model.ResponseVS;
import org.votingsystem.signature.smime.SMIMEMessage;
import org.votingsystem.test.util.SignatureService;
import org.votingsystem.test.util.SimulationData;
import org.votingsystem.util.ContextVS;
import org.votingsystem.util.DateUtils;
import org.votingsystem.util.JSON;

import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.ExecutorCompletionService;
import java.util.logging.Logger;

public class Multisign {

    private static Logger log =  Logger.getLogger(Multisign.class.getName());

    private static ExecutorCompletionService completionService;

    public static void main(String[] args) throws Exception {
        new ContextVS(null, null).initTestEnvironment(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("TestsApp.properties"), "./TestDir");
        SimulationData simulationData = new SimulationData();
        simulationData.setServerURL("http://sistemavotacion.org/AccessControl");
        simulationData.setMaxPendingResponses(10);
        simulationData.setNumRequestsProjected(1);
        Map timerMap = new HashMap<>();
        timerMap.put("active", false);
        timerMap.put("time", "00:00:10");
        simulationData.setTimerMap(timerMap);
        Map dataToSignMap = new HashMap<>();
        dataToSignMap.put("UUID", UUID.randomUUID().toString());

        SignatureService signatureService = SignatureService.genUserVSSignatureService("08888888D");
        SignatureService signatureService1 = SignatureService.genUserVSSignatureService("00111222V");
        SignatureService signatureService2 = SignatureService.genUserVSSignatureService("03455543T");
        SMIMEMessage smimeMessage = signatureService.getSMIME("08888888D", "00111222V",
                JSON.getMapper().writeValueAsString(dataToSignMap), DateUtils.getDateStr(new Date()));

        SMIMEMessage smimeSigned = signatureService1.getSMIMEMultiSigned("03455543T", "08888888D", smimeMessage,
                DateUtils.getDateStr(new Date()));
        //log.info(new String(smimeSigned.getBytes()))
        X509Certificate x509Cert = signatureService1.getCertSigner();
        smimeSigned.isValidSignature();
        Collection result = smimeSigned.checkSignerCert(x509Cert);
        log.info("cert matches: " + result.size());
        simulationData.finishAndExit(ResponseVS.SC_OK, null);
    }
}

